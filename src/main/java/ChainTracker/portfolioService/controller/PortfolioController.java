package ChainTracker.portfolioService.controller;

import ChainTracker.portfolioService.dto.PortfolioRequestDto;
import ChainTracker.portfolioService.dto.PortfolioResponseDto;
import ChainTracker.portfolioService.dto.PositionTotalResume;
import ChainTracker.portfolioService.model.Portfolio;
import ChainTracker.portfolioService.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Portfólios", description = "Criação, listagem, resumo e exclusão de portfólios.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/portfolio")
@AllArgsConstructor
public class PortfolioController {
    private PortfolioService portfolioService;

    @Operation(summary = "Cria um portfólio", description = "Cria um portfólio para o usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Portfólio criado"),
            @ApiResponse(responseCode = "400", description = "Nome inválido"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    @PostMapping
    public ResponseEntity<PortfolioResponseDto> save(
            @RequestBody @Valid PortfolioRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {

        Portfolio portfolio = portfolioService.createPortfolio(dto.name(), jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(PortfolioResponseDto.from(portfolio));
    }

    @Operation(summary = "Exclui um portfólio", description = "Exclui o portfólio e todas as suas posições.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Portfólio excluído"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Portfólio não encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do portfólio", example = "0f6c2d4e-8a3b-4c5d-9e7f-123456789abc")
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        portfolioService.deletePortfolio(id, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Consulta o resumo da carteira", description = "Devolve valor total, PnL e melhores e piores desempenhos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumo retornado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Portfólio não encontrado")
    })
    @GetMapping("/{id}/resume")
    public ResponseEntity<PositionTotalResume> getPortfolioResume(
            @Parameter(description = "ID do portfólio", example = "0f6c2d4e-8a3b-4c5d-9e7f-123456789abc")
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(portfolioService.getResume(id, jwt.getSubject()));
    }

    @Operation(summary = "Lista os portfólios do usuário", description = "Retorna os portfólios do usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    @GetMapping
    public ResponseEntity<List<PortfolioResponseDto>> listAllPortfolioByOwnerId(@AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(portfolioService.listAllPortfolio(jwt.getSubject()));
    }

    @Operation(summary = "Lista portfólios por userId", description = "Listagem alternativa que recebe o userId como query parameter.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    @GetMapping("/bot")
    public ResponseEntity<List<PortfolioResponseDto>> listAllPortfolioByOwnerIdbot(
            @Parameter(description = "ID do usuário no Keycloak", example = "3f2a1b4c-5d6e-7f80-9a1b-2c3d4e5f6a7b")
            @RequestParam String userId){
        return ResponseEntity.ok(portfolioService.listAllPortfolio(userId));
    }

}
