package ChainTracker.portfolioService.service;

import ChainTracker.portfolioService.model.Portfolio;
import ChainTracker.portfolioService.model.Position;
import ChainTracker.portfolioService.repository.PortfolioRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@AllArgsConstructor
public class PortfolioService {
    private PortfolioRepository repository;
    public Portfolio findById(Long id){
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public void addAsset(Long portfolioId, Position position){
        Portfolio portfolio = repository.findById(portfolioId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<Position> positions = portfolio.getPositions();
        positions.add(position);
        repository.save(portfolio);

    }
}
