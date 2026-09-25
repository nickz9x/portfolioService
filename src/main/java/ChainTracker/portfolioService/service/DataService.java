package ChainTracker.portfolioService.service;

import ChainTracker.portfolioService.client.AssetMarketDataClient;
import ChainTracker.portfolioService.dto.MarketResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DataService {
    private final AssetMarketDataClient client;



    public MarketResponse getCoin(String id){
        return client.getCoin(id);
    }
}
