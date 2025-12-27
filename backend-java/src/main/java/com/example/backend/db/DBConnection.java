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
        // Try configured DB (MySQL) first. If it fails or env is not set, fallback to an embedded H2 DB
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASS);
            try { ensureSeedUsers(conn); } catch(Exception ex){ System.err.println("Failed to ensure seed users: " + ex.getMessage()); }
            return conn;
        } catch (Exception e) {
            System.err.println("MySQL connection failed, falling back to embedded H2: " + e.getMessage());
            try {
                // H2 in file mode inside project folder so data persists between runs
                String h2Url = System.getenv().getOrDefault("H2_URL", "jdbc:h2:./data/tienda;MODE=MySQL;AUTO_SERVER=TRUE;LOCK_MODE=3");
                String h2User = System.getenv().getOrDefault("H2_USER", "sa");
                String h2Pass = System.getenv().getOrDefault("H2_PASS", "");
                Connection h2 = DriverManager.getConnection(h2Url, h2User, h2Pass);
                ensureH2Schema(h2);
                try { ensureSeedUsers(h2); } catch(Exception ex){ System.err.println("Failed to ensure seed users on H2: " + ex.getMessage()); }
                return h2;
            } catch (Exception ex2) {
                throw new SQLException("Unable to obtain database connection (MySQL and H2 failed): " + ex2.getMessage(), ex2);
            }
        }
    }

    private static void ensureSeedUsers(Connection conn) {
        try (Statement st = conn.createStatement()){
            // check if Usuario table exists by trying a simple count
            try (ResultSet rs = st.executeQuery("SELECT 1 FROM Usuario LIMIT 1")){
                // table exists; continue
            } catch (Exception ex){
                // table missing; nothing to seed here (schema not present)
                return;
            }
            // Insert default users if they do not exist
            String[] emails = {"admin@tienda.com","empresa@acme.com","user@cliente.com"};
            String[] inserts = new String[]{
                "INSERT INTO Usuario (correo, password, role, estado, nickname, fecha_nacimiento) SELECT 'admin@tienda.com','admin123','ADMIN','ACTIVA','admin','1990-01-01' WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE correo='admin@tienda.com')",
                "INSERT INTO Usuario (correo, password, role, estado, nickname, fecha_nacimiento) SELECT 'empresa@acme.com','empresa123','EMPRESA','ACTIVA','acme_user','1985-05-05' WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE correo='empresa@acme.com')",
                "INSERT INTO Usuario (correo, password, role, estado, nickname, fecha_nacimiento) SELECT 'user@cliente.com','user123','USUARIO','ACTIVA','gamer123','2000-06-01' WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE correo='user@cliente.com')"
            };
            for (String sql : inserts){
                try { st.execute(sql); } catch(Exception ex){ /* ignore individual insert failures */ }
            }
            // Ensure seeded empresa user is linked to the Acme company (dev convenience)
            try { st.execute("UPDATE Usuario SET empresa_id = 1 WHERE correo = 'empresa@acme.com'"); } catch(Exception ex) { /* ignore */ }
            // Ensure Videojuego table has url_imagen and categoria columns (safe to run on MySQL/H2)
            try { st.execute("ALTER TABLE Videojuego ADD COLUMN url_imagen VARCHAR(500)"); } catch(Exception ex) { /* ignore if exists */ }
            try { st.execute("ALTER TABLE Videojuego ADD COLUMN categoria VARCHAR(100)"); } catch(Exception ex) { /* ignore if exists */ }
            // Seed demo videojuegos if not present
            try { st.execute("INSERT INTO Videojuego (nombre, descripcion, empresa_id, precio, estado, fecha_lanzamiento, edad_clasificacion, url_imagen, categoria) SELECT 'Mario Strikers', 'Mario Strikers es un juego de fútbol arcade con power-ups y acción multijugador.', 1, 19.99, 'PUBLICADO', '2021-07-01', 'E', '/assets/ore/mario_strikers.jpg', 'Deportes' WHERE NOT EXISTS (SELECT 1 FROM Videojuego WHERE nombre='Mario Strikers')"); } catch(Exception ex){ }
            try { st.execute("INSERT INTO Videojuego (nombre, descripcion, empresa_id, precio, estado, fecha_lanzamiento, edad_clasificacion, url_imagen, categoria) SELECT 'Demo Racer X', 'Carreras arcade con pistas dinámicas y potenciadores.', 1, 9.99, 'PUBLICADO', '2020-05-10', 'E', '/assets/ore/demo_racer.jpg', 'Carreras' WHERE NOT EXISTS (SELECT 1 FROM Videojuego WHERE nombre='Demo Racer X')"); } catch(Exception ex){ }
            try { st.execute("INSERT INTO Videojuego (nombre, descripcion, empresa_id, precio, estado, fecha_lanzamiento, edad_clasificacion, url_imagen, categoria) SELECT 'Pixel Adventure', 'Plataformas retro con niveles creados por la comunidad.', 1, 4.99, 'PUBLICADO', '2019-11-20', 'E', '/assets/ore/pixel_adventure.jpg', 'Plataformas' WHERE NOT EXISTS (SELECT 1 FROM Videojuego WHERE nombre='Pixel Adventure')"); } catch(Exception ex){ }
        } catch (Exception ex){
            System.err.println("ensureSeedUsers failed: " + ex.getMessage());
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
            st.execute("CREATE TABLE Usuario (id INT AUTO_INCREMENT PRIMARY KEY, correo VARCHAR(150) NOT NULL UNIQUE, password VARCHAR(255) NOT NULL, role VARCHAR(20) NOT NULL, estado VARCHAR(20) NOT NULL, nickname VARCHAR(100), fecha_nacimiento DATE, telefono VARCHAR(50), pais VARCHAR(100), empresa_id INT NULL, created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE Videojuego (id INT AUTO_INCREMENT PRIMARY KEY, nombre VARCHAR(150) NOT NULL, descripcion TEXT, empresa_id INT NOT NULL, precio DECIMAL(10,2) DEFAULT 0, estado VARCHAR(30) DEFAULT 'REGISTRADO', fecha_lanzamiento DATE, edad_clasificacion VARCHAR(10), url_imagen VARCHAR(500), categoria VARCHAR(100), FOREIGN KEY (empresa_id) REFERENCES Empresa(id) ON DELETE CASCADE)");
            st.execute("CREATE TABLE Videojuego_Categoria (videojuego_id INT NOT NULL, categoria_id INT NOT NULL, PRIMARY KEY (videojuego_id, categoria_id), FOREIGN KEY (videojuego_id) REFERENCES Videojuego(id) ON DELETE CASCADE, FOREIGN KEY (categoria_id) REFERENCES Categoria(id_categoria) ON DELETE CASCADE)");
            st.execute("CREATE TABLE Compra (id INT AUTO_INCREMENT PRIMARY KEY, usuario_id INT NOT NULL, videojuego_id INT NOT NULL, fecha DATETIME DEFAULT CURRENT_TIMESTAMP, total DECIMAL(10,2), platform_commission DECIMAL(12,4), company_amount DECIMAL(12,4), FOREIGN KEY (usuario_id) REFERENCES Usuario(id) ON DELETE CASCADE, FOREIGN KEY (videojuego_id) REFERENCES Videojuego(id) ON DELETE CASCADE)");
            st.execute("CREATE TABLE Comentario (id INT AUTO_INCREMENT PRIMARY KEY, usuario_id INT NOT NULL, videojuego_id INT NOT NULL, texto TEXT, puntuacion INT, fecha DATETIME DEFAULT CURRENT_TIMESTAMP, visible BOOLEAN DEFAULT TRUE, FOREIGN KEY (usuario_id) REFERENCES Usuario(id) ON DELETE CASCADE, FOREIGN KEY (videojuego_id) REFERENCES Videojuego(id) ON DELETE CASCADE)");
            st.execute("CREATE TABLE Cartera (id INT AUTO_INCREMENT PRIMARY KEY, usuario_id INT NOT NULL, saldo DECIMAL(12,2) DEFAULT 0, FOREIGN KEY (usuario_id) REFERENCES Usuario(id) ON DELETE CASCADE)");
            st.execute("CREATE TABLE Banner (id INT AUTO_INCREMENT PRIMARY KEY, url_imagen VARCHAR(255), fecha_inicio TIMESTAMP, fecha_fin TIMESTAMP)");
            st.execute("CREATE TABLE Grupo_Familiar (id INT AUTO_INCREMENT PRIMARY KEY, nombre VARCHAR(150), owner_id INT NOT NULL, created_at DATETIME DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (owner_id) REFERENCES Usuario(id) ON DELETE CASCADE)");
            st.execute("CREATE TABLE Grupo_Usuario (grupo_id INT NOT NULL, usuario_id INT NOT NULL, PRIMARY KEY (grupo_id, usuario_id), FOREIGN KEY (grupo_id) REFERENCES Grupo_Familiar(id) ON DELETE CASCADE, FOREIGN KEY (usuario_id) REFERENCES Usuario(id) ON DELETE CASCADE)");
            st.execute("CREATE TABLE Comision_Global (id INT PRIMARY KEY, percent DECIMAL(5,2) NOT NULL, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE Comision_Empresa (empresa_id INT PRIMARY KEY, percent DECIMAL(5,2) NOT NULL, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (empresa_id) REFERENCES Empresa(id))");

            // Seed initial data
            st.execute("INSERT INTO Empresa (id, nombre, correo, telefono, estado) VALUES (1, 'Acme Games', 'contact@acmegames.com', '+123456', 'ACTIVA'), (2, 'PixelSoft', 'hello@pixelsoft.com', '+987654', 'ACTIVA')");
            st.execute("INSERT INTO Usuario (id, correo, password, role, estado, nickname, fecha_nacimiento, empresa_id) VALUES (1, 'admin@tienda.com', 'admin123', 'ADMIN', 'ACTIVA', 'admin', '1990-01-01', NULL), (2, 'empresa@acme.com', 'empresa123', 'EMPRESA', 'ACTIVA', 'acme_user', '1985-05-05', 1), (3, 'user@cliente.com', 'user123', 'USUARIO', 'ACTIVA', 'gamer123', '2000-06-01', NULL)");
            st.execute("INSERT INTO Cartera (usuario_id, saldo) VALUES (1, 0.00), (2, 0.00), (3, 100.00)");
            st.execute("INSERT INTO Categoria (id_categoria, nombre, descripcion) VALUES (1, 'Acción', 'Juegos de acción'), (2, 'Aventura', 'Juegos de aventura'), (3, 'Indie', 'Juegos independientes')");
            st.execute("INSERT INTO Videojuego (id, nombre, descripcion, empresa_id, precio, estado, fecha_lanzamiento, edad_clasificacion, url_imagen, categoria) VALUES (1, 'Space Shooter', 'Shooter espacial', 1, 9.99, 'PUBLICADO', '2022-01-01', 'E', '/assets/ore/space_shooter.jpg', 'Acción'), (2, 'Island Adventure', 'Aventura en isla', 2, 14.99, 'PUBLICADO', '2023-06-15', 'T', '/assets/ore/island_adventure.jpg', 'Aventura')");
            st.execute("INSERT INTO Videojuego_Categoria (videojuego_id, categoria_id) VALUES (1,1),(2,2)");
            // Seed demo games (permanent for dev environment)
            st.execute("INSERT INTO Videojuego (nombre, descripcion, empresa_id, precio, estado, fecha_lanzamiento, edad_clasificacion, url_imagen, categoria) SELECT 'Mario Strikers', 'Mario Strikers es un juego de fútbol arcade con power-ups y acción multijugador.', 1, 19.99, 'PUBLICADO', '2021-07-01', 'E', '/assets/ore/mario_strikers.jpg', 'Deportes' WHERE NOT EXISTS (SELECT 1 FROM Videojuego WHERE nombre='Mario Strikers')");
            st.execute("INSERT INTO Videojuego (nombre, descripcion, empresa_id, precio, estado, fecha_lanzamiento, edad_clasificacion, url_imagen, categoria) SELECT 'Demo Racer X', 'Carreras arcade con pistas dinámicas y potenciadores.', 1, 9.99, 'PUBLICADO', '2020-05-10', 'E', '/assets/ore/demo_racer.jpg', 'Carreras' WHERE NOT EXISTS (SELECT 1 FROM Videojuego WHERE nombre='Demo Racer X')");
            st.execute("INSERT INTO Videojuego (nombre, descripcion, empresa_id, precio, estado, fecha_lanzamiento, edad_clasificacion, url_imagen, categoria) SELECT 'Pixel Adventure', 'Plataformas retro con niveles creados por la comunidad.', 1, 4.99, 'PUBLICADO', '2019-11-20', 'E', '/assets/ore/pixel_adventure.jpg', 'Plataformas' WHERE NOT EXISTS (SELECT 1 FROM Videojuego WHERE nombre='Pixel Adventure')");
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