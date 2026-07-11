[English](README.md) | Português

# back-template-spring

Template de backend em Spring Boot: Java 17, Maven, PostgreSQL + Flyway + Spring Data JPA, autenticação JWT via Spring Security, e um domínio de referência `Note` (CRUD) pra copiar em novos recursos. Irmão de `back-template-laravel` e `back-template-nest`.

## Features

- Autenticação JWT com access tokens de vida curta e refresh tokens rotativos e revogáveis (o refresh token é hasheado e persistido pra poder ser revogado de verdade, não só deixado expirar)
- Fluxos de verificação de email e reset de senha, com fallback de console pro email em dev, então não tem nada pra configurar pra testar localmente
- Controle de acesso por role (`USER` / `ADMIN`) aplicado com `@PreAuthorize`, mais uma rota admin-only de referência
- Rate limiting em `/auth/login` e `/auth/register` (Bucket4j, token bucket em memória por IP)
- Schema gerenciado pelo Flyway com `hibernate.ddl-auto=validate`, então nada desalinha silenciosamente
- Docs OpenAPI e Swagger UI gerados a partir das anotações dos controllers
- App e Postgres dockerizados via docker-compose, com healthcheck nos dois
- CI no GitHub Actions: checagem de formatação, testes unitários, build do Docker, testes de integração com Testcontainers
- Domínio de referência `Note` (CRUD) pra usar de modelo em novos recursos

## Stack

- Java 17, Spring Boot 3.5 (Web, Data JPA, Security, Validation, Actuator, Mail)
- PostgreSQL, migrations com Flyway
- JWT via `jjwt` 0.12.6
- Bucket4j pro rate limiting
- springdoc-openapi pra docs da API e Swagger UI
- Lombok
- JUnit 5 + Testcontainers pros testes de integração
- Spotless (Google Java Format) pra formatação
- Docker / docker-compose
- CI no GitHub Actions

## Estrutura do projeto

```
src/main/java/com/example/backtemplate/
├── account/        troca de senha, exclusão de conta
├── admin/          endpoints admin-only (listar usuários)
├── auth/           registro, login, refresh, JWT, usuários, roles
├── common/         entidade base, exceptions da API, tratamento global de erro
├── config/         security chain, filtro JWT, rate limiting, OpenAPI, app properties tipadas
├── email/          serviço SMTP + fallback de console pra dev
├── notes/          domínio de referência (CRUD)
└── BackTemplateApplication.java

src/main/resources/
├── application.yml, application-{dev,prod,test}.yml
└── db/migration/   migrations SQL do Flyway
```

## Pré-requisitos

Java 17, Maven (ou o `./mvnw` que já vem no repo), e Docker (pro Postgres, Testcontainers, e pra stack containerizada completa).

## Quickstart

```bash
cp .env.example .env
make docker-up      # sobe o Postgres
make db-migrate
make dev            # roda a app em :8080 (profile dev)
```

Ou roda a stack inteira containerizada (app + db):

```bash
docker compose up -d --build
```

## Scripts

| Comando | Faz |
|---|---|
| `make dev` | roda com o profile dev (`spring-boot:run`) |
| `make build` | empacota o jar |
| `make start` | roda o jar empacotado |
| `make lint` / `make format-check` | checa formatação (Spotless) |
| `make format` | aplica formatação (Spotless) |
| `make test` | só testes unitários |
| `make test-e2e` | testes de integração (Testcontainers, precisa de Docker) |
| `make db-migrate` | aplica as migrations do Flyway |
| `make db-generate` | cria um arquivo de migration vazio com timestamp |
| `make docker-up` / `make docker-down` | stack completa via compose |

## Endpoints

- `POST /auth/register`, `GET /auth/verify-email?token=`, `POST /auth/login`, `POST /auth/refresh`
- `POST /auth/forgot-password`, `POST /auth/reset-password`
- `PATCH /account/password`, `DELETE /account` (exige Bearer token)
- `GET/POST/PUT/DELETE /api/notes[/{id}]` (exige Bearer token) — CRUD de referência
- `GET /admin/users` (admin only) — referência pra proteger uma rota admin-only
- `GET /actuator/health`

## Roles

Todo usuário tem um `role` (`USER` ou `ADMIN`, default `USER`), carregado como claim assinada no JWT (`JwtService.generateAccessToken`) e transformado em `GrantedAuthority` `ROLE_*` pelo `JwtAuthFilter`. Nunca confie num `role` vindo do body da requisição. `GET /admin/users` é a referência pra proteger uma rota com `@PreAuthorize("hasRole('ADMIN')")` (precisa de `@EnableMethodSecurity` no `SecurityConfig`, já adicionado). Não tem jeito de virar admin sozinho: muda a coluna direto no banco pra testar localmente (`UPDATE users SET role = 'ADMIN' WHERE email = '...'`).

## Sessões

`login` e `refresh` retornam `{ accessToken, refreshToken }`, os dois JWTs (`JwtService`). O refresh token também tem um hash SHA-256 dele mesmo persistido na tabela `refresh_tokens` pra poder ser revogado de verdade; um JWT stateless puro não dá pra "des-emitir" antes de expirar. Validação de access token em toda outra requisição continua totalmente stateless. A consulta no banco só acontece nos caminhos `/auth/refresh` e `/auth/logout`.

`refresh` rotaciona: a linha antiga é apagada no momento que um novo par é emitido, então um refresh token roubado e reusado para de funcionar assim que o cliente legítimo fizer o próximo refresh. `logout` revoga um refresh token de vez e é idempotente (um token ausente ou já revogado ainda retorna 200). O JWT de refresh também carrega uma claim `jti` aleatória. Sem ela, dois tokens emitidos pro mesmo usuário dentro do mesmo segundo seriam idênticos byte a byte (claims de JWT têm precisão de segundo), o que derrubaria a rotação silenciosamente.

## Documentação da API

Docs OpenAPI gerados a partir das anotações de Bean Validation e das anotações `@Tag`/`@Operation`/`@SecurityRequirement` do [springdoc-openapi](https://springdoc.org/) nos controllers. Com o app rodando, abre o Swagger UI em `/swagger-ui/index.html` (spec bruto em `/v3/api-docs`): `http://localhost:8080` se você subiu com `make dev`, ou `http://localhost:8081` se estiver rodando a stack do docker-compose (a porta padrão do host em `.env.example`, sobrescreve com `PORT`). Usa o botão "Authorize" com um JWT de `/auth/login` pra testar as rotas protegidas (`account`, `api/notes`).

## Pre-commit hook

```bash
git config core.hooksPath .githooks
```

Roda `spotless:check` antes de cada commit.

## Env vars

Ver `.env.example` pra lista completa. `JWT_SECRET` é obrigatória (mín. 32 caracteres); a app falha rápido no boot se estiver ausente ou curta demais. Deixa `MAIL_HOST` sem valor em dev pra usar o fallback de console no envio de email transacional: tokens de verificação e reset ficam só no log em vez de serem enviados de verdade.

## Notas de design

- `hibernate.ddl-auto` é `validate` em todo lugar; o Flyway é dono do schema. Nunca troca pra `update` ou `create`, nem localmente, porque isso mascara silenciosamente migrations que você esqueceu de escrever.
- **O caminho de binding do `@ConfigurationProperties` precisa bater exatamente com o aninhamento do YAML.** Um campo Java flat (ex: `jwtSecret`) só faz bind numa propriedade flat (`app.jwt-secret`), nunca num caminho YAML aninhado tipo `app.jwt.secret`; isso precisa de uma classe aninhada de verdade (ver `AppProperties.Jwt`). Isso já mordeu a gente na prática: o descompasso deixava o campo `null` não importa como o valor fosse passado (env var, system property, tanto faz), e um teste unitário fraco mascarou isso porque um campo `null` *também* falha a validação `@NotBlank`, então o teste de "fail fast" passava pelo motivo errado. Se adicionar um novo grupo de config aninhado, espelha o formato do YAML numa classe aninhada em vez de achatar.
- Evita também `@Configuration` mais `@EnableConfigurationProperties(X.class)` na *mesma* classe `X`: isso registra o bean duas vezes (uma instância passa pelo binding correto, a outra é um proxy CGLIB puro com campos não-bindados), e qualquer uma das duas pode ser a que o autowiring pega primeiro. Usa `@ConfigurationProperties` puro com `@ConfigurationPropertiesScan` na classe principal da aplicação em vez disso.
- `ConditionalOnProperty` não consegue expressar "vazio vs. não-setado" de forma limpa. Uma string vazia resolvida via env (`${MAIL_HOST:}` sem nada setado) ainda conta como "presente" pra um `@ConditionalOnProperty(name = "host")` simples, então tanto `ConsoleEmailService` quanto `SmtpEmailService` ativavam ao mesmo tempo. Corrigido com classes `Condition` explícitas usando `StringUtils.hasText`.
- O Actuator auto-inclui um health indicator de mail assim que `spring-boot-starter-mail` entra no classpath. Ele tenta alcançar o host SMTP configurado (ou o default `localhost:587`) e falha sempre que nenhum está configurado, que é exatamente o cenário de dev/fallback-console que esse template usa. Desligado via `management.health.mail.enabled=false`.
- A imagem `maven:3.9.9-eclipse-temurin-17` do estágio de build do Docker seta `MAVEN_CONFIG=/root/.m2`; o script do Maven Wrapper injeta esse valor como *primeiro* argumento de CLI pro Maven, que aí tenta interpretar isso como fase de lifecycle e falha ("Unknown lifecycle phase /root/.m2"). Limpo via `ENV MAVEN_CONFIG=""` no Dockerfile.
- O `env_file: .env` do `docker-compose.yml` injeta o `PORT` do `.env` (pensado como porta do lado do *host* pro mapeamento em `ports:`) no container também, o que faz o Spring bindar nessa porta internamente. Isso dessincroniza do `EXPOSE 8080` e do `HEALTHCHECK`, deixando o container preso em "health: starting" pra sempre. Corrigido sobrescrevendo `PORT=8080` explicitamente no bloco `environment:` do serviço `app`, que tem precedência sobre `env_file:` pra mesma chave.
- Testcontainers precisa de um daemon Docker alcançável de onde os testes rodam. Runners hospedados no GitHub Actions já têm isso por padrão; runners self-hosted precisam de Docker instalado e o socket acessível. Em algumas combinações Windows/Docker Desktop o transporte npipe que o docker-java usa pode falhar de vez (`BadRequestException` em toda named pipe). Se acontecer localmente, verifica o fluxo manualmente contra o Postgres do compose em vez disso (`make docker-up`, roda o jar empacotado, testa os endpoints com curl) e confia no CI pra rodar o Testcontainers de verdade.
- Parsing de JWT não tem margem pra clock-skew configurada; uma máquina de dev com relógio errado vai ver 401 intermitente em tokens recém-emitidos.
- Os buckets do Bucket4j são em memória e por instância: resetam no restart e não coordenam entre múltiplas instâncias da app. Ok pra um template ou deploy de instância única; escalar horizontalmente precisaria de um bucket store com Redis.
- `make db-generate` só cria um arquivo vazio com timestamp. O Flyway Community não tem autogeração de diff de entidades, então escreve o SQL na mão.
- Sem comando `test:watch`. O ecossistema JVM/Maven não tem um test runner de watch-mode idiomático sem plugin de terceiro, então roda `make test` manualmente ou usa o test runner da tua IDE.
- Trocar o *último* caractere de um JWT codificado em base64url pra "adulterar" ele num teste pode decodificar pros mesmos bytes exatos (bits de padding não usados no caractere final), então o token "adulterado" verifica com sucesso mesmo assim e o teste fica flaky. Troca um caractere do meio em vez disso.
