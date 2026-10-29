# Easy Eats — Backend + Ambiente Docker

API REST do Easy Eats (Spring Boot 4, Java 25, PostgreSQL). Este repositório também contém o
`docker-compose.yaml` que sobe a aplicação **completa**: banco, backend e frontend
([Easy-Eats](https://github.com/ZacML/Easy-Eats)).

## Arquitetura

```
Navegador ──► web (nginx :80) ──/api/*──► api (Spring Boot :8080) ──► db (PostgreSQL :5432)
              publicado em 4200           publicado em 8080            publicado em 5434
```

- Os três serviços ficam na rede `easy-eats-net` e se falam **pelo nome do serviço** (`db`, `api`);
  nenhum deles usa `localhost` para falar com outro container.
- O frontend chama a API em `/api`; o nginx remove o prefixo e encaminha para `http://api:8080`.
  Como o navegador só enxerga uma origem, **não há problema de CORS**.
- A ordem de subida é garantida por healthchecks: `db` saudável → `api` saudável → `web`.
- Dados do banco persistem no volume `pgdata`.

## Pré-requisitos

- Docker Engine/Desktop com **Docker Compose v2** (`docker compose version`)
- Acesso à internet no primeiro build (imagens base, dependências Maven/npm e o código do frontend no GitHub)
- Portas livres no host: `4200`, `8080` e `5434` (configuráveis, veja abaixo)

## Configuração (opcional)

Sem nenhuma configuração o ambiente sobe com valores de **desenvolvimento**. Para alterar portas,
senha do banco ou segredo JWT:

```bash
cp .env.example .env     # edite os valores; o .env não é versionado
```

| Variável | Padrão | Uso |
|---|---|---|
| `WEB_PORT` / `API_PORT` / `DB_PORT` | `4200` / `8080` / `5434` | Portas publicadas no host |
| `DB_NAME` / `DB_USER` / `DB_PASSWORD` | `easy_eats` / `postgres` / `easyeats` | Banco de dados |
| `JWT_SECRET` | valor de desenvolvimento | Base64, mínimo 32 bytes (`openssl rand -base64 32`) |
| `FRONTEND_CONTEXT` | repositório do frontend no GitHub | Use `../Easy-Eats` para buildar uma cópia local |

> Em produção, troque `DB_PASSWORD` e `JWT_SECRET`. Os padrões são públicos neste repositório.

## Inicialização

```bash
git clone -b feat/docker https://github.com/ZacML/Easy-Eats-Api-2.git
cd Easy-Eats-Api-2
docker compose up -d --build
```

O primeiro build leva alguns minutos. Acompanhe com `docker compose ps` até `db`, `api` e `web`
ficarem `healthy`/`running`. Depois acesse:

- Aplicação: http://localhost:4200
- API direta: http://localhost:8080 (health: http://localhost:8080/actuator/health)

### Scripts do banco

Ficam em `db/init/` e são executados pelo PostgreSQL **apenas na primeira inicialização**
(volume vazio), em ordem alfabética:

| Arquivo | Função |
|---|---|
| `01-schema.sql` | Cria as tabelas (espelha as entidades JPA). Usa `IF NOT EXISTS`. |
| `02-seed.sql` | Cria as roles `ADMIN`, `GERENTE` e `FUNCIONARIO`. O cadastro de usuário (`POST /usuarios`) exige que elas existam. |

O Hibernate continua com `ddl-auto=update` como rede de segurança. Para reexecutar os scripts
do zero: `docker compose down -v && docker compose up -d --build` (**apaga os dados**).

## Testes

Em Linux/macOS/Git Bash (no PowerShell use `curl.exe`):

```bash
# 1. Containers saudáveis
docker compose ps

# 2. Frontend responde
curl -I http://localhost:4200

# 3. Banco inicializado (13 tabelas e 3 roles)
docker compose exec db psql -U postgres -d easy_eats -c "select * from role;"

# 4. Frontend -> Backend pelo proxy /api (deve responder {"status":"UP"})
curl http://localhost:4200/api/actuator/health

# 5. Criar usuário (rota pública) e fazer login (retorna o token JWT)
curl -X POST http://localhost:4200/api/usuarios \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123","roles":["ADMIN"]}'

TOKEN=$(curl -s -X POST "http://localhost:4200/api/auth/login?username=admin&password=admin123")

# 6. Rota autenticada: criar e listar mesa
curl -X POST http://localhost:4200/api/mesa -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"numero":1}'
curl http://localhost:4200/api/mesa -H "Authorization: Bearer $TOKEN"

# 7. Persistência: derrube e suba de novo; o usuário e a mesa continuam lá
docker compose down
docker compose up -d
curl http://localhost:4200/api/usuarios
```

Comunicação por nome de serviço (de dentro do container do frontend):

```bash
docker compose exec web wget -qO- http://api:8080/actuator/health
```

## Operação

```bash
docker compose ps                 # estado e saúde dos serviços
docker compose logs -f            # logs de todos (ou: logs -f api)
docker compose stop               # para sem remover
docker compose start              # retoma
docker compose up -d --build      # reconstrói após alterar código
```

## Encerramento

```bash
docker compose down               # remove containers e rede; MANTÉM os dados do banco
docker compose down -v            # remove também o volume (apaga os dados)
```

## Problemas comuns

- **Porta em uso:** altere `WEB_PORT`, `API_PORT` ou `DB_PORT` no `.env`.
- **`api` demora a ficar saudável:** a primeira inicialização do Spring leva até ~40 s; veja `docker compose logs api`.
- **Mudei o schema e nada mudou:** os scripts só rodam com o volume vazio (`docker compose down -v`).
- **Login pela tela do frontend falha:** o backend espera `POST /auth/login?username=&password=`,
  mas o Angular envia JSON `{email, senha}`. É uma divergência de contrato entre os projetos,
  independente do Docker; os testes acima usam a API diretamente.
