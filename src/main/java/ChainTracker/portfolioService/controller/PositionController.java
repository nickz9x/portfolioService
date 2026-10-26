package ChainTracker.portfolioService.controller;

import ChainTracker.portfolioService.dto.PositionAnotherPriceDto;
import ChainTracker.portfolioService.dto.PositionRequestDto;
import ChainTracker.portfolioService.dto.PositionResponseDto;
import ChainTracker.portfolioService.service.PositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Posições", description = "Compra, listagem e venda de posições de criptomoedas e ações.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/portfolio/{id}/positions")
@AllArgsConstructor
public class PositionController {
    private PositionService positionService;

    @Operation(summary = "Lista as posições do portfólio", description = "Retorna as posições com valor atual e PnL calculados com a cotação de mercado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posições retornadas"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Portfólio ou dado de mercado não encontrado")
    })
    @GetMapping()
    public ResponseEntity<List<PositionResponseDto>> viewAllPosition(
            @Parameter(description = "ID do portfólio", example = "0f6c2d4e-8a3b-4c5d-9e7f-123456789abc")
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(positionService.findAllPositionByPortfolio(id, jwt.getSubject()));
    }

    @Operation(summary = "Adiciona cripto a preço de mercado", description = "Cria ou atualiza a posição usando a cotação atual e recalcula o preço médio ponderado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição atualizada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Portfólio, ativo ou dado de mercado não encontrado")
    })
    @PostMapping("/cripto/market")
    public ResponseEntity<PositionResponseDto> createCryptoPosition(
            @Parameter(description = "ID do portfólio", example = "0f6c2d4e-8a3b-4c5d-9e7f-123456789abc")
            @PathVariable String id,
            @RequestBody @Valid PositionRequestDto dto,
            @AuthenticationPrincipal Jwt jwt){
       return ResponseEntity.ok(positionService.createCryptoPosition(dto.assetId(), dto.quantity(), id, jwt.getSubject()));
    }

    @Operation(summary = "Adiciona cripto a preço informado", description = "Cria ou atualiza a posição usando o preço unitário informado no corpo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição atualizada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Portfólio, ativo ou dado de mercado não encontrado")
    })
    @PostMapping("/cripto")
    public ResponseEntity<PositionResponseDto> createCryptoWithCustomPricePosition(
            @Parameter(description = "ID do portfólio", example = "0f6c2d4e-8a3b-4c5d-9e7f-123456789abc")
            @PathVariable String id,
            @RequestBody @Valid PositionAnotherPriceDto dto,
            @AuthenticationPrincipal Jwt jwt){
       return ResponseEntity.ok(positionService.createCryptoWithCustomPricePosition(dto.assetId(), dto.quantity(),dto.price() ,id, jwt.getSubject()));
    }

    @Operation(summary = "Adiciona ação a preço de mercado", description = "Cria ou atualiza a posição usando a cotação atual e recalcula o preço médio ponderado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição atualizada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Portfólio, ação ou dado de mercado não encontrado")
    })
    @PostMapping("/stock/market")
    public ResponseEntity<PositionResponseDto> createStockPosition(
            @Parameter(description = "ID do portfólio", example = "0f6c2d4e-8a3b-4c5d-9e7f-123456789abc")
            @PathVariable String id,
            @RequestBody @Valid PositionRequestDto dto,
            @AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(positionService.createStockPosition(dto.assetId(), dto.quantity(), id, jwt.getSubject()));
    }

    @Operation(summary = "Adiciona ação a preço informado", description = "Cria ou atualiza a posição usando o preço unitário informado no corpo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição atualizada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Portfólio, ação ou dado de mercado não encontrado")
    })
    @PostMapping("/stock")
    public ResponseEntity<PositionResponseDto> createStockWithCustomPricePosition(
            @Parameter(description = "ID do portfólio", example = "0f6c2d4e-8a3b-4c5d-9e7f-123456789abc")
            @PathVariable String id,
            @RequestBody @Valid PositionAnotherPriceDto dto,
            @AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(positionService.createStockWithCustomPricePosition(dto.assetId(), dto.quantity(), dto.price(),id, jwt.getSubject()));
    }

    @Operation(summary = "Vende uma posição", description = "Reduz a quantidade da posição. Quando a quantidade chega a zero, o preço médio também é zerado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Venda processada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Portfólio ou ativo não encontrado"),
            @ApiResponse(responseCode = "409", description = "Quantidade inválida ou saldo insuficiente")
    })
    @PostMapping("/delete")
    public void sellPosition(
            @Parameter(description = "ID do portfólio", example = "0f6c2d4e-8a3b-4c5d-9e7f-123456789abc")
            @PathVariable String id,
            @RequestBody @Valid PositionRequestDto dto,
            @AuthenticationPrincipal Jwt jwt){
        positionService.sellPosition(dto.type(),dto.assetId(),dto.quantity(),id, jwt.getSubject());
    }


}
