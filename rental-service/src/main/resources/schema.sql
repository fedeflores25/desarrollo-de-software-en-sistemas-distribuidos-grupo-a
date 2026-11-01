-- Base propia del Rental Service: rentar_reservas
-- Guarda solo los ids de cliente y vehiculo (sin foreign keys entre servicios).
-- Los indices van dentro del CREATE TABLE para que el script se pueda ejecutar
-- mas de una vez (MySQL no admite CREATE INDEX IF NOT EXISTS).
CREATE TABLE IF NOT EXISTS reserva (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    vehiculo_id BIGINT NOT NULL,
    fecha_inicio DATETIME NOT NULL,
    fecha_fin DATETIME NOT NULL,
    precio_diario DECIMAL(10,2) NOT NULL,
    cantidad_dias INT NOT NULL,
    importe_total DECIMAL(10,2) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    fecha_alta DATETIME NOT NULL,
    fecha_cancelacion DATETIME,
    CONSTRAINT chk_reserva_estado CHECK (estado IN ('CONFIRMADA', 'CANCELADA', 'FINALIZADA')),
    CONSTRAINT chk_reserva_fechas CHECK (fecha_fin > fecha_inicio),
    CONSTRAINT chk_reserva_precio CHECK (precio_diario > 0),
    CONSTRAINT chk_reserva_dias CHECK (cantidad_dias > 0),
    CONSTRAINT chk_reserva_total CHECK (importe_total > 0),
    INDEX idx_reserva_cliente (cliente_id),
    INDEX idx_reserva_vehiculo_fechas (vehiculo_id, fecha_inicio, fecha_fin),
    INDEX idx_reserva_estado (estado)
);

-- Resultado del mantenimiento preventivo de cada reserva (Hito 3).
-- Se crea PENDIENTE al confirmar la reserva y se actualiza con el evento mantenimiento.resultado.
-- Las reservas anteriores al Hito 3 no tienen fila (mantenimiento sin informar).
CREATE TABLE IF NOT EXISTS reserva_mantenimiento (
    reserva_id BIGINT PRIMARY KEY,
    estado VARCHAR(20) NOT NULL,
    fecha_actualizacion DATETIME NOT NULL,
    ultimo_event_id VARCHAR(36),
    CONSTRAINT chk_mantenimiento_estado CHECK (estado IN ('PENDIENTE', 'REALIZADO', 'NO_REALIZADO')),
    CONSTRAINT fk_mantenimiento_reserva FOREIGN KEY (reserva_id) REFERENCES reserva (id) ON DELETE CASCADE
);
