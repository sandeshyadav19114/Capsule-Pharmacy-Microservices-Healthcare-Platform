package com.apigateway.filter;

import com.apigateway.GatewayConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * AuthFilter — JWT Authentication Filter for Spring Cloud Gateway
 *
 * Responsibilities:
 * 1. Skip public endpoints
 * 2. Validate JWT token
 * 3. Forward user details to downstream services
 * 4. Block unauthorized requests
 */
@Component
@Slf4j
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    @Value("${jwt.secret}")
    private String secret;

   // @Value("${gateway.public-paths}")
    //private List<String> publicPaths;

    /**
     * 🔥 VERY IMPORTANT FIX
     * Without this, you get ClassCastException
     */

    private final GatewayConfig gatewayConfig;

    public AuthFilter(GatewayConfig gatewayConfig) {
        super(Config.class);
        this.gatewayConfig = gatewayConfig;
    }
    @Override
    public GatewayFilter apply(Config config) {

        return (exchange, chain) -> {

            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            log.debug("Incoming request path: {}", path);

            // ✅ 1. Skip public endpoints
            if (isPublicPath(path)) {
                log.debug("Public endpoint, skipping auth: {}", path);
                return chain.filter(exchange);
            }

            // ✅ 2. Check Authorization header
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid Authorization format", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                // ✅ 3. Validate token
                Claims claims = validateToken(token);

                String userId = String.valueOf(claims.get("userId"));
                String role = String.valueOf(claims.get("role"));
                String email = claims.getSubject();

                log.info("Authenticated user: {} | role: {}", userId, role);

                // ✅ 4. Add headers for downstream services
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Id", userId)
                        .header("X-User-Role", role)
                        .header("X-User-Email", email)
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (Exception e) {
                log.error("JWT validation failed: {}", e.getMessage());
                return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    /**
     * Validate JWT token
     */
    private Claims validateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Check if request path is public
     */
    private boolean isPublicPath(String path) {
        return gatewayConfig.getPublicPaths().stream().anyMatch(p ->
                p.endsWith("/**")
                        ? path.startsWith(p.replace("/**", ""))
                        : path.equals(p)
        );
    }

    /**
     * Return error response
     */
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {
                  "status": %d,
                  "error": "%s",
                  "message": "%s"
                }
                """.formatted(status.value(), status.getReasonPhrase(), message);

        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    /**
     * Empty config class (required by Spring)
     */
    public static class Config {
    }
}