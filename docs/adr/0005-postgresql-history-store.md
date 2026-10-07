# ADR 0005: manter o histórico operacional no PostgreSQL

## Status

Aceita até que métricas demonstrem necessidade de outro armazenamento.

## Contexto

As transações e seus estados participam do fluxo de idempotência, outbox, concorrência e consulta por cliente. Não há neste projeto volume observado, padrão de consulta de histórico nem requisito de retenção que demonstre gargalo no PostgreSQL.

## Decisão

Manter PostgreSQL como fonte de verdade para transações e seus estados. Não adicionar DynamoDB/MongoDB apenas para cumprir uma lista de tecnologias. Antes de adotar NoSQL, coletar volume, taxa de escrita, distribuição das consultas, retenção, p95/p99 e custo; então definir um caso de uso isolado (por exemplo, eventos imutáveis de auditoria) e aceitar consistência eventual por contrato.

## Consequências

Há uma única autoridade para o estado transacional e a outbox mantém atomicidade local. Uma futura projeção de histórico pode ser reconstruída por eventos, mas exige política de retenção, idempotência do consumidor, reconciliação e decisão explícita sobre dados pessoais.
