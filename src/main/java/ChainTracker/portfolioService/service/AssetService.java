package ChainTracker.portfolioService.service;

import ChainTracker.portfolioService.dto.AssetDataSimpleReponse;
import ChainTracker.portfolioService.dto.CoinDataSimpleResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AssetService {
    private DataService dataService;

    public CoinDataSimpleResponse getCoin(String assetId){
        return dataService.getCoin(assetId);
    }

    public AssetDataSimpleReponse getAsset(String symbol){
        return dataService.getAsset(symbol);
    }

}
