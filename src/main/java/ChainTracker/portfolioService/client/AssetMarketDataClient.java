package ChainTracker.portfolioService.client;

import ChainTracker.portfolioService.dto.CryptoDataResponse;
import ChainTracker.portfolioService.dto.CryptoMarketData;
import ChainTracker.portfolioService.dto.StockDataResponse;
import ChainTracker.portfolioService.dto.StockMarketData;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Component
public class AssetMarketDataClient {
    private final RestClient restClient;
    private String url = "http://assetmarketdata-service:8082/api";

    public AssetMarketDataClient(RestClient.Builder restClientBuilder, JwtForwardingInterceptor jwtForwardingInterceptor) {
        this.restClient = restClientBuilder
                .baseUrl(url)
                .requestInterceptor(jwtForwardingInterceptor)
                .build();
    }

    /**
     * Catalogo publico das 100 maiores moedas (id/symbol/name). Usado para
     * aceitar ticker ou nome no create de posicao, nao so o id do CoinGecko.
     */
    public List<CryptoMarketData> getCryptoCatalog(){
        return restClient.get()
                .uri(isBotRequest() ? "/bot/currency" : "/currency")
                .retrieve()
                .body(new ParameterizedTypeReference<List<CryptoMarketData>>() {});
    }

    public CryptoDataResponse getCryptoData(String assetId){
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(isBotRequest()
                                ? "/bot/currency/{assetId}/data"
                                : "/internal/data/currency/{assetId}")
                        .build(assetId)
                ).retrieve().body(CryptoDataResponse.class);
    }

    public StockDataResponse getStockData(String symbol){
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(isBotRequest()
                                ? "/bot/stock/{symbol}/data"
                                : "/internal/data/stock/{symbol}")
                        .build(symbol)
                ).retrieve()
                .body(StockDataResponse.class);
    }

    public CryptoMarketData getCryptoMarketData(String assetId){
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(isBotRequest()
                                ? "/bot/currency/{assetId}"
                                : "/currency/{assetId}")
                        .build(assetId)
                ).retrieve()
                .body(CryptoMarketData.class);
    }

    public StockMarketData getStockMarketData(String symbol){
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(isBotRequest()
                                ? "/bot/stock/{symbol}"
                                : "/stock/{symbol}")
                        .build(symbol)
                        ).retrieve()
                .body(StockMarketData.class);
    }

    /**
     * O mesmo client atende o client web (rotas autenticadas) e o bot (rotas
     * /api/bot/**, liberadas). A escolha vem do path da requisicao que o
     * portfolioService esta processando neste momento: se ela veio de
     * /api/bot/**, as chamadas de saida tambem usam as copias publicas.
     */
    private static boolean isBotRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequest) {
            return servletRequest.getRequest().getRequestURI().startsWith("/api/bot/");
        }
        return false;
    }
}
