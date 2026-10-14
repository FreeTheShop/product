package com.oldnew.product.application.catalog;

import java.util.List;
import java.util.Map;

/** What shoppers see. No status, version, audit fields or internal notes. */
public record ShopProductView(String id, String sku, String name, String description, String categoryId,
                              List<Variant> variants) {

    public record Variant(String sku, Map<String, String> attributes) {
    }
}
