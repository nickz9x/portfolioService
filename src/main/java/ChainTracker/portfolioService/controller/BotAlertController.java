package ChainTracker.portfolioService.controller;

import ChainTracker.portfolioService.dto.AlertRequestDto;
import ChainTracker.portfolioService.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Bot - alertas", description = "Criação de alertas pelo bot do Telegram. Exige token com escopo bot.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/bot/alert")
@AllArgsConstructor
public class BotAlertController {
    private final AlertService alertService;

    @Operation(
            summary = "Cria um alerta para um userId",
            description = "Normaliza o ativo e publica o alerta no tópico Kafka alert.created."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Alerta publicado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Token sem o escopo bot"),
            @ApiResponse(responseCode = "404", description = "Ativo não encontrado")
    })
    @PostMapping
    public ResponseEntity<Void> createAlert(
            @Parameter(description = "ID do usuário no Keycloak", example = "3f2a1b4c-5d6e-7f80-9a1b-2c3d4e5f6a7b")
            @RequestParam String userId,
            @RequestBody @Valid AlertRequestDto dto) {
        alertService.createAlert(dto, userId);
        return ResponseEntity.noContent().build();
    }
}
