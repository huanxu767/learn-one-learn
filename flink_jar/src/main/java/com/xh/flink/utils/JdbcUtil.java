package com.xh.flink.utils;

import com.xh.flink.config.GlobalConfig;
import com.xh.flink.pojo.DbConfig;

import java.sql.*;



public class JdbcUtil {



    public static Connection getConnection(DbConfig dbConfig,String driveClass) {

        try {
            Class.forName(driveClass);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            Connection connection = DriverManager.getConnection(dbConfig.getUrl(), dbConfig.getUserName(), dbConfig.getPassword());
            return connection;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static Connection getImpalaConnection(String impalaConnectionUrl) {
        try {
            Class.forName(GlobalConfig.IMPALA_DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            Connection connection = DriverManager.getConnection(impalaConnectionUrl);
            return connection;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static Connection getConnection(DbConfig dbConfig) {
        return getConnection(dbConfig,GlobalConfig.MYSQL_DRIVER_CLASS);
    }

    public static void close(Statement statement, Connection connection) {
        close(null,statement,connection);
    }
    public static void close(ResultSet resultSet, Statement statement, Connection connection) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }
}
