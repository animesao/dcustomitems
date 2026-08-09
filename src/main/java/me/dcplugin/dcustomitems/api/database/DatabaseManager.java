package me.dcplugin.dcustomitems.api.database;

import me.dcplugin.dcustomitems.Main;

import java.io.File;
import java.sql.*;
import java.util.*;

/**
 * Менеджер базы данных (SQLite)
 * 
 * Пример использования:
 * <pre>
 * DatabaseManager db = new DatabaseManager(plugin);
 * db.connect();
 * 
 * // Создать таблицу
 * db.createTable("player_stats", 
 *     "uuid TEXT PRIMARY KEY, " +
 *     "kills INTEGER DEFAULT 0, " +
 *     "deaths INTEGER DEFAULT 0, " +
 *     "money DOUBLE DEFAULT 0"
 * );
 * 
 * // Вставить данные
 * db.execute("INSERT OR REPLACE INTO player_stats (uuid, kills) VALUES (?, ?)", 
 *     player.getUniqueId().toString(), 10);
 * 
 * // Получить данные
 * int kills = db.queryInt("SELECT kills FROM player_stats WHERE uuid = ?", 
 *     player.getUniqueId().toString());
 * 
 * // Получить все строки
 * List<Map<String, Object>> rows = db.queryAll("SELECT * FROM player_stats");
 * </pre>
 */
public class DatabaseManager {

    private final Main plugin;
    private Connection connection;
    private final String dbPath;

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
        this.dbPath = plugin.getDataFolder() + File.separator + "data.db";
    }

    // ===== ПОДКЛЮЧЕНИЕ =====

    /**
     * Подключиться к базе данных
     */
    public boolean connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            plugin.getLogger().info("База данных подключена: " + dbPath);
            return true;
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("SQLite драйвер не найден!");
            return false;
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка подключения к БД: " + e.getMessage());
            return false;
        }
    }

    /**
     * Отключиться от базы данных
     */
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("База данных отключена.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка отключения от БД: " + e.getMessage());
        }
    }

    /**
     * Проверить подключение
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Переподключиться
     */
    public void reconnect() {
        disconnect();
        connect();
    }

    // ===== СОЗДАНИЕ ТАБЛИЦ =====

    /**
     * Создать таблицу
     */
    public boolean createTable(String tableName, String columns) {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" + columns + ")";
        return execute(sql);
    }

    /**
     * Проверить существует ли таблица
     */
    public boolean tableExists(String tableName) {
        try {
            DatabaseMetaData meta = connection.getMetaData();
            ResultSet rs = meta.getTables(null, null, tableName, new String[]{"TABLE"});
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    // ===== ВЫПОЛНЕНИЕ ЗАПРОСОВ =====

    /**
     * Выполнить SQL запрос (без возврата)
     */
    public boolean execute(String sql, Object... params) {
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            setParams(stmt, params);
            stmt.execute();
            stmt.close();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка SQL: " + e.getMessage());
            return false;
        }
    }

    /**
     * Вставить данные
     */
    public boolean insert(String table, Map<String, Object> data) {
        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        List<Object> values = new ArrayList<>();

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (columns.length() > 0) {
                columns.append(", ");
                placeholders.append(", ");
            }
            columns.append(entry.getKey());
            placeholders.append("?");
            values.add(entry.getValue());
        }

        String sql = "INSERT INTO " + table + " (" + columns + ") VALUES (" + placeholders + ")";
        return execute(sql, values.toArray());
    }

    /**
     * Обновить данные
     */
    public boolean update(String table, Map<String, Object> data, String where, Object... whereParams) {
        StringBuilder sets = new StringBuilder();
        List<Object> values = new ArrayList<>();

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (sets.length() > 0) sets.append(", ");
            sets.append(entry.getKey()).append(" = ?");
            values.add(entry.getValue());
        }

        // Добавляем where параметры
        Collections.addAll(values, whereParams);

        String sql = "UPDATE " + table + " SET " + sets + " WHERE " + where;
        return execute(sql, values.toArray());
    }

    /**
     * Удалить данные
     */
    public boolean delete(String table, String where, Object... params) {
        String sql = "DELETE FROM " + table + " WHERE " + where;
        return execute(sql, params);
    }

    // ===== ПОЛУЧЕНИЕ ДАННЫХ =====

    /**
     * Получить одно значение (int)
     */
    public int queryInt(String sql, Object... params) {
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            setParams(stmt, params);
            ResultSet rs = stmt.executeQuery();
            int result = rs.next() ? rs.getInt(1) : 0;
            rs.close();
            stmt.close();
            return result;
        } catch (SQLException e) {
            return 0;
        }
    }

    /**
     * Получить одно значение (String)
     */
    public String queryString(String sql, Object... params) {
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            setParams(stmt, params);
            ResultSet rs = stmt.executeQuery();
            String result = rs.next() ? rs.getString(1) : null;
            rs.close();
            stmt.close();
            return result;
        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * Получить одно значение (double)
     */
    public double queryDouble(String sql, Object... params) {
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            setParams(stmt, params);
            ResultSet rs = stmt.executeQuery();
            double result = rs.next() ? rs.getDouble(1) : 0.0;
            rs.close();
            stmt.close();
            return result;
        } catch (SQLException e) {
            return 0.0;
        }
    }

    /**
     * Получить одно значение (boolean)
     */
    public boolean queryBoolean(String sql, Object... params) {
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            setParams(stmt, params);
            ResultSet rs = stmt.executeQuery();
            boolean result = rs.next() && rs.getBoolean(1);
            rs.close();
            stmt.close();
            return result;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Получить все строки
     */
    public List<Map<String, Object>> queryAll(String sql, Object... params) {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            setParams(stmt, params);
            ResultSet rs = stmt.executeQuery();
            
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(meta.getColumnName(i), rs.getObject(i));
                }
                results.add(row);
            }
            
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка запроса: " + e.getMessage());
        }
        return results;
    }

    /**
     * Получить первую строку
     */
    public Map<String, Object> queryOne(String sql, Object... params) {
        List<Map<String, Object>> results = queryAll(sql, params);
        return results.isEmpty() ? null : results.get(0);
    }

    // ===== СЧЁТЧИКИ =====

    /**
     * Увеличить значение на 1
     */
    public boolean increment(String table, String column, String where, Object... whereParams) {
        String sql = "UPDATE " + table + " SET " + column + " = " + column + " + 1 WHERE " + where;
        return execute(sql, whereParams);
    }

    /**
     * Увеличить значение на N
     */
    public boolean add(String table, String column, double amount, String where, Object... whereParams) {
        String sql = "UPDATE " + table + " SET " + column + " = " + column + " + ? WHERE " + where;
        List<Object> params = new ArrayList<>();
        params.add(amount);
        Collections.addAll(params, whereParams);
        return execute(sql, params.toArray());
    }

    // ===== УТИЛИТЫ =====

    private void setParams(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }

    /**
     * Получить соединение (для расширенного использования)
     */
    public Connection getConnection() {
        return connection;
    }
}
