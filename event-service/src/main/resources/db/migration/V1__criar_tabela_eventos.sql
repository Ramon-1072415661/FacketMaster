CREATE TABLE IF NOT EXISTS eventos
(
    id                    BIGSERIAL PRIMARY KEY,
    nome                  VARCHAR(150)   NOT NULL,
    data_evento           TIMESTAMP      NOT NULL,
    valor                 NUMERIC(10, 2) NOT NULL,
    quantidade_disponivel INTEGER        NOT NULL,
    quantidade_total      INTEGER        NOT NULL,
    status                VARCHAR(20)    NOT NULL DEFAULT 'ATIVO',
    descricao             VARCHAR(500),
    local                 VARCHAR(200),
    criado_em             TIMESTAMP      NOT NULL DEFAULT NOW(),
    atualizado_em         TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_quantidade_positiva CHECK (quantidade_disponivel >= 0),
    CONSTRAINT chk_quantidade_total CHECK (quantidade_total >= 1),
    CONSTRAINT chk_quantidade_consistente CHECK (quantidade_disponivel <= quantidade_total),
    CONSTRAINT chk_valor_positivo CHECK (valor >= 0),
    CONSTRAINT chk_status_valido CHECK (
        status IN ('ATIVO', 'ESGOTADO', 'CANCELADO', 'ENCERRADO')
        )
);

CREATE INDEX IF NOT EXISTS idx_eventos_status ON eventos (status);

CREATE INDEX IF NOT EXISTS idx_eventos_data_evento ON eventos (data_evento);

CREATE INDEX IF NOT EXISTS idx_eventos_nome ON eventos (LOWER(nome));

CREATE OR REPLACE FUNCTION atualizar_timestamp() RETURNS TRIGGER AS
$$
BEGIN
    NEW.atualizado_em = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_eventos_atualizado_em ON eventos;

CREATE TRIGGER trg_eventos_atualizado_em
    BEFORE
        UPDATE
    ON eventos
    FOR EACH ROW
EXECUTE FUNCTION atualizar_timestamp();