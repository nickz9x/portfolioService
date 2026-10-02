package ChainTracker.portfolioService.service;

import ChainTracker.portfolioService.dto.MarketResponse;
import ChainTracker.portfolioService.dto.PositionResponseDto;
import ChainTracker.portfolioService.model.Asset;
import ChainTracker.portfolioService.model.Portfolio;
import ChainTracker.portfolioService.model.Position;
import ChainTracker.portfolioService.repository.PositionRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class PositionService {
    private PortfolioService portfolioService;
    private AssetService assetService;
    private PositionRepository positionRepository;


    @Transactional
    public PositionResponseDto createPosition(String assetId, BigDecimal quantity, String portfolioId, String ownerId) {
        Portfolio portfolio = portfolioService.findByIdAndOwner(portfolioId, ownerId);

        Optional<Position> byPortfolioIdAndAssetId = positionRepository.findByPortfolioIdAndAssetId(portfolio.getId(), asset.getId());
//        if (byPortfolioIdAndAssetId.isPresent()){
//            Position position = byPortfolioIdAndAssetId.get();
//            position.buyAsset(quantity, quote.currentPrice());
//            positionRepository.save(position);
//        } else {
//            Position position = new Position();
//            position.setQuantity(quantity);
//            position.setAsset(asset);
//            position.setPortfolio(portfolio);
//            position.setAveragePrice(quote.currentPrice());
//            positionRepository.save(position);
//        }
//          return new PositionResponseDto(assetId,
//                asset.getSymbol(),
//                asset.getName(),
//                quantity,
//                quote.currentPrice(),BigDecimal.ZERO
//                );
    }

    public List<PositionResponseDto> findAllPositionByPortfolio(String portfolioId, String ownerId){
        Portfolio portfolio = portfolioService.findByIdAndOwner(portfolioId, ownerId);
        List<Position> positions = positionRepository.findAllByPortfolioId(portfolio.getId());

        if (positions.isEmpty()){
            return List.of();
        }

        Set<String> assetIds = positions.stream()
                .map(position -> position.getAsset().getAssetId())
                .collect(Collectors.toSet());

        // Uma ida ao Redis para toda a carteira, em vez de uma por posicao.
        Map<String, MarketResponse> quotes = assetService.getQuotes(assetIds);

        return positions.stream()
                .map(position -> {
                    MarketResponse quote = quotes.get(position.getAsset().getAssetId());
                    BigDecimal currentPrice = quote == null ? null : quote.currentPrice();

                    return new PositionResponseDto(
                            position.getAsset().getAssetId(),
                            position.getAsset().getSymbol(),
                            position.getAsset().getName(),
                            position.getQuantity(),
                            position.getAveragePrice(),
                            position.calculePNL(currentPrice));
                })
                .toList();
    }
}
