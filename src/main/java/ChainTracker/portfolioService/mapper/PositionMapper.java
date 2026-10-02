package ChainTracker.portfolioService.mapper;

import ChainTracker.portfolioService.dto.PositionResponseDto;
import ChainTracker.portfolioService.model.Position;

import java.math.BigDecimal;

public class PositionMapper {
    public static PositionResponseDto toResponse(Position position,BigDecimal currentPrice){
        return new PositionResponseDto(position.getAssetId(), position.getSymbol(), position.getName(),
                position.getQuantity(),position.getAveragePrice(),position.positionValue(currentPrice),position.pnlValue(currentPrice),position.calculePNL(currentPrice));
    }
}
