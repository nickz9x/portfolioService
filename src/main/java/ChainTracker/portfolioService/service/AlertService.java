package ChainTracker.portfolioService.service;

import ChainTracker.portfolioService.dto.AlertKafkaDto;
import ChainTracker.portfolioService.dto.AlertRequestDto;
import lombok.AllArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@AllArgsConstructor
public class AlertService {
    private KafkaTemplate<String, AlertKafkaDto> kafkaTemplate;
    private ObjectMapper objectMapper;




    public void createAlert(AlertRequestDto dto, String userId){
        AlertKafkaDto kafkaDto = new AlertKafkaDto(userId,dto.assetId(),dto.price());
        kafkaTemplate.send("alert.created",userId,kafkaDto);
    }
}
