CREATE TABLE empregados (
    empregado_id UUID PRIMARY KEY,
    matricula_empregado BIGINT NOT NULL UNIQUE,
    nome_empregado VARCHAR(255) NOT NULL
);
CREATE TABLE itens_perfil (
    empregado_id UUID NOT NULL REFERENCES empregados(empregado_id),
    ordem INTEGER NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    horas INTEGER CHECK (horas >= 0),
    meses INTEGER CHECK (meses >= 0),
    PRIMARY KEY (empregado_id, ordem)
);
