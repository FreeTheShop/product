package com.oldnew.product.domain.event;

import java.time.Instant;

/** Something that happened in the domain. Recorded by aggregates, published after a successful save. */
public sealed interface DomainEvent permits ProductPublished, ProductArchived {

    String productId();

    Instant occurredAt();
}
