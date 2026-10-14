# ADR-0004: Resilience and rate limits live in each service; gateway later

**Status:** Proposed · **Date:** 2026-10-08

## Decision
- **Outbound:** every HTTP call has connect and read timeouts (1 s / 2 s). Resilience4j wraps it in retry (outer: 3 attempts, 200 ms × 2) around a circuit breaker (inner: window 20, opens at 50 % failures or 80 % slow calls, 30 s open). Only idempotent calls are retried, and never on 4xx. Today that is only the JWKS fetch from Identity; Price joins in Sprint 3.
- **Inbound:** Bucket4j token buckets per caller (token subject, else IP): shop 100/min, admin 30/min, internal 300/min. Over the limit returns 429 with `Retry-After`. Buckets are in memory until there are two instances, then `bucket4j-redis`.
- **Gateway:** Spring Cloud Gateway arrives with the UIs for TLS, CORS and coarse limits. These in-service limits stay as defence in depth.

## Consequences
- Identity being slow or down cannot exhaust Product's threads.
- If Identity is down when the JWKS cache expires, new tokens fail with 401 until it is back. Tokens with a cached `kid` keep working.
