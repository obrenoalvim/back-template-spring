[English](README.md) | Português

# back-template-spring

Template de backend em Spring Boot. Java 17, Maven, Postgres + Flyway + Spring Data JPA, autenticação JWT via Spring Security, domínio de referência `Note` (CRUD). Irmão de `back-template-laravel` e `back-template-nest`.

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

Todo usuário tem um `role` (`USER` | `ADMIN`, default `USER`), carregado como claim assinada no JWT (`JwtService.generateAccessToken`) e transformado em `GrantedAuthority` `ROLE_*` pelo `JwtAuthFilter` — nunca confia num `role` vindo do body da requisição. `GET /admin/users` é a referência pra proteger uma rota: `@PreAuthorize("hasRole('ADMIN')")` (precisa de `@EnableMethodSecurity` no `SecurityConfig`, já adicionado). Sem jeito de virar admin sozinho — muda a coluna direto no banco (`UPDATE users SET role = 'ADMIN' WHERE email = '...'`) pra testar localmente.

## Documentação da API

Docs OpenAPI gerados a partir das anotações de Bean Validation e das anotações `@Tag`/`@Operation`/`@SecurityRequirement` do [springdoc-openapi](https://springdoc.org/) nos controllers. Com o app rodando, abre `http://localhost:8081/swagger-ui/index.html` pro Swagger UI interativo (spec bruto em `/v3/api-docs`). Usa o botão "Authorize" com um JWT de `/auth/login` pra testar as rotas protegidas (`account`, `api/notes`).

## Pre-commit hook

```bash
git config core.hooksPath .githooks
```

Roda `spotless:check` antes de cada commit.

## Env vars

Ver `.env.example` pra lista completa. `JWT_SECRET` é obrigatória (mín. 32 caracteres) — a app falha rápido no boot se estiver ausente ou curta demais. Deixa `MAIL_HOST` sem valor em dev pra usar o fallback de console no envio de email transacional (tokens de verificação/reset ficam só no log em vez de serem enviados de verdade).

## Notas de design

- `hibernate.ddl-auto` é `validate` em todo lugar — o Flyway é dono do schema. Nunca troca pra `update`/`create`, nem localmente; isso mascara silenciosamente migrations que você esqueceu de escrever.
- **O caminho de binding do `@ConfigurationProperties` precisa bater exatamente com o aninhamento do YAML.** Um campo Java flat (ex: `jwtSecret`) só faz bind numa propriedade flat (`app.jwt-secret`), nunca num caminho YAML aninhado tipo `app.jwt.secret` — isso precisa de uma classe aninhada de verdade (ver `AppProperties.Jwt`). Isso já mordeu a gente: o descompasso deixava o campo `null` silenciosamente não importa como o valor fosse passado (env var, system property, tanto faz), e um teste unitário fraco mascarou isso porque um campo `null` *também* falha a validação `@NotBlank` — então o teste de "fail fast" passava pelo motivo errado. Se adicionar um novo grupo de config aninhado, espelha o formato do YAML numa classe aninhada — não achata.
- Evita também `@Configuration` + `@EnableConfigurationProperties(X.class)` na *mesma* classe `X` — isso registra o bean duas vezes (uma instância passa pelo binding correto, a outra é um proxy CGLIB puro com campos não-bindados) e qualquer uma das duas pode ser a que o autowiring pega primeiro. Usa `@ConfigurationProperties` puro + `@ConfigurationPropertiesScan` na classe principal da aplicação em vez disso.
- `ConditionalOnProperty` não consegue expressar "vazio vs. não-setado" de forma limpa — uma string vazia resolvida via env (`${MAIL_HOST:}` sem nada setado) ainda conta como "presente" pra um `@ConditionalOnProperty(name = "host")` simples, então tanto `ConsoleEmailService` quanto `SmtpEmailService` ativavam ao mesmo tempo. Corrigido com classes `Condition` explícitas usando `StringUtils.hasText`.
- O Actuator auto-inclui um health indicator de mail assim que `spring-boot-starter-mail` entra no classpath; ele tenta alcançar o host SMTP configurado (ou o default `localhost:587`) e falha sempre que nenhum está configurado — exatamente o cenário de dev/fallback-console que esse template usa. Desligado via `management.health.mail.enabled=false`.
- A imagem `maven:3.9.9-eclipse-temurin-17` do estágio de build do Docker seta `MAVEN_CONFIG=/root/.m2`; o script do Maven Wrapper injeta esse valor como *primeiro* argumento de CLI pro Maven, que aí tenta interpretar isso como fase de lifecycle e falha ("Unknown lifecycle phase /root/.m2"). Limpo via `ENV MAVEN_CONFIG=""` no Dockerfile.
- O `env_file: .env` do `docker-compose.yml` injeta o `PORT` do `.env` (pensado como porta do lado do *host* pro mapeamento em `ports:`) no container também, o que faz o Spring bindar nessa porta internamente — dessincronizando do `EXPOSE 8080` e do `HEALTHCHECK`, deixando o container preso em "health: starting" pra sempre. Corrigido sobrescrevendo `PORT=8080` explicitamente no bloco `environment:` do serviço `app` (que tem precedência sobre `env_file:` pra mesma chave).
- Testcontainers precisa de um daemon Docker alcançável de onde os testes rodam. Runners hospedados no GitHub Actions já têm isso por padrão; runners self-hosted precisam de Docker instalado e o socket acessível. Em algumas combinações Windows/Docker Desktop o transporte npipe que o docker-java usa pode falhar de vez (`BadRequestException` em toda named pipe) — se acontecer localmente, verifica o fluxo manualmente contra o Postgres do compose em vez disso (`make docker-up`, roda o jar empacotado, testa os endpoints com curl) e confia no CI pra rodar o Testcontainers de verdade.
- Parsing de JWT não tem margem pra clock-skew configurada; uma máquina de dev com relógio errado vai ver 401 intermitente em tokens recém-emitidos.
- Os buckets do Bucket4j são em memória e por instância — reseta no restart, e não coordena entre múltiplas instâncias da app. Ok pra um template/deploy de instância única; precisaria de um bucket store com Redis pra escalar horizontalmente.
- `make db-generate` só cria um arquivo vazio com timestamp — o Flyway Community não tem autogeração de diff de entidades; escreve o SQL na mão.
- Sem `test:watch` — o ecossistema JVM/Maven não tem um test runner de watch-mode idiomático sem plugin de terceiro; roda `make test` manualmente ou usa o test runner da tua IDE.
- Trocar o *último* caractere de um JWT codificado em base64url pra "adulterar" ele num teste pode decodificar exatamente pros mesmos bytes (bits de padding não usados no caractere final) — o token "adulterado" aí verifica com sucesso mesmo assim, deixando o teste flaky. Troca um caractere do meio em vez disso.
