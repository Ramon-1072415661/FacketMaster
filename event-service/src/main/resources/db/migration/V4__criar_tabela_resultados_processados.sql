CREATE TABLE IF NOT EXISTS resultados_processados
(
    pedido_id     UUID PRIMARY KEY,
    evento_id     BIGINT      NOT NULL,
    status_pedido VARCHAR(30) NOT NULL,
    quantidade    INTEGER     NOT NULL,
    processado_em TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_resultado_evento FOREIGN KEY (evento_id) REFERENCES eventos (id)
);

CREATE INDEX IF NOT EXISTS idx_resultados_processados_evento_id ON resultados_processados (evento_id);
