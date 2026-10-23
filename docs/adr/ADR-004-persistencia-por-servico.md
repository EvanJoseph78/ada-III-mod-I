# ADR-004 — Persistência e limites de dados por serviço

Status: aceita
Data: 2026-10-22

## Contexto

Empregados e Vagas mantinham listas em memória; Candidaturas retornava sucesso sem gravar a inscrição. O CORE exige serviços com bases segregadas. A infraestrutura já define três bancos e usuários PostgreSQL, conforme ADR-001. A ADR-002 prevê testes com H2 sem Docker.

As APIs iniciais usam matrícula e ID numéricos, enquanto os contratos de integração usam UUID. Esta etapa precisa persistir dados mínimos sem antecipar a avaliação assíncrona ou alterar os contratos compartilhados.

## Decisão

- Usar Spring Data JPA para persistência e Flyway para migrações versionadas em cada serviço. Hibernate apenas valida o schema (`ddl-auto: validate`).
- Empregados possui o cadastro, a matrícula única e os itens do perfil; Vagas possui o cadastro, o status e os requisitos. Itens e requisitos são coleções de valores pertencentes ao respectivo cadastro.
- Manter os identificadores numéricos existentes na API e acrescentar UUIDs estáveis de empregado e vaga para os contratos. O ID numérico da vaga passa a ser gerado pelo banco.
- Candidaturas possui UUID, matrícula, ID numérico da vaga e data de registro; a combinação matrícula/vaga é única no banco. Essa unicidade evita inscrições repetidas, mas não comprova idempotência de consumidor Kafka.
- Não compartilhar entidades JPA nem criar chaves estrangeiras entre bancos. As referências externas de Candidaturas são valores locais, sem consultas diretas aos outros bancos.
- Expor perfil e critérios pelos DTOs de `shared-contracts`, mantendo o prefixo real das APIs `/api/v1`.
- Executar testes HTTP/JPA na JVM com H2 em modo PostgreSQL, aplicando as migrações reais. O H2 fica restrito ao escopo de teste.

## Alternativas consideradas

| Alternativa | Benefício | Motivo para não ser a escolha |
| --- | --- | --- |
| Listas em memória | Simplicidade do protótipo | Perdem os dados ao reiniciar e não exercitam as bases segregadas |
| Schema compartilhado com chaves entre serviços | Integridade referencial centralizada | Acopla os serviços por dados e contraria o corte de domínio |
| Hibernate criando/atualizando tabelas automaticamente | Menos configuração inicial | Não mantém um histórico explícito e revisável das mudanças de schema |
| Substituir imediatamente todos os IDs numéricos por UUID | Modelo único de identificação | Quebra as entradas atuais; manter ambos permite evolução gradual sem mudar `shared-contracts` |

## Consequências e trade-offs

- Os dados são armazenados nos bancos próprios e não dependem do ciclo de vida de listas Java.
- Migrações aplicadas não devem ser editadas; evoluções exigem novos arquivos versionados.
- Restrições únicas protegem matrícula e inscrição mesmo diante de requisições concorrentes. Duplicatas retornam HTTP 409; entradas inválidas retornam 400 e consultas inexistentes retornam 404.
- Manter identificador numérico e UUID exige documentar a diferença: consulta básica de empregado usa matrícula, consulta básica de vaga usa `idVaga`, perfil usa `empregadoId` e critérios usam `vagaId`.
- A inscrição ainda não verifica existência do empregado, abertura da vaga ou critérios por HTTP. Essa integração deverá resolver as referências para UUID e capturar os snapshots antes de publicar `CandidaturaRealizada`. Portanto, esta etapa entrega o registro persistente, não o fluxo completo de inscrição/avaliação.
- A exclusão de vagas não propaga mudanças para Candidaturas. Política de exclusão e transições de status serão tratadas na evolução do fluxo.
- H2 não reproduz todas as diferenças do PostgreSQL. É necessária validação com os bancos reais; não foi possível nesta sessão porque Docker não estava disponível.

## Evidências

- Entidades, repositórios, configurações e migrações em cada serviço.
- `EmpregadosPersistenceTest`, `VagasPersistenceTest` e `CandidaturasPersistenceTest`.
- [Resultado e roteiro de verificação](../evidencias/persistencia.md).
