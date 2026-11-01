CREATE TABLE IF NOT EXISTS solicitud_mantenimiento (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reserva_id BIGINT NOT NULL,
    vehiculo_id BIGINT NOT NULL,
    fecha_inicio DATETIME NOT NULL,
    fecha_fin DATETIME NOT NULL,
    estado VARCHAR(20) NOT NULL,
    event_id VARCHAR(36) NOT NULL,
    correlation_id VARCHAR(36) NOT NULL,
    creado_en DATETIME NOT NULL,
    actualizado_en DATETIME NOT NULL,
    CONSTRAINT uk_solicitud_mantenimiento_reserva UNIQUE (reserva_id),
    CONSTRAINT uk_solicitud_mantenimiento_evento UNIQUE (event_id)
);
