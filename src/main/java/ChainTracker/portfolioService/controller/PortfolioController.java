package ChainTracker.portfolioService.controller;

import ChainTracker.portfolioService.dto.PortfolioRequestDto;
import ChainTracker.portfolioService.dto.PortfolioResponseDto;
import ChainTracker.portfolioService.dto.PositionTotalResume;
import ChainTracker.portfolioService.model.Portfolio;
import ChainTracker.portfolioService.service.PortfolioService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@AllArgsConstructor
public class PortfolioController {
    private PortfolioService portfolioService;

    @PostMapping
    public ResponseEntity<PortfolioResponseDto> save(
            @RequestBody @Valid PortfolioRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {

        Portfolio portfolio = portfolioService.createPortfolio(dto.name(), jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(PortfolioResponseDto.from(portfolio));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        portfolioService.deletePortfolio(id, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/resume")
    public ResponseEntity<PositionTotalResume>getPortfolioResume(@PathVariable String id, @AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(portfolioService.getResume(id, jwt.getSubject()));
    }

    @GetMapping
    public ResponseEntity<List<PortfolioResponseDto>> listAllPortfolioByOwnerId(@AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(portfolioService.listAllPortfolio(jwt.getSubject()));
    }
    @GetMapping("/bot")
    public ResponseEntity<List<PortfolioResponseDto>> listAllPortfolioByOwnerIdbot(@RequestParam String userId){
        return ResponseEntity.ok(portfolioService.listAllPortfolio(userId));
    }

}
