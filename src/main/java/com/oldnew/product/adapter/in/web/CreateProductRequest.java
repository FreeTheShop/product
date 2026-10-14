package com.oldnew.product.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Only these fields can be set when creating a product. Status, id, version and audit fields are never
 * read from the request (no mass assignment). Domain value objects apply the exact rules.
 */
public record CreateProductRequest(
        @Schema(example = "RUN-2026") @NotBlank @Size(max = 40) String sku,
        @Schema(example = "Trail Runner 2026") @NotBlank @Size(max = 200) String name,
        @Schema(example = "Lightweight trail shoe.") @Size(max = 5000) String description,
        @Schema(example = "running-shoes") @NotBlank @Size(max = 64) String categoryId,
        @Size(max = 100) List<@Valid VariantRequest> variants) {
}
