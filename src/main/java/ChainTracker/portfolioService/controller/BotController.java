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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Bot - portfólios", description = "Operações de portfólio e posição usadas pelo bot do Telegram. Exigem token com escopo bot.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/bot/portfolio")
@AllArgsConstructor
public class BotController {
    private PortfolioService portfolioService;
    private PositionService positionService;


    @Operation(summary = "Lista os portfólios de um usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Token sem o escopo bot")
    })
    @GetMapping
    public ResponseEntity<List<PortfolioResponseDto>> listAllPortfolioByOwnerIdbot(
            @Parameter(description = "ID do usuário no Keycloak", example = "3f2a1b4c-5d6e-7f80-9a1b-2c3d4e5f6a7b")
            @RequestParam String userId){
        return ResponseEntity.ok(portfolioService.listAllPortfolio(userId));
    }

    @Operation(summary = "Cria um portfólio para um usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Portfólio criado"),
            @ApiResponse(responseCode = "400", description = "Nome inválido"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Token sem o escopo bot")
    })
    @PostMapping
    public ResponseEntity<PortfolioResponseDto> createPortfolio(
            @Parameter(description = "ID do usuário no Keycloak", example = "3f2a1b4c-5d6e-7f80-9a1b-2c3d4e5f6a7b")
            @RequestParam String userId,
            @RequestBody @Valid PortfolioRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PortfolioResponseDto.from(portfolioService.createPortfolio(dto.name(), userId)));
    }

    @Operation(summary = "Exclui um portfólio de um usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Portfólio excluído"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Token sem o escopo bot"),
            @ApiResponse(responseCode = "404", description = "Portfólio não encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePortfolio(
            @Parameter(description = "ID do portfólio", example = "0f6c2d4e-8a3b-4c5d-9e7f-123456789abc")
            @PathVariable String id,
            @Parameter(description = "ID do usuário no Keycloak", example = "3f2a1b4c-5d6e-7f80-9a1b-2c3d4e5f6a7b")
            @RequestParam String userId) {
        portfolioService.deletePortfolio(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Lista as posições de um portfólio")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posições retornadas"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Token sem o escopo bot"),
            @ApiResponse(responseCode = "404", description = "Portfólio ou dado de mercado não encontrado")
    })
    @GetMapping("/{id}/positions")
    public ResponseEntity<List<PositionResponseDto>> viewAllPosition(
            @Parameter(description = "ID do portfólio", example = "0f6c2d4e-8a3b-4c5d-9e7f-123456789abc")
            @PathVariable String id,
            @Parameter(description = "ID do usuário no Keycloak", example = "3f2a1b4c-5d6e-7f80-9a1b-2c3d4e5f6a7b")
            @RequestParam String userId){
        return ResponseEntity.ok(positionService.findAllPositionByPortfolio(id, userId));
    }

    @Operation(summary = "Consulta o resumo de um portfólio")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumo retornado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Token sem o escopo bot"),
            @ApiResponse(responseCode = "404", description = "Portfólio não encontrado")
    })
    @GetMapping("/{id}/resume")
    public ResponseEntity<PositionTotalResume>getPortfolioResume(
            @Parameter(description = "ID do portfólio", example = "0f6c2d4e-8a3b-4c5d-9e7f-123456789abc")
            @PathVariable String id,
            @Parameter(description = "ID do usuário no Keycloak", example = "3f2a1b4c-5d6e-7f80-9a1b-2c3d4e5f6a7b")
            @RequestParam String userId){
        return ResponseEntity.ok(portfolioService.getResume(id, userId));
    }

    @Operation(summary = "Adiciona cripto a preço de mercado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição atualizada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Token sem o escopo bot"),
            @ApiResponse(responseCode = "404", description = "Portfólio, ativo ou dado de mercado não encontrado")
    })
    @PostMapping("/{id}/positions/cripto/market")
    public ResponseEntity<PositionResponseDto> createCryptoPositionAtMarketPrice(
            @Parameter(description = "ID do portfólio", example = "0f6c2d4e-8a3b-4c5d-9e7f-123456789abc")
            @PathVariable String id,
            @Parameter(description = "ID do usuário no Keycloak", example = "3f2a1b4c-5d6e-7f80-9a1b-2c3d4e5f6a7b")
            @RequestParam String userId,
            @RequestBody @Valid PositionRequestDto dto) {
        return ResponseEntity.ok(positionService.createCryptoPosition(dto.assetId(), dto.quantity(), id, userId));
    }

    @Operation(summary = "Adiciona cripto a preço informado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição atualizada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Token sem o escopo bot"),
            @ApiResponse(responseCode = "404", description = "Portfólio, ativo ou dado de mercado não encontrado")
    })
    @PostMapping("/{id}/positions/cripto")
    public ResponseEntity<PositionResponseDto> createCryptoPositionAtCustomPrice(
            @Parameter(description = "ID do portfólio", example = "0f6c2d4e-8a3b-4c5d-9e7f-123456789abc")
            @PathVariable String id,
            @Parameter(description = "ID do usuário no Keycloak", example = "3f2a1b4c-5d6e-7f80-9a1b-2c3d4e5f6a7b")
            @RequestParam String userId,
            @RequestBody @Valid PositionAnotherPriceDto dto) {
        return ResponseEntity.ok(positionService.createCryptoWithCustomPricePosition(dto.assetId(), dto.quantity(), dto.price(), id, userId));
    }

    @Operation(summary = "Adiciona ação a preço de mercado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição atualizada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Token sem o escopo bot"),
            @ApiResponse(responseCode = "404", description = "Portfólio, ação ou dado de mercado não encontrado")
    })
    @PostMapping("/{id}/positions/stock/market")
    public ResponseEntity<PositionResponseDto> createStockPositionAtMarketPrice(
            @Parameter(description = "ID do portfólio", example = "0f6c2d4e-8a3b-4c5d-9e7f-123456789abc")
            @PathVariable String id,
            @Parameter(description = "ID do usuário no Keycloak", example = "3f2a1b4c-5d6e-7f80-9a1b-2c3d4e5f6a7b")
            @RequestParam String userId,
            @RequestBody @Valid PositionRequestDto dto) {
        return ResponseEntity.ok(positionService.createStockPosition(dto.assetId(), dto.quantity(), id, userId));
    }

    @Operation(summary = "Adiciona ação a preço informado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição atualizada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Token sem o escopo bot"),
            @ApiResponse(responseCode = "404", description = "Portfólio, ação ou dado de mercado não encontrado")
    })
    @PostMapping("/{id}/positions/stock")
    public ResponseEntity<PositionResponseDto> createStockPositionAtCustomPrice(
            @Parameter(description = "ID do portfólio", example = "0f6c2d4e-8a3b-4c5d-9e7f-123456789abc")
            @PathVariable String id,
            @Parameter(description = "ID do usuário no Keycloak", example = "3f2a1b4c-5d6e-7f80-9a1b-2c3d4e5f6a7b")
            @RequestParam String userId,
            @RequestBody @Valid PositionAnotherPriceDto dto) {
        return ResponseEntity.ok(positionService.createStockWithCustomPricePosition(dto.assetId(), dto.quantity(), dto.price(), id, userId));
    }

    @Operation(summary = "Vende uma posição")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Venda processada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Token sem o escopo bot"),
            @ApiResponse(responseCode = "404", description = "Portfólio ou ativo não encontrado"),
            @ApiResponse(responseCode = "409", description = "Quantidade inválida ou saldo insuficiente")
    })
    @PostMapping("/{id}/positions/sell")
    public ResponseEntity<Void> sellPosition(
            @Parameter(description = "ID do portfólio", example = "0f6c2d4e-8a3b-4c5d-9e7f-123456789abc")
            @PathVariable String id,
            @Parameter(description = "ID do usuário no Keycloak", example = "3f2a1b4c-5d6e-7f80-9a1b-2c3d4e5f6a7b")
            @RequestParam String userId,
            @RequestBody @Valid SellPositionRequestDto dto) {
        positionService.sellPosition(dto.type(), dto.assetId(), dto.quantity(), id, userId);
        return ResponseEntity.noContent().build();
    }
}
