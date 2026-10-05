package ChainTracker.portfolioService.service;

import ChainTracker.portfolioService.client.AssetMarketDataClient;
import ChainTracker.portfolioService.dto.CryptoDataResponse;
import ChainTracker.portfolioService.dto.CryptoMarketData;
import ChainTracker.portfolioService.dto.StockDataResponse;
import ChainTracker.portfolioService.dto.StockMarketData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DataService {
    private final AssetMarketDataClient client;



    public CryptoDataResponse getCryptoData(String id){
        return client.getCryptoData(id);
    }

    public StockDataResponse getStockData(String symbol){
        return client.getStockData(symbol);
    }

    public CryptoMarketData getCryptoMarketData(String assetId){
        return client.getCryptoMarketData(assetId);
    }
    public StockMarketData getStockMarketData(String symbol){return client.getStockMarketData(symbol);}
}
