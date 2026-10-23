# Uso crítico de IA

## Contribuições

Organização do roteiro e criação da estrutura inicial a partir do escopo e do enunciado fornecidos.

## Revisão humana

Pendente: validar responsabilidades, fluxo assíncrono e estrutura gerada com o grupo.

## Registro de desenvolvimento

Registrar para cada contribuição relevante: tarefa, sugestão da IA, decisão humana, correções e verificação executada. Não apresentar testes ou funcionalidades pendentes como concluídos.

### 2026-10-22 — Organização dos POMs

- **Tarefa:** revisar a relação entre o POM raiz e os POMs dos serviços após a criação das aplicações Spring Boot.
- **Sugestão da IA:** centralizar o parent do Boot e a configuração Java na raiz, fazendo os serviços herdarem de `selecao-interna`.
- **Análise crítica:** a configuração anterior era válida; o ajuste é uma escolha de manutenção, não uma condição obrigatória para compilar. Alternativas e consequências estão na [ADR-003](adr/ADR-003-heranca-maven.md).
- **Decisão humana:** o responsável pelo repositório autorizou a padronização e limitou a alteração aos POMs. As correções funcionais ficaram para o autor do código.
- **Correções de escopo:** mudanças de código e testes inicialmente adicionados pela IA foram removidos antes da validação final.
- **Verificação pela IA:** `mvn -o clean verify` na raiz, sem Docker, com sucesso em todos os módulos após a remoção das alterações funcionais. Não havia testes automatizados na versão final; build aprovado não comprova regras de negócio, integração ou compatibilidade dos contratos.
- **Revisão humana pendente:** conferir o diff final e revisar com o grupo as consequências da herança compartilhada antes do merge. Não se atribui ao grupo execução manual das verificações realizadas pela IA.

### 2026-10-22 — Persistência mínima dos serviços

- **Tarefa:** preparar a primeira etapa funcional do CORE: empregado com perfil, vaga com requisitos e candidatura registrada em bancos separados.
- **Decisão humana:** o responsável autorizou assumir a implementação nesta etapa, ampliando o escopo antes limitado aos POMs. A divisão com o autor inicial deve ser alinhada pelo responsável.
- **Sugestão e implementação pela IA:** JPA, migrações Flyway por serviço, validação de entradas e restrições únicas; preservação das entradas numéricas e adição de UUIDs para os contratos. Decisões e alternativas estão na [ADR-004](adr/ADR-004-persistencia-por-servico.md).
- **Verificação pela IA:** build completo e seis testes HTTP/JPA em H2 aprovados sem Docker; detalhes em [persistencia.md](evidencias/persistencia.md).
- **Limites e revisão pendente:** Docker não estava disponível nesta sessão. O grupo deve executar o roteiro PostgreSQL e conferir a permanência dos dados após reiniciar. A candidatura ainda não valida referências externas nem publica eventos. Não tratar unicidade de inscrição como evidência de consumidor idempotente.
