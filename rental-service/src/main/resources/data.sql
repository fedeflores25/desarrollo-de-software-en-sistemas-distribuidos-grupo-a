-- Datos iniciales de prueba. Solo reservas ya terminadas o canceladas, para que no
-- ocupen vehiculos hacia adelante ni dependan de la fecha en que se corran las pruebas.
INSERT IGNORE INTO reserva (id, cliente_id, vehiculo_id, fecha_inicio, fecha_fin, precio_diario, cantidad_dias, importe_total, estado, fecha_alta, fecha_cancelacion) VALUES
(1, 1, 7, '2026-09-12 08:00:00', '2026-09-13 08:00:00', 59000.00, 1, 59000.00, 'FINALIZADA', '2026-09-10 09:00:00', NULL),
(2, 2, 1, '2026-09-16 10:00:00', '2026-09-18 10:00:00', 45000.00, 2, 90000.00, 'FINALIZADA', '2026-09-15 08:30:00', NULL),
(3, 3, 2, '2026-09-16 09:00:00', '2026-09-17 09:00:00', 42000.00, 1, 42000.00, 'FINALIZADA', '2026-09-15 09:00:00', NULL),
(4, 7, 5, '2026-09-19 08:00:00', '2026-09-22 08:00:00', 78000.00, 3, 234000.00, 'CANCELADA', '2026-09-15 10:00:00', '2026-09-15 11:00:00');
