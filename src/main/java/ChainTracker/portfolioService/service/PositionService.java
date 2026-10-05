package ChainTracker.portfolioService.service;

import ChainTracker.portfolioService.dto.CryptoMarketData;
import ChainTracker.portfolioService.dto.PositionResponseDto;
import ChainTracker.portfolioService.dto.StockDataResponse;
import ChainTracker.portfolioService.dto.StockMarketData;
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
    public PositionResponseDto createCryptoPosition(String assetId, BigDecimal quantity, String portfolioId, String ownerId) {
        Portfolio portfolio = portfolioService.findByIdAndOwner(portfolioId, ownerId);
        CryptoMarketData cryptoMarketData = dataService.getCryptoMarketData(assetId);
        if (cryptoMarketData == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        BigDecimal currentPrice = cryptoMarketData.currentPrice();
        Optional<Position> cryptoPosition = positionRepository.findByPortfolioIdAndAssetId(portfolio.getId(), assetId);
        if (cryptoPosition.isPresent()) {
            Position position = cryptoPosition.get();
            position.buyAsset(quantity, currentPrice);
            position.setName(cryptoMarketData.name());
            position.setType(PositionType.CRIPTO);
            positionRepository.save(position);
            return new PositionResponseDto(assetId, cryptoMarketData.symbol(), cryptoMarketData.name(), quantity, position.getAveragePrice(), position.positionValue(currentPrice),position.pnlValue(currentPrice),position.calculePNL(currentPrice));

        } else {
            Position position = new Position();
            position.setAssetId(assetId);
            position.setQuantity(quantity);
            position.setPortfolio(portfolio);
            position.setSymbol(cryptoMarketData.symbol());
            position.setName(cryptoMarketData.name());
            position.setAveragePrice(cryptoMarketData.currentPrice());
            position.setType(PositionType.CRIPTO);
            positionRepository.save(position);

            return new PositionResponseDto(assetId, cryptoMarketData.symbol(), cryptoMarketData.name(), quantity, position.getAveragePrice(),position.positionValue(currentPrice),position.pnlValue(currentPrice) ,position.calculePNL(currentPrice));
        }
    }

    @Transactional
    public PositionResponseDto createStockPosition(String symbol, BigDecimal quantity, String portfolioId, String ownerId) {
        StockDataResponse stockData = dataService.getStockData(symbol);
        Portfolio portfolio = portfolioService.findByIdAndOwner(portfolioId, ownerId);
        StockMarketData stockMarketData = dataService.getStockMarketData(symbol);

        if (stockMarketData == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        BigDecimal currentPrice = stockMarketData.currentPrice();
        Optional<Position> stockPosition = positionRepository.findByPortfolioIdAndAssetId(portfolio.getId(), symbol);
        if (stockPosition.isPresent()) {
            Position position = stockPosition.get();
            position.buyAsset(quantity, currentPrice);
            position.setName(stockData.name());
            position.setExchange(stockData.exchange());
            position.setSymbol(stockData.symbol());
            position.setType(PositionType.STOCK);
            positionRepository.save(position);
            //tem q refazer esse dto pra casar com dto das cripto
            return new PositionResponseDto(stockData.symbol(),stockData.symbol(),stockData.name(),quantity,currentPrice,position.positionValue(currentPrice),position.pnlValue(currentPrice),position.calculePNL(currentPrice));

        } else {
            Position position = new Position();
            position.setAssetId(stockData.symbol());
            position.setQuantity(quantity);
            position.setPortfolio(portfolio);
            position.setSymbol(stockData.symbol());
            position.setName(stockData.name());
            position.setType(PositionType.STOCK);
            position.setAveragePrice(currentPrice);
            positionRepository.save(position);

            return new PositionResponseDto(stockData.symbol(),stockData.symbol(),stockData.name(),quantity,currentPrice,position.positionValue(currentPrice),position.pnlValue(currentPrice),position.calculePNL(currentPrice));
        }
    }
    public List<PositionResponseDto> findAllPositionByPortfolio(String portfolioId, String ownerId) {
        Portfolio portfolio = portfolioService.findByIdAndOwner(portfolioId, ownerId);
        List<Position> positionList = positionRepository.findAllByPortfolioId(portfolio.getId());
        List<PositionResponseDto> responseDtos = new ArrayList<>();
        for (Position position : positionList) {
            if(position.getType() == PositionType.CRIPTO) {
                CryptoMarketData cryptoMarketData = dataService.getCryptoMarketData(position.getAssetId());
                BigDecimal currentPrice = cryptoMarketData.currentPrice();
                PositionResponseDto response = PositionMapper.toResponse(position, currentPrice);
                responseDtos.add(response);
            }
            else {
                StockDataResponse stockData = dataService.getStockData(position.getSymbol());
                StockMarketData stockMarketData = dataService.getStockMarketData(stockData.symbol());
                BigDecimal currentPrice = stockMarketData.currentPrice();
                PositionResponseDto response = PositionMapper.toResponse(position, currentPrice);
                responseDtos.add(response);
            }
        }
        return responseDtos;
    }
}
