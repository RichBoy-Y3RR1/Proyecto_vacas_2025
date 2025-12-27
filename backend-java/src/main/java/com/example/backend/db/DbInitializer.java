package com.example.backend.db;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Scanner;

@WebListener
public class DbInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("DbInitializer: initializing DB schema if necessary...");
        initEmbedded();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // nothing
    }

    // add helper to initialize DB when running the embedded HTTP server
    public static void initEmbedded() {
        System.out.println("DbInitializer.initEmbedded: running schema.sql...");
        try (Connection conn = DBConnection.getConnection()) {
            InputStream is = DbInitializer.class.getClassLoader().getResourceAsStream("db/schema.sql");
            if (is == null) {
                System.out.println("DbInitializer.initEmbedded: schema.sql not found in classpath");
                return;
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            String[] stmts = sb.toString().split(";\\s*\\n");
            try (Statement st = conn.createStatement()) {
                for (String s : stmts) {
                    String sql = s.trim();
                    if (sql.isEmpty()) continue;
                    try { st.execute(sql); } catch (Exception e) { System.out.println("DbInitializer.initEmbedded: stmt failed: " + e.getMessage()); }
                }
            }
            // report count of users
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) AS c FROM Usuario")){
                if (rs.next()){
                    int c = rs.getInt("c");
                    System.out.println("DbInitializer.initEmbedded: Usuario rows="+c);
                }
            } catch(Exception ex){ /* ignore */ }
        } catch (Exception e) {
            System.out.println("DbInitializer.initEmbedded: could not initialize DB: " + e.getMessage());
        }
    }
}
