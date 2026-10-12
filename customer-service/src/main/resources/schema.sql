-- Estructura de la base de datos de Customer Service

CREATE TABLE IF NOT EXISTS cliente (
    id BIGINT NOT NULL AUTO_INCREMENT,
    activo BIT(1) NOT NULL,
    apellido VARCHAR(255) NOT NULL,
    documento VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    fecha_nacimiento DATE DEFAULT NULL,
    nombre VARCHAR(255) NOT NULL,
    telefono VARCHAR(255) DEFAULT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_cliente_documento UNIQUE (documento),
    CONSTRAINT uk_cliente_email UNIQUE (email)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;