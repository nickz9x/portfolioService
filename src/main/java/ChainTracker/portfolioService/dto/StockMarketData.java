package ChainTracker.portfolioService.dto;

import java.math.BigDecimal;

public record StockMarketData(String symbol,
                              BigDecimal currentPrice,
                              BigDecimal openingPrice,
                              BigDecimal highestPrice,
                              BigDecimal lowestPrice) {
}
