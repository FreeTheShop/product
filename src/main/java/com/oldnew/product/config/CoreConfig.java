package com.oldnew.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
class CoreConfig {

    /** One clock for the whole app, so tests can fix time. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
