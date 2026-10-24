# Persistência — resultado dos testes e roteiro PostgreSQL

Data: 2026-10-22.

## Verificação executada pela IA

Na raiz, `mvn clean verify` terminou com `BUILD SUCCESS` para os cinco projetos do reactor, com seis testes, zero falhas e zero erros:

| Classe | Cenários verificados |
| --- | --- |
| `EmpregadosPersistenceTest` (2 testes) | Cadastro com itens, leitura por outra transação, perfil no formato compartilhado, matrícula duplicada, entrada inválida e consultas inexistentes |
| `VagasPersistenceTest` (2 testes) | Cadastro com requisitos, leitura de critérios, exclusão pelo ID, preservação de outra vaga, novo ID após exclusão, entrada inválida e consultas inexistentes |
| `CandidaturasPersistenceTest` (2 testes) | Registro e consulta de inscrição, data persistida, duplicata rejeitada, entrada inválida e consulta inexistente |

Os testes carregam o contexto Spring, exercitam HTTP via MockMvc e gravam em H2 no modo PostgreSQL, sem mocks dos repositórios. Flyway aplica as migrações V1 de cada módulo e Hibernate valida o schema. As requisições de criação e leitura ocorrem em transações separadas.

Esses testes não comprovam reinício com PostgreSQL, integração entre serviços, mensageria, idempotência de consumidor, cache, Pact ou resiliência. Docker não estava disponível no ambiente desta sessão; a execução abaixo continua pendente.

## Validação da integração na develop — 2026-10-24

Verificação executada pela IA após integrar a persistência com a organização em camadas de empregados:

- `mvn -o clean verify`: `BUILD SUCCESS` nos cinco projetos, sete testes, zero falhas e zero erros. Empregados passou a ter três testes; o novo cenário verifica a separação de experiências e formação nas duas rotas de currículo.
- Os três JARs iniciaram contra seus bancos locais em `localhost:5432`, com PostgreSQL **17.11**, migrações Flyway e `ddl-auto: validate`. Foram usadas portas HTTP livres com `--server.port=0` para não interferir em aplicações existentes.
- Foram cadastrados um empregado fictício com curso e experiência, uma vaga com requisito de 10 pontos e uma candidatura. Perfil, critérios e inscrição foram lidos por HTTP; a rota profissional retornou somente a experiência e uma inscrição duplicada retornou 409.
- Os três processos Java criados para essa verificação foram encerrados e reiniciados. As consultas recuperaram o perfil com dois itens, os critérios com 10 pontos e a candidatura original. Os processos de verificação foram encerrados ao final; o banco e seu volume não foram reiniciados nem apagados.
- Registro fictício preservado para rastreabilidade: matrícula `1792879477284`, empregado `c1d2673d-56f9-4cda-b183-350886edd335`, vaga numérica `1` / UUID `ac1f2e04-a9dd-4943-ac57-61a1a8494ac5`, candidatura `a0a13fe3-682f-4e55-a187-bb547dfac4f3`.

Resultado observado:

```text
PostgreSQL: cadastro e leitura de perfil, criterios e candidatura aprovados; curriculo filtrado; duplicata retornou 409.
APROVADO: os tres servicos reiniciaram e recuperaram os registros e colecoes no PostgreSQL.
Database: jdbc:postgresql://localhost:5432/empregados (PostgreSQL 17.11)
```

Essa execução valida a persistência dos três serviços e sua permanência após reiniciar as aplicações. Não comprova o fluxo entre serviços, avaliação automática, Kafka, cache, Pact, clone limpo ou reinício do próprio PostgreSQL. A ausência do CLI Docker não impediu o acesso ao PostgreSQL que já estava disponível; não se atribui a esta sessão a criação ou verificação dos contêineres.

## Roteiro para repetir a validação — PostgreSQL

Seguir o [README](../../README.md) para iniciar PostgreSQL, configurar as credenciais e iniciar os três JARs. Em um banco sem os registros do exemplo, executar em PowerShell:

```powershell
$empregado = Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/v1/empregados -ContentType application/json -Body '{"matriculaEmpregado":123,"nomeEmpregado":"Ana","itens":[{"tipo":"CURSO","descricao":"Java","horas":40}]}'
$vaga = Invoke-RestMethod -Method Post -Uri http://localhost:8082/api/v1/vagas -ContentType application/json -Body '{"nomeVaga":"Desenvolvimento Java","requisitos":[{"tipo":"CURSO","palavraChave":"Java","obrigatorio":true,"pontos":10}]}'
$inscricao = @{ matriculaEmpregado = $empregado.matriculaEmpregado; idVaga = $vaga.idVaga } | ConvertTo-Json
$candidatura = Invoke-RestMethod -Method Post -Uri http://localhost:8083/api/v1/candidaturas -ContentType application/json -Body $inscricao

Invoke-RestMethod "http://localhost:8081/api/v1/empregados/$($empregado.empregadoId)/perfil"
Invoke-RestMethod "http://localhost:8082/api/v1/vagas/$($vaga.vagaId)/criterios"
Invoke-RestMethod "http://localhost:8083/api/v1/candidaturas/$($candidatura.candidaturaId)"
```

Respostas esperadas: criação HTTP 201, perfil com o curso Java, critérios com 10 pontos e candidatura com ID e data. Repetir a mesma inscrição deve retornar 409.

Encerrar os três processos Java com Ctrl+C e iniciá-los novamente, mantendo PostgreSQL e seu volume. Repetir somente as três consultas: os dados devem permanecer. Não executar novamente os cadastros de matrícula/inscrição já existentes; a matrícula 123 e a combinação matrícula/vaga são únicas.

Ao repetir, acrescentar ambiente, resultado real e eventuais diferenças nesta evidência. Não apagar o volume para realizar a verificação.
