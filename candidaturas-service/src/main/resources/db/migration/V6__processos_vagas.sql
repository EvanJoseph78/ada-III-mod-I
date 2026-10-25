CREATE TABLE processos_vagas (
    vaga_id UUID PRIMARY KEY,
    id_vaga BIGINT NOT NULL UNIQUE,
    encerrada_em TIMESTAMP WITH TIME ZONE NOT NULL,
    event_id UUID UNIQUE,
    publicado_em TIMESTAMP WITH TIME ZONE,
    resultado_json TEXT
);
-- Barreira transacional de admissão. Não representa uma vaga de negócio.
INSERT INTO processos_vagas(vaga_id, id_vaga, encerrada_em)
VALUES ('00000000-0000-0000-0000-000000000000', 0, '1970-01-01 00:00:00+00');
