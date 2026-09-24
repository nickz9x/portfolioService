package ChainTracker.portfolioService.service;

import ChainTracker.portfolioService.dto.MarketResponse;
import ChainTracker.portfolioService.model.Asset;
import ChainTracker.portfolioService.model.Portfolio;
import ChainTracker.portfolioService.model.Position;
import ChainTracker.portfolioService.repository.PositionRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PositionService {
    private PortfolioService portfolioService;
    private AssetService assetService;
    private PositionRepository positionRepository;


    @Transactional
    public Position createPosition(String assetId, BigDecimal quantity, String portfolioId, String ownerId) {

        Asset asset = assetService.findByAssetId(assetId);
        Portfolio portfolio = portfolioService.findByIdAndOwner(portfolioId, ownerId);
        MarketResponse quote = assetService.getQuote(assetId);
        Optional<Position> byPortfolioIdAndAssetId = positionRepository.findByPortfolioIdAndAssetId(portfolio.getId(), asset.getId());
        if (byPortfolioIdAndAssetId.isPresent()){
            Position position = byPortfolioIdAndAssetId.get();
            position.buyAsset(quantity,quote.currentPrice());
            return positionRepository.save(position);
        }
        Position position = new Position();
        position.setQuantity(quantity);
        position.setAsset(asset);
        position.setPortfolio(portfolio);
        position.setAveragePrice(quote.currentPrice());
        return positionRepository.save(position);
    }

}
