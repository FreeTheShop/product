package com.oldnew.product.config;

import com.oldnew.product.config.ratelimit.RateLimitRule;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.List;

/** Product-specific settings, bound from {@code product.*}. Defaults in application.yml. */
@ConfigurationProperties("product")
public record ProductProperties(Security security, Http http, List<RateLimitRule> rateLimits, Cors cors) {

    /**
     * @param issuer    must equal the "iss" claim Identity puts in tokens
     * @param jwkSetUri Identity's public keys
     * @param audience  tokens must carry this in "aud"
     */
    public record Security(String issuer, URI jwkSetUri, String audience) {
    }

    /** Timeouts for every outbound HTTP call. A call without a timeout can hang a thread forever. */
    public record Http(Duration connectTimeout, Duration readTimeout) {
    }

    /** Browser origins allowed to call this API. Empty = no cross-origin browser calls. */
    public record Cors(List<String> allowedOrigins) {
    }
}
