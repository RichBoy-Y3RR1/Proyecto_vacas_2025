-- Schema aligned with DAOs/servlets used by the app
CREATE TABLE IF NOT EXISTS Empresa (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(200) NOT NULL,
  correo VARCHAR(200),
  telefono VARCHAR(50),
  estado VARCHAR(50) DEFAULT 'ACTIVA'
);

CREATE TABLE IF NOT EXISTS Usuario (
  id INT AUTO_INCREMENT PRIMARY KEY,
  correo VARCHAR(200) NOT NULL UNIQUE,
  password VARCHAR(200) NOT NULL,
  role VARCHAR(50) NOT NULL,
  nickname VARCHAR(100),
  fecha_nacimiento DATE,
  telefono VARCHAR(50),
  pais VARCHAR(100),
  empresa_id INT NULL,
  FOREIGN KEY (empresa_id) REFERENCES Empresa(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS Videojuego (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(255) NOT NULL,
  descripcion TEXT,
  precio DECIMAL(10,2) DEFAULT 0,
  empresa_id INT,
  estado VARCHAR(50) DEFAULT 'REGISTRADO',
  fecha_lanzamiento DATE,
  edad_clasificacion VARCHAR(10),
  FOREIGN KEY (empresa_id) REFERENCES Empresa(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Compra (
  id INT AUTO_INCREMENT PRIMARY KEY,
  usuario_id INT NOT NULL,
  videojuego_id INT NOT NULL,
  total DECIMAL(10,2) NOT NULL,
  platform_commission DECIMAL(12,4) DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (usuario_id) REFERENCES Usuario(id) ON DELETE CASCADE,
  FOREIGN KEY (videojuego_id) REFERENCES Videojuego(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Comentario (
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

CREATE TABLE IF NOT EXISTS Cartera (
  id INT AUTO_INCREMENT PRIMARY KEY,
  usuario_id INT NOT NULL UNIQUE,
  saldo DECIMAL(12,2) DEFAULT 0,
  FOREIGN KEY (usuario_id) REFERENCES Usuario(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Revoked_Token (
  jti VARCHAR(100) PRIMARY KEY,
  expiry TIMESTAMP
);

CREATE TABLE IF NOT EXISTS Banner (
  id INT AUTO_INCREMENT PRIMARY KEY,
  url_imagen VARCHAR(500),
  fecha_inicio TIMESTAMP NULL,
  fecha_fin TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS Categoria (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(200) NOT NULL,
  descripcion TEXT
);

CREATE TABLE IF NOT EXISTS Comision (
  id INT AUTO_INCREMENT PRIMARY KEY,
  porcentaje DECIMAL(5,2) DEFAULT 10.0
);

-- Seed data: company and admin user
INSERT INTO Empresa (nombre, correo, telefono, estado) SELECT 'Empresa Demo','demo@empresa.local','+000', 'ACTIVA' WHERE NOT EXISTS (SELECT 1 FROM Empresa WHERE nombre='Empresa Demo');

INSERT INTO Usuario (correo,password,role,empresa_id,nickname)
SELECT 'admin@example.com','pass','ADMIN',(SELECT id FROM Empresa WHERE nombre='Empresa Demo'),'admin' WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE correo='admin@example.com');

-- Sample videojuego
INSERT INTO Videojuego (nombre, descripcion, precio, empresa_id, estado)
SELECT 'Demo Game','Demo description',9.99,(SELECT id FROM Empresa WHERE nombre='Empresa Demo'),'PUBLICADO' WHERE NOT EXISTS (SELECT 1 FROM Videojuego WHERE nombre='Demo Game');

-- Initialize cartera for admin
INSERT INTO Cartera (usuario_id, saldo)
SELECT u.id, 100.00 FROM Usuario u WHERE u.correo='admin@example.com' AND NOT EXISTS (SELECT 1 FROM Cartera c WHERE c.usuario_id = u.id);
