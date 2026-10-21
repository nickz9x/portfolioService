package ChainTracker.portfolioService.controller;

import ChainTracker.portfolioService.dto.AlertRequestDto;
import ChainTracker.portfolioService.service.AlertService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Copia da criacao de alerta para o bot do Telegram. A rota original
 * (POST /api/alert) continua exigindo JWT; esta fica sob /api/bot/**,
 * liberado no SecurityConfiguration, e recebe o userId por query param.
 */
@RestController
@RequestMapping("/api/bot/alert")
@AllArgsConstructor
public class BotAlertController {
    private final AlertService alertService;

    @PostMapping
    public ResponseEntity<Void> createAlert(
            @RequestParam String userId,
            @RequestBody @Valid AlertRequestDto dto) {
        alertService.createAlert(dto, userId);
        return ResponseEntity.noContent().build();
    }
}
