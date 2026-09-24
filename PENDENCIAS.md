# Pendências — portfolioService

Atualizado: 2026-09-24

---

## Contexto rápido

Serviço de portfólio de cripto do projeto **ChainTracker**. Guarda carteiras e
posições por usuário, com o preço vindo de um serviço de mercado.

**Stack:** Spring Boot 4.1.1, Java 21, PostgreSQL, Redis, Kafka, Flyway, Keycloak (JWT).

**Portas:**

| Serviço | Porta |
|---|---|
| identity-service | 8080 |
| Keycloak | 8081 |
| assetMarketDataService | 8082 |
| portfolioService | 8087 |
| Postgres do portfolio | 5434 |
| Postgres do identity | 5432 |
| Redis | 6379 |

**Como a autenticação funciona:** o usuário loga no identity-service, que repassa
pro Keycloak e devolve o token. O id do usuário é o claim `sub` — o mesmo UUID que
`UserService.createUser` devolve. O portfolio guarda esse valor em `owner_id`.
Não existe FK entre os serviços; a referência é por valor.

---

## 🔴 Bloqueadores

### 1. O schema não existe — `relation "portfolio" does not exist`

**Estado:** erro atual, ao chamar `POST /api/portfolio`.

**Causa:** não existe *nada* que crie as tabelas.

- `src/main/resources/` só tem `application.yaml` — não existe pasta `db/migration`
  nem nenhuma migration escrita.
- O Flyway está no `pom.xml` como biblioteca, mas **não há migration nenhuma para ele rodar**.
- O `ddl-auto` do Hibernate, para banco não-embarcado (PostgreSQL), é `none` por
  padrão, e o `application.yaml` não define outro valor. Então o Hibernate também
  não cria as tabelas.

**Não verificado:** há uma suspeita de que o Spring Boot 4 também exija o módulo
`spring-boot-flyway` (mesmo padrão do bug do RestClient, que precisou de
`spring-boot-restclient`). A verificação foi interrompida — **conferir antes de
escrever a migration**, senão ela é escrita e o Flyway continua não rodando:

```bash
ls ~/.m2/repository/org/springframework/boot/ | grep -i flyway
./mvnw -o dependency:tree | grep -i flyway
```

Se o modulo `spring-boot-flyway` nao existir no classpath, adicionar no `pom.xml`,
igual foi feito com `spring-boot-restclient`.

**Fix:**

1. Confirmar o módulo do Flyway (acima).
2. Escrever `src/main/resources/db/migration/V1__create_tables.sql` com as tabelas
   `portfolio`, `asset` e `position`, com `owner_id` em portfolio e as FKs
   `portfolio_id` / `asset_id` em position.
3. Definir `spring.jpa.hibernate.ddl-auto: validate` no `application.yaml` —
   o Flyway cria o schema, o Hibernate só confere se as entidades batem.

**Atalho para destravar agora (não serve para produção):**
`spring.jpa.hibernate.ddl-auto: update` faz o Hibernate criar as tabelas a partir
das entidades. Resolve em 1 minuto, mas aí o schema tem duas fontes de verdade.

---

### 2. A tabela `asset` nunca é populada

**Estado:** descoberto, ainda não corrigido.

`AssetService.save(AssetRequestDto)` (`AssetService.java:41`) **não é chamado por
ninguém** — não há controller nem listener que o invoque.

Consequência: a tabela `asset` fica vazia, e
`assetService.findByAssetId(assetId)` (`PositionService.java:26`) sempre cai no
`orElseThrow` e devolve 404. O fluxo de criar posição vai falhar sempre.

O `KafkaListener` `consumeAsset` (`AssetService.java:34`) recebe o mercado e grava
**só no Redis** — não persiste o `Asset`.

**Fix:** decidir onde o `Asset` nasce. Duas opções:
- Chamar `assetService.save(...)` de dentro do `consumeAsset` (o Asset entra no
  banco junto com o primeiro preço que chega do Kafka), ou
- Criar um endpoint que receba `AssetRequestDto`.

A opção do listener é a mais natural: a entidade já chega pronta no payload.

---

### 3. Client secret do Keycloak commitado no git

**Onde:** `identity-service/src/main/resources/application.yaml`, linha 19
(`security.client-secret`).

**Situação:** o arquivo é rastreado pelo git, o `.gitignore` não cobre, e o valor
está no **histórico** — commits `87b6279`, `6bb01fb` e `90d8366`.

**Fix:**

1. Trocar por variável de ambiente:
   `security.client-secret: ${KEYCLOAK_CLIENT_SECRET}`
2. Registrar a variável na config da IDE e cobrir `.env` no `.gitignore`.
3. **Rotacionar o secret no Keycloak** — gerar um novo e invalidar o antigo.

O passo 3 é o que resolve de verdade: tirar do arquivo não apaga o histórico, e
quem clonou o repo já tem o valor. Reescrever histórico com `filter-repo` só
compensa se o repositório for público.

---

## 🟡 Importante

### 4. `UNIQUE (portfolio_id, asset_id)` na tabela position

O `@Transactional` que está no `createPosition` **não impede** duas compras
simultâneas do mesmo asset criarem duas linhas. Com `READ_COMMITTED`, as duas
transações leem "não existe" antes de qualquer uma commitar, e as duas inserem.

Quem resolve é o banco:

```sql
ALTER TABLE position ADD CONSTRAINT uk_position_portfolio_asset
    UNIQUE (portfolio_id, asset_id);
```

Depois disso a segunda transação toma erro de constraint — tratar como
"concorrência, recarrega e soma" em vez de deixar passar.

### 5. `UNIQUE (owner_id)` ou índice em `owner_id`

Usado pra listar os portfolios do usuário. Decidir:
- **1 portfolio por usuário** → `UNIQUE (owner_id)` fecha a questão.
- **Vários** → índice simples em `owner_id`, senão é full scan.

Vai na migration do item 1.

### 6. Endpoint de listagem — `GET /api/portfolio`

`PortfolioRepository.findByOwnerId` já existe, mas nenhum controller expõe.
Deve usar `jwt.getSubject()` como `ownerId`, nunca um parâmetro do request.

### 7. Controller de posição (não existe)

`PositionService.createPosition` passou a exigir um quarto parâmetro
`String ownerId` (mudança feita para fechar o furo do `findById` sem dono).
Não há controller chamando — quando criar, passar `jwt.getSubject()`.

Como efeito, `Position.sellAsset` e `Position.calculePNL` estão escritos e testados
na lógica, mas **nenhum caminho de código os alcança** ainda.

### 8. `POST /api/auth/refresh` no identity

O identity só tem `/register` e `/login`. Falta o refresh, que é o que permite o
front renovar o access token sem pedir senha de novo (o refresh token dura 30 min,
o access token só 5).

---

## 🔵 Limpeza

### 9. `spring-boot-starter-test` declarado duas vezes no `pom.xml`

Por volta da linha 148. O Maven avisa:

> `'dependencies.dependency.(groupId:artifactId:type:classifier)' must be unique`
> ... future Maven versions might no longer support building such malformed projects

Remover a segunda declaração.

### 10. Imports órfãos

- `Portfolio.java:6-7` — `java.util.ArrayList` e `java.util.List`
- `AssetService.java` — `Portfolio` (6), `Position` (7), `PortfolioRepository` (9),
  `Header` (14), `BigDecimal` (19)

### 11. `spring.jpa.open-in-view`

O log avisa que está ligado por padrão. Como a coleção `List<Position>` foi
removida do `Portfolio`, não há mais necessidade de lazy loading fora da
transação. Definir `spring.jpa.open-in-view: false` para silenciar e evitar
query acidental na serialização.

### 12. identity-service — config hardcoded e import órfão

- `serverUrl`, `realm` e `clientId` repetidos em 3 lugares: `UserService` (2x) e
  `KeycloakAdminConfiguration`. Já existe `@Value` no projeto — puxar tudo pra config.
- `UserService` importa `AllArgsConstructor` (linha 7) mas usa `@RequiredArgsConstructor`.

### 13. `existsByassetId` — nome com 'a' minúsculo

`AssetRepository.java:10` e `AssetService.java:42`. Funciona (o Spring Data deriva
do nome exato), mas o padrão é `existsByAssetId`. Renomear nos dois lados juntos.

---

## 🧪 DX e testes

### 14. Arquivo `api.http` para o IntelliJ

Evita copiar token à mão. Roda o login, captura o `access_token` numa variável
global e injeta nos requests seguintes. Conteúdo discutido na conversa — recriar
quando voltar.

### 15. Keycloak — lifespan do access token

O default de 300s (5 min) é atrapalha o desenvolvimento. **Desenvolvimento:**
Realm settings → Tokens → Access Token Lifespan → 30 min. **Produção:** manter 300s
e resolver com refresh token (item 8).

### 16. Não existe teste de verdade

Só o `PortfolioServiceApplicationTests` gerado pelo Initializr. Nada cobre
`buyAsset`, `sellAsset` ou `calculePNL` — que são lógica pura, sem banco nem
Spring, e seriam os mais baratos de testar.

---

## ✅ Já resolvido (não refazer)

- Lista `List<Position>` removida do `Portfolio`. A FK vive em `Position`, e a
  escrita passou a ser por `positionRepository.save(position)`.
- `@Data` → `@Getter/@Setter` + `@EqualsAndHashCode.Include` no id, nas 3 entidades.
- `calculePNL` corrigido: era `A/(C-A)*100` (dava 1000% no lugar de 10%), virou
  `(C-A)/A*100` com guard para divisão por zero.
- `buyAsset` com média ponderada por valor — conferido.
- `PositionRepository.findByPortfolioIdAndAssetId` — dedupe por portfolio + asset,
  pra segunda compra somar em vez de criar linha nova.
- `PortfolioService.findById(String)` **removido** — virou `findByIdAndOwner`.
  Busca por id sem dono era o principal furo (IDOR).
- `createPortfolio(name, ownerId)` + `POST /api/portfolio` + DTOs de request/response.
- `SecurityConfiguration` — faltava `@Bean` no `filterChain`, o que fazia o Spring
  ignorar a config e cair na segurança padrão.
- `spring.security.oauth2.resourceserver.jwt.issuer-uri` no `application.yaml`.
- `pom.xml` — adicionado `spring-boot-restclient`. No Spring Boot 4 a auto-config
  do RestClient saiu do `spring-boot-autoconfigure` e virou módulo próprio, que o
  `spring-boot-starter-web` não traz.
