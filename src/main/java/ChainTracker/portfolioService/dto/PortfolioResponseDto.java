package ChainTracker.portfolioService.dto;

import ChainTracker.portfolioService.model.Portfolio;

public record PortfolioResponseDto(String id, String name) {

    public static PortfolioResponseDto from(Portfolio portfolio) {
        return new PortfolioResponseDto(portfolio.getId(), portfolio.getName());
    }
}
