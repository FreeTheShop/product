package com.oldnew.product.adapter.out.audit;

import com.oldnew.product.domain.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Today: domain events become structured log lines (searchable by traceId).
 * Later: the place to add an outbox when Price or Client Core need to react to them.
 */
@Component
class DomainEventLog {

    private static final Logger log = LoggerFactory.getLogger(DomainEventLog.class);

    @EventListener
    void on(DomainEvent event) {
        log.info("domain.event type={} productId={} at={}", event.getClass().getSimpleName(), event.productId(),
                event.occurredAt());
    }
}
