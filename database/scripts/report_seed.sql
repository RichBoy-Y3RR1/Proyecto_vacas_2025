-- report_seed.sql
-- Minimal seed data to exercise reports (adapt column names/types to your schema as needed)

-- Sample users
INSERT INTO Usuario (id, name, email, password) VALUES (100, 'Test Gamer','gamer@example.com','password');

-- Sample company
INSERT INTO Empresa (id, nombre) VALUES (10, 'Test Company');

-- Sample commission defaults (if your schema uses a table)
-- If you have a Comision_Global table: insert or update
INSERT INTO Comision_Global (percent) VALUES (15.0);
-- Company commission
INSERT INTO Comision_Empresa (empresa_id, percent) VALUES (10, 7.0);

-- Sample videogames
INSERT INTO Videojuego (id, nombre, empresa_id, precio) VALUES (1000, 'Space Adventure', 10, 9.99);
INSERT INTO Videojuego (id, nombre, empresa_id, precio) VALUES (1001, 'Puzzle World', 10, 4.99);
INSERT INTO Videojuego (id, nombre, empresa_id, precio) VALUES (1002, 'Racing Pro', 10, 14.99);

-- Create a wallet for the test user (Cartera)
INSERT INTO Cartera (id, usuario_id, saldo) VALUES (100, 100, 100.00);

-- Create purchases (Compra) for testing top5/ventas
INSERT INTO Compra (id, usuario_id, videojuego_id, total, platform_commission, company_amount, fecha) VALUES (10000, 100, 1000, 9.99, 1.50, 8.49, NOW());
INSERT INTO Compra (id, usuario_id, videojuego_id, total, platform_commission, company_amount, fecha) VALUES (10001, 100, 1001, 4.99, 0.75, 4.24, NOW());
INSERT INTO Compra (id, usuario_id, videojuego_id, total, platform_commission, company_amount, fecha) VALUES (10002, 100, 1002, 14.99, 2.25, 12.74, NOW());

-- Sample comments for feedback report
INSERT INTO Comentario (id, usuario_id, videojuego_id, texto, puntuacion, fecha) VALUES (5000, 100, 1000, 'Great game!', 5, NOW());
INSERT INTO Comentario (id, usuario_id, videojuego_id, texto, puntuacion, fecha) VALUES (5001, 100, 1001, 'Nice puzzles', 4, NOW());

-- Notes:
-- 1) This SQL assumes table and column names used in the project (Usuario, Empresa, Videojuego, Compra, Comentario, Cartera, Comision_Empresa, Comision_Global).
-- 2) If your schema differs, adapt column names/types accordingly before running.
-- 3) Run this against a fresh database (or after cleaning test rows) to seed sample data used by the report endpoints.
