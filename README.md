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
- Bruno (cliente API), para usar os cenários prontos em `util/client_bruno`

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

Observação: a imagem não possui tag `latest`. Use `:1.0.0`.

## 4. Configuração da aplicação

A aplicação já possui defaults em `src/main/resources/application.properties`:
- porta da API: `3003`
- MySQL: `jdbc:mysql://localhost:3306/desafio_estapar`
- usuário/senha: `root/root`
- simulador: `http://localhost:3000`
- timezone de receita: `America/Sao_Paulo`

Se quiser sobrescrever, use variáveis de ambiente:
- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `SIMULATOR_BASE_URL`
- `SIMULATOR_BOOTSTRAP_REQUIRED` (`true`/`false`)
- `APP_REVENUE_TIME_ZONE` (ex.: `America/Sao_Paulo`)

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

Se quiser subir a API sem simulador (modo desenvolvimento), rode com:

```powershell
$env:SIMULATOR_BOOTSTRAP_REQUIRED="false"
.\mvnw.cmd spring-boot:run
```

ou em Linux/macOS:

```bash
SIMULATOR_BOOTSTRAP_REQUIRED=false ./mvnw spring-boot:run
```

## 6.1 Subir em modo debug

Debug remoto na porta `5005`:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

Se quiser pausar o startup até conectar o debugger (`suspend=y`):

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
```

## 7. Testar rapidamente (curl)

### 7.1 Webhook ENTRY

```bash
curl -X POST http://localhost:3003/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "license_plate": "ZUL0001",
    "entry_time": "2025-01-01T12:00:00.000Z",
    "event_type": "ENTRY"
  }'
```

### 7.2 Webhook PARKED

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

### 7.3 Webhook EXIT

```bash
curl -X POST http://localhost:3003/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "license_plate": "ZUL0001",
    "exit_time": "2025-01-01T13:40:00.000Z",
    "event_type": "EXIT"
  }'
```

### 7.4 Consultar receita

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

## 8. Usando o Bruno (`util/client_bruno`)

Existe uma collection pronta em `util/client_bruno/estapar`.

### 8.1 Importar e selecionar ambiente

1. Abra o Bruno.
2. `Open Collection` apontando para `util/client_bruno/estapar`.
3. Selecione o ambiente `local` (`util/client_bruno/estapar/environments/local.yml`).

### 8.2 Sequência recomendada de requests

Execute na ordem:
1. `garage` (consulta o simulador em `localhost:3000`)
2. `Webhook ENTRY`
3. `Webhook PARKED`
4. `Webhook EXIT`
5. `consulta receita`

### 8.3 Dicas para `client_bruno`

- A variável `posicao1` do ambiente `local` é injetada no body do `Webhook PARKED`.
- Se o simulador retornar coordenadas diferentes, atualize `posicao1` com um par `lat/lng` válido.
- A receita só é gravada no `EXIT`. Se pular esse evento, `/revenue` retorna `0`.
- A data do `/revenue` deve bater com a data calculada no timezone da aplicação (`APP_REVENUE_TIME_ZONE`, default `America/Sao_Paulo`).

## 9. Rodar testes

### Windows (PowerShell)

```powershell
.\mvnw.cmd test
```

### Linux/macOS

```bash
./mvnw test
```

## 10. Encerrar ambiente

```bash
docker stop desafio-estapar-mysql garagem-sim
docker rm desafio-estapar-mysql garagem-sim
```
