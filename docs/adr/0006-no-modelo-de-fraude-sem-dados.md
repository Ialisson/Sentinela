# ADR 0006: não servir modelo de fraude sem dados representativos

## Status

Aceita para o estágio atual do projeto.

## Contexto

O repositório não contém conjunto rotulado de transações, definição de fraude confirmada, avaliação temporal nem baseline que permita medir precisão, recall, falsos positivos ou deriva. Gerar dados sintéticos e apresentá-los como desempenho de produção seria enganoso.

## Decisão

Manter as regras explicáveis atuais como baseline e não introduzir um serviço de IA ainda. Retomar quando houver dados legalmente utilizáveis, rótulos com atraso conhecido, separação temporal de treino/validação, comparação com baseline, versionamento de artefatos e monitoramento de qualidade e deriva. O modelo deve inicialmente atuar em modo sombra e não decidir bloqueios sem validação operacional.

## Consequências

O fluxo permanece simples e auditável. A próxima etapa de IA depende de governança e qualidade dos dados, não de um algoritmo arbitrário.
