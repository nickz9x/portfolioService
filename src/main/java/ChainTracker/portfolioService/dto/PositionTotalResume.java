package ChainTracker.portfolioService.dto;

import java.math.BigDecimal;

public record PositionTotalResume(String bestPerformer, String worstPerformer, BigDecimal portfolioPnlValue, BigDecimal portfolioPnlPercentage, BigDecimal portfolioValue) {

    public PositionTotalResume {
        portfolioPnlValue = TwoDecimals.round(portfolioPnlValue);
        portfolioPnlPercentage = TwoDecimals.round(portfolioPnlPercentage);
        portfolioValue = TwoDecimals.round(portfolioValue);
    }
}
