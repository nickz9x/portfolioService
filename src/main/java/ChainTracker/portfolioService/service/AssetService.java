package ChainTracker.portfolioService.service;

import ChainTracker.portfolioService.dto.AssetRequestDto;
import ChainTracker.portfolioService.dto.MarketResponse;
import ChainTracker.portfolioService.model.Asset;
import ChainTracker.portfolioService.model.Portfolio;
import ChainTracker.portfolioService.model.Position;
import ChainTracker.portfolioService.repository.AssetRepository;
import ChainTracker.portfolioService.repository.PortfolioRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;

@Service
@AllArgsConstructor
public class AssetService {
    private final RedisTemplate<String,Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private AssetRepository assetRepository;
    private DataService dataService;

    @KafkaListener(
            topics = "market.updated",
            groupId = "portfolio.consumer"
    )
    public void consumeAsset(String payload){
        MarketResponse marketResponse = objectMapper.readValue(payload, MarketResponse.class);

        redisTemplate.opsForValue().set("coin:"+marketResponse.id(), marketResponse, Duration.ofMinutes(2));
    }


    public void save(AssetRequestDto dto){
        if (!assetRepository.existsByassetId(dto.id())){
            Asset asset = new Asset();
            asset.setAssetId(dto.id());
            asset.setName(dto.name());
            asset.setSymbol(dto.symbol());
            assetRepository.save(asset);
        }
    }
    public MarketResponse getQuote(String assetId){
        Object cached = redisTemplate.opsForValue().get("coin:" + assetId);
        MarketResponse response = (MarketResponse) cached;
        if (cached != null){
            return response;
        }
        MarketResponse coin = dataService.getCoin(assetId);
        redisTemplate.opsForValue().set("coin:"+assetId, coin,Duration.ofMinutes(2));
        return coin;
    };

    public Asset findByAssetId(String assetId){
        return assetRepository.findByAssetId(assetId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
