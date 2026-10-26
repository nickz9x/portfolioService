package ChainTracker.portfolioService.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
@Configuration
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http){
        http
            .csrf(csrf -> csrf.disable())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .authorizeHttpRequests(auth -> {
                // Pagina de erro interna do Spring. Sem liberar, qualquer falha
                // (inclusive um 404 do bot) vira 401 e esconde a causa real.
                auth.requestMatchers("/error").permitAll();
                auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/webjars/**").permitAll();
                // Prefixo unico das rotas duplicadas para o bot do Telegram.
                // As rotas originais (/api/portfolio/**) exigem JWT.
                auth.requestMatchers("/api/bot/**").hasAuthority("SCOPE_bot");
                auth.anyRequest().authenticated();
            });
        return http.build();
    }
}
