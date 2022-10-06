/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database;

import constants.ServerConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Fate
 */
public class ManagerDatabasePool {

    private static final List<ManagerDatabasePool.ConWrapper> connections = new ArrayList<>();
    private static boolean propsInited = false;
    private static long connectionTimeOut = 2 * 60 * 1000; // 2 minutes
    private static final int max = 20;

    public static final int CLOSE_CURRENT_RESULT = 1;
    public static final int KEEP_CURRENT_RESULT = 2;
    public static final int CLOSE_ALL_RESULTS = 3;
    public static final int SUCCESS_NO_INFO = -2;
    public static final int EXECUTE_FAILED = -3;
    public static final int RETURN_GENERATED_KEYS = 1;
    public static final int NO_GENERATED_KEYS = 2;

    public static synchronized Connection getConnection() throws SQLException {

        // 如果池中沒有Connection就建立新的
        if (connections.isEmpty()) {
            Connection retCon = connectToDB();
            ManagerDatabasePool.ConWrapper ret = new ManagerDatabasePool.ConWrapper(retCon);
            return ret.getConnection();
        } else {
            // 取得最後一個Connection
            int lastIndex = connections.size() - 1;
            Connection retCon = connections.remove(lastIndex).getConnection();

            // 如果從池中取出的Connection是Null就建立新的
            if (retCon == null) {
                ManagerDatabasePool.ConWrapper ret = new ManagerDatabasePool.ConWrapper(retCon);
                return ret.getConnection();
            }
            return retCon;
        }
    }

    public static synchronized void closeConnection(Connection conn) throws SQLException {
        if (connections.size() == max) {
            conn.close();
        } else {
            // 回收連線到池中
            ManagerDatabasePool.ConWrapper ret = new ManagerDatabasePool.ConWrapper(conn);
            connections.add(ret);
        }
    }

    private static long getWaitTimeout(Connection con) {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery("SHOW VARIABLES LIKE 'wait_timeout'");
            if (rs.next()) {
                return Math.max(1000, rs.getInt(2) * 1000 - 1000);
            } else {
                return -1;
            }
        } catch (SQLException ex) {
            return -1;
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                } finally {
                    if (rs != null) {
                        try {
                            rs.close();
                        } catch (SQLException ex1) {
                        }
                    }
                }
            }
        }
    }

    private static Connection connectToDB() throws SQLException {
        try {
            Class.forName("com.mysql.jdbc.Driver"); // touch the MySQL driver
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }

        Connection con = DriverManager.getConnection(
                "jdbc:mysql://" + ServerConfig.SQL_IP + ":" + ServerConfig.SQL_PORT + "/" + ServerConfig.SQL_DATABASE
                + "?autoReconnect=true&&maxReconnects=999&characterEncoding=UTF8",
                ServerConfig.SQL_USER, ServerConfig.SQL_PASSWORD);
        if (!propsInited) {
            long timeout = getWaitTimeout(con);
            if (timeout == -1) {
            } else {
                connectionTimeOut = timeout;
            }
            propsInited = true;
        }
        return con;
    }

    private static class ConWrapper {

        private long lastAccessTime = 0;
        private Connection connection;
        private int id;
        private boolean isUsed = false;

        public ConWrapper(Connection con) {
            this.connection = con;
        }

        public Connection getConnection() throws SQLException {
            if (expiredConnection()) {
                try { // Assume that the connection is stale
                    connection.close();
                } catch (Throwable err) {
                    // Who cares
                }
                this.connection = connectToDB();
            }

            lastAccessTime = System.currentTimeMillis(); // Record Access
            return this.connection;
        }

        /**
         * Returns whether this connection has expired
         *
         * @return
         */
        public boolean expiredConnection() {
            try {
                if (lastAccessTime == 0 && !connection.isClosed()) {
                    return false;
                }
                return System.currentTimeMillis() - lastAccessTime >= connectionTimeOut || connection.isClosed();
            } catch (Throwable ex) {
                return true;
            }
        }
    }

    public static void closeAll() throws SQLException {
        for (ManagerDatabasePool.ConWrapper con : connections) {
            con.connection.close();
        }
        connections.clear();
    }
}
