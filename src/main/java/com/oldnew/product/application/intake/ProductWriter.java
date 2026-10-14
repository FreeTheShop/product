package com.oldnew.product.application.intake;

import com.oldnew.product.domain.error.DomainException;
import com.oldnew.product.domain.error.ErrorCode;
import com.oldnew.product.domain.event.DomainEvent;
import com.oldnew.product.domain.model.Product;
import com.oldnew.product.domain.model.ProductId;
import com.oldnew.product.domain.port.ProductRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Shared steps of every change to an existing product: load it, check the caller's version (If-Match),
 * save it, then publish its domain events. Keeps each use case down to the one rule it is about.
 */
@Component
class ProductWriter {

    private final ProductRepository products;
    private final ApplicationEventPublisher events;

    ProductWriter(ProductRepository products, ApplicationEventPublisher events) {
        this.products = products;
        this.events = events;
    }

    Product loadForChange(ProductId id, long expectedVersion) {
        Product product = products.findById(id)
                .orElseThrow(() -> new DomainException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found"));
        if (!Objects.equals(product.version(), expectedVersion)) {
            throw new DomainException(ErrorCode.VERSION_MISMATCH,
                    "The product changed since you read it; reload and retry");
        }
        return product;
    }

    AdminProductView save(Product product) {
        List<DomainEvent> pending = product.pullEvents();
        Product saved = products.save(product);
        pending.forEach(events::publishEvent);
        return AdminProductView.of(saved);
    }
}
