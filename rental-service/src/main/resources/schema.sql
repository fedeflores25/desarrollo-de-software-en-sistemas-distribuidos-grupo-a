-- Base propia del Rental Service: rentar_reservas
-- Guarda solo los ids de cliente y vehiculo (sin foreign keys entre servicios).
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
    CONSTRAINT chk_reserva_total CHECK (importe_total > 0)
);

CREATE INDEX idx_reserva_cliente ON reserva (cliente_id);
CREATE INDEX idx_reserva_vehiculo_fechas ON reserva (vehiculo_id, fecha_inicio, fecha_fin);
CREATE INDEX idx_reserva_estado ON reserva (estado);
