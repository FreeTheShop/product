package com.oldnew.product.adapter.in.web;

import com.oldnew.product.application.intake.AddVariant;
import com.oldnew.product.application.intake.AdminProductView;
import com.oldnew.product.application.intake.ArchiveProduct;
import com.oldnew.product.application.intake.CreateProduct;
import com.oldnew.product.application.intake.CreateProductCommand;
import com.oldnew.product.application.intake.GetAdminProduct;
import com.oldnew.product.application.intake.PublishProduct;
import com.oldnew.product.domain.model.ProductId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Intake (Admin context). Needs scope product:write AND role ADMIN or CATALOG_MANAGER:
 * the scope says the app may do it, the role says this person may.
 */
@RestController
@RequestMapping("/api/v1/admin/products")
@Tag(name = "Admin: products", description = "Scope product:write + role ADMIN or CATALOG_MANAGER")
class AdminProductController {

    private static final String IF_MATCH_HELP = "The product's current ETag, e.g. \"3\"";

    private final CreateProduct createProduct;
    private final GetAdminProduct getProduct;
    private final AddVariant addVariant;
    private final PublishProduct publishProduct;
    private final ArchiveProduct archiveProduct;

    AdminProductController(CreateProduct createProduct, GetAdminProduct getProduct, AddVariant addVariant,
                           PublishProduct publishProduct, ArchiveProduct archiveProduct) {
        this.createProduct = createProduct;
        this.getProduct = getProduct;
        this.addVariant = addVariant;
        this.publishProduct = publishProduct;
        this.archiveProduct = archiveProduct;
    }

    @PostMapping
    @Operation(summary = "Create a draft product")
    ResponseEntity<AdminProductView> create(@Valid @RequestBody CreateProductRequest request,
                                            @AuthenticationPrincipal Jwt jwt) {
        List<CreateProductCommand.VariantInput> variants = request.variants() == null ? List.of()
                : request.variants().stream().map(v -> new CreateProductCommand.VariantInput(v.sku(), v.attributes())).toList();
        AdminProductView created = createProduct.handle(new CreateProductCommand(request.sku(), request.name(),
                request.description(), request.categoryId(), variants), jwt.getSubject());
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).eTag(Long.toString(created.version())).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Read a product in any status, with its ETag")
    ResponseEntity<AdminProductView> get(@PathVariable String id) {
        return withETag(getProduct.handle(ProductId.of(id)));
    }

    @PostMapping("/{id}/variants")
    @Operation(summary = "Add a variant")
    ResponseEntity<AdminProductView> addVariant(@PathVariable String id,
                                                @Parameter(description = IF_MATCH_HELP) @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch,
                                                @Valid @RequestBody VariantRequest request) {
        return withETag(addVariant.handle(ProductId.of(id), EntityTags.requireVersion(ifMatch), request.sku(), request.attributes()));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish a draft", description = "Needs at least one variant. Fires ProductPublished.")
    ResponseEntity<AdminProductView> publish(@PathVariable String id,
                                             @Parameter(description = IF_MATCH_HELP) @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch,
                                             @AuthenticationPrincipal Jwt jwt) {
        return withETag(publishProduct.handle(ProductId.of(id), EntityTags.requireVersion(ifMatch), jwt.getSubject()));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive for good", description = "Removed from the shop; cannot be undone.")
    ResponseEntity<AdminProductView> archive(@PathVariable String id,
                                             @Parameter(description = IF_MATCH_HELP) @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch,
                                             @AuthenticationPrincipal Jwt jwt) {
        return withETag(archiveProduct.handle(ProductId.of(id), EntityTags.requireVersion(ifMatch), jwt.getSubject()));
    }

    private static ResponseEntity<AdminProductView> withETag(AdminProductView view) {
        return ResponseEntity.ok().eTag(Long.toString(view.version())).body(view);
    }
}
