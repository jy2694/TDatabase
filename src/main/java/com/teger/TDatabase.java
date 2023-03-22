package com.teger;

import com.teger.database.PlayerDatabaseManager;
import com.teger.exception.ConnectionException;
import com.teger.exception.NotRegisteredClassException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
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

    public void closeDatabase(Plugin plugin) throws SQLException, ConnectionException {
        playerDatabaseManager.closeDatabase(plugin);
    }

    public void initializeDatabase(Plugin plugin, Class... classes) throws SQLException, ConnectionException, NotRegisteredClassException {
        playerDatabaseManager.initializeDatabase(plugin, classes);
    }

    public <T> T getDataFromDatabase(Plugin plugin, String pk, Class<T> c) throws SQLException, NoSuchFieldException, ConnectionException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException, NotRegisteredClassException {
        return playerDatabaseManager.getInstance(plugin, pk, c);
    }

    public <T> T saveData(Plugin plugin, T instance) throws SQLException, ConnectionException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        return playerDatabaseManager.saveInstance(plugin, instance);
    }
}