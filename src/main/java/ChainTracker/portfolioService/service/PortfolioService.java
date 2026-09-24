package ChainTracker.portfolioService.service;

import ChainTracker.portfolioService.model.Portfolio;
import ChainTracker.portfolioService.repository.PortfolioRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class PortfolioService {
    private PortfolioRepository repository;

    public Portfolio createPortfolio(String name, String ownerId) {
        Portfolio portfolio = new Portfolio();
        portfolio.setName(name);
        portfolio.setOwnerId(ownerId);
        return repository.save(portfolio);
    }

    // 404 e nao 403 quando o portfolio existe mas e de outro usuario:
    // 403 revelaria que aquele id existe, deixando enumerar portfolios alheios.
    public Portfolio findByIdAndOwner(String id, String ownerId) {
        return repository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
