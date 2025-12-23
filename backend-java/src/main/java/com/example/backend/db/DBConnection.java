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
            String h2Url = "jdbc:h2:mem:tienda;DB_CLOSE_DELAY=-1;MODE=MySQL;DB_CLOSE_ON_EXIT=FALSE";
            Connection h2 = null;
            SQLException lastEx = null;
            // Try using the same USER/PASS environment values first (useful when tests set DB_USER/DB_PASS)
            try {
                System.err.println("H2 attempt: url=" + h2Url + " user=" + USER + " (env)");
                h2 = DriverManager.getConnection(h2Url, USER, PASS);
            } catch (SQLException ex) {
                lastEx = ex;
                System.err.println("H2 attempt failed with env creds: " + ex.getMessage());
                // try lowercase common H2 default user
                try {
                    System.err.println("H2 attempt: url=" + h2Url + " user=sa (default)");
                    h2 = DriverManager.getConnection(h2Url, "sa", "");
                } catch (SQLException ex2) {
                    lastEx = ex2;
                    System.err.println("H2 attempt failed with default sa: " + ex2.getMessage());
                    // final fallback: shorter URL without extra params
                    System.err.println("H2 attempt: url=jdbc:h2:mem:tienda;DB_CLOSE_DELAY=-1 user=sa");
                    h2 = DriverManager.getConnection("jdbc:h2:mem:tienda;DB_CLOSE_DELAY=-1", "sa", "");
                }
            }
            ensureH2Schema(h2);
            return h2;
        }
    }

    private static void ensureH2Schema(Connection conn) {
        try (Statement st = conn.createStatement()){
            // Create full schema and seed data to match the provided MySQL schema for local testing
            st.execute("DROP TABLE IF EXISTS Videojuego_Categoria");
            st.execute("DROP TABLE IF EXISTS Compra");
            st.execute("DROP TABLE IF EXISTS Comentario");
            st.execute("DROP TABLE IF EXISTS Cartera");
            st.execute("DROP TABLE IF EXISTS Grupo_Usuario");
            st.execute("DROP TABLE IF EXISTS Grupo_Familiar");
            st.execute("DROP TABLE IF EXISTS Comision_Empresa");
            st.execute("DROP TABLE IF EXISTS Comision_Global");
            st.execute("DROP TABLE IF EXISTS Videojuego");
            st.execute("DROP TABLE IF EXISTS Categoria");
            st.execute("DROP TABLE IF EXISTS Usuario");
            st.execute("DROP TABLE IF EXISTS Empresa");

            st.execute("CREATE TABLE Categoria (id_categoria INT AUTO_INCREMENT PRIMARY KEY, nombre VARCHAR(100) NOT NULL UNIQUE, descripcion TEXT, fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE Empresa (id INT AUTO_INCREMENT PRIMARY KEY, nombre VARCHAR(150) NOT NULL, correo VARCHAR(150) UNIQUE, telefono VARCHAR(50), estado VARCHAR(20) NOT NULL)");
            st.execute("CREATE TABLE Usuario (id INT AUTO_INCREMENT PRIMARY KEY, correo VARCHAR(150) NOT NULL UNIQUE, password VARCHAR(255) NOT NULL, role VARCHAR(20) NOT NULL, estado VARCHAR(20) NOT NULL, nickname VARCHAR(100), fecha_nacimiento DATE, telefono VARCHAR(50), pais VARCHAR(100), created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE Videojuego (id INT AUTO_INCREMENT PRIMARY KEY, nombre VARCHAR(150) NOT NULL, descripcion TEXT, empresa_id INT NOT NULL, precio DECIMAL(10,2) DEFAULT 0, estado VARCHAR(30) DEFAULT 'REGISTRADO', fecha_lanzamiento DATE, edad_clasificacion VARCHAR(10), FOREIGN KEY (empresa_id) REFERENCES Empresa(id) ON DELETE CASCADE)");
            st.execute("CREATE TABLE Videojuego_Categoria (videojuego_id INT NOT NULL, categoria_id INT NOT NULL, PRIMARY KEY (videojuego_id, categoria_id), FOREIGN KEY (videojuego_id) REFERENCES Videojuego(id) ON DELETE CASCADE, FOREIGN KEY (categoria_id) REFERENCES Categoria(id_categoria) ON DELETE CASCADE)");
            st.execute("CREATE TABLE Compra (id INT AUTO_INCREMENT PRIMARY KEY, usuario_id INT NOT NULL, videojuego_id INT NOT NULL, fecha DATETIME DEFAULT CURRENT_TIMESTAMP, total DECIMAL(10,2), platform_commission DECIMAL(12,4), company_amount DECIMAL(12,4), FOREIGN KEY (usuario_id) REFERENCES Usuario(id) ON DELETE CASCADE, FOREIGN KEY (videojuego_id) REFERENCES Videojuego(id) ON DELETE CASCADE)");
            st.execute("CREATE TABLE Comentario (id INT AUTO_INCREMENT PRIMARY KEY, usuario_id INT NOT NULL, videojuego_id INT NOT NULL, texto TEXT, puntuacion INT, fecha DATETIME DEFAULT CURRENT_TIMESTAMP, visible BOOLEAN DEFAULT TRUE, FOREIGN KEY (usuario_id) REFERENCES Usuario(id) ON DELETE CASCADE, FOREIGN KEY (videojuego_id) REFERENCES Videojuego(id) ON DELETE CASCADE)");
            st.execute("CREATE TABLE Cartera (id INT AUTO_INCREMENT PRIMARY KEY, usuario_id INT NOT NULL, saldo DECIMAL(12,2) DEFAULT 0, FOREIGN KEY (usuario_id) REFERENCES Usuario(id) ON DELETE CASCADE)");
            st.execute("CREATE TABLE Grupo_Familiar (id INT AUTO_INCREMENT PRIMARY KEY, nombre VARCHAR(150), owner_id INT NOT NULL, created_at DATETIME DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (owner_id) REFERENCES Usuario(id) ON DELETE CASCADE)");
            st.execute("CREATE TABLE Grupo_Usuario (grupo_id INT NOT NULL, usuario_id INT NOT NULL, PRIMARY KEY (grupo_id, usuario_id), FOREIGN KEY (grupo_id) REFERENCES Grupo_Familiar(id) ON DELETE CASCADE, FOREIGN KEY (usuario_id) REFERENCES Usuario(id) ON DELETE CASCADE)");
            st.execute("CREATE TABLE Comision_Global (id INT PRIMARY KEY, percent DECIMAL(5,2) NOT NULL, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE Comision_Empresa (empresa_id INT PRIMARY KEY, percent DECIMAL(5,2) NOT NULL, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (empresa_id) REFERENCES Empresa(id))");

            // Seed initial data
            st.execute("INSERT INTO Empresa (id, nombre, correo, telefono, estado) VALUES (1, 'Acme Games', 'contact@acmegames.com', '+123456', 'ACTIVA'), (2, 'PixelSoft', 'hello@pixelsoft.com', '+987654', 'ACTIVA')");
            st.execute("INSERT INTO Usuario (id, correo, password, role, estado, nickname, fecha_nacimiento) VALUES (1, 'admin@tienda.com', 'admin123', 'ADMIN', 'ACTIVA', 'admin', '1990-01-01'), (2, 'empresa@acme.com', 'empresa123', 'EMPRESA', 'ACTIVA', 'acme_user', '1985-05-05'), (3, 'user@cliente.com', 'user123', 'USUARIO', 'ACTIVA', 'gamer123', '2000-06-01')");
            st.execute("INSERT INTO Cartera (usuario_id, saldo) VALUES (1, 0.00), (2, 0.00), (3, 100.00)");
            st.execute("INSERT INTO Categoria (id_categoria, nombre, descripcion) VALUES (1, 'Acción', 'Juegos de acción'), (2, 'Aventura', 'Juegos de aventura'), (3, 'Indie', 'Juegos independientes')");
            st.execute("INSERT INTO Videojuego (id, nombre, descripcion, empresa_id, precio, estado, fecha_lanzamiento, edad_clasificacion) VALUES (1, 'Space Shooter', 'Shooter espacial', 1, 9.99, 'PUBLICADO', '2022-01-01', 'E'), (2, 'Island Adventure', 'Aventura en isla', 2, 14.99, 'PUBLICADO', '2023-06-15', 'T')");
            st.execute("INSERT INTO Videojuego_Categoria (videojuego_id, categoria_id) VALUES (1,1),(2,2)");
            st.execute("INSERT INTO Compra (id, usuario_id, videojuego_id, fecha, total, platform_commission, company_amount) VALUES (1, 3, 1, CURRENT_TIMESTAMP(), 9.99, 1.4985, 8.4915)");
            st.execute("INSERT INTO Comentario (id, usuario_id, videojuego_id, texto, puntuacion, fecha, visible) VALUES (1, 3, 1, 'Excelente juego', 5, CURRENT_TIMESTAMP(), TRUE)");
            st.execute("INSERT INTO Banner (id, url_imagen, fecha_inicio, fecha_fin) VALUES (1, '/assets/banners/banner1.jpg', CURRENT_TIMESTAMP(), DATEADD('DAY', 30, CURRENT_TIMESTAMP()))");
            st.execute("INSERT INTO Comision_Global (id, percent) VALUES (1, 15.00)");
            st.execute("INSERT INTO Comision_Empresa (empresa_id, percent) VALUES (1, 12.50)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_videojuego_nombre ON Videojuego(nombre)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_compra_fecha ON Compra(fecha)");
        } catch (Exception ex){
            System.err.println("Failed to initialize H2 schema: " + ex.getMessage());
        }
    }
}
