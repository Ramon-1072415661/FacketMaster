CREATE TABLE IF NOT EXISTS usuarios
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    name
    VARCHAR
(
    100
) NOT NULL,
    email VARCHAR
(
    150
) NOT NULL UNIQUE,
    password VARCHAR
(
    255
) NOT NULL,
    role VARCHAR
(
    20
) NOT NULL DEFAULT 'USER',
    criado_em TIMESTAMP NOT NULL DEFAULT NOW
(
),
    atualizado_em TIMESTAMP NOT NULL DEFAULT NOW
(
),
    CONSTRAINT chk_role_valido CHECK
(
    role
    IN
(
    'ADMIN',
    'USER'
)
    )
    );

CREATE INDEX IF NOT EXISTS idx_usuarios_email ON usuarios (email);

CREATE
OR REPLACE FUNCTION atualizar_timestamp() RETURNS TRIGGER AS
$$
BEGIN
    NEW.atualizado_em
= NOW();
RETURN NEW;
END;
$$
LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_usuarios_atualizado_em ON usuarios;

CREATE TRIGGER trg_usuarios_atualizado_em
    BEFORE UPDATE
    ON usuarios
    FOR EACH ROW
    EXECUTE FUNCTION atualizar_timestamp();