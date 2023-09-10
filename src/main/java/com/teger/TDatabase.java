package com.teger;

import com.teger.database.PlayerDatabaseManager;
import com.teger.exception.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TDatabase extends JavaPlugin {

    private PlayerDatabaseManager playerDatabaseManager;
    private static TDatabase plugin;
    @Override
    public void onEnable() {
        plugin = this;
        playerDatabaseManager = new PlayerDatabaseManager(this);
    }

    @Override
    public void onDisable() {
        try { playerDatabaseManager.closeAll(); } catch (SQLException e) { e.printStackTrace(); }
    }

    public static TDatabase getInstance(){
        return plugin;
    }

    public void closeDatabase(Plugin plugin) throws CloseDatabaseException {
        try {
            playerDatabaseManager.closeDatabase(plugin);
        } catch (SQLException | ConnectionException e) {
            throw new CloseDatabaseException(e.getMessage());
        }
    }

    public void initializeDatabase(Plugin plugin, Class... classes) throws InitializeDatabaseException {
        try {
            playerDatabaseManager.initializeDatabase(plugin, classes);
        } catch (ConnectionException | SQLException | NotRegisteredClassException e) {
            throw new InitializeDatabaseException(e.getMessage());
        }
    }

    public <T> T getDataFromDatabase(Plugin plugin, String pk, Class<T> c) throws NoSuchInstanceInDatabaseException {
        try {
            return playerDatabaseManager.getInstance(plugin, pk, c);
        } catch (ConnectionException | SQLException | InvocationTargetException | InstantiationException |
                 IllegalAccessException | NoSuchFieldException | NoSuchMethodException | NotRegisteredClassException e) {
            throw new NoSuchInstanceInDatabaseException(e.getMessage());
        }
    }

    public <T> T saveData(Plugin plugin, T instance) throws DatabaseSaveException {
        try {
            return playerDatabaseManager.saveInstance(plugin, instance);
        } catch (ConnectionException | InvocationTargetException | IllegalAccessException | SQLException |
                 NoSuchMethodException e) {
            throw new DatabaseSaveException(e.getMessage());
        }
    }

    public String[] getPrimaryKeysFromDatabase(Plugin plugin, Class c) throws NoSuchInstanceInDatabaseException {
        try {
            return playerDatabaseManager.getPrimaryKeys(plugin, c).toArray(String[]::new);
        } catch (ConnectionException | SQLException | NotRegisteredClassException e) {
            throw new NoSuchInstanceInDatabaseException(e.getMessage());
        }
    }

    public <T> List<T> getAllInstance(Plugin plugin, Class<T> c) throws NoSuchInstanceInDatabaseException {
        List<T> instanceList = new ArrayList<>();
        for(String primaryKey : getPrimaryKeysFromDatabase(plugin, c)){
            instanceList.add(getDataFromDatabase(plugin, primaryKey, c));
        }
        return instanceList;
    }
}