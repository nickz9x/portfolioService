package ChainTracker.portfolioService.controller;


import ChainTracker.portfolioService.dto.AlertRequestDto;
import ChainTracker.portfolioService.service.AlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alert")
@RequiredArgsConstructor
public class AlertController {
    private final AlertService alertService;


    @PostMapping
    public void createAlert(@RequestBody @Valid AlertRequestDto dto , @AuthenticationPrincipal Jwt jwt){
    alertService.createAlert(dto, jwt.getSubject());
    }
}
