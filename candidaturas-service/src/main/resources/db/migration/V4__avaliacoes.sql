CREATE TABLE avaliacoes (
    candidatura_id UUID PRIMARY KEY REFERENCES candidaturas(candidatura_id),
    event_id UUID NOT NULL UNIQUE,
    empregado_id UUID NOT NULL,
    vaga_id UUID NOT NULL,
    elegivel BOOLEAN NOT NULL,
    pontuacao INTEGER NOT NULL CHECK (pontuacao >= 0),
    avaliada_em TIMESTAMP WITH TIME ZONE NOT NULL
);
