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

## Roteiro manual pendente — PostgreSQL

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

Após executar, acrescentar ambiente, resultado real e eventuais diferenças nesta evidência. Não apagar o volume para realizar a verificação.
