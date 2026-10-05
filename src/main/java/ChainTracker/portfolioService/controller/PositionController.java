package ChainTracker.portfolioService.controller;

import ChainTracker.portfolioService.dto.PositionAnotherPriceDto;
import ChainTracker.portfolioService.dto.PositionRequestDto;
import ChainTracker.portfolioService.dto.PositionResponseDto;
import ChainTracker.portfolioService.model.Portfolio;
import ChainTracker.portfolioService.model.Position;
import ChainTracker.portfolioService.service.PortfolioService;
import ChainTracker.portfolioService.service.PositionService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio/{id}/positions")
@AllArgsConstructor
public class PositionController {
    private PositionService positionService;

    @GetMapping()
    public ResponseEntity<List<PositionResponseDto>> viewAllPosition(@PathVariable String id, @AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(positionService.findAllPositionByPortfolio(id, jwt.getSubject()));
    }

    @PostMapping("/cripto/market")
    public ResponseEntity<PositionResponseDto> createCryptoPosition(@PathVariable String id, @RequestBody @Valid PositionRequestDto dto, @AuthenticationPrincipal Jwt jwt){
       return ResponseEntity.ok(positionService.createCryptoPosition(dto.assetId(), dto.quantity(), id, jwt.getSubject()));
    }
    @PostMapping("/cripto")
    public ResponseEntity<PositionResponseDto> createCryptoWithCustomPricePosition(@PathVariable String id, @RequestBody @Valid PositionAnotherPriceDto dto, @AuthenticationPrincipal Jwt jwt){
       return ResponseEntity.ok(positionService.createCryptoWithCustomPricePosition(dto.assetId(), dto.quantity(),dto.price() ,id, jwt.getSubject()));
    }

    @PostMapping("/stock/market")
    public ResponseEntity<PositionResponseDto> createStockPosition(@PathVariable String id, @RequestBody @Valid PositionRequestDto dto, @AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(positionService.createStockPosition(dto.assetId(), dto.quantity(), id, jwt.getSubject()));
    }
    @PostMapping("/stock")
    public ResponseEntity<PositionResponseDto> createStockWithCustomPricePosition(@PathVariable String id, @RequestBody @Valid PositionAnotherPriceDto dto, @AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(positionService.createStockWithCustomPricePosition(dto.assetId(), dto.quantity(), dto.price(),id, jwt.getSubject()));
    }

    @DeleteMapping("/delete")
    public void sellPosition(@PathVariable String id,@RequestBody @Valid PositionRequestDto dto, @AuthenticationPrincipal Jwt jwt){
        positionService.sellPosition(dto.assetId(),dto.quantity(),id, jwt.getSubject());
    }


}
