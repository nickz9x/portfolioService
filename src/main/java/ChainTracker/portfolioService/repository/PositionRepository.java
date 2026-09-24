package ChainTracker.portfolioService.repository;

import ChainTracker.portfolioService.model.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position,Long> {
    Optional<Position> findByPortfolioIdAndAssetId(String portfolioId,Long assetId);
}
