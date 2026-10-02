package ChainTracker.portfolioService.service;

import ChainTracker.portfolioService.client.AssetMarketDataClient;
import ChainTracker.portfolioService.dto.AssetDataSimpleReponse;
import ChainTracker.portfolioService.dto.CoinDataSimpleResponse;
import ChainTracker.portfolioService.dto.MarketResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DataService {
    private final AssetMarketDataClient client;



    public CoinDataSimpleResponse getCoin(String id){
        return client.getCoin(id);
    }

    public AssetDataSimpleReponse getAsset(String symbol){
        return client.getAsset(symbol);
    }

}
