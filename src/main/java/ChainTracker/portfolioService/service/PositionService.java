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
            return new PositionResponseDto(assetId, cryptoMarketData.symbol(), cryptoMarketData.name(), position.getQuantity(), position.getAveragePrice(), position.positionValue(currentPrice), position.pnlValue(currentPrice), position.calculePNL(currentPrice));

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

            return new PositionResponseDto(assetId, cryptoMarketData.symbol(), cryptoMarketData.name(), position.getQuantity(), position.getAveragePrice(), position.positionValue(currentPrice), position.pnlValue(currentPrice), position.calculePNL(currentPrice));
        }
    }

    @Transactional
    public PositionResponseDto createStockPosition(String symbol, BigDecimal quantity, String portfolioId, String ownerId) {
        StockDataResponse stockData = dataService.getStockData(symbol);
        if (stockData == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Portfolio portfolio = portfolioService.findByIdAndOwner(portfolioId, ownerId);
        StockMarketData stockMarketData = dataService.getStockMarketData(symbol);

        if (stockMarketData == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        BigDecimal currentPrice = stockMarketData.currentPrice();
        // Busca pelo symbol canonico que voltou do mercado, nao pelo texto do
        // request: e esse valor que vai no asset_id do insert, e a
        // uk_position_portfolio_asset compara exatamente os dois. Buscando com o
        // texto cru, "aapl" nao acha a linha gravada como "AAPL", o fluxo cai no
        // else e o insert esbarra na constraint (500) no lugar de somar.
        Optional<Position> stockPosition = positionRepository.findByPortfolioIdAndAssetId(portfolio.getId(), stockData.symbol());
        if (stockPosition.isPresent()) {
            Position position = stockPosition.get();
            position.buyAsset(quantity, currentPrice);
            position.setName(stockData.name());
            position.setExchange(stockData.exchange());
            position.setSymbol(stockData.symbol());
            position.setType(PositionType.STOCK);
            positionRepository.save(position);
            return new PositionResponseDto(stockData.symbol(), stockData.symbol(), stockData.name(), position.getQuantity(), position.getAveragePrice(), position.positionValue(currentPrice), position.pnlValue(currentPrice), position.calculePNL(currentPrice));

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

            return new PositionResponseDto(stockData.symbol(), stockData.symbol(), stockData.name(), position.getQuantity(), position.getAveragePrice(), position.positionValue(currentPrice), position.pnlValue(currentPrice), position.calculePNL(currentPrice));
        }
    }
    public List<PositionResponseDto> findAllPositionByPortfolio(String portfolioId, String ownerId) {
        Portfolio portfolio = portfolioService.findByIdAndOwner(portfolioId, ownerId);
        List<Position> positionList = positionRepository.findAllByPortfolioId(portfolio.getId());
        List<PositionResponseDto> responseDtos = new ArrayList<>();
        for (Position position : positionList) {
            if(position.getType() == PositionType.CRIPTO) {
                CryptoMarketData cryptoMarketData = dataService.getCryptoMarketData(position.getAssetId());
                if (cryptoMarketData == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND);
                }
                BigDecimal currentPrice = cryptoMarketData.currentPrice();
                PositionResponseDto response = PositionMapper.toResponse(position, currentPrice);
                responseDtos.add(response);
            }
            else {
                StockDataResponse stockData = dataService.getStockData(position.getSymbol());
                if (stockData == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND);
                }
                StockMarketData stockMarketData = dataService.getStockMarketData(stockData.symbol());
                if (stockMarketData == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND);
                }
                BigDecimal currentPrice = stockMarketData.currentPrice();
                PositionResponseDto response = PositionMapper.toResponse(position, currentPrice);
                responseDtos.add(response);
            }
        }
        return responseDtos;
    }
}
