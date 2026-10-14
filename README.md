# Product Service

The product catalog of the Old New Ecosystem, in three contexts:

- **Intake** (`/api/v1/admin/products`): catalog managers create, change, publish and archive products.
- **Core Shop** (`/api/v1/products`): the public catalog. Published products only.
- **Core Transactional** (`/api/v1/internal/products`): other services check sellability at checkout.

Built with Spring Boot 4.1, Java 21 and MongoDB. It is an OAuth2 resource server: tokens come from the Identity service and are checked locally against its JWKS.

- **Port 8080:** API
- **Port 8081:** health, metrics. Internal only; never expose it publicly.

## Quick start

Prerequisites: JDK 21, Maven 3.9+, Docker, and Identity running on :9000 (see `../identity`).

```bash
docker compose up -d                                   # MongoDB on :27017
mvn spring-boot:run -Dspring-boot.run.profiles=dev     # http://localhost:8080/swagger-ui.html
```

Try it once both services run:

```bash
curl -s http://localhost:8080/api/v1/products                           # public, empty at first
TOKEN=$(curl -s -u price-service:dev-price-secret -d grant_type=client_credentials \
  -d scope=product:internal http://localhost:9000/oauth2/token | jq -r .access_token)
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/internal/products/<id>
```

Admin calls need a user token with scope `product:write` and the role CATALOG_MANAGER or ADMIN. You get one through the `admin-ui` client (authorization code + PKCE). The Admin UI on the roadmap does this for you; until then, use Swagger UI with a token from an OAuth tool such as Postman.

## Tests

| Command | Runs | Needs Docker |
| --- | --- | --- |
| `mvn test` | Domain and application unit tests, ArchUnit layering rules | No |
| `mvn verify` | Everything above, plus `ProductApiIT` and the Cucumber features against real MongoDB | Yes |
| `mvn -Powasp verify` | OWASP dependency-check; fails on CVSS ≥ 7 (needs `NVD_API_KEY`) | No |

## API rules

| Rule | How |
| --- | --- |
| Who may write | Path needs scope `product:write`; the use case also needs role ADMIN or CATALOG_MANAGER. Scope alone gets 403 |
| Lost updates | Every change needs `If-Match: "<version>"` from the last ETag. Missing → 428 `VERSION_REQUIRED`. Stale → 412 `VERSION_MISMATCH` |
| Paging | Keyset: `?size=20&cursor=<nextCursor>`. Max size 100. Page 1000 costs the same as page 1 |
| Errors | `application/problem+json` with stable `code` and `traceId`. Codes are in `domain/error/ErrorCode.java` |
| Rate limits | Shop 100/min, admin 30/min, internal 300/min per caller. Over the limit → 429 + `Retry-After` |
| Caching | Shop responses carry `Cache-Control: public, max-age=60` |

## Lifecycle

`DRAFT` → `PUBLISHED` → `ARCHIVED`, or `DRAFT` → `ARCHIVED`.

- Publishing needs at least one variant.
- Archiving is final.
- SKUs are unique across products and variants within a product.
- At most 100 variants per product.

## Configuration

| Setting | Default | Notes |
| --- | --- | --- |
| `MONGODB_URI` | localhost | Includes 2 s server-selection and connect timeouts |
| `IDENTITY_ISSUER` | `http://localhost:9000` | Must equal the `iss` in tokens |
| `IDENTITY_JWKS_URI` | `http://localhost:9000/oauth2/jwks` | Fetched lazily, cached, behind the `identity-jwks` breaker |
| `product.security.audience` | `product-service` | Tokens without it in `aud` get 401 |
| `product.cors.allowed-origins` | none (dev: localhost:3000/3001) | Explicit allowlist only |

## Production checklist

- [ ] `SPRING_PROFILES_ACTIVE=prod` (the default in the Docker image)
- [ ] Port 8081 reachable only by the orchestrator and Prometheus
- [ ] `IDENTITY_ISSUER` and `IDENTITY_JWKS_URI` point at the real Identity, over TLS
- [ ] One instance until rate limits move to Redis (or accept per-instance limits)

## Layout

```
domain/        Product aggregate, value objects, events, ports, error codes. Plain Java (ArchUnit-enforced)
application/   intake/ (commands + admin view), catalog/ (queries, read port, shop + transactional views)
adapter/in/    web: controllers, request records, ETag handling, Problem Details
adapter/out/   mongo: aggregate store, catalog reader, schema + indexes. audit: domain event log
config/        security (resource server, scopes), JWKS client with Resilience4j, rate limits, OpenAPI
```

ADRs are in `docs/adr/`. Architecture, sprint plan and roadmap live in the Old New Ecosystem docs.
