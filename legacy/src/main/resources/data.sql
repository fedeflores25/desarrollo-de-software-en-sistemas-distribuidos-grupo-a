INSERT IGNORE INTO cliente (id, documento, nombre, apellido, email, telefono, fecha_nacimiento, activo) VALUES
(1, '30111222', 'Ana', 'Gomez', 'ana.gomez@example.com', '1123450001', '1990-04-12', TRUE),
(2, '30222333', 'Bruno', 'Perez', 'bruno.perez@example.com', '1123450002', '1988-08-21', TRUE),
(3, '30333444', 'Carla', 'Lopez', 'carla.lopez@example.com', '1123450003', '1995-01-30', TRUE),
(4, '30444555', 'Diego', 'Fernandez', 'diego.fernandez@example.com', '1123450004', '1985-06-15', TRUE),
(5, '30555666', 'Elena', 'Martinez', 'elena.martinez@example.com', '1123450005', '1992-11-03', TRUE),
(6, '30666777', 'Federico', 'Sosa', 'federico.sosa@example.com', '1123450006', '1991-02-18', TRUE),
(7, '30777888', 'Gabriela', 'Ruiz', 'gabriela.ruiz@example.com', '1123450007', '1987-09-09', TRUE),
(8, '30888999', 'Hector', 'Diaz', 'hector.diaz@example.com', '1123450008', '1994-12-25', TRUE),
(9, '30999000', 'Irene', 'Castro', 'irene.castro@example.com', '1123450009', '1993-07-07', TRUE),
(10, '31000111', 'Javier', 'Molina', 'javier.molina@example.com', '1123450010', '1989-03-19', FALSE);

INSERT IGNORE INTO vehiculo (id, patente, marca, modelo, anio, color, tipo, precio_diario, estado, activo) VALUES
(1, 'ABC123', 'Toyota', 'Corolla', 2021, 'Blanco', 'SEDAN', 45000.00, 'RESERVADO', TRUE),
(2, 'DEF456', 'Chevrolet', 'Cruze', 2020, 'Gris', 'SEDAN', 42000.00, 'RESERVADO', TRUE),
(3, 'GHI789', 'Jeep', 'Compass', 2022, 'Negro', 'SUV', 65000.00, 'RESERVADO', TRUE),
(4, 'JKL012', 'Ford', 'EcoSport', 2019, 'Azul', 'SUV', 52000.00, 'DISPONIBLE', TRUE),
(5, 'MNO345', 'Toyota', 'Hilux', 2023, 'Rojo', 'PICKUP', 78000.00, 'DISPONIBLE', TRUE),
(6, 'PQR678', 'Ford', 'Ranger', 2021, 'Blanco', 'PICKUP', 74000.00, 'DISPONIBLE', TRUE),
(7, 'STU901', 'Peugeot', 'RCZ', 2018, 'Negro', 'COUPE', 59000.00, 'DISPONIBLE', TRUE),
(8, 'VWX234', 'Audi', 'TT', 2020, 'Gris', 'COUPE', 88000.00, 'RESERVADO', TRUE),
(9, 'BCD890', 'Volkswagen', 'Golf', 2022, 'Azul', 'HATCHBACK', 47000.00, 'RESERVADO', TRUE),
(10, 'EFG567', 'Renault', 'Sandero', 2019, 'Plata', 'HATCHBACK', 35000.00, 'DISPONIBLE', FALSE);

INSERT IGNORE INTO usuario (id, username, password_hash, rol, cliente_id) VALUES
(1, 'admin.rentar', '$2a$10$7EqJtq98hPqEX7fNZaFWoOQ8yZJrXf9kzpy0MjduTGHFkHoCw0R8C', 'ADMIN', NULL),
(2, 'operador.rentar', '$2a$10$7EqJtq98hPqEX7fNZaFWoOQ8yZJrXf9kzpy0MjduTGHFkHoCw0R8C', 'ADMIN', NULL),
(3, 'ana.cliente', '$2a$10$7EqJtq98hPqEX7fNZaFWoOQ8yZJrXf9kzpy0MjduTGHFkHoCw0R8C', 'CLIENTE', 1),
(4, 'bruno.cliente', '$2a$10$7EqJtq98hPqEX7fNZaFWoOQ8yZJrXf9kzpy0MjduTGHFkHoCw0R8C', 'CLIENTE', 2);

INSERT IGNORE INTO reserva (id, cliente_id, vehiculo_id, fecha_inicio, fecha_fin, precio_diario, cantidad_dias, importe_total, estado, fecha_alta, fecha_cancelacion) VALUES
(1, 1, 7, '2026-09-12 08:00:00', '2026-09-13 08:00:00', 59000.00, 1, 59000.00, 'FINALIZADA', '2026-09-10 09:00:00', NULL),
(2, 2, 1, '2026-09-16 10:00:00', '2026-09-18 10:00:00', 45000.00, 2, 90000.00, 'CONFIRMADA', '2026-09-15 08:30:00', NULL),
(3, 3, 1, '2026-09-18 10:00:00', '2026-09-20 10:00:00', 45000.00, 2, 90000.00, 'CONFIRMADA', '2026-09-15 08:45:00', NULL),
(4, 4, 2, '2026-09-16 09:00:00', '2026-09-17 09:00:00', 42000.00, 1, 42000.00, 'CONFIRMADA', '2026-09-15 09:00:00', NULL),
(5, 5, 2, '2026-09-23 09:00:00', '2026-09-25 09:00:00', 42000.00, 2, 84000.00, 'CONFIRMADA', '2026-09-15 09:15:00', NULL),
(6, 6, 3, '2026-09-21 12:00:00', '2026-09-24 12:00:00', 65000.00, 3, 195000.00, 'CONFIRMADA', '2026-09-15 09:30:00', NULL),
(7, 7, 5, '2026-09-19 08:00:00', '2026-09-22 08:00:00', 78000.00, 3, 234000.00, 'CANCELADA', '2026-09-15 10:00:00', '2026-09-15 11:00:00'),
(8, 8, 8, '2026-09-26 10:00:00', '2026-09-28 10:00:00', 88000.00, 2, 176000.00, 'CONFIRMADA', '2026-09-15 10:15:00', NULL),
(9, 9, 9, '2026-09-29 09:00:00', '2026-09-30 09:00:00', 47000.00, 1, 47000.00, 'CONFIRMADA', '2026-09-15 10:30:00', NULL);
