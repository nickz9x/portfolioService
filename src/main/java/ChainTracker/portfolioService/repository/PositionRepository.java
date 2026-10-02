package ChainTracker.portfolioService.repository;

import ChainTracker.portfolioService.model.Position;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PositionRepository extends JpaRepository<Position,Long> {
    Optional<Position> findByPortfolioIdAndAssetId(String portfolioId,String assetId);
    Optional<Position> findByPortfolioIdAndSymbol(String portfolioId,String symbol);
//
//
//    @Query("SELECT p FROM Position p WHERE p.portfolioId = :portfolioId AND (p.assetId = :assetId OR p.symbol = :symbol)")
//    Optional<Position> findByPortfolioAndAssetOrSymbol(
//            @Param("portfolioId") String portfolioId,
//            @Param("assetId") Long assetId,
//            @Param("symbol") String symbol
//
////            falta arrumar isso tem q conseguir buscar a stock e a cripto
//    );
    /**
     * Traz as posicoes e seus assets no mesmo SELECT.
     *
     * Position.asset e @ManyToOne, que por padrao e EAGER: sem isto, o Hibernate
     * carrega as N posicoes e depois dispara mais N consultas, uma por asset —
     * o classico N+1. Aqui ele resolve tudo com um LEFT JOIN so.
     */
    @EntityGraph(attributePaths = "portfolio")
    List<Position> findAllByPortfolioId(String portfolioId);
}
