package ChainTracker.portfolioService.service;

import ChainTracker.portfolioService.dto.MarketResponse;
import ChainTracker.portfolioService.model.Asset;
import ChainTracker.portfolioService.model.Portfolio;
import ChainTracker.portfolioService.model.Position;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;

@Service
@AllArgsConstructor
public class PositionService {
    private final RedisTemplate<String, Object> redisTemplate;
    private DataService dataService;
    private PortfolioService portfolioService;
    private AssetService assetService;


    public Position createPosition(String assetId, BigDecimal quantity, Long portfolioId) {
        Position position = new Position();
        Portfolio portfolio = portfolioService.findById(portfolioId);
        Object cached = redisTemplate.opsForValue().get("coin:" + assetId);
        MarketResponse response = (MarketResponse) cached;
        if (cached != null) {
            position.setAsset(assetService.findByAssetId(response.id()));
            position.setPortfolio(portfolio);
            position.setQuantity(quantity);
            position.setAveragePrice(response.currentPrice());
        } else {
            try {
                MarketResponse coin = dataService.getCoin(assetId);
                redisTemplate.opsForValue().set("coin:" + assetId, coin, Duration.ofMinutes(2));
                position.setAsset(assetService.findByAssetId(coin.id()));
                position.setPortfolio(portfolio);
                position.setQuantity(quantity);
                position.setAveragePrice(response.currentPrice());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return position;

    }

}
