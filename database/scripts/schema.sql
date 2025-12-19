DROP DATABASE IF EXISTS tienda;
CREATE DATABASE tienda CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE tienda;

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
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
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

-- Grupo familiar (opcional)
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

-- Trigger: si la comision global baja, ajustar las comisiones empresa > nueva global
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

-- Datos iniciales de prueba
INSERT INTO Empresa (id, nombre, correo, telefono, estado) VALUES
  (1, 'Acme Games', 'contact@acmegames.com', '+123456', 'ACTIVA'),
  (2, 'PixelSoft', 'hello@pixelsoft.com', '+987654', 'ACTIVA');

INSERT INTO Usuario (id, correo, password, role, estado, nickname, fecha_nacimiento) VALUES
  (1, 'admin@tienda.com', 'admin123', 'ADMIN', 'ACTIVA', 'admin', '1990-01-01'),
  (2, 'empresa@acme.com', 'empresa123', 'EMPRESA', 'ACTIVA', 'acme_user', '1985-05-05'),
  (3, 'user@cliente.com', 'user123', 'USUARIO', 'ACTIVA', 'gamer123', '2000-06-01');

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