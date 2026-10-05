package ChainTracker.portfolioService.service;

import ChainTracker.portfolioService.dto.AssetDataSimpleReponse;
import ChainTracker.portfolioService.dto.AssetMarketData;
import ChainTracker.portfolioService.dto.MarketResponse;
import ChainTracker.portfolioService.dto.PositionResponseDto;
import ChainTracker.portfolioService.enums.PositionType;
import ChainTracker.portfolioService.mapper.PositionMapper;
import ChainTracker.portfolioService.model.Portfolio;
import ChainTracker.portfolioService.model.Position;
import ChainTracker.portfolioService.repository.PositionRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class PositionService {
    private PortfolioService portfolioService;
    private DataService dataService;
    private PositionRepository positionRepository;


    @Transactional
    public PositionResponseDto createAssetPosition(String assetId, BigDecimal quantity, String portfolioId, String ownerId) {
        Portfolio portfolio = portfolioService.findByIdAndOwner(portfolioId, ownerId);
        MarketResponse coinMarketData = dataService.getCoinMarketData(assetId);
        if (coinMarketData == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        BigDecimal currentPrice = coinMarketData.currentPrice();
        Optional<Position> cryptoPosition = positionRepository.findByPortfolioIdAndAssetId(portfolio.getId(), assetId);
        if (cryptoPosition.isPresent()) {
            Position position = cryptoPosition.get();
            position.buyAsset(quantity, currentPrice);
            position.setName(coinMarketData.name());
            position.setType(PositionType.CRIPTO);
            positionRepository.save(position);
            return new PositionResponseDto(assetId, coinMarketData.symbol(), coinMarketData.name(), quantity, position.getAveragePrice(), position.positionValue(currentPrice),position.pnlValue(currentPrice),position.calculePNL(currentPrice));

        } else {
            Position position = new Position();
            position.setAssetId(assetId);
            position.setQuantity(quantity);
            position.setPortfolio(portfolio);
            position.setSymbol(coinMarketData.symbol());
            position.setName(coinMarketData.name());
            position.setAveragePrice(coinMarketData.currentPrice());
            position.setType(PositionType.CRIPTO);
            positionRepository.save(position);

            return new PositionResponseDto(assetId, coinMarketData.symbol(), coinMarketData.name(), quantity, position.getAveragePrice(),position.positionValue(currentPrice),position.pnlValue(currentPrice) ,position.calculePNL(currentPrice));
        }
    }

    @Transactional
    public PositionResponseDto createStockPosition(String symbol, BigDecimal quantity, String portfolioId, String ownerId) {
        AssetDataSimpleReponse staticData = dataService.getAsset(symbol);
        Portfolio portfolio = portfolioService.findByIdAndOwner(portfolioId, ownerId);
        AssetMarketData assetMarketData = dataService.getAssetMarketData(symbol);
        BigDecimal currentedPrice = assetMarketData.currentPrice();
        if (assetMarketData == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        BigDecimal currentPrice = assetMarketData.currentPrice();
        Optional<Position> stockPosition = positionRepository.findByPortfolioIdAndAssetId(portfolio.getId(), symbol);
        if (stockPosition.isPresent()) {
            Position position = stockPosition.get();
            position.buyAsset(quantity, currentPrice);
            position.setName(staticData.name());
            position.setExchange(staticData.exchange());
            position.setSymbol(staticData.symbol());
            position.setType(PositionType.STOCK);
            position.setAveragePrice(currentedPrice);
            positionRepository.save(position);
            //tem q refazer esse dto pra casar com dto das cripto
            return new PositionResponseDto(staticData.symbol(),staticData.symbol(),staticData.name(),quantity,currentedPrice,position.positionValue(currentPrice),position.pnlValue(currentPrice),position.calculePNL(currentPrice));

        } else {
            Position position = new Position();
            position.setAssetId(staticData.symbol());
            position.setQuantity(quantity);
            position.setPortfolio(portfolio);
            position.setSymbol(staticData.symbol());
            position.setName(staticData.name());
            position.setAveragePrice(assetMarketData.currentPrice());
            position.setType(PositionType.STOCK);
            position.setAveragePrice(currentedPrice);
            positionRepository.save(position);

            return new PositionResponseDto(staticData.symbol(),staticData.symbol(),staticData.name(),quantity,currentedPrice,position.positionValue(currentPrice),position.pnlValue(currentPrice),position.calculePNL(currentPrice));
        }
    }
    public List<PositionResponseDto> findAllPositionByPortfolio(String portfolioId, String ownerId) {
        Portfolio portfolio = portfolioService.findByIdAndOwner(portfolioId, ownerId);
        List<Position> positionList = positionRepository.findAllByPortfolioId(portfolio.getId());
        List<PositionResponseDto> responseDtos = new ArrayList<>();
        for (Position position : positionList) {
            if(position.getType() == PositionType.CRIPTO) {
                MarketResponse coinMarketData = dataService.getCoinMarketData(position.getAssetId());
                BigDecimal currentPrice = coinMarketData.currentPrice();
                PositionResponseDto response = PositionMapper.toResponse(position, currentPrice);
                responseDtos.add(response);
            }
            else {
                AssetDataSimpleReponse asset = dataService.getAsset(position.getSymbol());
                AssetMarketData assetMarketData = dataService.getAssetMarketData(asset.symbol());
                BigDecimal currentPrice = assetMarketData.currentPrice();
                PositionResponseDto response = PositionMapper.toResponse(position, currentPrice);
                responseDtos.add(response);
            }
        }
        return responseDtos;
    }
}
