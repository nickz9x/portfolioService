package ChainTracker.portfolioService.controller;

import ChainTracker.portfolioService.dto.PortfolioRequestDto;
import ChainTracker.portfolioService.dto.PortfolioResponseDto;
import ChainTracker.portfolioService.model.Portfolio;
import ChainTracker.portfolioService.service.PortfolioService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
