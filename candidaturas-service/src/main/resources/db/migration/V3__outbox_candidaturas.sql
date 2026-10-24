CREATE TABLE outbox_candidaturas (
    event_id UUID PRIMARY KEY,
    candidatura_id UUID NOT NULL UNIQUE REFERENCES candidaturas(candidatura_id),
    payload TEXT NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    publicado_em TIMESTAMP WITH TIME ZONE
);
CREATE INDEX ix_outbox_pendentes ON outbox_candidaturas(publicado_em, criado_em);
