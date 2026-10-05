package ChainTracker.portfolioService.service;

import ChainTracker.portfolioService.dto.PositionResponseDto;
import ChainTracker.portfolioService.dto.StockDataResponse;
import ChainTracker.portfolioService.dto.StockMarketData;
import ChainTracker.portfolioService.enums.PositionType;
import ChainTracker.portfolioService.model.Portfolio;
import ChainTracker.portfolioService.model.Position;
import ChainTracker.portfolioService.repository.PositionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

    @Mock
    private PortfolioService portfolioService;
    @Mock
    private DataService dataService;
    @Mock
    private PositionRepository positionRepository;
    @InjectMocks
    private PositionService positionService;

    private static final String PORTFOLIO_ID = "portfolio-1";
    private static final String OWNER_ID = "owner-1";

    @Test
    void repeatStockBuyKeepsWeightedAverage() {
        Portfolio portfolio = new Portfolio();
        portfolio.setId(PORTFOLIO_ID);
        when(portfolioService.findByIdAndOwner(PORTFOLIO_ID, OWNER_ID)).thenReturn(portfolio);
        when(dataService.getStockData("AAPL")).thenReturn(new StockDataResponse("AAPL", "Apple", "NASDAQ"));
        when(dataService.getStockMarketData("AAPL")).thenReturn(new StockMarketData("AAPL", new BigDecimal("150"), null, null, null));

        Position existing = new Position();
        existing.setAssetId("AAPL");
        existing.setQuantity(new BigDecimal("10"));
        existing.setAveragePrice(new BigDecimal("100"));
        existing.setType(PositionType.STOCK);
        when(positionRepository.findByPortfolioIdAndAssetId(PORTFOLIO_ID, "AAPL")).thenReturn(Optional.of(existing));

        PositionResponseDto response = positionService.createStockPosition("AAPL", new BigDecimal("10"), PORTFOLIO_ID, OWNER_ID);

        // 10 a 100 + 10 a 150 = 20 a 125 de media
        assertThat(existing.getQuantity()).isEqualByComparingTo("20");
        assertThat(existing.getAveragePrice()).isEqualByComparingTo("125");
        assertThat(response.quantity()).isEqualByComparingTo("20");
        assertThat(response.averagePrice()).isEqualByComparingTo("125");
    }

    @Test
    void repeatStockBuyLooksUpByTheCanonicalSymbol() {
        Portfolio portfolio = new Portfolio();
        portfolio.setId(PORTFOLIO_ID);
        when(portfolioService.findByIdAndOwner(PORTFOLIO_ID, OWNER_ID)).thenReturn(portfolio);
        // request em minusculas, o mercado devolve o ticker canonico
        when(dataService.getStockData("aapl")).thenReturn(new StockDataResponse("AAPL", "Apple", "NASDAQ"));
        when(dataService.getStockMarketData("aapl")).thenReturn(new StockMarketData("AAPL", new BigDecimal("150"), null, null, null));

        Position existing = new Position();
        existing.setAssetId("AAPL");
        existing.setQuantity(new BigDecimal("10"));
        existing.setAveragePrice(new BigDecimal("100"));
        existing.setType(PositionType.STOCK);
        // a linha gravada esta sob "AAPL", nao sob o texto que veio no request
        when(positionRepository.findByPortfolioIdAndAssetId(PORTFOLIO_ID, "AAPL")).thenReturn(Optional.of(existing));

        PositionResponseDto response = positionService.createStockPosition("aapl", new BigDecimal("10"), PORTFOLIO_ID, OWNER_ID);

        assertThat(existing.getQuantity()).isEqualByComparingTo("20");
        assertThat(existing.getAveragePrice()).isEqualByComparingTo("125");
        assertThat(response.quantity()).isEqualByComparingTo("20");
    }

    @Test
    void stockWithoutMarketDataReturns404() {
        Portfolio portfolio = new Portfolio();
        portfolio.setId(PORTFOLIO_ID);
        when(portfolioService.findByIdAndOwner(PORTFOLIO_ID, OWNER_ID)).thenReturn(portfolio);
        when(dataService.getStockData("AAPL")).thenReturn(new StockDataResponse("AAPL", "Apple", "NASDAQ"));
        when(dataService.getStockMarketData("AAPL")).thenReturn(null);

        assertThatThrownBy(() -> positionService.createStockPosition("AAPL", BigDecimal.ONE, PORTFOLIO_ID, OWNER_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void stockWithoutStaticDataReturns404() {
        when(dataService.getStockData("AAPL")).thenReturn(null);

        assertThatThrownBy(() -> positionService.createStockPosition("AAPL", BigDecimal.ONE, PORTFOLIO_ID, OWNER_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }
}
