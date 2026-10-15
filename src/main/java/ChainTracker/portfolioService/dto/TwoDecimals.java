package ChainTracker.portfolioService.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Arredondamento de 2 casas dos DTOs de resposta. Estava copiado em tres lugares
 * (formatMoney, formatPercentage e toTwoDecimals) — um DTO podia arredondar
 * diferente do outro sem ninguem notar.
 *
 * null continua null: "nao sei" nao pode virar 0,00, que afirmaria "esta no
 * preco medio".
 */
final class TwoDecimals {

    private TwoDecimals() {
    }

    static BigDecimal round(BigDecimal value) {
        if (value == null) {
            return null;
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
