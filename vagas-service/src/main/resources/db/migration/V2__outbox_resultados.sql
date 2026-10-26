CREATE TABLE outbox_resultados (
    event_id UUID PRIMARY KEY,
    vaga_id UUID NOT NULL UNIQUE,
    payload TEXT NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    publicado_em TIMESTAMP WITH TIME ZONE
);
CREATE INDEX ix_outbox_resultados_pendentes ON outbox_resultados(publicado_em, criado_em);
