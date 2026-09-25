package ChainTracker.portfolioService.repository;

import ChainTracker.portfolioService.model.Position;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position,Long> {
    Optional<Position> findByPortfolioIdAndAssetId(String portfolioId,Long assetId);

    /**
     * Traz as posicoes e seus assets no mesmo SELECT.
     *
     * Position.asset e @ManyToOne, que por padrao e EAGER: sem isto, o Hibernate
     * carrega as N posicoes e depois dispara mais N consultas, uma por asset —
     * o classico N+1. Aqui ele resolve tudo com um LEFT JOIN so.
     */
    @EntityGraph(attributePaths = "asset")
    List<Position> findAllByPortfolioId(String portfolioId);
}
