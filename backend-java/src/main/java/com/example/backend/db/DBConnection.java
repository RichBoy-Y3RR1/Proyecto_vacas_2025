package com.example.backend.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

public class DBConnection {
      
        private static final String URL = System.getenv().getOrDefault("DB_URL",
            "jdbc:mysql://localhost:3306/tienda?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC");
        private static final String USER = System.getenv().getOrDefault("DB_USER","root");
        private static final String PASS = System.getenv().getOrDefault("DB_PASS","tu_password");

    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (SQLException e) {
            System.err.println("MySQL connection failed: " + e.getMessage());
            System.err.println("Falling back to in-memory H2 database for development.");
            String h2Url = "jdbc:h2:mem:tienda;DB_CLOSE_DELAY=-1";
            Connection h2 = DriverManager.getConnection(h2Url, "sa", "");
            ensureH2Schema(h2);
            return h2;
        }
    }

    private static void ensureH2Schema(Connection conn) {
        try (Statement st = conn.createStatement()){
            // crear Empresa
            st.execute("CREATE TABLE IF NOT EXISTS Empresa (id INT AUTO_INCREMENT PRIMARY KEY, nombre VARCHAR(255), correo VARCHAR(255), telefono VARCHAR(50), estado VARCHAR(50))");
            // crear Videojuego
            st.execute("CREATE TABLE IF NOT EXISTS Videojuego (id INT AUTO_INCREMENT PRIMARY KEY, nombre VARCHAR(255), descripcion TEXT, empresa_id INT, precio DECIMAL(10,2), estado VARCHAR(50), fecha_lanzamiento DATE, edad_clasificacion VARCHAR(20))");
            // insertar empresa de ejemplo si no hay ninguna
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM Empresa");
            if (rs.next() && rs.getInt(1) == 0){
                st.execute("INSERT INTO Empresa (nombre, correo, telefono, estado) VALUES ('ACME Games','acme@example.com',NULL,'ACTIVA')");
            }
        } catch (Exception ex){
            System.err.println("Failed to initialize H2 schema: " + ex.getMessage());
        }
    }
}
