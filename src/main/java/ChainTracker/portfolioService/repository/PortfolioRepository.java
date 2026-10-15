package ChainTracker.portfolioService.repository;

import ChainTracker.portfolioService.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, String> {
    Optional<Portfolio> findByIdAndOwnerId(String id, String ownerId);
    List<Portfolio> findByOwnerId(String ownerId);
}
