package com.oldnew.product.application.intake;

import com.oldnew.product.domain.model.Product;
import com.oldnew.product.domain.model.ProductId;
import com.oldnew.product.domain.model.Sku;
import com.oldnew.product.domain.model.Variant;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Map;

/** Use case (Intake): add a sellable variant to a product. */
@Service
public class AddVariant {

    private final ProductWriter writer;
    private final Clock clock;

    public AddVariant(ProductWriter writer, Clock clock) {
        this.writer = writer;
        this.clock = clock;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CATALOG_MANAGER')")
    public AdminProductView handle(ProductId id, long expectedVersion, String variantSku, Map<String, String> attributes) {
        Product product = writer.loadForChange(id, expectedVersion);
        product.addVariant(new Variant(Sku.of(variantSku), attributes), clock.instant());
        return writer.save(product);
    }
}
