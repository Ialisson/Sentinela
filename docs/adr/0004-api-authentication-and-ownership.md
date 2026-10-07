# ADR 0004: autenticação stateless e propriedade por cliente

## Status

Aceita para a API de demonstração.

## Contexto

Uma consulta de transação não pode depender apenas de um identificador difícil de adivinhar. A API precisa autenticar clientes e impedir que um cliente leia dados de outro.

## Decisão

- Usar HTTP Basic sem sessão para manter o exemplo simples e suportar vários clientes configurados.
- Receber credenciais por `API_CLIENTS_JSON` ou, para um único cliente local, por `API_USERNAME` e `API_PASSWORD`.
- Codificar senhas em BCrypt no processo; não armazenar senhas em tabelas da aplicação.
- Usar o principal autenticado como `client_id` dono da transação e escopo da chave de idempotência.
- Responder `404` quando uma transação não pertence ao cliente, sem revelar se outro cliente a possui.
- Publicar a API somente atrás de TLS fora do desenvolvimento local. A configuração de demonstração não substitui um provedor de identidade ou um cofre de segredos.

## Consequências

Clientes precisam enviar credenciais em cada chamada, e o sistema permanece stateless entre réplicas. A lista de credenciais é carregada na inicialização; rotação exige atualizar a configuração e reiniciar o processo. Para produção com gestão avançada de usuários, scopes e rotação, integrar um provedor OIDC/OAuth2.
