package ChainTracker.portfolioService.client;

import ChainTracker.portfolioService.dto.AssetDataSimpleReponse;
import ChainTracker.portfolioService.dto.AssetMarketData;
import ChainTracker.portfolioService.dto.CoinDataSimpleResponse;
import ChainTracker.portfolioService.dto.MarketResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AssetMarketDataClient {
    private final RestClient restClient;
    private String url = "http://localhost:8082/api";

    // O interceptor repassa o Bearer token de quem chamou o portfolioService.
    // Sem ele o client bate no assetMarketDataService anonimo, e as rotas
    // /api/stock/** e /api/internal/** de la respondem 401.
    public AssetMarketDataClient(RestClient.Builder restClientBuilder, JwtForwardingInterceptor jwtForwardingInterceptor) {
        this.restClient = restClientBuilder
                .baseUrl(url)
                .requestInterceptor(jwtForwardingInterceptor)
                .build();
    }

    public CoinDataSimpleResponse getCoin(String assetId){
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/data/currency/{assetId}")
                        .build(assetId)
                ).retrieve().body(CoinDataSimpleResponse.class);
    }

        public AssetDataSimpleReponse getAsset(String symbol){
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/data/stock/{symbol}")
                        .build(symbol)
                ).retrieve()
                .body(AssetDataSimpleReponse.class);
    }

    public MarketResponse getCoinMarketData(String assetId){
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/currency/{assetId}")
                        .build(assetId)
                ).retrieve()
                .body(MarketResponse.class);
    }

    public AssetMarketData getAssetMarketData(String symbol){
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/stock/{symbol}")
                        .build(symbol)
                        ).retrieve()
                .body(AssetMarketData.class);
    }

}
