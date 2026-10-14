package com.oldnew.product.adapter.in.web;

import com.oldnew.product.application.catalog.CatalogPage;
import com.oldnew.product.application.catalog.ShopCatalog;
import com.oldnew.product.application.catalog.ShopProductView;
import com.oldnew.product.domain.model.ProductId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/** Core Shop: the public catalog. Published products only. Rate-limited per IP or per user. */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Shop catalog", description = "Public, read-only")
@SecurityRequirements
class ShopProductController {

    private static final CacheControl SHORT_PUBLIC_CACHE = CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic();

    private final ShopCatalog catalog;

    ShopProductController(ShopCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    @Operation(summary = "Browse published products", description = "Keyset paging: pass nextCursor back as cursor.")
    ResponseEntity<PageResponse<ShopProductView>> list(
            @RequestParam(required = false) @Size(max = 64) String category,
            @RequestParam(required = false) @Size(max = 200) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(ShopCatalog.MAX_PAGE_SIZE) int size) {
        CatalogPage<ShopProductView> page = catalog.list(category, cursor, size);
        return ResponseEntity.ok().cacheControl(SHORT_PUBLIC_CACHE).body(new PageResponse<>(page.items(), page.nextCursor()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "One published product")
    ResponseEntity<ShopProductView> get(@PathVariable String id) {
        return ResponseEntity.ok().cacheControl(SHORT_PUBLIC_CACHE).body(catalog.get(ProductId.of(id)));
    }
}
