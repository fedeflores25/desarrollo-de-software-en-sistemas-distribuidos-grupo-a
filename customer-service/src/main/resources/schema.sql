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

-- Usuarios de acceso para la demostracion. Un administrador no se asocia a
-- cliente; cada usuario cliente queda asociado a un unico cliente activo.
CREATE TABLE IF NOT EXISTS usuario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(60) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    cliente_id BIGINT DEFAULT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_usuario_username UNIQUE (username),
    CONSTRAINT uk_usuario_cliente UNIQUE (cliente_id),
    CONSTRAINT chk_usuario_rol CHECK (rol IN ('ADMIN', 'CLIENTE')),
    CONSTRAINT fk_usuario_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;
