package ChainTracker.portfolioService.repository;

import ChainTracker.portfolioService.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset,Long> {
    boolean existsByassetId(String assetId);
    Optional<Asset> findByAssetId(String assetId);
}
