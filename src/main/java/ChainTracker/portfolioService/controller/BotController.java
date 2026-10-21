package ChainTracker.portfolioService.controller;

import ChainTracker.portfolioService.dto.PortfolioRequestDto;
import ChainTracker.portfolioService.dto.PortfolioResponseDto;
import ChainTracker.portfolioService.dto.PositionAnotherPriceDto;
import ChainTracker.portfolioService.dto.PositionRequestDto;
import ChainTracker.portfolioService.dto.PositionResponseDto;
import ChainTracker.portfolioService.dto.PositionTotalResume;
import ChainTracker.portfolioService.dto.SellPositionRequestDto;
import ChainTracker.portfolioService.service.PortfolioService;
import ChainTracker.portfolioService.service.PositionService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bot/portfolio")
@AllArgsConstructor
public class BotController {
    private PortfolioService portfolioService;
    private PositionService positionService;


    @GetMapping
    public ResponseEntity<List<PortfolioResponseDto>> listAllPortfolioByOwnerIdbot(@RequestParam String userId){
        return ResponseEntity.ok(portfolioService.listAllPortfolio(userId));
    }

    @PostMapping
    public ResponseEntity<PortfolioResponseDto> createPortfolio(
            @RequestParam String userId,
            @RequestBody @Valid PortfolioRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PortfolioResponseDto.from(portfolioService.createPortfolio(dto.name(), userId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePortfolio(@PathVariable String id, @RequestParam String userId) {
        portfolioService.deletePortfolio(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/positions")
    public ResponseEntity<List<PositionResponseDto>> viewAllPosition(@PathVariable String id, @RequestParam String userId){
        return ResponseEntity.ok(positionService.findAllPositionByPortfolio(id, userId));
    }
    @GetMapping("/{id}/resume")
    public ResponseEntity<PositionTotalResume>getPortfolioResume(@PathVariable String id, @RequestParam String userId){
        return ResponseEntity.ok(portfolioService.getResume(id, userId));
    }

    // Mutacoes do bot: mesmo userId das consultas, porque o fluxo do Telegram
    // nao carrega JWT de usuario.

    @PostMapping("/{id}/positions/cripto/market")
    public ResponseEntity<PositionResponseDto> createCryptoPositionAtMarketPrice(
            @PathVariable String id,
            @RequestParam String userId,
            @RequestBody @Valid PositionRequestDto dto) {
        return ResponseEntity.ok(positionService.createCryptoPosition(dto.assetId(), dto.quantity(), id, userId));
    }

    @PostMapping("/{id}/positions/cripto")
    public ResponseEntity<PositionResponseDto> createCryptoPositionAtCustomPrice(
            @PathVariable String id,
            @RequestParam String userId,
            @RequestBody @Valid PositionAnotherPriceDto dto) {
        return ResponseEntity.ok(positionService.createCryptoWithCustomPricePosition(dto.assetId(), dto.quantity(), dto.price(), id, userId));
    }

    @PostMapping("/{id}/positions/stock/market")
    public ResponseEntity<PositionResponseDto> createStockPositionAtMarketPrice(
            @PathVariable String id,
            @RequestParam String userId,
            @RequestBody @Valid PositionRequestDto dto) {
        return ResponseEntity.ok(positionService.createStockPosition(dto.assetId(), dto.quantity(), id, userId));
    }

    @PostMapping("/{id}/positions/stock")
    public ResponseEntity<PositionResponseDto> createStockPositionAtCustomPrice(
            @PathVariable String id,
            @RequestParam String userId,
            @RequestBody @Valid PositionAnotherPriceDto dto) {
        return ResponseEntity.ok(positionService.createStockWithCustomPricePosition(dto.assetId(), dto.quantity(), dto.price(), id, userId));
    }

    @PostMapping("/{id}/positions/sell")
    public ResponseEntity<Void> sellPosition(
            @PathVariable String id,
            @RequestParam String userId,
            @RequestBody @Valid SellPositionRequestDto dto) {
        positionService.sellPosition(dto.type(), dto.assetId(), dto.quantity(), id, userId);
        return ResponseEntity.noContent().build();
    }
}
