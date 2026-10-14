# ADR-0003: DDD with hexagonal packages and light CQRS, one database

**Status:** Proposed · **Date:** 2026-10-08

## Context
Product has one write side, Intake (catalog managers), and two read sides: Shop (public, high volume) and Transactional (other services at checkout). The three differ in shape, load and security.

## Decision
- One `Product` aggregate enforces every rule. Commands load it, change it through methods and save it, with optimistic locking via `@Version`, exposed as ETag / If-Match.
- Queries skip the aggregate. `CatalogReader` returns read models straight from MongoDB with projections and index-backed keyset paging.
- One database, no event sourcing, no message bus. Domain events are logged today; an outbox comes when another service must react to them.
- `domain` is plain Java. ArchUnit fails the build if it imports Spring, Mongo or Swagger.

## Consequences
- Rules are unit-tested in milliseconds without Spring.
- A separate read store (search index) comes only if Shop search outgrows MongoDB indexes.
