package ChainTracker.portfolioService.service;

import ChainTracker.portfolioService.dto.PortfolioResponseDto;
import ChainTracker.portfolioService.dto.PositionTotalResume;
import ChainTracker.portfolioService.model.Portfolio;
import ChainTracker.portfolioService.repository.PortfolioRepository;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PortfolioService {
    private PortfolioRepository repository;
    private final PositionService positionService;

    // O @Lazy quebra o ciclo fazendo o Spring injetar um proxy inicialmente
    public PortfolioService(PortfolioRepository repository, @Lazy PositionService positionService) {
        this.repository = repository;
        this.positionService = positionService;
    }

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

    public PositionTotalResume getResume(String portfolioId, String ownerId) {
        return positionService.positionTotalResume(portfolioId, ownerId);
    }

    public List<PortfolioResponseDto> listAllPortfolio(String ownerId){
        return repository.findByOwnerId(ownerId).stream()
                .map(portfolio -> new PortfolioResponseDto(portfolio.getId(), portfolio.getName())).toList();
    }
}
