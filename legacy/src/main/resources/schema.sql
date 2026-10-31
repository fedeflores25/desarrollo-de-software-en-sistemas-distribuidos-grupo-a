CREATE TABLE IF NOT EXISTS cliente (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    documento VARCHAR(20) NOT NULL,
    nombre VARCHAR(80) NOT NULL,
    apellido VARCHAR(80) NOT NULL,
    email VARCHAR(120) NOT NULL,
    telefono VARCHAR(30),
    fecha_nacimiento DATE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_cliente_documento UNIQUE (documento),
    CONSTRAINT uk_cliente_email UNIQUE (email)
);

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

CREATE TABLE IF NOT EXISTS usuario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(60) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    cliente_id BIGINT,
    CONSTRAINT uk_usuario_username UNIQUE (username),
    CONSTRAINT uk_usuario_cliente UNIQUE (cliente_id),
    CONSTRAINT fk_usuario_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id),
    CONSTRAINT chk_usuario_rol CHECK (rol IN ('ADMIN', 'CLIENTE'))
);

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
    CONSTRAINT fk_reserva_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id),
    CONSTRAINT fk_reserva_vehiculo FOREIGN KEY (vehiculo_id) REFERENCES vehiculo(id),
    CONSTRAINT chk_reserva_estado CHECK (estado IN ('CONFIRMADA', 'CANCELADA', 'FINALIZADA')),
    CONSTRAINT chk_reserva_fechas CHECK (fecha_fin > fecha_inicio),
    CONSTRAINT chk_reserva_precio CHECK (precio_diario > 0),
    CONSTRAINT chk_reserva_dias CHECK (cantidad_dias > 0),
    CONSTRAINT chk_reserva_total CHECK (importe_total > 0)
);

CREATE INDEX idx_reserva_cliente ON reserva (cliente_id);
CREATE INDEX idx_reserva_vehiculo_fechas ON reserva (vehiculo_id, fecha_inicio, fecha_fin);
CREATE INDEX idx_reserva_estado ON reserva (estado);
