package com.example.backend.db;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class EmbeddedDbInitializer {
    public static void init() {
        System.out.println("EmbeddedDbInitializer: running schema.sql...");
        try (Connection conn = DBConnection.getConnection()) {
            InputStream is = EmbeddedDbInitializer.class.getClassLoader().getResourceAsStream("db/schema.sql");
            if (is == null) {
                System.out.println("EmbeddedDbInitializer: schema.sql not found in classpath");
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
                // If schema already present, skip applying schema.sql to avoid duplicate-create and column mismatches
                boolean schemaPresent = false;
                try (java.sql.ResultSet rsCheck = st.executeQuery("SELECT COUNT(*) AS c FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='EMPRESA'")){
                    if (rsCheck.next() && rsCheck.getInt("c")>0) schemaPresent = true;
                } catch(Exception ex) { /* ignore */ }
                if (!schemaPresent) {
                    for (String s : stmts) {
                        String sql = s.trim();
                        if (sql.isEmpty()) continue;
                        try { st.execute(sql); } catch (Exception e) { System.out.println("EmbeddedDbInitializer: stmt failed: " + e.getMessage()); }
                    }
                } else {
                    System.out.println("EmbeddedDbInitializer: schema already exists, skipping schema.sql");
                }
                // ensure Usuario.empresa_id exists (some older schemas lacked it)
                try (ResultSet rs = st.executeQuery("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='USUARIO' AND COLUMN_NAME='EMPRESA_ID'")){
                    if (!rs.next()){
                        try { st.execute("ALTER TABLE Usuario ADD COLUMN empresa_id INT NULL"); System.out.println("EmbeddedDbInitializer: added column empresa_id to Usuario"); } catch(Exception ex) { System.out.println("EmbeddedDbInitializer: could not add empresa_id: " + ex.getMessage()); }
                    }
                } catch(Exception ex){ /* ignore */ }
            }
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) AS c FROM Usuario")){
                if (rs.next()){
                    int c = rs.getInt("c");
                    System.out.println("EmbeddedDbInitializer: Usuario rows="+c);
                    if (c == 0) {
                        System.out.println("EmbeddedDbInitializer: no usuarios found, seeding default admin and cartera...");
                        try {
                            // ensure Empresa 'Empresa Demo' exists
                            st.executeUpdate("INSERT INTO Empresa (nombre, correo, telefono, estado) SELECT 'Empresa Demo','demo@empresa.local','+000','ACTIVA' WHERE NOT EXISTS (SELECT 1 FROM Empresa WHERE nombre='Empresa Demo')");
                        } catch(Exception ex) { System.out.println("EmbeddedDbInitializer: insert empresa failed: " + ex.getMessage()); }
                        try {
                            // insert admin user (include estado to satisfy schemas that require it)
                            st.executeUpdate("INSERT INTO Usuario (correo,password,role,estado,empresa_id,nickname) SELECT 'admin@example.com','pass','ADMIN','ACTIVA',(SELECT id FROM Empresa WHERE nombre='Empresa Demo'),'admin' WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE correo='admin@example.com')");
                        } catch(Exception ex) { System.out.println("EmbeddedDbInitializer: insert admin failed: " + ex.getMessage()); }
                        try {
                            // ensure cartera for admin
                            st.executeUpdate("INSERT INTO Cartera (usuario_id, saldo) SELECT u.id, 100.00 FROM Usuario u WHERE u.correo='admin@example.com' AND NOT EXISTS (SELECT 1 FROM Cartera c WHERE c.usuario_id = u.id)");
                        } catch(Exception ex) { System.out.println("EmbeddedDbInitializer: insert cartera failed: " + ex.getMessage()); }
                    }
                }
            } catch(Exception ex){ /* ignore */ }
        } catch (Exception e) {
            System.out.println("EmbeddedDbInitializer: could not initialize DB: " + e.getMessage());
        }
    }
}
