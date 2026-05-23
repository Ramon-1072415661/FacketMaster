CREATE TABLE IF NOT EXISTS usuarios (
    id      serial PRIMARY KEY,
    nome    VARCHAR(100) NOT NULL,
    email   VARCHAR(150) NOT NULL UNIQUE,
    senha   VARCHAR(255) NOT NULL,
    role    VARCHAR(20)  NOT NULL,

    CONSTRAINT chk_usuario_role CHECK (role IN ('ADMIN', 'USER'))
);