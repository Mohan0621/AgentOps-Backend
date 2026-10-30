package com.agentops.DB;

import java.sql.Connection;
import java.sql.DriverManager;

public class Poolmanager {
    String url = "jdbc:mysql://localhost:3306/movierec_db";
    String host = "root";
    String pass = "12345678";

    public Connection getCon() {
        Connection con = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(url, host, pass);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return con;
    }
}
