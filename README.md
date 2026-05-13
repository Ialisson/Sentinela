# Sentinela

Transaction Risk Detection Platform for e-commerce fraud prevention.

## Overview

Sentinela is a backend platform designed to analyze e-commerce transactions and classify fraud risk based on scoring strategies.

The system evaluates:

- Transaction amount
- Country risk
- Multiple card attempts
- New account behavior

## Tech Stack

- Java 21
- Spring Boot
- Maven
- REST API
- Swagger/OpenAPI

## Features

- Risk score calculation
- Risk level classification
- Fraud recommendation engine
- Strategy pattern implementation
- API documentation with Swagger

## Endpoint

POST `/api/v1/risk/analyze`

### Example Request

```json
{
  "transactionId": "TXN001",
  "userId": "USER123",
  "amount": 9500,
  "country": "NG",
  "ipAddress": "192.168.1.100",
  "cardAttempts": 5,
  "emailAgeDays": 2
}