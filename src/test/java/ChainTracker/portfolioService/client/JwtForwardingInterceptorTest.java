package ChainTracker.portfolioService.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class JwtForwardingInterceptorTest {

    private final JwtForwardingInterceptor interceptor = new JwtForwardingInterceptor();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void forwardsBearerTokenFromSecurityContext() throws IOException {
        Jwt jwt = Jwt.withTokenValue("token-abc")
                .header("alg", "RS256")
                .claim("sub", "usuario")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        MockClientHttpRequest request =
                new MockClientHttpRequest(HttpMethod.GET, URI.create("http://localhost:8082/api/stock/AAPL"));

        interceptor.intercept(request, new byte[0],
                (req, body) -> new MockClientHttpResponse(new byte[0], HttpStatusCode.valueOf(200)));

        assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token-abc");
    }

    @Test
    void leavesHeaderUntouchedWithoutJwtAuthentication() throws IOException {
        MockClientHttpRequest request =
                new MockClientHttpRequest(HttpMethod.GET, URI.create("http://localhost:8082/api/currency/bitcoin"));

        interceptor.intercept(request, new byte[0],
                (req, body) -> new MockClientHttpResponse(new byte[0], HttpStatusCode.valueOf(200)));

        assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isNull();
    }
}
