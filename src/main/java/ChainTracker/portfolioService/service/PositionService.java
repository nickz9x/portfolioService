package ChainTracker.portfolioService.service;

import ChainTracker.portfolioService.dto.*;
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
import java.math.RoundingMode;
import java.util.*;

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
        if (cryptoMarketData == null || cryptoMarketData.symbol() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        BigDecimal currentPrice = cryptoMarketData.currentPrice();
        Optional<Position> cryptoPosition = positionRepository.findByPortfolioIdAndAssetId(portfolio.getId(), assetId);
        if (cryptoPosition.isPresent()) {
            Position position = cryptoPosition.get();
            position.buyAsset(quantity, currentPrice);
            position.setName(cryptoMarketData.name());
            position.setType(PositionType.CRIPTO);
            positionRepository.saveAndFlush(position);
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
    public void sellPosition(String assetId, BigDecimal quantity, String portfolioId, String ownerId) {
        Portfolio portfolio = portfolioService.findByIdAndOwner(portfolioId, ownerId);
        Position position = positionRepository.findByPortfolioIdAndAssetId(portfolio.getId(), assetId).orElseThrow(() -> new IllegalArgumentException("portfolio ou ativo invalido"));
        position.sellAsset(quantity);
    }


    @Transactional
    public PositionResponseDto createStockPosition(String symbol, BigDecimal quantity, String portfolioId, String ownerId) {
        StockDataResponse stockData = dataService.getStockData(symbol);
        if (stockData == null || stockData.symbol() == null) {
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
        List<PositionResponseDto> responseDtos = new ArrayList<>();
        for (Position position : positionRepository.findAllByPortfolioId(portfolio.getId())) {
            responseDtos.add(PositionMapper.toResponse(position, currentPriceOf(position)));
        }
        return responseDtos;
    }

    /**
     * Preco atual da posicao, buscado no assetMarketDataService conforme o tipo.
     * 404 quando o mercado nao devolve o dado — mesma regra do create.
     */
    private BigDecimal currentPriceOf(Position position) {
        if (position.getType() == PositionType.CRIPTO) {
            CryptoMarketData cryptoMarketData = dataService.getCryptoMarketData(position.getAssetId());
            if (cryptoMarketData == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            return cryptoMarketData.currentPrice();
        }
        StockDataResponse stockData = dataService.getStockData(position.getSymbol());
        if (stockData == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        StockMarketData stockMarketData = dataService.getStockMarketData(stockData.symbol());
        if (stockMarketData == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return stockMarketData.currentPrice();
    }

    /**
     * Par (posicao, desempenho). O placar anda junto da posicao que o gerou —
     * com dois acumuladores soltos existe o risco de "melhor = BTC" guardado com
     * o percentual do ETH.
     */
    private record Performer(Position position, BigDecimal pnlPercentage) {
    }

    public PositionTotalResume positionTotalResume(String portfolioId, String ownerId) {
        Portfolio portfolio = portfolioService.findByIdAndOwner(portfolioId, ownerId);

        // Soma a partir do Position cru, e nao do PositionResponseDto: o DTO ja
        // arredonda para 2 casas no construtor compacto, e somar valor de tela
        // zeraria posicao pequena (0,004 viraria 0,00).
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        Performer best = null;
        Performer worst = null;

        for (Position position : positionRepository.findAllByPortfolioId(portfolio.getId())) {
            BigDecimal currentPrice = currentPriceOf(position);
            totalValue = totalValue.add(position.positionValue(currentPrice));
            totalCost = totalCost.add(position.getAveragePrice().multiply(position.getQuantity()));

            // Percentual, nao valor: "desempenho" e relativo. Pelo valor absoluto
            // a maior posicao ganharia sempre, mesmo rendendo menos.
            BigDecimal positionPnl = position.calculePNL(currentPrice);
            // null = preco desconhecido. Fica fora do ranking em vez de entrar
            // como 0 — nao saber o desempenho nao e o mesmo que nao ter desempenho.
            if (positionPnl == null) {
                continue;
            }
            // Vendida por completo (quantity 0): o calculePNL devolve 0, e esse 0
            // ganharia de qualquer carteira no negativo. Nao concorre.
            // A partir daqui quantity nao e null — o calculePNL ja barrou acima.
            if (position.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            if (best == null || positionPnl.compareTo(best.pnlPercentage()) > 0) {
                best = new Performer(position, positionPnl);
            }
            if (worst == null || positionPnl.compareTo(worst.pnlPercentage()) < 0) {
                worst = new Performer(position, positionPnl);
            }
        }

        // Somar pnlValue de cada posicao da o mesmo que totalValue - totalCost,
        // e o percentual da carteira e ponderado por valor, nao media dos percentuais.
        BigDecimal pnlValue = totalValue.subtract(totalCost);
        BigDecimal pnlPercentage = totalCost.compareTo(BigDecimal.ZERO) == 0
                ? null
                : pnlValue.divide(totalCost, 12, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        return new PositionTotalResume(
                best == null ? null : best.position().getSymbol(),
                worst == null ? null : worst.position().getSymbol(),
                pnlValue,
                pnlPercentage,
                totalValue);
    }

    @Transactional
    public PositionResponseDto createCryptoWithCustomPricePosition(String assetId, BigDecimal quantity, BigDecimal price, String portfolioId, String ownerId) {
        Portfolio portfolio = portfolioService.findByIdAndOwner(portfolioId, ownerId);
        CryptoMarketData cryptoMarketData = dataService.getCryptoMarketData(assetId);
        if (cryptoMarketData == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        BigDecimal currentPrice = cryptoMarketData.currentPrice();
        Optional<Position> cryptoPosition = positionRepository.findByPortfolioIdAndAssetId(portfolio.getId(), assetId);
        if (cryptoPosition.isPresent()) {
            Position position = cryptoPosition.get();
            position.buyAsset(quantity, price);
            position.setName(cryptoMarketData.name());
            position.setType(PositionType.CRIPTO);
            positionRepository.save(position);
            return new PositionResponseDto(assetId, cryptoMarketData.symbol(), cryptoMarketData.name(),
                    position.getQuantity(), position.getAveragePrice(), position.positionValue(currentPrice),
                    position.pnlValue(currentPrice), position.calculePNL(currentPrice));

        } else {
            Position position = new Position();
            position.setAssetId(assetId);
            position.setQuantity(quantity);
            position.setPortfolio(portfolio);
            position.setSymbol(cryptoMarketData.symbol());
            position.setName(cryptoMarketData.name());
            position.setAveragePrice(price);
            position.setType(PositionType.CRIPTO);
            positionRepository.save(position);

            return new PositionResponseDto(assetId, cryptoMarketData.symbol(), cryptoMarketData.name(),
                    position.getQuantity(), position.getAveragePrice(), position.positionValue(currentPrice),
                    position.pnlValue(currentPrice), position.calculePNL(currentPrice));
        }
    }
    @Transactional
    public PositionResponseDto createStockWithCustomPricePosition(String symbol, BigDecimal quantity, BigDecimal price, String portfolioId, String ownerId) {
        Portfolio portfolio = portfolioService.findByIdAndOwner(portfolioId, ownerId);
        StockMarketData stockMarketData = dataService.getStockMarketData(symbol);
        if (stockMarketData == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        StockDataResponse stockData = dataService.getStockData(symbol);
        BigDecimal currentPrice = stockMarketData.currentPrice();
        Optional<Position> stockPosition = positionRepository.findByPortfolioIdAndAssetId(portfolio.getId(), stockData.symbol());
        if (stockPosition.isPresent()) {
            Position position = stockPosition.get();
            position.buyAsset(quantity, price);
            position.setName(stockData.name());
            position.setType(PositionType.STOCK);
            positionRepository.save(position);
            return new PositionResponseDto(stockData.symbol(), stockData.symbol(), stockData.name(),
                    position.getQuantity(), position.getAveragePrice(), position.positionValue(currentPrice),
                    position.pnlValue(currentPrice), position.calculePNL(currentPrice));

        } else {
            Position position = new Position();
            position.setAssetId(stockData.symbol());
            position.setQuantity(quantity);
            position.setPortfolio(portfolio);
            position.setSymbol(stockData.symbol());
            position.setName(stockData.name());
            position.setAveragePrice(price);
            position.setType(PositionType.STOCK);
            positionRepository.save(position);

            return new PositionResponseDto(stockData.symbol(), stockData.symbol(), stockData.name(),
                    position.getQuantity(), position.getAveragePrice(), position.positionValue(currentPrice),
                    position.pnlValue(currentPrice), position.calculePNL(currentPrice));
        }
    }
}
