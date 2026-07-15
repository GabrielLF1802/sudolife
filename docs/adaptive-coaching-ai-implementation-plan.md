# Plano de Implementação da IA para Adaptive Coaching

## Objetivo

Este documento define quando introduzir uma IA open source no Sudolife e em qual ordem implementar as issues relacionadas ao Adaptive Coaching.

A IA não será a autoridade sobre segurança, progressão, metas, lesões ou persistência. Ela produzirá propostas estruturadas dentro de restrições calculadas pelo backend, conforme definido no [ADR 0005](adr/0005-backend-authoritative-adaptive-coaching.md).

## Decisão

O container da IA não deve ser criado antes da preparação dos dados e das restrições de coaching. Ele deve ser introduzido durante a implementação da issue [#23](https://github.com/GabrielLF1802/sudolife/issues/23), depois que o contexto estruturado, o contrato do provider e o validador determinístico estiverem definidos.

A sequência recomendada é:

1. [#31 — Extended Running History Context For Adaptive Coaching](https://github.com/GabrielLF1802/sudolife/issues/31)
2. [#24 — Running Availability Schedules Adaptive Running Plans](https://github.com/GabrielLF1802/sudolife/issues/24)
3. Definir o contrato do provider, os schemas e o validador determinístico
4. Configurar o modelo open source em container e implementar seu adapter
5. Concluir [#23 — AI-Drafted Adaptive Running Plan With Backend Validation](https://github.com/GabrielLF1802/sudolife/issues/23)
6. [#25 — Persist Adaptive Running Plan History](https://github.com/GabrielLF1802/sudolife/issues/25)
7. [#26 — Adapt The Next Planned Session](https://github.com/GabrielLF1802/sudolife/issues/26)
8. [#27 — Match Imported Runs To Planned Sessions](https://github.com/GabrielLF1802/sudolife/issues/27)
9. [#28 — Handle Missed Sessions And Extra Running Activities](https://github.com/GabrielLF1802/sudolife/issues/28)
10. [#29 — Clear Injury Concern And Resume Conservatively](https://github.com/GabrielLF1802/sudolife/issues/29)
11. [#30 — Use Post-Session Perceived Effort For Adaptation](https://github.com/GabrielLF1802/sudolife/issues/30)

## Por que preparar o contexto primeiro

A issue #31 deve ser concluída antes da integração com a IA porque separa consistência recente de experiência histórica e produz informações estruturadas como:

- volume semanal;
- frequência de corrida;
- maior corrida;
- ritmo representativo, quando disponível;
- tendência de volume;
- classificação de histórico recente suficiente.

Essas informações devem ser calculadas deterministicamente pelo backend. O modelo não deve receber atividades brutas para inferir conceitos que o domínio já consegue calcular de forma verificável.

A issue #24 deve ser concluída preferencialmente antes da #23 porque Running Availability é uma restrição relevante para a distribuição das sessões. Implementá-la depois exigiria alterar novamente o `TrainingSnapshot`, os schemas, o prompt, a validação e os testes do provider.

## Implementação interna da issue #23

A issue #23 deve ser dividida nos seguintes passos:

1. Construir o `TrainingSnapshot` estruturado na aplicação.
2. Criar uma porta requerida, como `AiRunningPlanProvider`.
3. Definir um schema estrito para o `AI Plan Proposal`.
4. Implementar o validador determinístico do backend.
5. Testar o caso de uso inicialmente com um provider fake ou stub.
6. Escolher e configurar o modelo open source em container.
7. Implementar o adapter de saída que chama o container por HTTP.
8. Tratar resposta inválida, indisponibilidade, timeout e política limitada de retry.
9. Integrar o plano aceito e sua explicação com o frontend.

## Fronteira arquitetural

```text
Adapter de entrada
        |
        v
Provided Port
        |
        v
Caso de uso de geração
        |
        +--> TrainingSnapshot
        |
        +--> AiRunningPlanProvider --------> Adapter HTTP --------> Container da IA
        |                                         |
        |                                         v
        |                                  AI Plan Proposal
        |                                         |
        +<----------------------------------------+
        |
        v
Validador determinístico
        |
        v
Plano aceito, ajustado ou rejeitado
```

`AiRunningPlanProvider` deve ser uma porta requerida da aplicação. A tecnologia de inferência, o cliente HTTP e os detalhes do container devem permanecer no adapter de saída. A aplicação não deve depender do modelo, do servidor de inferência ou de uma biblioteca específica de IA.

## Responsabilidades do backend

O backend permanece responsável por:

- classificar Sufficient Running History;
- selecionar Safe Running Milestone;
- preservar Long-Term Running Goal;
- calcular limites máximos de progressão;
- limitar sessões intensas;
- aplicar Coaching Heart-Rate Zones e limites de Perceived Effort;
- aplicar Injury Concern e Low Readiness;
- validar todas as sessões propostas;
- ajustar problemas menores considerados seguros;
- rejeitar ou solicitar novamente propostas estruturalmente inválidas;
- persistir o plano aceito e seu histórico.

## Responsabilidades da IA

A IA pode:

- distribuir sessões dentro das restrições fornecidas;
- propor uma variação permitida de Planned Session Types;
- produzir um `AI Plan Proposal` estruturado;
- escrever uma Plan Explanation amigável ao usuário.

A IA não pode:

- decidir se uma Running Goal é segura;
- substituir a Safe Running Milestone escolhida pelo backend;
- ultrapassar limites de progressão ou intensidade;
- ignorar Injury Concern ou Low Readiness;
- definir Coaching Heart-Rate Zones;
- persistir ou aceitar diretamente um plano.

## Estratégia de integração com o container

O container deve ser tratado como dependência externa e substituível. A configuração deve permitir trocar modelo e servidor de inferência sem alterar os casos de uso.

As configurações esperadas incluem:

- URL do provider;
- identificador do modelo;
- timeout de conexão e resposta;
- limite de tentativas;
- parâmetros de geração controlados;
- versão do schema de proposta.

O primeiro corte deve usar resposta estruturada validável. Texto livre não deve ser utilizado como fonte do plano. A Plan Explanation pode ser textual, mas somente os campos estruturados e validados determinam o comportamento do coaching.

## Estratégia de testes

Os testes da aplicação não devem depender do container real. Eles devem usar fake ou stub da porta requerida para cobrir:

- proposta válida;
- ajuste seguro pelo backend;
- rejeição de proposta insegura;
- resposta fora do schema;
- falha do provider;
- Injury Concern prevalecendo sobre a proposta;
- ausência de métricas opcionais no `TrainingSnapshot`.

Os testes do adapter devem verificar separadamente:

- serialização do `TrainingSnapshot`;
- desserialização do `AI Plan Proposal`;
- timeout;
- indisponibilidade;
- resposta inválida;
- compatibilidade com a versão configurada do schema.

Um teste de integração ou smoke test pode usar o container real, mas a suíte principal deve continuar rápida, determinística e independente da disponibilidade do modelo.

## Critério para iniciar o container

O trabalho no container pode começar quando estes itens estiverem estáveis:

- o histórico estendido da #31 estiver disponível;
- Running Availability da #24 estiver definida ou tiver uma decisão explícita de adiamento;
- o `TrainingSnapshot` possuir schema conhecido;
- a porta `AiRunningPlanProvider` estiver definida;
- o `AI Plan Proposal` possuir schema versionado;
- o validador determinístico possuir testes sem depender da IA real.

Nesse ponto, o container deixa de orientar o design da aplicação e passa a implementar um contrato já controlado pelo Sudolife.
