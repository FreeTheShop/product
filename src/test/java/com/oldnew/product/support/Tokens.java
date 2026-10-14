package com.oldnew.product.support;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/** Ready-made callers. The JWT signature is skipped in tests; issuer/audience checks are covered separately. */
public final class Tokens {

    private Tokens() {
    }

    public static RequestPostProcessor catalogManager() {
        return as("manager-1", "SCOPE_product:write", "ROLE_CATALOG_MANAGER");
    }

    public static RequestPostProcessor customerWithWriteScope() {
        return as("customer-1", "SCOPE_product:write", "ROLE_CUSTOMER");
    }

    public static RequestPostProcessor priceService() {
        return as("price-service", "SCOPE_product:internal");
    }

    public static RequestPostProcessor as(String subject, String... authorities) {
        return jwt().jwt(j -> j.subject(subject))
                .authorities(Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }
}
