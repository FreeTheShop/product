package com.oldnew.product.application.intake;

import com.oldnew.product.domain.model.Product;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Full view for catalog managers, including status and the version for If-Match. */
public record AdminProductView(String id, String sku, String name, String description, String categoryId,
                               String status, List<VariantView> variants, Instant createdAt, Instant updatedAt,
                               long version) {

    public record VariantView(String sku, Map<String, String> attributes) {
    }

    public static AdminProductView of(Product p) {
        return new AdminProductView(p.id().value(), p.sku().value(), p.name().value(), p.description().value(),
                p.categoryId().value(), p.status().name(),
                p.variants().stream().map(v -> new VariantView(v.sku().value(), v.attributes())).toList(),
                p.createdAt(), p.updatedAt(), p.version() == null ? 0 : p.version());
    }
}
