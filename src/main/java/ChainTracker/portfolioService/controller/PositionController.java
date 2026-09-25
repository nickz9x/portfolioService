package ChainTracker.portfolioService.controller;

import ChainTracker.portfolioService.dto.PositionRequestDto;
import ChainTracker.portfolioService.dto.PositionResponseDto;
import ChainTracker.portfolioService.model.Portfolio;
import ChainTracker.portfolioService.model.Position;
import ChainTracker.portfolioService.service.PortfolioService;
import ChainTracker.portfolioService.service.PositionService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
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

    @PostMapping()
    public ResponseEntity<PositionResponseDto> createPosition(@PathVariable String id, @RequestBody @Valid PositionRequestDto dto, @AuthenticationPrincipal Jwt jwt){
       return ResponseEntity.ok(positionService.createPosition(dto.assetId(), dto.quantity(), id, jwt.getSubject()));
    }

    @GetMapping()
    public ResponseEntity<List<PositionResponseDto>> viewAllPosition(@PathVariable String id, @AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(positionService.findAllPositionByPortfolio(id, jwt.getSubject()));
    }
}
