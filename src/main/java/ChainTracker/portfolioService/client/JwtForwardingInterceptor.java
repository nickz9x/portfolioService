package ChainTracker.portfolioService.client;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Repassa o Bearer token do usuario que chamou o portfolioService para as
 * chamadas de saida ao assetMarketDataService.
 *
 * Sem isso a chamada sai anonima, e la so /api/currency/** e publico:
 * /api/stock/** e /api/internal/** exigem token do realm ChainTracker. O fluxo
 * de cripto escapava porque so usa a rota publica; o de stock morria na
 * primeira chamada com 401.
 *
 * O token vem do SecurityContextHolder, que e preenchido pelo resource server
 * no thread que atende o request. O RestClient e bloqueante, entao a chamada
 * de saida roda nesse mesmo thread e enxerga o mesmo contexto.
 */
@Component
public class JwtForwardingInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            request.getHeaders().setBearerAuth(jwtAuthentication.getToken().getTokenValue());
        }
        return execution.execute(request, body);
    }
}
