package com.oldnew.product.application.intake;

import com.oldnew.product.domain.error.DomainException;
import com.oldnew.product.domain.error.ErrorCode;
import com.oldnew.product.domain.model.CategoryId;
import com.oldnew.product.domain.model.Description;
import com.oldnew.product.domain.model.Product;
import com.oldnew.product.domain.model.ProductId;
import com.oldnew.product.domain.model.ProductName;
import com.oldnew.product.domain.model.Sku;
import com.oldnew.product.domain.model.Variant;
import com.oldnew.product.domain.port.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;

/** Use case (Intake): a catalog manager adds a product as a draft. */
@Service
public class CreateProduct {

    private static final Logger log = LoggerFactory.getLogger(CreateProduct.class);

    private final ProductRepository products;
    private final ProductWriter writer;
    private final Clock clock;

    public CreateProduct(ProductRepository products, ProductWriter writer, Clock clock) {
        this.products = products;
        this.writer = writer;
        this.clock = clock;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CATALOG_MANAGER')")
    public AdminProductView handle(CreateProductCommand command, String actor) {
        Sku sku = Sku.of(command.sku());
        List<Variant> variants = command.variants() == null ? List.of() : command.variants().stream()
                .map(v -> new Variant(Sku.of(v.sku()), v.attributes()))
                .toList();
        Product product = Product.draft(ProductId.newId(clock), sku, new ProductName(command.name()),
                new Description(command.description()), CategoryId.of(command.categoryId()), variants, actor,
                clock.instant());
        if (products.existsBySku(sku)) {
            // The unique index still guards the race between this check and the insert.
            throw new DomainException(ErrorCode.SKU_ALREADY_EXISTS, "A product with this SKU already exists");
        }
        AdminProductView created = writer.save(product);
        log.info("audit.product.created actor={} productId={} sku={}", actor, created.id(), created.sku());
        return created;
    }
}
