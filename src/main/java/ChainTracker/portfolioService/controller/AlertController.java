package ChainTracker.portfolioService.controller;


import ChainTracker.portfolioService.dto.AlertRequestDto;
import ChainTracker.portfolioService.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Alertas", description = "Criação de alertas de preço para o usuário autenticado.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/alert")
@RequiredArgsConstructor
public class AlertController {
    private final AlertService alertService;


    @Operation(
            summary = "Cria um alerta de preço",
            description = "Normaliza o ativo e publica o alerta no tópico Kafka alert.created."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alerta publicado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Ativo não encontrado")
    })
    @PostMapping
    public void createAlert(@RequestBody @Valid AlertRequestDto dto , @AuthenticationPrincipal Jwt jwt){
    alertService.createAlert(dto, jwt.getSubject());
    }
}
