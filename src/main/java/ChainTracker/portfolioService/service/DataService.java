package ChainTracker.portfolioService.service;

import ChainTracker.portfolioService.client.AssetMarketDataClient;
import ChainTracker.portfolioService.dto.CryptoDataResponse;
import ChainTracker.portfolioService.dto.CryptoMarketData;
import ChainTracker.portfolioService.dto.StockDataResponse;
import ChainTracker.portfolioService.dto.StockMarketData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DataService {
    private final AssetMarketDataClient client;



    public CryptoDataResponse getCryptoData(String id){
        return client.getCryptoData(id);
    }

    public List<CryptoMarketData> getCryptoCatalog(){
        return client.getCryptoCatalog();
    }

    /**
     * Normaliza o texto digitado para o id do CoinGecko. O usuario pode enviar
     * o proprio id ("hyperliquid"), o ticker ("HYPE") ou o nome ("Hyperliquid");
     * o restante do fluxo usa sempre o id. Se o catalogo estiver indisponivel,
     * devolve o texto original e deixa o fluxo normal reportar o erro.
     */
    public String resolveCryptoAssetId(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            return assetId;
        }

        String termo = assetId.trim();
        try {
            List<CryptoMarketData> catalogo = getCryptoCatalog();
            if (catalogo == null) {
                return termo;
            }

            for (CryptoMarketData coin : catalogo) {
                if (coin.id() != null && coin.id().equalsIgnoreCase(termo)) {
                    return coin.id();
                }
            }
            for (CryptoMarketData coin : catalogo) {
                if (coin.symbol() != null && coin.symbol().equalsIgnoreCase(termo)) {
                    return coin.id();
                }
            }
            for (CryptoMarketData coin : catalogo) {
                if (coin.name() != null && coin.name().equalsIgnoreCase(termo)) {
                    return coin.id();
                }
            }
        } catch (Exception ignored) {
            // Catalogo indisponivel: mantem o comportamento anterior.
        }
        return termo;
    }

    public StockDataResponse getStockData(String symbol){
        return client.getStockData(symbol);
    }

    public CryptoMarketData getCryptoMarketData(String assetId){
        return client.getCryptoMarketData(assetId);
    }
    public StockMarketData getStockMarketData(String symbol){return client.getStockMarketData(symbol);}
}
