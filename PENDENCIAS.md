# Pendências — portfolioService

Atualizado: 2026-10-15 (revisão 2 — cada item aberto conferido contra o código)

**Resolvidos nesta revisão: 8, 9, 12, 13, 14, 22, 24, 25 e 32.** O item 23
fechou pela metade: os guards do create agora usam `||`, mas o custom price
continua sem guard. Os demais itens abertos foram reconferidos e continuam
como estavam.

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

**Alertas:** `POST /api/alert` publica no tópico Kafka `alert.created` (chave =
id do usuário). O portfolio não grava, não lista e não cancela alerta — quem
guarda e dispara é o serviço que consome.

---

## 🔴 Bloqueadores

### 4. Client secret do Keycloak commitado no git

**Onde:** `identity-service/src/main/resources/application.yaml`, linha 19
(`security.client-secret`).

**Situação:** o arquivo é rastreado, o `.gitignore` não cobre, e os valores estão
no histórico — o **antigo** nos commits `87b6279`, `d124220` e `6bb01fb`, e o
**novo** a partir do commit que rotacionou o secret. Os repositórios são privados,
o que limita o alcance, mas quem já clonou tem os dois valores.

**Reconferido em 2026-10-15:** continua igual — a linha 19 do `application.yaml`
do identity-service ainda tem o valor literal, e o arquivo segue rastreado.

**Fix:**

1. Trocar por variável de ambiente:
   `security.client-secret: ${KEYCLOAK_CLIENT_SECRET}`
2. Registrar a variável na config da IDE e cobrir `.env` no `.gitignore`.
3. **Rotacionar de novo no Keycloak** e desta vez não commitar o valor. Tirar do
   arquivo não apaga o histórico; reescrever com `filter-repo` só compensa se o
   repositório virar público.

### 27. As V4–V7 quebram o banco vazio

**Onde:** `db/migration/V4__add_column_enchange_into_position_table.sql` até
`V7__add_column_type_into_position.sql` (commit de 2026-10-08).

**Situação:** as quatro são `alter table position add column ...` **sem
`if not exists`**, e a V1 já cria `position` com essas quatro colunas
(`exchange`, `name`, `symbol`, `type`). Em banco vazio a sequência para na V4:

> ERROR: column "exchange" of relation "position" already exists

O Flyway aborta e a aplicação não sobe — quem clona o projeto hoje não consegue
rodar. As V4–V7 só funcionam num banco que já passou pela V1 (que portanto não
roda de novo) e não tem essas colunas — o caso do banco de dev, baselinado na
V1, quando a entidade ganhou `name`/`symbol`/`exchange`/`type` e o
`ddl-auto: validate` passou a barrar o boot por causa delas.

**O schema ficou diferente nos dois caminhos** — mesma tabela, definição
diferente conforme o banco:

- `symbol`: a V6 cria `varchar(10)`; a V1 e a entidade usam `varchar(255)`. O
  `validate` do Hibernate confere o tipo (varchar), não o comprimento, então o
  boot passa — mas o banco de dev estoura em qualquer symbol com mais de 10
  caracteres, e o banco novo não.
- `type`: a V7 cria `varchar(25)` e **sem** o
  `position_type_check CHECK (type IN ('CRIPTO', 'STOCK'))` da V1. O banco que
  ficou sem o check é justamente o de dev, o que é usado.

**Fix:** `add column if not exists` nas quatro — idempotente e correta nos dois
caminhos. Quanto ao typo do nome (`enchange` → `exchange`), renomear só junto com
um `flyway repair`: o nome aplicado está em `flyway_schema_history.script` e um
rename sem repair faz o validate acusar "applied migration not resolved locally"
no boot seguinte.

### 28. `application.yaml` está com o Flyway desligado no working tree

Não commitado (`git status` mostra só esse arquivo), mas inverte o que os
próprios comentários do arquivo dizem: `ddl-auto: validate` → `update` e
`flyway.enabled: false`, com os comentários do `validate` e do
`baseline-on-migrate` intactos logo acima. Se foi teste local,
`git checkout -- src/main/resources/application.yaml`; se é para ficar, os
comentários precisam sair junto — do jeito que está, quem lê o arquivo acredita
numa coisa e o runtime faz outra. Commitado, o Flyway para de rodar e o schema
volta a ser do Hibernate, que adiciona a coluna que falta mas não remove nem
corrige a que já existe: exatamente o que a V1 existe para impedir.

---

## 🟡 Importante

### 5. `createPosition` ainda não trata a violação da `uk_position_portfolio_asset`

**O caso determinístico foi corrigido em 2026-10-05; falta o concorrente.**

O que quebrava não era só concorrência. O `createStockPosition` buscava pelo
texto cru do request e gravava o ticker canônico que volta do mercado: com
`googl` na entrada, a busca não achava a linha gravada como `GOOGL`, o fluxo caía
no `else`, e o `INSERT` esbarrava na constraint — 500 no lugar de somar. Corrigido
buscando com `stockData.symbol()`, com `repeatStockBuyLooksUpByTheCanonicalSymbol`
cobrindo a regressão.

O caso **concorrente** (duas transações simultâneas, ambas passando pela busca
antes de qualquer uma gravar) continua aberto — e não é um `try/catch`: com JPA o
`INSERT` só vai pro banco no commit, **fora** do método, então envolver o `save()`
num try não captura a exceção. Precisa de `saveAndFlush` dentro de uma transação
`REQUIRES_NEW` (ou retry na camada de fora) para capturar, recarregar e somar.

**Reconferido em 2026-10-15:** continua aberto, com um passo andado — o ramo da
posição existente do `createCryptoPosition` já usa `saveAndFlush`, então ali o
INSERT acontece dentro do método e um `try/catch` passaria a capturar. O que não
dá para dispensar é o `REQUIRES_NEW`: depois de um flush que falha, o Hibernate
marca a transação como rollback-only e não deixa seguir somando na mesma
transação.

### 6. `UNIQUE (owner_id)` ou índice em `owner_id`

Decidido por ora: **vários portfolios por usuário** são permitidos (o
`createPortfolio` não bloqueia duplicata e o `findByOwnerId` devolve `List`), então
a `V3__add_portfolio_owner_id_index.sql` criou um **índice simples** em `owner_id`.

Se a regra de produto virar "1 portfolio por usuário", o caminho é uma migration
nova com `UNIQUE (owner_id)` — que torna este índice redundante.

### ✅ 8. `GET /api/portfolio` — listagem de portfolios

**Resolvido em 2026-10-15** (no working tree, sem commit):
`PortfolioController.listAllPortfolioByOwnerId` expõe o `GET /api/portfolio`,
delegando para `PortfolioService.listAllPortfolio(ownerId)`, que monta os
`PortfolioResponseDto` a partir do `findByOwnerId`. Usa `jwt.getSubject()` como
dono, como o item pedia — nenhum id vem do request.

### ✅ 9. A venda existia, mas buscava pela chave errada e errava o status

**Resolvido em 2026-10-15** (no working tree, sem commit) — os três pontos:

- **Chave de busca.** O `type` passou a vir no request (`PositionRequestDto`
  ganhou `type`) e o `sellPosition` normaliza por ele: `assetId.toLowerCase()`
  para `CRIPTO`, `toUpperCase()` para `STOCK`. O que ainda falta nessa
  normalização está no item 36.
- **Status.** Posição inexistente agora lança
  `ResponseStatusException(NOT_FOUND, "portfolio ou ativo invalido")` — 404, não
  mais 500. E o `sellAsset` trocou os `IllegalArgumentException` por
  `ResponseStatusException(CONFLICT)`: 409 para quantidade inválida e saldo
  insuficiente (ver item 37 sobre a entidade conhecer HTTP).
- **Corpo no DELETE.** A rota virou `POST /api/portfolio/{id}/positions/delete`,
  então o body deixou de ser problema. O nome `/delete` num POST é feio (o
  natural seria `/sell`), mas é cosmético.

### 10. `POST /api/auth/refresh` no identity

O identity só tem `/register` e `/login`. Falta o refresh, que é o que permite o
front renovar o access token sem pedir senha de novo (o refresh token dura 30 min,
o access token só 5).

### 20. Erros do assetMarketDataService não têm tratamento

O `RestClient` usa `.retrieve()`, que estoura `HttpClientErrorException` em 4xx/5xx
do serviço de mercado. Nenhum caller trata — vira 500 na cara do usuário. Falta
decidir o comportamento (502 com mensagem, retry, degradar a listagem sem preço).

### 21. As rotas `/cripto` e `/stock` MUDARAM DE SIGNIFICADO

Antes existiam duas rotas de criação, ambas com preço de mercado. Agora são
quatro, e as duas antigas passaram a significar outra coisa:

| Rota | Antes | Agora |
|---|---|---|
| `POST .../positions/cripto` | preço de mercado | **preço informado no request** |
| `POST .../positions/stock` | preço de mercado | **preço informado no request** |
| `POST .../positions/cripto/market` | — | preço de mercado |
| `POST .../positions/stock/market` | — | preço de mercado |

Quem já chamava `/cripto` ou `/stock` esperando preço de mercado agora cai no
`PositionAnotherPriceDto` — que exige o campo `price` — e leva 400. Não há
período de transição nem rota antiga preservada. **É mudança de contrato com o
front**, e a PENDENCIAS anterior registrava explicitamente que `/cripto` era
contrato.

**Já subiu.** O commit está em `origin/main` desde 2026-10-05, então não há mais
"antes de subir" para confirmar: se o front chamava `/cripto` esperando preço de
mercado, está levando 400 desde então. O caminho agora é avisar quem consome ou
manter as duas rotas.

**E a venda mudou de rota em 2026-10-15** (ainda sem commit): saiu de
`DELETE /api/portfolio/{id}/positions/delete` para
`POST /api/portfolio/{id}/positions/delete`, e o body agora exige `type`. Quem
ainda chama o DELETE recebe 405.

### ✅ 22. `PositionAnotherPriceDto.price` aceitava null

**Resolvido em 2026-10-15** (no working tree, sem commit): o campo virou
`@NotNull @Positive BigDecimal price`. Era o último item que continuava em aberto
dos que ficaram para decidir depois do push de 2026-10-05.

O buraco espelhado **continua** no `AlertRequestDto`: `@NotNull` no `price` e
nenhum `@Positive`, então preço zero ou negativo passa (item 30).

### 23. Os guards de mercado nulo do create — fechado nos creates, falta o custom price

O `||` entrou nos dois guards (`PositionService.java:31` e `:76`) e o NPE sumiu.
O guard agora erra para o lado certo nos dois casos:

- **mercado nulo → 404**, como era antes da regressão;
- **`symbol` nulo → 404 também**, que era o caso que passava direto: no Postgres
  `NULL` não colide em unique constraint, então cada chamada inseria uma linha
  nova que a busca por `asset_id` nunca encontrava — duplicata silenciosa.

**Sobrou um ponto:** o `createStockWithCustomPricePosition` chama
`dataService.getStockData(symbol)` e usa `stockData.symbol()` direto
(`PositionService.java:254-255`), sem guard nenhum — mesmo NPE possível ali, e
nesse caminho nunca houve 404.

### 29. O alerta continua se perdendo em silêncio — a tentativa de fix não faz nada

Em 2026-10-15 o retorno do `send` deixou de ser ignorado e ganhou um
`.whenComplete(...)`. O caminho de erro, porém, **não chega em ninguém**:

```java
kafkaTemplate.send("alert.created", userId, kafkaDto).whenComplete((result, ex) -> {
    if (ex == null) { System.out.println("Alerta gravado com sucesso..."); }
    else { throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED); }
});
```

O `throw` roda na thread do produtor do Kafka, dentro do callback: não volta para
a requisição HTTP. O controller segue respondendo **200 sem corpo**, o cliente não
fica sabendo de nada, e a exceção só completa o `CompletionStage` que ninguém
observa. Ou seja, o alerta continua se perdendo sem log, sem erro e sem retry —
agora com um `System.out.println` no caminho de sucesso (o projeto não tem logger
nenhum; `System.out` em serviço é ruído, não observabilidade).

O que funcionaria: **não lançar** dentro do callback — logar o erro ali, com um
logger de verdade, e decidir a política fora dele. Para o cliente saber da falha é
preciso esperar o envio (`send(...).get(timeout)`/`join`), devolver 503, ou
persistir antes de publicar (outbox). E a resposta merece 201 com corpo, como o
`POST /api/portfolio` já faz.

### 30. O contrato do tópico `alert.created`

O que precisa ser combinado com quem consome:

- **Type header.** O `JacksonJsonSerializer` do Spring Kafka 4.1.1 grava
  `__TypeId__` com o nome completo da classe
  (`ChainTracker.portfolioService.dto.AlertKafkaDto`) — `addTypeInfo` é `true`
  por padrão, conferido no jar. O consumidor precisa resolver esse nome; se for
  de outra linguagem, o produtor tem que desligar o header
  (`spring.kafka.producer.properties.spring.json.add.type.headers: false`). Sem
  isso o consumo quebra só na integração, que é quando custa mais caro.
- **`assetId` — resolvido.** O alerta confere o ativo no mercado (404 se não
  existir) e publica o **id canônico**: `cryptoData.assetId()` no cripto e
  `stockData.symbol()` no stock. É esse id que casa com o `alertas:<assetId>` do
  consumidor; antes ia o texto cru do request, então um alerta de `aapl` era
  salvo em `alertas:aapl` e nunca disparava.
- **O alerta não diz o sentido.** Vão só `assetId` e `price`: nada diz se é
  "avisar quando passar de" ou "quando cair abaixo de". Se a direção não viaja
  em outro campo, o consumidor não tem o que fazer com o número.
- **Onde o alerta vive.** O portfolio só publica — não lista, não cancela, não
  guarda. Confirmar que isso é do serviço que consome.

---

## 🔵 Limpeza

### ✅ 12. Imports órfãos

**Resolvido em 2026-10-15:** removidos — `PositionRequestDto` e `Position` do
`PortfolioController`, e `Portfolio`, `Position`, `PortfolioService` e `Param` do
`PositionController`.

### ✅ 13. `spring-boot-starter-test` declarado duas vezes no `pom.xml`

**Resolvido em 2026-10-15:** a segunda declaração saiu.

### ✅ 14. `spring.jpa.open-in-view`

**Resolvido em 2026-10-15:** `spring.jpa.open-in-view: false` no
`application.yaml`.

### ✅ 24. `PositionDataResumeDto` declarado e sem uso

**Resolvido em 2026-10-15:** o arquivo foi apagado — zero referências.

### ✅ 25. O formatador de 2 casas está triplicado

**Resolvido em 2026-10-15:** as três cópias viraram `dto/TwoDecimals.round`,
chamada pelos dois DTOs. O contrato do `null` (null continua null, não vira
`0,00`) ficou documentado lá.

### 26. `PortfolioService` → `PositionService` com `@Lazy`

O `getResume` fez o `PortfolioService` depender do `PositionService`, que já
depende do `PortfolioService` — ciclo quebrado com `@Lazy`. Funciona, mas o
`PortfolioController` poderia orquestrar os dois serviços diretamente e o ciclo
não existiria. Revisitar se a cadeia crescer.

### ✅ 32. `AlertService.objectMapper` era campo morto

**Resolvido em 2026-10-15:** o campo e o import saíram, e as duas classes
passaram a `@RequiredArgsConstructor` + `private final`.

E a armadilha que este item avisava apareceu na prática: no mesmo dia o
`AlertService` ganhou `private DataService dataService;` **sem** `final` — com
`@RequiredArgsConstructor` campo não-final não entra no construtor, fica `null`, e
a validação nova de ativo estourava NPE em toda criação de alerta. Corrigido com
`final`. Os dois andam sempre juntos: `@RequiredArgsConstructor` **e** `final`.

### 33. Javadoc do `calculePNL` — decidir se volta

Removido em `65651a1` e ainda não reposto. Ele documentava o contrato que o
`positionTotalResume` usa nos dois `continue`: `null` = preço desconhecido,
`0` = ativo no preço médio, e os dois não são a mesma coisa. A remoção deixou
uma linha em branco dupla no `Position.java` (entre o `sellAsset` e o
`calculePNL`). Não quebra nada, mas a restauração ficou para depois do push de
2026-10-05 e nunca foi decidida — perguntar antes de mexer no arquivo.

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

7 testes no total — o mesmo número da atualização anterior: os alertas
(2026-10-08) entraram sem teste nenhum. `JwtForwardingInterceptorTest` cobre o
repasse do token; `PositionServiceTest` cobre a média ponderada na compra
repetida de stock, a busca pelo ticker canônico (a regressão do 500) e os 404s
de mercado nulo.

Continuam **sem teste**: `positionTotalResume` (o método inteiro — melhor/pior
performer, os dois `continue` do ranking, o percentual ponderado por valor),
`sellPosition`, `createCryptoWithCustomPricePosition`,
`createStockWithCustomPricePosition`, `findAllPositionByPortfolio`, o
`AlertService` e os fluxos com banco (`PortfolioService`).

### 34. A cadeia de migrations não é exercitada em banco vazio

Um teste que sobe as migrations num Postgres vazio (Testcontainers) pega
exatamente o erro do item 27. É a categoria de falha que o dev não vê: o banco
dele já está montado, então a migration quebrada passa despercebida até alguém
tentar do zero. Não há CI no repositório hoje; quando houver, esse é o primeiro
candidato a rodar lá.

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
  *(Desde 2026-10-05 o path `/cripto` mudou de significado — ver item 21.)*
- **`createStockPosition` sem NPE e com média certa:** guard antes do deref, o
  `buyAsset` fica dono da média ponderada na compra repetida, e mercado nulo
  agora vira 404 no create e na listagem (cripto e stock).
- **Resposta do create devolve a posição como ela ficou:** `quantity` e
  `averagePrice` são os totais da posição (antes devolvia só a compra), igual ao
  que o `GET` já devolvia via `PositionMapper`.
- **`GET /api/portfolio/{id}/resume`** — resumo da carteira
  (`PositionTotalResume`: melhor e pior performer, PnL e valor totais, percentual
  ponderado). Fica no `PositionService` porque é ele que já tem os preços em mão
  no loop da listagem; o `PortfolioService` só delega. Decisões que não são
  óbvias, para não desfazer:
  - Melhor/pior saem do `calculePNL` (**percentual**), não do `pnlValue`: pelo
    valor absoluto a maior posição ganharia sempre, mesmo rendendo menos.
  - Preço desconhecido (`calculePNL` nulo) e posição já vendida (`quantity` 0,
    para a qual o `calculePNL` devolve `0`) ficam **fora do ranking** — um 0%
    falso ganharia de uma carteira inteira no negativo.
  - Empate fica com o primeiro que o banco devolver: não há `ORDER BY`.
  - O total é somado a partir do `Position` **cru**, e o arredondamento para 2
    casas acontece só no construtor compacto do DTO. Somar o
    `PositionResponseDto` já formatado zeraria posição pequena (0,004 → 0,00).
  - `null` continua `null` (carteira sem posição não tem performer nem
    percentual) — não vira `0,00` nem `""`.
- **`BigDecimal` é imutável.** `total.add(x)` sem atribuir descarta o resultado e
  o acumulador fica em `ZERO` para sempre, sem estourar exceção. Era o bug do
  resumo antes de existir `PositionTotalResume`. Vale para toda soma do serviço.
- **O 500 do create de stock em posição existente.** Busca com o texto do request
  e insert com o ticker canônico do mercado (`googl` não achava `GOOGL`) → caía no
  `else` → `INSERT` batia na `uk_position_portfolio_asset`. Agora busca com
  `stockData.symbol()`, coberto por teste.
- **Laço duplicado na listagem e no resumo.** O `if/else` cripto/stock virou
  `currentPriceOf(Position)`; `findAllPositionByPortfolioWithoutOwnerId`,
  `calculateTotalValue` e `calculateTotalPnlValue` foram removidos — o resumo soma
  numa passada só. De quebra sumiu a variante que não checava o dono.
- **Verificado no banco de dev:** zero duplicatas em `(portfolio_id, asset_id)`,
  então o erro do create não vinha de `Optional` com resultado múltiplo — e o
  `IncorrectResultSizeDataAccessException` está descartado como causa.
