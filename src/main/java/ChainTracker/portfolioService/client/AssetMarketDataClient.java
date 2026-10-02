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

    public AssetMarketDataClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl(url).build();
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
