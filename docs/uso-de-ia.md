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
