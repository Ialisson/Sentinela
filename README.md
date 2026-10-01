# Sentinela

API REST para avaliação determinística de risco de transações de comércio eletrônico. O projeto é stateless: recebe os dados, aplica regras de pontuação e retorna a decisão e as regras acionadas. Não persiste transações.

## Requisitos

- Java 21
- Maven (ou Maven Wrapper incluído)

## Executar

```bash
./mvnw spring-boot:run
```

No Windows, use `mvnw.cmd spring-boot:run`. A documentação OpenAPI fica disponível em `/swagger-ui.html`.

## Analisar uma transação

`POST /api/v1/risk/analyze` com `Content-Type: application/json`:

```json
{
  "transactionId": "TXN001",
  "userId": "USER123",
  "amount": 9500.00,
  "country": "NG",
  "ipAddress": "192.168.1.100",
  "cardAttempts": 5,
  "emailAgeDays": 2
}
```

Resposta:

```json
{
  "riskScore": 100,
  "riskLevel": "HIGH",
  "recommendedAction": "BLOCK",
  "triggeredRules": [
    "FOREIGN_COUNTRY",
    "HIGH_AMOUNT",
    "MULTIPLE_CARD_ATTEMPTS",
    "NEW_ACCOUNT"
  ]
}
```

O score é limitado a 100. O campo `amount` deve estar em BRL. As faixas são `LOW` (0–39, `APPROVE`), `MEDIUM` (40–69, `REVIEW`) e `HIGH` (70–100, `BLOCK`). Os pesos atuais são: valor acima de BRL 5.000 (+40), país diferente de BR (+20), três ou mais tentativas de cartão (+25) e e-mail com menos de sete dias (+30). As regras são componentes independentes e podem ser adicionadas sem alterar o serviço de análise.

`transactionId`, `userId`, `amount`, `country` e `cardAttempts` são obrigatórios. O valor deve ser positivo, ter no máximo duas casas decimais, `country` deve conter um código de país de duas letras, as tentativas devem ser pelo menos uma e `emailAgeDays`, quando informado, não pode ser negativo. Erros de validação retornam HTTP 400 no formato Problem Details, com detalhes por campo em `errors`.

## Verificação

```bash
./mvnw test
```
