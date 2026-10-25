CREATE TABLE IF NOT EXISTS vehiculo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patente VARCHAR(15) NOT NULL,
    marca VARCHAR(60) NOT NULL,
    modelo VARCHAR(60) NOT NULL,
    anio INT NOT NULL,
    color VARCHAR(40),
    tipo VARCHAR(20) NOT NULL,
    precio_diario DECIMAL(10,2) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_vehiculo_patente UNIQUE (patente),
    CONSTRAINT chk_vehiculo_tipo CHECK (tipo IN ('SEDAN', 'SUV', 'PICKUP', 'COUPE', 'HATCHBACK')),
    CONSTRAINT chk_vehiculo_estado CHECK (estado IN ('DISPONIBLE', 'RESERVADO', 'EN_ALQUILER')),
    CONSTRAINT chk_vehiculo_anio CHECK (anio >= 1900),
    CONSTRAINT chk_vehiculo_precio CHECK (precio_diario > 0)
);

CREATE TABLE IF NOT EXISTS evento_verificacion_procesado (
    event_id VARCHAR(36) PRIMARY KEY,
    vehiculo_id BIGINT NOT NULL,
    resultado VARCHAR(20) NOT NULL,
    procesado_en DATETIME NOT NULL,
    CONSTRAINT chk_evento_verificacion_resultado CHECK (resultado IN ('VALIDADO', 'ROBADO'))
);
