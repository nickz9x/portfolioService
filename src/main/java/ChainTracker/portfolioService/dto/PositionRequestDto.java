package ChainTracker.portfolioService.dto;

import ChainTracker.portfolioService.enums.PositionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PositionRequestDto(@NotBlank String assetId, @NotNull @Positive BigDecimal quantity, PositionType type) {
}
