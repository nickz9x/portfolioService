package ChainTracker.portfolioService.client;

import ChainTracker.portfolioService.dto.CryptoDataResponse;
import ChainTracker.portfolioService.dto.CryptoMarketData;
import ChainTracker.portfolioService.dto.StockDataResponse;
import ChainTracker.portfolioService.dto.StockMarketData;
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

    public CryptoDataResponse getCryptoData(String assetId){
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/data/currency/{assetId}")
                        .build(assetId)
                ).retrieve().body(CryptoDataResponse.class);
    }

    public StockDataResponse getStockData(String symbol){
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/data/stock/{symbol}")
                        .build(symbol)
                ).retrieve()
                .body(StockDataResponse.class);
    }

    public CryptoMarketData getCryptoMarketData(String assetId){
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/currency/{assetId}")
                        .build(assetId)
                ).retrieve()
                .body(CryptoMarketData.class);
    }

    public StockMarketData getStockMarketData(String symbol){
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/stock/{symbol}")
                        .build(symbol)
                        ).retrieve()
                .body(StockMarketData.class);
    }

}
