package com.teger.database;

import com.teger.TDatabase;
import com.teger.annotation.Column;
import com.teger.annotation.PrimaryKey;
import com.teger.exception.ClassValidationException;
import com.teger.exception.ConnectionException;
import com.teger.exception.NotRegisteredClassException;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.*;

public class PlayerDatabaseManager {

    private static final File RESOURCE_FOLDER_PATH = new File("plugins/TDatabase");
    private HashMap<Plugin, Connection> connections = new HashMap<>();
    private HashMap<Plugin, Class[]> registeredClasses = new HashMap<>();

    private Plugin plugin;

    public PlayerDatabaseManager(TDatabase plugin){
        this.plugin = plugin;
        if(!RESOURCE_FOLDER_PATH.exists()) RESOURCE_FOLDER_PATH.mkdirs();
        try{
            Class.forName("org.sqlite.JDBC");
        }catch(Exception e) {
            Bukkit.getLogger().warning("데이터베이스 드라이버를 찾을 수 없습니다. (" + e.getMessage() + ")");
        }
    }

    public void initializeDatabase(Plugin plugin, Class... tableClasses) throws ConnectionException, SQLException, NotRegisteredClassException {
        if(getConnection(plugin) != null)
            throw new ConnectionException(plugin.getName() + " 플러그인은 이미 연결되어있습니다.");
        //Class Validation Check & Create Connection
        for(Class c : tableClasses)
            try{
                classValidationCheck(c);
            } catch(ClassValidationException e){
                Bukkit.getLogger().warning(e.getMessage());
                return;
            }
        registeredClasses.put(plugin, tableClasses);
        createConnection(plugin);
        //Create Table
        createTables(plugin, tableClasses);
        //Create Not Exist Fields
        for(Class c : tableClasses) comparingFields(plugin, c);
    }

    public void closeDatabase(Plugin plugin) throws SQLException, ConnectionException {
        Connection connection = getConnection(plugin);
        if(connection == null) throw new ConnectionException(plugin + "플러그인은 연결되어 있지 않습니다.");
        connection.close();
        connections.remove(plugin);
        registeredClasses.remove(plugin);
    }

    public void closeAll() throws SQLException {
        for(Plugin plugin : connections.keySet()){
            getConnection(plugin).close();
            connections.remove(plugin);
            registeredClasses.remove(plugin);
        }
    }

    private void classValidationCheck(Class c) throws ClassValidationException {
        boolean hasPK = false;
        Field[] fields = c.getDeclaredFields();
        for(Field field : fields){
            try {
                c.getDeclaredMethod("get" + field.getName().substring(0,1).toUpperCase() + field.getName().substring(1));
            } catch (NoSuchMethodException e) {
                throw new ClassValidationException(field.getName() + " field not have getter.");
            }
            try{
                c.getDeclaredMethod("set" + field.getName().substring(0,1).toUpperCase() + field.getName().substring(1), field.getType());
            } catch(NoSuchMethodException e){
                throw new ClassValidationException(field.getName() + " field not have setter.");
            }
            if(!hasPK){
                PrimaryKey pk = field.getDeclaredAnnotation(PrimaryKey.class);
                if(pk != null) hasPK = true;
            }
        }

        if(!hasPK) throw new ClassValidationException("This table not have primary key.");
    }

    private void createConnection(Plugin plugin) throws SQLException {
        File file = new File("plugins/TDatabase/"+plugin.getName() + ".db");
        Connection connection = DriverManager.getConnection("jdbc:sqlite:"+file.getAbsolutePath());
        connections.put(plugin, connection);
    }

    private void createTables(Plugin plugin, Class[] classes) throws ConnectionException, SQLException {
        Connection connection = getConnection(plugin);
        if(connection == null) throw new ConnectionException(plugin + "플러그인은 연결되어 있지 않습니다.");
        for(Class c : classes){
            String query = "CREATE TABLE IF NOT EXISTS " + CaseConverter.camelToSnake(c.getSimpleName()) + "(";
            Field[] fields = c.getDeclaredFields();
            for(int i = 0; i < fields.length; i ++){
                Field field = fields[i];
                PrimaryKey pk = field.getAnnotation(PrimaryKey.class);
                Column col = field.getAnnotation(Column.class);

                query += CaseConverter.camelToSnake(field.getName())
                        + (pk != null ? " PRIMARY KEY" : (col != null && col.nullable() ? " NOT NULL" : ""));
                if(i != fields.length-1) query += ", ";
            }
            query += ")";
            Statement stm = connection.createStatement();
            stm.execute(query);
        }
    }

    private Map<String, String> getColumnTypes(Plugin plugin, Class c) throws ConnectionException, SQLException, NotRegisteredClassException {
        Connection connection = getConnection(plugin);
        if(connection == null) throw new ConnectionException(plugin + "플러그인은 연결되어 있지 않습니다.");
        if(!isRegisteredClass(plugin, c)) throw new NotRegisteredClassException(plugin + "플러그인의 " + c.getSimpleName() + " 클래스는 등록되지 않았습니다.");
        HashMap<String, String> result = new HashMap<>();
        String query = "pragma table_info("+CaseConverter.camelToSnake(c.getSimpleName())+")";
        Statement stm = connection.createStatement();
        ResultSet set = stm.executeQuery(query);
        while(set.next()) {
            result.put(set.getString("name"),set.getString("type"));
        }
        return result;
    }

    private void comparingFields(Plugin plugin, Class c) throws SQLException, ConnectionException, NotRegisteredClassException {
        Connection connection = getConnection(plugin);
        if(connection == null) throw new ConnectionException(plugin + "플러그인은 연결되어 있지 않습니다.");
        if(!isRegisteredClass(plugin, c)) throw new NotRegisteredClassException(plugin + "플러그인의 " + c.getSimpleName() + " 클래스는 등록되지 않았습니다.");
        Map<String, String> columns = getColumnTypes(plugin, c);
        Field[] fields = c.getDeclaredFields();
        for(Field field : fields){
            if(!columns.containsKey(CaseConverter.camelToSnake(field.getName()))){
                Statement stm = connection.createStatement();
                stm.execute("ALTER TABLE "+CaseConverter.camelToSnake(c.getSimpleName())+" ADD COLUMN " + CaseConverter.camelToSnake(field.getName()));
            }
        }
    }

    private String getPrimaryKey(Plugin plugin, Class c) throws SQLException, ConnectionException, NotRegisteredClassException {
        Connection connection = getConnection(plugin);
        if(connection == null) throw new ConnectionException(plugin + "플러그인은 연결되어 있지 않습니다.");
        if(!isRegisteredClass(plugin, c)) throw new NotRegisteredClassException(plugin.getName() + "플러그인의 " + c.getSimpleName() + " 클래스는 등록되지 않았습니다.");
        String query = "pragma table_info("+CaseConverter.camelToSnake(c.getSimpleName())+")";
        Statement stm = connection.createStatement();
        ResultSet set = stm.executeQuery(query);
        while(set.next()){
            if(set.getBoolean("pk")){
                return set.getString("name");
            }
        }
        return null;
    }

    private Connection getConnection(Plugin plugin){
        return connections.get(plugin);
    }

    public <T> T getInstance(Plugin plugin, String pk, Class<T> c) throws ConnectionException,
            SQLException,
            InvocationTargetException,
            InstantiationException,
            IllegalAccessException,
            NoSuchFieldException,
            NoSuchMethodException, NotRegisteredClassException {
        Connection connection = getConnection(plugin);
        if(connection == null) throw new ConnectionException(plugin + "플러그인은 연결되어 있지 않습니다.");
        Map<String, String> columns = getColumnTypes(plugin, c);
        Constructor<?> constructor = c.getDeclaredConstructors()[0];
        Object object = constructor.newInstance();
        String query = "SELECT * from " + CaseConverter.camelToSnake(c.getSimpleName()) + " WHERE " + getPrimaryKey(plugin, c) + " = \"" + pk + "\"";
        HashMap<String, Object> values = new HashMap<>();
        Statement statement = connection.createStatement();
        ResultSet set = statement.executeQuery(query);
        while(set.next()) for(String key : columns.keySet()) values.put(key, set.getObject(key));
        for(String column : columns.keySet()){
            String fieldName = CaseConverter.snakeToCamel(column);
            fieldName = fieldName.substring(0, 1).toLowerCase() + fieldName.substring(1);
            String methodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            Field field = c.getDeclaredField(fieldName);
            c.getMethod(methodName, field.getType()).invoke(object, values.get(column));
        }
        return c.cast(object);

    }

    public <T> T saveInstance(Plugin plugin, T instance) throws ConnectionException,
            NoSuchMethodException,
            InvocationTargetException,
            IllegalAccessException,
            SQLException {
        Connection connection = getConnection(plugin);
        if(connection == null) throw new ConnectionException(plugin + "플러그인은 연결되어 있지 않습니다.");
        Object obj = instance.getClass().cast(instance);
        Field[] fields = instance.getClass().getDeclaredFields();
        String query = "INSERT INTO " + CaseConverter.camelToSnake(instance.getClass().getSimpleName()) + " VALUES(";
        for(int i = 0; i < fields.length; i ++){
            Field field = fields[i];
            String fieldName = field.getName();
            Method method = instance.getClass().getDeclaredMethod("get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1));
            Object value = method.invoke(obj);
            if(value instanceof String || value instanceof Character){
                query += "\"" + value + "\"";
            } else query += value.toString();
            if(i != fields.length-1) query += ", ";
        }
        query += ")";
        Statement stm = connection.createStatement();
        stm.execute(query);
        return instance;
    }

    public List<String> getPrimaryKeys(Plugin plugin, Class c) throws ConnectionException, SQLException, NotRegisteredClassException {
        Connection connection = getConnection(plugin);
        if(connection == null) throw new ConnectionException(plugin + "플러그인은 연결되어 있지 않습니다.");
        if(!isRegisteredClass(plugin, c)) throw new NotRegisteredClassException(plugin.getName() + "플러그인의 " + c.getSimpleName() + " 클래스는 등록되지 않았습니다.");
        String columnName = getPrimaryKey(plugin, c);
        String query = "SELECT " + columnName + " FROM " + CaseConverter.camelToSnake(c.getSimpleName());
        List<String> result = new ArrayList<>();
        Statement stm = connection.createStatement();
        ResultSet set = stm.executeQuery(query);
        while(set.next())
            result.add(set.getString(0));
        return result;
    }

    public boolean isRegisteredClass(Plugin plugin, Class c) throws ConnectionException {
        Class[] classes = registeredClasses.get(plugin);
        if(classes == null) throw new ConnectionException(plugin + "플러그인은 연결되어 있지 않습니다.");
        return Arrays.stream(classes).filter(cl -> cl.getName().equals(c.getName())).findAny().isPresent();
    }
}
