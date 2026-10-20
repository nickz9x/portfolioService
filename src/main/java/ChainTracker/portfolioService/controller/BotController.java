package ChainTracker.portfolioService.controller;

import ChainTracker.portfolioService.dto.PortfolioResponseDto;
import ChainTracker.portfolioService.dto.PositionResponseDto;
import ChainTracker.portfolioService.service.PortfolioService;
import ChainTracker.portfolioService.service.PositionService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

    @GetMapping("/{id}/positions")
    public ResponseEntity<List<PositionResponseDto>> viewAllPosition(@PathVariable String id, @RequestParam String userId){
        return ResponseEntity.ok(positionService.findAllPositionByPortfolio(id, userId));
    }
}
