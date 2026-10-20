# Sistema de Gestão de Seleção Interna e Movimentação de Empregados

## Visão Geral

O projeto consiste em uma plataforma para gerenciamento completo dos processos de seleção interna de empregados, permitindo a publicação de vagas, inscrição de candidatos, avaliação automática dos requisitos e acompanhamento das etapas do processo seletivo.

O objetivo é centralizar informações profissionais dos empregados, aumentar a transparência dos processos seletivos e automatizar a classificação dos candidatos conforme critérios definidos para cada vaga.

---

## Problema Resolvido

Atualmente, processos de seleção interna podem envolver diversas fontes de informação, exigindo análise manual de currículos, certificações e experiências profissionais.

O sistema proposto concentra essas informações em um único ambiente e realiza automaticamente o cálculo da pontuação dos candidatos conforme os requisitos de cada vaga.

---

## Funcionalidades Principais

### Gestão do Perfil Profissional

Cada empregado poderá cadastrar e manter atualizado:

- Capacitações internas;
- Diplomas;
- Cursos;
- Certificações;
- Experiência profissional interna;
- Experiência profissional externa.

---

### Gestão de Vagas

Os gestores poderão:

- Criar vagas internas;
- Informar unidade de lotação;
- Definir requisitos obrigatórios;
- Definir critérios de pontuação;
- Definir período de inscrição;
- Publicar resultados.

---

### Inscrição em Vagas

Os empregados poderão:

- Consultar vagas disponíveis;
- Realizar inscrições;
- Acompanhar sua situação no processo seletivo.

---

### Avaliação Automática

O sistema será responsável por:

- Verificar requisitos obrigatórios;
- Comparar currículo do candidato com os critérios da vaga;
- Calcular pontuação automaticamente;
- Gerar ranking dos candidatos.

---

### Acompanhamento do Processo

Cada vaga possuirá um fluxo de status:

1. Aberta para inscrições
2. Em avaliação curricular
3. Resultado publicado
4. Processo concluído

Os candidatos poderão acompanhar em tempo real a evolução do processo.

---

## Arquitetura Proposta

### Serviço de Gestão de Empregados

Responsável por:

- Dados profissionais;
- Certificações;
- Cursos;
- Experiências.

### Serviço de Vagas

Responsável por:

- Cadastro de vagas;
- Critérios de avaliação;
- Publicação de resultados.

### Serviço de Candidaturas

Responsável por:

- Inscrições;
- Cálculo de pontuação;
- Ranking dos candidatos.

---

## Aplicação dos Conceitos do Curso

### Comunicação Assíncrona

Quando um empregado se inscrever em uma vaga:

- Um evento de candidatura será publicado.
- Um consumidor realizará o cálculo da pontuação.
- O processo ocorrerá de forma assíncrona.

### Cache

Consultas frequentes de:

- Perfil do empregado;
- Ranking de vagas;
- Resultados das seleções;

serão armazenadas em cache para reduzir carga no banco.

### Kafka

Eventos de domínio:

- Candidatura realizada;
- Avaliação concluída;
- Resultado publicado.

Serão disponibilizados para múltiplos consumidores.

### Resiliência

Implementação de:

- Retry com backoff;
- Dead Letter Queue (DLQ);
- Consumidores idempotentes.

---

## Benefícios Esperados

- Maior transparência dos processos internos;
- Redução de trabalho manual dos gestores;
- Critérios de seleção mais objetivos;
- Melhor aproveitamento dos talentos internos;
- Histórico completo de participações e resultados;
- Escalabilidade para milhares de empregados e vagas.

---

## Possíveis Evoluções

- Integração com sistemas corporativos de RH;
- Recomendação de vagas utilizando IA;
- Sugestão automática de capacitações para evolução profissional;
- Dashboard gerencial de movimentação interna;
- Ranking de aderência entre empregado e vaga.