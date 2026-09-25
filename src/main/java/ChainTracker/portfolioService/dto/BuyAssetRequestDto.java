package ChainTracker.portfolioService.dto;

import jakarta.validation.constraints.NotBlank;

public record BuyAssetRequestDto(@NotBlank String assetId, @NotBlank String portfolio) {
}
