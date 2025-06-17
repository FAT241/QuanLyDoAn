package org.projectmanagement.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/projectmagement?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";   // sửa theo user của bạn
    private static final String PASSWORD = "";   // sửa password

    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        }
        return connection;
    }
}
