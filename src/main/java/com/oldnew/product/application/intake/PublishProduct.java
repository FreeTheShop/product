package com.oldnew.product.application.intake;

import com.oldnew.product.domain.model.Product;
import com.oldnew.product.domain.model.ProductId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Clock;

/** Use case (Intake): make a draft visible in the shop. */
@Service
public class PublishProduct {

    private static final Logger log = LoggerFactory.getLogger(PublishProduct.class);

    private final ProductWriter writer;
    private final Clock clock;

    public PublishProduct(ProductWriter writer, Clock clock) {
        this.writer = writer;
        this.clock = clock;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CATALOG_MANAGER')")
    public AdminProductView handle(ProductId id, long expectedVersion, String actor) {
        Product product = writer.loadForChange(id, expectedVersion);
        product.publish(clock.instant());
        AdminProductView published = writer.save(product);
        log.info("audit.product.published actor={} productId={}", actor, id);
        return published;
    }
}
