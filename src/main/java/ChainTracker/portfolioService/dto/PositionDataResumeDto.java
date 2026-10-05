package ChainTracker.portfolioService.dto;

import java.math.BigDecimal;

public record PositionDataResumeDto(String assetId, BigDecimal pnlValue, BigDecimal pnlPercentage) {
}
