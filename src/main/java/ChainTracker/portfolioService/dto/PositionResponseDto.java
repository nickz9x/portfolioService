package ChainTracker.portfolioService.dto;

import java.math.BigDecimal;

import java.math.RoundingMode;

public record PositionResponseDto(
        String assetId,
        String symbol,
        String name,
        BigDecimal quantity,
        BigDecimal averagePrice,
        BigDecimal positionValue,
        BigDecimal pnlAmount,
        BigDecimal pnlPercentage
) {

    public PositionResponseDto {

        quantity = formatQuantity(quantity);
        averagePrice = formatPrice(averagePrice);

        positionValue = formatMoney(positionValue);
        pnlAmount = formatMoney(pnlAmount);
        pnlPercentage = formatPercentage(pnlPercentage);
    }

    private static BigDecimal formatQuantity(BigDecimal value) {
        if (value == null) {
            return null;
        }

        BigDecimal formatted = value
                .setScale(8, RoundingMode.HALF_UP)
                .stripTrailingZeros();

        if (formatted.scale() < 0) {
            formatted = formatted.setScale(0);
        }

        return formatted;
    }

    private static BigDecimal formatPrice(BigDecimal value) {
        if (value == null) {
            return null;
        }

        if (value.abs().compareTo(BigDecimal.ONE) >= 0) {
            return value.setScale(2, RoundingMode.HALF_UP);
        }

        return value
                .setScale(8, RoundingMode.HALF_UP)
                .stripTrailingZeros();
    }

    private static BigDecimal formatMoney(BigDecimal value) {
        if (value == null) {
            return null;
        }

        return value.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private static BigDecimal formatPercentage(BigDecimal value) {
        if (value == null) {
            return null;
        }

        return value.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }
}
