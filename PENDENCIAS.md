# Pendências — portfolioService

Atualizado: 2026-10-04

---

## Contexto rápido

Serviço de portfólio de cripto do projeto **ChainTracker**. Guarda carteiras e
posições por usuário, com o preço vindo de um serviço de mercado.

**Stack:** Spring Boot 4.1.1, Java 21, PostgreSQL, Redis, Kafka, Keycloak (JWT),
Flyway (schema em `src/main/resources/db/migration/`).

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

### 5. `createPosition` não trata a violação da `uk_position_portfolio_asset`

A `UNIQUE (portfolio_id, asset_id)` agora existe (migrations `V1`/`V2`). Ela faz a
segunda compra concorrente do mesmo ativo esbarrar em vez de criar linha
duplicada — mas o `PositionService` ainda não trata o caso: o
`DataIntegrityViolationException` sobe como 500. Tratar como "concorrência,
recarrega e soma" em vez de deixar passar.

### 6. `UNIQUE (owner_id)` ou índice em `owner_id`

Decidido por ora: **vários portfolios por usuário** são permitidos (o
`createPortfolio` não bloqueia duplicata e o `findByOwnerId` devolve `List`), então
a `V3__add_portfolio_owner_id_index.sql` criou um **índice simples** em `owner_id`.

Se a regra de produto virar "1 portfolio por usuário", o caminho é uma migration
nova com `UNIQUE (owner_id)` — que torna este índice redundante.

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

### 20. Erros do assetMarketDataService não têm tratamento

O `RestClient` usa `.retrieve()`, que estoura `HttpClientErrorException` em 4xx/5xx
do serviço de mercado. Nenhum caller trata — vira 500 na cara do usuário. Falta
decidir o comportamento (502 com mensagem, retry, degradar a listagem sem preço).

---

## 🔵 Limpeza

### 12. Imports órfãos

- `controller/PortfolioController.java:5-6` — `PositionRequestDto` e `Position`,
  adicionados e nunca usados.

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

---

## 🧪 DX e testes

### 16. Arquivo `api.http` para o IntelliJ

Evita copiar token à mão. Roda o login, captura o `access_token` numa variável
global e injeta nos requests seguintes.

### 17. Keycloak — lifespan do access token

O default de 300s (5 min) atrapalha o desenvolvimento. **Desenvolvimento:**
Realm settings → Tokens → Access Token Lifespan → 30 min. **Produção:** manter
300s e resolver com refresh token (item 10).

### 18. Cobertura de teste ainda rasa

O `JwtForwardingInterceptorTest` cobre o repasse do token e o `PositionServiceTest`
a média ponderada na compra repetida de stock e os 404s de mercado nulo. Continuam
sem teste: `sellAsset`, `calculePNL` isolado e os fluxos com banco (`PortfolioService`).

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
- **Schema agora vem de migration, não do Hibernate.** `spring-boot-starter-flyway`
  + `flyway-database-postgresql` no `pom.xml`, `ddl-auto: validate` e
  `baseline-on-migrate: true` (o banco de dev já existia sem histórico de
  migration). A `V1` cria o schema da modelagem nova; a `V2` adota o banco
  legado: solta a FK de `position.asset_id`, converte o id interno para o id
  externo via tabela `asset` (1 → "bitcoin"), dropa a tabela `asset` e cria a
  `uk_position_portfolio_asset`. Verificado nos dois caminhos: banco legado
  (baseline + V2) e banco vazio (V1 + V2 + `validate` do Hibernate).
- `model/Asset.java` e `repository/AssetRepository.java` deletados; a
  unicidade que era deles virou `uk_position_portfolio_asset` na V2.
- `V1__add_asset_unique_constraint.sql` apagada — a tabela `asset` não existe
  mais.
- **401 no fluxo de stock.** O `AssetMarketDataClient` chamava
  `/api/stock/**` e `/api/internal/**` do assetMarketDataService sem token —
  lá só `/api/currency/**` é público, e o fluxo de cripto escapava exatamente
  por isso. Agora o `JwtForwardingInterceptor` repassa o Bearer do usuário nas
  chamadas de saída (mais `JwtForwardingInterceptorTest` cobrindo os dois
  casos).
- **Build Maven quebrado no JDK 26.** O `JAVA_HOME` da máquina aponta para o
  Corretto 26, e o JDK 23+ desligou o processamento implícito de anotações: o
  Lombok parava de ser descoberto e a compilação virava uma chuva de
  "cannot find symbol" nos getters/setters. Resolvido declarando o Lombok como
  `annotationProcessorPaths` no `maven-compiler-plugin`. A IDE não sofria
  porque compila com configurações próprias — só o `./mvnw` quebrava.
- **`createPosition` devolve o `pnl` calculado** pelo `calculePNL` — o zero
  literal saiu.
- **Código morto removido** (zero referências): `AssetService`, `AssetRequestDto`,
  `BuyAssetRequestDto`, `StockPositionResponseDto` — e o rascunho `CoinDataResponse`.
- **Nomes de cripto/stock simetrizados:** `CryptoDataResponse`/`StockDataResponse`
  (dados estáticos, sem cotação), `CryptoMarketData`/`StockMarketData` (cotação),
  métodos `getCryptoData`/`getStockData`/`getCryptoMarketData`/`getStockMarketData`
  e `createCryptoPosition`/`createStockPosition`. O enum `CRIPTO` e o path
  `/cripto` ficaram como estavam — são contrato (banco e front).
- **`createStockPosition` sem NPE e com média certa:** guard antes do deref, o
  `buyAsset` fica dono da média ponderada na compra repetida, e mercado nulo
  agora vira 404 no create e na listagem (cripto e stock).
- **Resposta do create devolve a posição como ela ficou:** `quantity` e
  `averagePrice` são os totais da posição (antes devolvia só a compra), igual ao
  que o `GET` já devolvia via `PositionMapper`.
