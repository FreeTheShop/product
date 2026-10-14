package com.oldnew.product.config.security;

import com.oldnew.product.config.ProductProperties;
import com.oldnew.product.config.http.ResilientHttpInterceptor;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Validates Identity's tokens locally (ADR-0002): signature against cached JWKS, then exp/nbf, iss and aud.
 * Fetching JWKS is lazy (first request, then on an unknown kid), has 1s/2s timeouts, retries, and sits
 * behind the "identity-jwks" circuit breaker so a sick Identity cannot tie up Product's threads.
 */
@Configuration(proxyBeanMethods = false)
class JwtDecoderConfig {

    static final String IDENTITY_JWKS = "identity-jwks";

    @Bean
    JwtDecoder jwtDecoder(ProductProperties properties, CircuitBreakerRegistry breakers, RetryRegistry retries) {
        ProductProperties.Security security = properties.security();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.http().connectTimeout());
        requestFactory.setReadTimeout(properties.http().readTimeout());
        RestTemplate jwksClient = new RestTemplate(requestFactory);
        jwksClient.getInterceptors().add(new ResilientHttpInterceptor(
                breakers.circuitBreaker(IDENTITY_JWKS), retries.retry(IDENTITY_JWKS)));

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(security.jwkSetUri().toString())
                .jwsAlgorithm(SignatureAlgorithm.ES256)
                .restOperations(jwksClient)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(security.issuer()),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                        aud -> aud != null && aud.contains(security.audience()))));
        return decoder;
    }
}
