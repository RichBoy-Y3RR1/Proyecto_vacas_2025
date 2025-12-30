-- Combined schema + seed for tienda (safe to run on a new/empty server)
-- Created by assistant to simplify import: runs schema then sample seed data

DROP DATABASE IF EXISTS tienda;
CREATE DATABASE tienda CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE tienda;

-- ==========================
-- SCHEMA (copied from schema.sql)
-- ==========================

-- Categorías 
CREATE TABLE Categoria (
  id_categoria INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL UNIQUE,
  descripcion TEXT,
  fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Banner Principal
CREATE TABLE Banner (
  id INT AUTO_INCREMENT PRIMARY KEY,
  url_imagen VARCHAR(255) NOT NULL,
  fecha_inicio DATETIME,
  fecha_fin DATETIME
);

-- Empresas
CREATE TABLE Empresa (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(150) NOT NULL,
  correo VARCHAR(150) UNIQUE,
  telefono VARCHAR(50),
  estado VARCHAR(20) NOT NULL
);

-- Usuarios (gamer, empresa user, admin)
CREATE TABLE Usuario (
  id INT AUTO_INCREMENT PRIMARY KEY,
  correo VARCHAR(150) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL,
  estado VARCHAR(20) NOT NULL,
  nickname VARCHAR(100),
  fecha_nacimiento DATE,
  telefono VARCHAR(50),
  pais VARCHAR(100),
  empresa_id INT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (empresa_id) REFERENCES Empresa(id) ON DELETE SET NULL
);

-- Juegos
CREATE TABLE Videojuego (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(150) NOT NULL,
  descripcion TEXT,
  empresa_id INT NOT NULL,
  precio DECIMAL(10,2) DEFAULT 0,
  estado VARCHAR(30) DEFAULT 'REGISTRADO',
  fecha_lanzamiento DATE,
  edad_clasificacion VARCHAR(10),
  FOREIGN KEY (empresa_id) REFERENCES Empresa(id) ON DELETE CASCADE
);

-- Relación N:M juego-categoría
CREATE TABLE Videojuego_Categoria (
  videojuego_id INT NOT NULL,
  categoria_id INT NOT NULL,
  PRIMARY KEY (videojuego_id, categoria_id),
  FOREIGN KEY (videojuego_id) REFERENCES Videojuego(id) ON DELETE CASCADE,
  FOREIGN KEY (categoria_id) REFERENCES Categoria(id_categoria) ON DELETE CASCADE
);

-- Compras
CREATE TABLE Compra (
  id INT AUTO_INCREMENT PRIMARY KEY,
  usuario_id INT NOT NULL,
  videojuego_id INT NOT NULL,
  fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
  total DECIMAL(10,2),
  platform_commission DECIMAL(12,4),
  company_amount DECIMAL(12,4),
  FOREIGN KEY (usuario_id) REFERENCES Usuario(id) ON DELETE CASCADE,
  FOREIGN KEY (videojuego_id) REFERENCES Videojuego(id) ON DELETE CASCADE
);

-- Comentarios / calificaciones
CREATE TABLE Comentario (
  id INT AUTO_INCREMENT PRIMARY KEY,
  usuario_id INT NOT NULL,
  videojuego_id INT NOT NULL,
  texto TEXT,
  puntuacion INT,
  fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
  visible BOOLEAN DEFAULT TRUE,
  FOREIGN KEY (usuario_id) REFERENCES Usuario(id) ON DELETE CASCADE,
  FOREIGN KEY (videojuego_id) REFERENCES Videojuego(id) ON DELETE CASCADE
);

-- Cartera (saldo)
CREATE TABLE Cartera (
  id INT AUTO_INCREMENT PRIMARY KEY,
  usuario_id INT NOT NULL,
  saldo DECIMAL(12,2) DEFAULT 0,
  FOREIGN KEY (usuario_id) REFERENCES Usuario(id) ON DELETE CASCADE
);

-- Grupo familiar 
CREATE TABLE Grupo_Familiar (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(150),
  owner_id INT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (owner_id) REFERENCES Usuario(id) ON DELETE CASCADE
);

CREATE TABLE Grupo_Usuario (
  grupo_id INT NOT NULL,
  usuario_id INT NOT NULL,
  PRIMARY KEY (grupo_id, usuario_id),
  FOREIGN KEY (grupo_id) REFERENCES Grupo_Familiar(id) ON DELETE CASCADE,
  FOREIGN KEY (usuario_id) REFERENCES Usuario(id) ON DELETE CASCADE
);

-- Comisiones: global y por empresa
CREATE TABLE Comision_Global (
  id INT PRIMARY KEY,
  percent DECIMAL(5,2) NOT NULL,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE Comision_Empresa (
  empresa_id INT PRIMARY KEY,
  percent DECIMAL(5,2) NOT NULL,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (empresa_id) REFERENCES Empresa(id)
);

--  si la comision global baja, ajustar las comisiones empresa > nueva global
DELIMITER $$
CREATE TRIGGER trg_comision_global_after_update
AFTER UPDATE ON Comision_Global
FOR EACH ROW
BEGIN
  IF NEW.percent < OLD.percent THEN
    UPDATE Comision_Empresa SET percent = NEW.percent WHERE percent > NEW.percent;
  END IF;
END$$
DELIMITER ;

-- ==========================
-- INITIAL DATA (schema-level seed)
-- ==========================

INSERT INTO Empresa (id, nombre, correo, telefono, estado) VALUES
  (1, 'Acme Games', 'contact@acmegames.com', '+123456', 'ACTIVA'),
  (2, 'PixelSoft', 'hello@pixelsoft.com', '+987654', 'ACTIVA');

What I need is to be able to test the company, administrator, and user accounts to see if I have any errors, but the default users are still not working.
ON DUPLICATE KEY UPDATE
  password=VALUES(password), role=VALUES(role), estado=VALUES(estado), nickname=VALUES(nickname), fecha_nacimiento=VALUES(fecha_nacimiento), empresa_id=VALUES(empresa_id);

INSERT INTO Cartera (usuario_id, saldo) VALUES
  (1, 0.00),
  (2, 0.00),
  (3, 100.00);

INSERT INTO Categoria (id_categoria, nombre, descripcion) VALUES
  (1, 'Acción', 'Juegos de acción'),
  (2, 'Aventura', 'Juegos de aventura'),
  (3, 'Indie', 'Juegos independientes');

INSERT INTO Videojuego (id, nombre, descripcion, empresa_id, precio, estado, fecha_lanzamiento, edad_clasificacion) VALUES
  (1, 'Space Shooter', 'Shooter espacial', 1, 9.99, 'PUBLICADO', '2022-01-01', 'E'),
  (2, 'Island Adventure', 'Aventura en isla', 2, 14.99, 'PUBLICADO', '2023-06-15', 'T');

INSERT INTO Videojuego_Categoria (videojuego_id, categoria_id) VALUES
  (1,1),(2,2);

INSERT INTO Compra (id, usuario_id, videojuego_id, fecha, total, platform_commission, company_amount) VALUES
  (1, 3, 1, NOW(), 9.99, 1.4985, 8.4915);

INSERT INTO Comentario (id, usuario_id, videojuego_id, texto, puntuacion, fecha, visible) VALUES
  (1, 3, 1, 'Excelente juego', 5, NOW(), TRUE);

INSERT INTO Banner (id, url_imagen, fecha_inicio, fecha_fin) VALUES
  (1, '/assets/banners/banner1.jpg', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY));

-- Comisiones: global por defecto y una específica para empresa 1 (inferior o igual al global)
INSERT INTO Comision_Global (id, percent) VALUES (1, 15.00);
INSERT INTO Comision_Empresa (empresa_id, percent) VALUES (1, 12.50);

-- Indices recomendados
CREATE INDEX idx_videojuego_nombre ON Videojuego(nombre);
CREATE INDEX idx_compra_fecha ON Compra(fecha);

-- ==========================
-- ADDITIONAL SAMPLE SEED (test company + test gamer)
-- Adapted from report_seed.sql to match schema column names
-- ==========================

-- Test company (id 10) and test gamer (id 100)
INSERT INTO Empresa (id, nombre, correo, telefono, estado) VALUES (10, 'Test Company', 'test@company.com', '+100', 'ACTIVA');

INSERT INTO Usuario (id, correo, password, role, estado, nickname, fecha_nacimiento) VALUES
  (100, 'gamer@example.com', 'password', 'USUARIO', 'ACTIVA', 'testgamer', '1995-01-01');


-- Commission defaults for testing
INSERT INTO Comision_Global (id, percent) VALUES (2, 15.00) ON DUPLICATE KEY UPDATE percent=VALUES(percent);
INSERT INTO Comision_Empresa (empresa_id, percent) VALUES (10, 7.00) ON DUPLICATE KEY UPDATE percent=VALUES(percent);

-- Sample videogames for test company
INSERT INTO Videojuego (id, nombre, descripcion, empresa_id, precio, estado) VALUES
  (1000, 'Space Adventure', 'Prueba top5', 10, 9.99, 'PUBLICADO'),
  (1001, 'Puzzle World', 'Prueba top5', 10, 4.99, 'PUBLICADO'),
  (1002, 'Racing Pro', 'Prueba top5', 10, 14.99, 'PUBLICADO');

-- Create a wallet for the test user (Cartera)
INSERT INTO Cartera (usuario_id, saldo) VALUES (100, 100.00);

-- Create purchases (Compra) for testing top5/ventas
INSERT INTO Compra (id, usuario_id, videojuego_id, total, platform_commission, company_amount, fecha) VALUES (10000, 100, 1000, 9.99, 1.50, 8.49, NOW());
INSERT INTO Compra (id, usuario_id, videojuego_id, total, platform_commission, company_amount, fecha) VALUES (10001, 100, 1001, 4.99, 0.75, 4.24, NOW());
INSERT INTO Compra (id, usuario_id, videojuego_id, total, platform_commission, company_amount, fecha) VALUES (10002, 100, 1002, 14.99, 2.25, 12.74, NOW());

-- Sample comments for feedback report
INSERT INTO Comentario (id, usuario_id, videojuego_id, texto, puntuacion, fecha) VALUES (5000, 100, 1000, 'Great game!', 5, NOW());
INSERT INTO Comentario (id, usuario_id, videojuego_id, texto, puntuacion, fecha) VALUES (5001, 100, 1001, 'Nice puzzles', 4, NOW());

-- End of combined schema_and_seed.sql
