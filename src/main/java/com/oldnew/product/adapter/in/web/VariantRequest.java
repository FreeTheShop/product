package com.oldnew.product.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record VariantRequest(
        @Schema(example = "RUN-2026-42") @NotBlank @Size(max = 40) String sku,
        @Schema(example = "{\"size\":\"42\",\"colour\":\"red\"}") @Size(max = 20) Map<String, String> attributes) {
}
