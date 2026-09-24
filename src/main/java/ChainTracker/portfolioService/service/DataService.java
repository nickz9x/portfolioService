package ChainTracker.portfolioService.service;

import ChainTracker.portfolioService.client.AssetMarketDataClient;
import ChainTracker.portfolioService.dto.MarketResponse;
import org.springframework.stereotype.Service;

@Service
public class DataService {
    private AssetMarketDataClient client;



    public MarketResponse getCoin(String id){
        return client.getCoin(id);
    }
}
