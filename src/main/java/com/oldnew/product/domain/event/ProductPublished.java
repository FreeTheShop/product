package com.oldnew.product.domain.event;

import java.time.Instant;

public record ProductPublished(String productId, String sku, Instant occurredAt) implements DomainEvent {
}
