-- Inscricoes anteriores permanecem sem snapshot e nao serao publicadas retroativamente.
ALTER TABLE candidaturas ADD COLUMN evento_candidatura TEXT;
