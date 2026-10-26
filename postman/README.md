# Coleção Postman

No Postman, usar **Import** e selecionar [Selecao-Interna.postman_collection.json](Selecao-Interna.postman_collection.json).

As URLs de Empregados, Vagas e Candidaturas ficam na aba **Variables** da coleção, com portas 8081/8082/8083. Não é necessário importar um environment. Os scripts capturam automaticamente matrículas/IDs das respostas; evitar variáveis de environment com os mesmos nomes, que podem prevalecer sobre as da coleção.

Com as aplicações e a infraestrutura prontas, enviar manualmente:

1. **Empregados:** `cadastrarAna` e `cadastrarBia`.
2. **Vagas:** `criarVaga`.
3. **Candidaturas:** `candidatarAna` e `candidatarBia`; consultar ambas até `AVALIADA`. `consultarRanking` deve mostrar Bia 30 e Ana 10.
4. **Vagas:** `encerrarInscricoes`. Em Candidaturas, conferir `consultarProntidao` com `apto=true`.
5. **Vagas:** `publicarResultado` (202). Em Candidaturas, repetir `consultarResultadoDefinitivo` até 200; depois executar `concluirVaga` em Vagas.

As pastas agrupam endpoints por serviço; **não representam a ordem de execução automática de um Runner**. Avaliação e publicação são assíncronas. `PENDENTE` e 404 na consulta definitiva podem exigir espera e nova consulta. Os scripts validam o status e capturam IDs; não fazem polling automático.

Para repetir, criar uma nova vaga/candidaturas. As matrículas dos cadastros são geradas por scripts. `encerrarBarreiraInterna` é uma rota de coordenação; preferir o fechamento normal por Vagas. A exclusão aponta somente para a vaga descartável criada separadamente.

Os 24 exemplos correspondem a [requisicoes.http](../requisicoes.http). A geração da coleção não executa chamadas nem popula o banco. Como iniciar os serviços: [README](../README.md).

Referência dos scripts de variáveis: [documentação oficial do Postman](https://learning.postman.com/docs/tests-and-scripts/write-scripts/postman-sandbox-reference/pm-variables/).
