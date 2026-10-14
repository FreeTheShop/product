package com.oldnew.product.application.intake;

import java.util.List;
import java.util.Map;

public record CreateProductCommand(String sku, String name, String description, String categoryId,
                                   List<VariantInput> variants) {

    public record VariantInput(String sku, Map<String, String> attributes) {
    }
}
