#!/bin/bash
set -e

create_db() {
  local db="$1" user="$2" password="$3"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-SQL
    CREATE USER ${user} WITH PASSWORD '${password}';
    CREATE DATABASE ${db} OWNER ${user};
    REVOKE ALL ON DATABASE ${db} FROM PUBLIC;
SQL
}

create_db empregados   empregados_svc   "$EMPREGADOS_DB_PASSWORD"
create_db vagas        vagas_svc        "$VAGAS_DB_PASSWORD"
create_db candidaturas candidaturas_svc "$CANDIDATURAS_DB_PASSWORD"