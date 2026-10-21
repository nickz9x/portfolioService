package ChainTracker.portfolioService.service;

import ChainTracker.portfolioService.dto.AlertKafkaDto;
import ChainTracker.portfolioService.dto.AlertRequestDto;
import ChainTracker.portfolioService.dto.CryptoDataResponse;
import ChainTracker.portfolioService.dto.StockDataResponse;
import ChainTracker.portfolioService.enums.PositionType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AlertService {
    private final KafkaTemplate<String, AlertKafkaDto> kafkaTemplate;
    private final DataService dataService;


    public void createAlert(AlertRequestDto dto, String userId) {
        String assetId;

        if (dto.type() == PositionType.CRIPTO) {
            // Aceita id, ticker ou nome e normaliza para o id do CoinGecko,
            // igual ao fluxo de criacao de posicao.
            String cryptoId = dataService.resolveCryptoAssetId(dto.assetId());
            CryptoDataResponse cryptoData = dataService.getCryptoData(cryptoId);
            if (cryptoData == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            assetId = cryptoData.assetId();
        }
        else {
            StockDataResponse stockData = dataService.getStockData(dto.assetId().toUpperCase());
            if (stockData == null){
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);

            }
            assetId = stockData.symbol();
        }
        AlertKafkaDto kafkaDto = new AlertKafkaDto(userId, assetId, dto.price());

        kafkaTemplate.send("alert.created", userId, kafkaDto).whenComplete((result, ex) -> {
            if (ex == null) {
                // Executado em background quando o Kafka confirmar
                System.out.println("Alerta gravado com sucesso no Kafka para o usuário: " + userId);
            } else {
                // Executado em background se o Kafka falhar
                throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED);
            }
        });
    }
}
