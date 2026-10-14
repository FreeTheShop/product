package com.oldnew.product.adapter.out.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Storage shape of a product. Never leaves this package. */
@Document(collection = ProductDocument.COLLECTION)
public record ProductDocument(
        @Id String id,
        String sku,
        String name,
        String description,
        String categoryId,
        String status,
        List<VariantDocument> variants,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        @Version Long version) {

    static final String COLLECTION = "products";

    public record VariantDocument(String sku, Map<String, String> attributes) {
    }
}
