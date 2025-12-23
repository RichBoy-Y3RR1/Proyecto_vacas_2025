package com.example.backend.db;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Scanner;

public class DbInitializer {
    public static void main(String[] args) throws Exception {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String pass = System.getenv("DB_PASS");

        if (url == null || url.isBlank()) {
            url = "jdbc:mysql://127.0.0.1:3306?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC&allowMultiQueries=true";
        } else if (!url.contains("allowMultiQueries=")) {
            if (url.contains("?")) url += "&allowMultiQueries=true"; else url += "?allowMultiQueries=true";
        }
        if (user == null) user = "root";
        if (pass == null) pass = "";

        System.out.println("Connecting to MySQL: " + url + " user=" + user);

        String sql;
        try (InputStream is = DbInitializer.class.getResourceAsStream("/init_mysql.sql")) {
            if (is == null) throw new IllegalStateException("init_mysql.sql not found in resources");
            try (Scanner s = new Scanner(is, StandardCharsets.UTF_8)) {
                s.useDelimiter("\\A");
                sql = s.hasNext() ? s.next() : "";
            }
        }

        // Normalize whitespace and ensure the script ends with semicolon
        sql = sql.replaceAll("\r\n", "\n");

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement st = conn.createStatement()) {
            conn.setAutoCommit(true);
            System.out.println("Executing SQL script (may take a few seconds)...");
            boolean ok = st.execute(sql);
            System.out.println("Script executed. Statement result flag: " + ok);
        }

        System.out.println("DB init complete.");
    }
}
