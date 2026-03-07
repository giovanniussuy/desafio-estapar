# desafio-estapar

Backend em Java 21 + Spring Boot para controle de estacionamento com:
- ingestão de configuração da garagem no startup (`GET /garage` no simulador)
- webhook de eventos (`ENTRY`, `PARKED`, `EXIT`)
- cálculo de faturamento por setor e data (`GET /revenue`)

## 1. Pré-requisitos

Instale:
- Java 21
- Docker Desktop
- Git

Opcional (não obrigatório):
- MySQL Workbench / DBeaver para inspecionar o banco

## 2. Clonar o projeto

```bash
git clone <url-do-repo>
cd desafio-estapar
```

## 3. Subir dependências locais (MySQL + simulador)

### 3.1 MySQL

```bash
docker run -d --name desafio-estapar-mysql -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=desafio_estapar -p 3306:3306  mysql:8.4
```

### 3.2 Simulador de garagem

```bash
docker run -d --name garagem-sim --network="host" cfontes0estapar/garage-sim:1.0.0
```

Se estiver no Windows e `--network="host"` não funcionar no seu ambiente Docker, rode:

```bash
docker run -d --name garagem-sim -p 3000:3000 cfontes0estapar/garage-sim:1.0.0
```

## 4. Configuração da aplicação

A aplicação já possui defaults em `src/main/resources/application.properties`:
- porta da API: `3003`
- MySQL: `jdbc:mysql://localhost:3306/desafio_estapar`
- usuário/senha: `root/root`
- simulador: `http://localhost:3000`

Se quiser sobrescrever, use variáveis de ambiente:
- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `SIMULATOR_BASE_URL`

Ideal trabalhar com secrets para chaves sensíveis, mas para simplicidade do desafio, variáveis de ambiente já são suficientes.

## 5. Subir a API

No diretório do projeto:

### Windows (PowerShell)

```powershell
.\mvnw.cmd spring-boot:run
```

### Linux/macOS

```bash
./mvnw spring-boot:run
```

No startup, a aplicação busca o `GET /garage` do simulador e persiste setores/vagas no MySQL.

## 6. Testar rapidamente

### 6.1 Webhook ENTRY

```bash
curl -X POST http://localhost:3003/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "license_plate": "ZUL0001",
    "entry_time": "2025-01-01T12:00:00.000Z",
    "event_type": "ENTRY"
  }'
```

### 6.2 Webhook PARKED

```bash
curl -X POST http://localhost:3003/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "license_plate": "ZUL0001",
    "lat": -23.561684,
    "lng": -46.655981,
    "event_type": "PARKED"
  }'
```

### 6.3 Webhook EXIT

```bash
curl -X POST http://localhost:3003/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "license_plate": "ZUL0001",
    "exit_time": "2025-01-01T13:40:00.000Z",
    "event_type": "EXIT"
  }'
```

### 6.4 Consultar receita

```bash
curl "http://localhost:3003/revenue?date=2025-01-01&sector=A"
```

Resposta esperada (exemplo):

```json
{
  "amount": 20.00,
  "currency": "BRL",
  "timestamp": "2026-03-04T13:00:00.000Z"
}
```

## 7. Rodar testes

### Windows (PowerShell)

```powershell
.\mvnw.cmd test
```

### Linux/macOS

```bash
./mvnw test
```

## 8. Encerrar ambiente

```bash
docker stop desafio-estapar-mysql garagem-sim
docker rm desafio-estapar-mysql garagem-sim
```
