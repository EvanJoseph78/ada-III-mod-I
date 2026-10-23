CREATE TABLE candidaturas (
    candidatura_id UUID PRIMARY KEY,
    matricula_empregado BIGINT NOT NULL,
    id_vaga BIGINT NOT NULL,
    realizada_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_candidatura_empregado_vaga UNIQUE (matricula_empregado, id_vaga)
);
