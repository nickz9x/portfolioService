package ChainTracker.portfolioService.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI portfolioServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ChainTracker - portfolioService")
                        .description("""
                                Portfólios, posições, resumo da carteira e alertas de preço.

                                As rotas /api/portfolio e /api/alert exigem um JWT do usuário. As rotas
                                /api/bot/portfolio e /api/bot/alert exigem um token de serviço com o
                                escopo bot e recebem o userId como query parameter.

                                Use o botão Authorize com um Bearer token do Keycloak para testar as rotas
                                protegidas.
                                """)
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT emitido pelo Keycloak. Para /api/bot/**, o token precisa ter o escopo bot.")));
    }
}
