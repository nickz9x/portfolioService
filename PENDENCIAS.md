# Pendências — portfolioService

Atualizado: 2026-09-25

---

## Contexto rápido

Serviço de portfólio de cripto do projeto **ChainTracker**. Guarda carteiras e
posições por usuário, com o preço vindo de um serviço de mercado.

**Stack:** Spring Boot 4.1.1, Java 21, PostgreSQL, Redis, Kafka, Keycloak (JWT).

**Portas:**

| Serviço | Porta |
|---|---|
| identity-service | 8080 |
| Keycloak | 8081 |
| assetMarketDataService | 8082 |
| portfolioService | 8087 |
| Postgres do portfolio | 5434 |
| Postgres do identity | 5432 |
| Postgres do asset | 5433 |
| Redis | 6379 |

**Como a autenticação funciona:** o usuário loga no identity-service, que repassa
pro Keycloak e devolve o token. O id do usuário é o claim `sub` — o mesmo UUID que
`UserService.createUser` devolve. O portfolio guarda esse valor em `owner_id`.
Não existe FK entre os serviços; a referência é por valor.

---

## 🔴 Bloqueadores

### 1. O schema vem do Hibernate, não de migration

**Estado:** destravado, mas com duas fontes de verdade.

Histórico: o schema não existia (nada criava as tabelas). O atalho foi ligado —
`spring.jpa.hibernate.ddl-auto: update` no `application.yaml:22` — e agora é o
Hibernate que cria `portfolio`, `asset` e `position` a partir das entidades.

Depois disso as dependências do **Flyway foram removidas do `pom.xml`**, e a
pasta `src/main/resources/db/migration/` ficou órfã (ver item 3).

**Consequência:** o schema é um efeito colateral do código Java. Não há histórico
de mudanças, não dá para revisar uma alteração de schema em code review, e
`ddl-auto: update` **nunca remove nada** — só adiciona. Foi exatamente isso que
produziu o item 2.

**Fix (quando o Flyway voltar):**

1. Escrever `V1__create_tables.sql` com as três tabelas.
2. Trocar `ddl-auto` para `validate` — o Flyway cria, o Hibernate só confere.
3. Lembrar que no Spring Boot 4 a auto-configuração do Flyway saiu do
   `spring-boot-autoconfigure` e virou o módulo `spring-boot-flyway`. Conferir
   se está no classpath antes de escrever a migration, senão ela não roda:

   ```bash
   ./mvnw -o dependency:tree | grep -i flyway
   ```

---

### 2. O banco tem uma constraint que a entidade não declara

**Estado:** inconsistência ativa, descoberta em 2026-09-25.

O banco tem `UNIQUE (asset_id)` na tabela `asset`, com o nome gerado pelo
Hibernate: `uk17bgdxenhefny6036bllfv86g`. Verificado e enforced — um `INSERT`
duplicado é rejeitado.

Mas `Asset.java` **não tem** `@Column(unique = true)`. A anotação existiu e foi
removida; o Hibernate não remove constraint sozinho, então ela ficou para trás.

**Consequência:** o banco e o código discordam. Se as tabelas forem recriadas
(drop + restart), a constraint **não volta** — e aí duas linhas com o mesmo
`asset_id` quebram toda leitura seguinte, porque `findByAssetId` devolve
`Optional<Asset>` e o Spring Data não monta `Optional` a partir de duas linhas
(`IncorrectResultSizeDataAccessException`).

**Fix:** decidir onde a verdade mora.
- Se a unicidade é regra de domínio → devolver `@Column(unique = true)` na
  entidade e recriar o schema.
- Se é regra de banco → escrever na migration (item 1) e nunca confiar no
  `ddl-auto`.

---

### 3. A migration em `db/migration/` está morta e, se ligada, quebra

**Estado:** arquivo existe, não roda.

`src/main/resources/db/migration/V1__add_asset_unique_constraint.sql` foi escrito
para adicionar `uk_asset_asset_id`. Hoje ele tem dois problemas:

1. **Não roda.** As dependências do Flyway saíram do `pom.xml`, então nada lê
   essa pasta.
2. **Se o Flyway for religado, ele quebra de duas formas:**
   - Em banco novo, migrations rodam **antes** do Hibernate. O `ALTER TABLE asset`
     falha porque a tabela ainda não existe.
   - O guard do arquivo procura por `conname = 'uk_asset_asset_id'`, mas a
     constraint real se chama `uk17bgdxenhefny6036bllfv86g`. O guard dá falso, o
     `ALTER` roda, e como o Postgres aceita duas constraints únicas na mesma
     coluna, ele cria um **índice único redundante em silêncio** — sem erro.

**Fix:** reescrever junto com o item 1. Ou apagar, se a unicidade voltar para a
entidade.

---

### 4. Client secret do Keycloak commitado no git

**Onde:** `identity-service/src/main/resources/application.yaml`, linha 19
(`security.client-secret`).

**Situação:** o arquivo é rastreado, o `.gitignore` não cobre, e os valores estão
no histórico — o **antigo** nos commits `87b6279`, `d124220` e `6bb01fb`, e o
**novo** a partir do commit que rotacionou o secret. Os repositórios são privados,
o que limita o alcance, mas quem já clonou tem os dois valores.

**Fix:**

1. Trocar por variável de ambiente:
   `security.client-secret: ${KEYCLOAK_CLIENT_SECRET}`
2. Registrar a variável na config da IDE e cobrir `.env` no `.gitignore`.
3. **Rotacionar de novo no Keycloak** e desta vez não commitar o valor. Tirar do
   arquivo não apaga o histórico; reescrever com `filter-repo` só compensa se o
   repositório virar público.

---

## 🟡 Importante

### 5. `UNIQUE (portfolio_id, asset_id)` na tabela position

O `@Transactional` do `createPosition` **não impede** duas compras simultâneas do
mesmo asset criarem duas linhas. Com `READ_COMMITTED`, as duas transações leem
"não existe" antes de qualquer uma commitar, e as duas inserem.

Quem resolve é o banco:

```sql
ALTER TABLE position ADD CONSTRAINT uk_position_portfolio_asset
    UNIQUE (portfolio_id, asset_id);
```

Depois disso a segunda transação toma erro de constraint — tratar como
"concorrência, recarrega e soma" em vez de deixar passar.

### 6. `UNIQUE (owner_id)` ou índice em `owner_id`

Usado pra listar os portfolios do usuário. Decidir:
- **1 portfolio por usuário** → `UNIQUE (owner_id)` fecha a questão.
- **Vários** → índice simples em `owner_id`, senão é full scan.

### 7. `createPosition` devolve `pnl` fixo em zero

`PositionService.java:50` monta o `PositionResponseDto` com `BigDecimal.ZERO` no
último campo, que é o `pnl`. O `averagePrice` já vem do `quote.currentPrice()`,
mas o PnL não é calculado — a posição acabou de ser criada, então o correto seria
`0` **de verdade** (preço atual = preço médio) ou o valor de `calculePNL`.
Hoje é um zero literal que não passou por cálculo nenhum.

### 8. `GET /api/portfolio` — listagem de portfolios

`PortfolioRepository.findByOwnerId` já existe, mas nenhum controller expõe.
Deve usar `jwt.getSubject()` como `ownerId`, nunca um parâmetro do request.

### 9. `sellAsset` e `calculePNL` não têm caminho de escrita

`Position.sellAsset` continua sem endpoint. O `calculePNL` agora é alcançado pela
listagem, mas a venda não — e é ela que fecha o ciclo da carteira.

### 10. `POST /api/auth/refresh` no identity

O identity só tem `/register` e `/login`. Falta o refresh, que é o que permite o
front renovar o access token sem pedir senha de novo (o refresh token dura 30 min,
o access token só 5).

---

## 🔵 Limpeza

### 11. `BuyAssetRequestDto` é código morto

`dto/BuyAssetRequestDto.java` só aparece na própria definição — nenhum controller,
service ou import usa. Ou vira o request de uma venda/compra futura, ou sai.

### 12. Imports órfãos

- `controller/PortfolioController.java:5-6` — `PositionRequestDto` e `Position`,
  adicionados e nunca usados.
- `model/Asset.java:6` — `java.math.BigDecimal`, sem campo BigDecimal na entidade.
- `AssetService.java` — conferir `AssetRequestDto`, `KafkaListener` (foram
  removidos numa limpeza anterior; confirmar que não voltaram).

### 13. `spring-boot-starter-test` declarado duas vezes no `pom.xml`

O Maven avisa no build:

> `'dependencies.dependency.(groupId:artifactId:type:classifier)' must be unique`
> ... future Maven versions might no longer support building such malformed projects

Remover a segunda declaração.

### 14. `spring.jpa.open-in-view`

O log avisa que está ligado por padrão. Como a coleção `List<Position>` foi
removida do `Portfolio`, não há mais necessidade de lazy loading fora da
transação. Definir `spring.jpa.open-in-view: false` para silenciar e evitar
query acidental na serialização.

### 15. `existsByassetId` — nome com 'a' minúsculo

`AssetRepository.java` e `AssetService.java`. Funciona (o Spring Data deriva do
nome exato), mas o padrão é `existsByAssetId`. Renomear nos dois lados juntos.

---

## 🧪 DX e testes

### 16. Arquivo `api.http` para o IntelliJ

Evita copiar token à mão. Roda o login, captura o `access_token` numa variável
global e injeta nos requests seguintes.

### 17. Keycloak — lifespan do access token

O default de 300s (5 min) atrapalha o desenvolvimento. **Desenvolvimento:**
Realm settings → Tokens → Access Token Lifespan → 30 min. **Produção:** manter
300s e resolver com refresh token (item 10).

### 18. Não existe teste de verdade

Só o `PortfolioServiceApplicationTests` gerado pelo Initializr. Nada cobre
`buyAsset`, `sellAsset` ou `calculePNL` — que são lógica pura, sem banco nem
Spring, e seriam os mais baratos de testar.

---

## ✅ Já resolvido (não refazer)

- **A tabela `asset` agora é populada.** `AssetService.findOrCreate` registra o
  asset no primeiro uso, chamado de dentro do `createPosition`. O `findByAssetId`
  deixou de cair sempre no `orElseThrow`.
- **`DataService` com injeção nula.** O campo `client` não tinha `final`,
  construtor nem `@Autowired` — ficava `null` e estourava NPE na primeira
  chamada. Resolvido com `@RequiredArgsConstructor` + `private final`.
- **N+1 do banco.** `Position.asset` é `@ManyToOne` (EAGER por padrão): carregava
  N posições e disparava mais N consultas. Resolvido com
  `@EntityGraph(attributePaths = "asset")` em `findAllByPortfolioId`.
- **N+1 do Redis.** A listagem fazia uma ida ao cache por posição. Virou
  `AssetService.getQuotes`, que resolve tudo com um `multiGet`.
- **NPE do `calculePNL`.** Guardas para `currentPrice`, `averagePrice` e
  `quantity` nulos, e para divisão por zero. `null` significa "não sei", `0`
  significa "está no preço médio" — são coisas diferentes e o retorno preserva
  a distinção.
- **Controller de posição.** `PositionController` expõe
  `POST`/`GET /api/portfolio/{id}/positions`, sempre com `jwt.getSubject()`.
- Lista `List<Position>` removida do `Portfolio`. A FK vive em `Position`, e a
  escrita passou a ser por `positionRepository.save(position)`.
- `@Data` → `@Getter/@Setter` + `@EqualsAndHashCode.Include` no id, nas 3 entidades.
- `calculePNL` corrigido: era `A/(C-A)*100` (dava 1000% no lugar de 10%), virou
  `(C-A)/A*100`.
- `buyAsset` com média ponderada por valor.
- `PositionRepository.findByPortfolioIdAndAssetId` — dedupe por portfolio + asset.
- `PortfolioService.findById(String)` **removido** — virou `findByIdAndOwner`.
  Busca por id sem dono era o principal furo (IDOR).
- `createPortfolio(name, ownerId)` + `POST /api/portfolio` + DTOs.
- `SecurityConfiguration` — faltava `@Bean` no `filterChain`.
- `spring.security.oauth2.resourceserver.jwt.issuer-uri` no `application.yaml`.
- `pom.xml` — `spring-boot-restclient`. No Spring Boot 4 a auto-config do
  RestClient saiu do `spring-boot-autoconfigure` e virou módulo próprio.
