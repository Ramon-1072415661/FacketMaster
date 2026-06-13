CREATE TABLE IF NOT EXISTS pedidos
(
    id
    UUID
    PRIMARY
    KEY
    DEFAULT
    gen_random_uuid
(
),
    evento_id BIGINT NOT NULL,
    usuario_id VARCHAR
(
    100
) NOT NULL,
    quantidade INTEGER NOT NULL,
    valor_unitario NUMERIC
(
    10,
    2
) NOT NULL,
    valor_total NUMERIC
(
    10,
    2
) NOT NULL,
    metodo_pagamento VARCHAR
(
    30
) NOT NULL,
    status_pedido VARCHAR
(
    30
) NOT NULL DEFAULT 'AGUARDANDO_PAGAMENTO',
    criado_em TIMESTAMPTZ NOT NULL DEFAULT NOW
(
),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT NOW
(
),
    CONSTRAINT fk_pedido_evento FOREIGN KEY
(
    evento_id
) REFERENCES eventos
(
    id
),
    CONSTRAINT chk_quantidade_pedido CHECK
(
    quantidade
    >=
    1
),
    CONSTRAINT chk_valor_unitario CHECK
(
    valor_unitario
    >=
    0
),
    CONSTRAINT chk_valor_total_positivo CHECK
(
    valor_total
    >=
    0
),
    CONSTRAINT chk_valor_total_consistente CHECK
(
    valor_total =
    ROUND
(
    quantidade
    *
    valor_unitario,
    2
)
    ),
    CONSTRAINT chk_metodo_pagamento CHECK
(
    metodo_pagamento
    IN
(
    'CARTAO_CREDITO',
    'PIX',
    'BOLETO'
)
    ),
    CONSTRAINT chk_status_pedido CHECK
(
    status_pedido
    IN
(
    'AGUARDANDO_PAGAMENTO',
    'PROCESSANDO',
    'APROVADO',
    'RECUSADO',
    'CANCELADO',
    'EXPIRADO'
)
    )
    );

CREATE TABLE IF NOT EXISTS pagamentos
(
    id
    UUID
    PRIMARY
    KEY
    DEFAULT
    gen_random_uuid
(
),
    pedido_id UUID NOT NULL UNIQUE,
    metodo_pagamento VARCHAR
(
    30
) NOT NULL,
    status_pagamento VARCHAR
(
    30
) NOT NULL DEFAULT 'PENDENTE',
    codigo_transacao VARCHAR
(
    100
),
    codigo_pagamento VARCHAR
(
    500
),
    url_pagamento VARCHAR
(
    1000
),
    data_expiracao TIMESTAMPTZ,
    data_aprovacao TIMESTAMPTZ,
    motivo_recusa VARCHAR
(
    500
),
    tentativas INTEGER NOT NULL DEFAULT 0,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT NOW
(
),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT NOW
(
),
    CONSTRAINT fk_pagamento_pedido FOREIGN KEY
(
    pedido_id
) REFERENCES pedidos
(
    id
) ON DELETE CASCADE,
    CONSTRAINT chk_tentativas CHECK
(
    tentativas
    >=
    0
),
    CONSTRAINT chk_metodo_pagamento_pag CHECK
(
    metodo_pagamento
    IN
(
    'CARTAO_CREDITO',
    'PIX',
    'BOLETO'
)
    ),
    CONSTRAINT chk_status_pagamento CHECK
(
    status_pagamento
    IN
(
    'PENDENTE',
    'PROCESSANDO',
    'APROVADO',
    'RECUSADO',
    'CANCELADO',
    'EXPIRADO'
)
    )
    );

CREATE TABLE IF NOT EXISTS ingressos
(
    id
    UUID
    PRIMARY
    KEY
    DEFAULT
    gen_random_uuid
(
),
    pedido_id UUID NOT NULL,
    evento_id BIGINT NOT NULL,
    usuario_id VARCHAR
(
    100
) NOT NULL,
    codigo_ingresso VARCHAR
(
    100
) NOT NULL UNIQUE,
    status_ingresso VARCHAR
(
    30
) NOT NULL DEFAULT 'ATIVO',
    emitido_em TIMESTAMPTZ NOT NULL DEFAULT NOW
(
),
    utilizado_em TIMESTAMPTZ,
    CONSTRAINT fk_ingresso_pedido FOREIGN KEY
(
    pedido_id
) REFERENCES pedidos
(
    id
) ON DELETE CASCADE,
    CONSTRAINT fk_ingresso_evento FOREIGN KEY
(
    evento_id
) REFERENCES eventos
(
    id
),
    CONSTRAINT chk_status_ingresso CHECK
(
    status_ingresso
    IN
(
    'ATIVO',
    'UTILIZADO',
    'CANCELADO'
)
    )
    );

CREATE INDEX IF NOT EXISTS idx_pedidos_evento_id ON pedidos (evento_id);

CREATE INDEX IF NOT EXISTS idx_pedidos_usuario_id ON pedidos (usuario_id);

CREATE INDEX IF NOT EXISTS idx_pedidos_status ON pedidos (status_pedido);

CREATE INDEX IF NOT EXISTS idx_pedidos_criado_em ON pedidos (criado_em DESC);

CREATE INDEX IF NOT EXISTS idx_pagamentos_pedido_id ON pagamentos (pedido_id);

CREATE INDEX IF NOT EXISTS idx_pagamentos_status ON pagamentos (status_pagamento);

CREATE INDEX IF NOT EXISTS idx_ingressos_pedido_id ON ingressos (pedido_id);

CREATE INDEX IF NOT EXISTS idx_ingressos_evento_id ON ingressos (evento_id);

CREATE INDEX IF NOT EXISTS idx_ingressos_usuario_id ON ingressos (usuario_id);

CREATE
OR REPLACE FUNCTION atualizar_timestamp () RETURNS TRIGGER AS $$
BEGIN
    NEW.atualizado_em
= NOW();
RETURN NEW;
END;
$$
LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_pedidos_atualizado_em ON pedidos;

CREATE TRIGGER trg_pedidos_atualizado_em
    BEFORE
        UPDATE
    ON pedidos
    FOR EACH ROW
    EXECUTE FUNCTION atualizar_timestamp ();

DROP TRIGGER IF EXISTS trg_pagamentos_atualizado_em ON pagamentos;

CREATE TRIGGER trg_pagamentos_atualizado_em
    BEFORE
        UPDATE
    ON pagamentos
    FOR EACH ROW
    EXECUTE FUNCTION atualizar_timestamp ();