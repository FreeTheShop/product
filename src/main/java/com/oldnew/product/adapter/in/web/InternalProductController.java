package com.oldnew.product.adapter.in.web;

import com.oldnew.product.application.catalog.TransactionalCatalog;
import com.oldnew.product.application.catalog.TransactionalProductView;
import com.oldnew.product.domain.model.ProductId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Core Transactional: for other services (client_credentials, scope product:internal). */
@RestController
@RequestMapping("/api/v1/internal/products")
@Tag(name = "Internal: products", description = "Service-to-service, scope product:internal")
class InternalProductController {

    private final TransactionalCatalog catalog;

    InternalProductController(TransactionalCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Sellability and variant SKUs at checkout time")
    TransactionalProductView get(@PathVariable String id) {
        return catalog.get(ProductId.of(id));
    }
}
