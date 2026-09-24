package ChainTracker.portfolioService.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public record MarketResponse(String id,
                             String symbol,
                             String name,
                             @JsonProperty("current_price")
                             BigDecimal currentPrice,

                             @JsonProperty("market_cap")
                             BigDecimal marketCap,

                             @JsonProperty("market_cap_rank")
                             Integer marketCapRank,

                             @JsonProperty("price_change_percentage_24h")
                             Double priceChangePercentage24h,

                             @JsonProperty("last_updated")
                             String lastUpdated) implements Serializable {
}
