package ChainTracker.portfolioService.dto;

import jakarta.validation.constraints.NotBlank;

public record PortfolioRequestDto(@NotBlank String name) {
}
