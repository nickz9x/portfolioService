package ChainTracker.portfolioService.dto;

import java.math.BigDecimal;

public record PositionResponseDto(String assetId, String symbol, String name, BigDecimal quantity,BigDecimal averagePrice,BigDecimal pnl) {
}
