package ChainTracker.portfolioService.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record PositionTotalResume(String bestPerformer, String worstPerformer, BigDecimal portfolioPnlValue, BigDecimal portfolioPnlPercentage, BigDecimal portfolioValue) {

    public PositionTotalResume {
        portfolioPnlValue = toTwoDecimals(portfolioPnlValue);
        portfolioPnlPercentage = toTwoDecimals(portfolioPnlPercentage);
        portfolioValue = toTwoDecimals(portfolioValue);
    }

    // null continua null: "nao sei" (carteira sem custo, totalCost zero) nao pode
    // virar 0,00, que afirmaria "esta no preco medio".
    private static BigDecimal toTwoDecimals(BigDecimal value) {
        if (value == null) {
            return null;
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
