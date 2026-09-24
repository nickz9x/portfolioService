package ChainTracker.portfolioService.client;

import ChainTracker.portfolioService.dto.MarketResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AssetMarketDataClient {
    private final RestClient restClient;
    private String url = "http://localhost:8082/api";

    public AssetMarketDataClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl(url).build();
    }

    public MarketResponse getCoin(String id){
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/currency/{id}")
                        .build(id)
                ).retrieve().body(MarketResponse.class);
    }
}
