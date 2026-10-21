package ChainTracker.portfolioService.dto;

import ChainTracker.portfolioService.enums.PositionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Venda vinda do bot. O price e aceito para o usuario informar o preco da
 * venda, mas o modelo atual nao guarda PnL realizado: a venda apenas reduz a
 * quantidade (e zera o preco medio quando ela chega a zero).
 */
public record SellPositionRequestDto(@NotNull PositionType type,
                                     @NotBlank String assetId,
                                     @NotNull @Positive BigDecimal quantity,
                                     BigDecimal price) {
}
