package com.oldnew.product.application.intake;

import com.oldnew.product.domain.model.Product;
import com.oldnew.product.domain.model.ProductId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Clock;

/** Use case (Intake): withdraw a product for good. It disappears from the shop; history is kept. */
@Service
public class ArchiveProduct {

    private static final Logger log = LoggerFactory.getLogger(ArchiveProduct.class);

    private final ProductWriter writer;
    private final Clock clock;

    public ArchiveProduct(ProductWriter writer, Clock clock) {
        this.writer = writer;
        this.clock = clock;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CATALOG_MANAGER')")
    public AdminProductView handle(ProductId id, long expectedVersion, String actor) {
        Product product = writer.loadForChange(id, expectedVersion);
        product.archive(clock.instant());
        AdminProductView archived = writer.save(product);
        log.info("audit.product.archived actor={} productId={}", actor, id);
        return archived;
    }
}
