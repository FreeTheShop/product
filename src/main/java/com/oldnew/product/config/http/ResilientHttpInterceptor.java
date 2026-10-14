package com.oldnew.product.config.http;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.retry.Retry;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Wraps one outbound HTTP call: retry (outer) around a circuit breaker (inner), so the breaker counts
 * every attempt. 5xx answers count as failures; 4xx do not (the caller's fault, retrying will not help).
 * Use only for idempotent calls (GET).
 */
public final class ResilientHttpInterceptor implements ClientHttpRequestInterceptor {

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public ResilientHttpInterceptor(CircuitBreaker circuitBreaker, Retry retry) {
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        CheckedSupplier<ClientHttpResponse> call = () -> {
            ClientHttpResponse response = execution.execute(request, body);
            if (response.getStatusCode().is5xxServerError()) {
                int status = response.getStatusCode().value();
                response.close();
                throw new UpstreamServerException(request.getURI().getHost() + " answered " + status);
            }
            return response;
        };
        CheckedSupplier<ClientHttpResponse> guarded =
                Retry.decorateCheckedSupplier(retry, CircuitBreaker.decorateCheckedSupplier(circuitBreaker, call));
        try {
            return guarded.get();
        } catch (IOException | RuntimeException e) {
            throw e instanceof IOException io ? io : new IOException("Outbound call failed: " + request.getURI().getHost(), e);
        } catch (Throwable t) {
            throw new IOException("Outbound call failed: " + request.getURI().getHost(), t);
        }
    }

    /** An upstream 5xx, raised so the breaker and retry treat it as a failure. */
    public static final class UpstreamServerException extends IOException {
        public UpstreamServerException(String message) {
            super(message);
        }
    }
}
