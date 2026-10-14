package com.oldnew.product.domain.model;

import com.oldnew.product.domain.error.DomainException;
import com.oldnew.product.domain.error.ErrorCode;
import com.oldnew.product.domain.event.DomainEvent;
import com.oldnew.product.domain.event.ProductArchived;
import com.oldnew.product.domain.event.ProductPublished;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root of the Product catalog.
 * <p>
 * Invariants:
 * <ul>
 *     <li>A product can be published only from DRAFT and only with at least one variant.</li>
 *     <li>Variant SKUs are unique within the product and never equal the product SKU; at most 100 variants.</li>
 *     <li>An archived product cannot change and never comes back.</li>
 *     <li>The product SKU never changes.</li>
 * </ul>
 * Prices live in the Price service and refer to this product by id.
 */
public final class Product {

    public static final int MAX_VARIANTS = 100;

    private final ProductId id;
    private final Sku sku;
    private final Instant createdAt;
    private final String createdBy;
    private final Long version;
    private final List<Variant> variants;
    private final List<DomainEvent> pendingEvents = new ArrayList<>();
    private ProductName name;
    private Description description;
    private CategoryId categoryId;
    private ProductStatus status;
    private Instant updatedAt;

    private Product(ProductId id, Sku sku, ProductName name, Description description, CategoryId categoryId,
                    ProductStatus status, List<Variant> variants, Instant createdAt, Instant updatedAt,
                    String createdBy, Long version) {
        this.id = Objects.requireNonNull(id, "id");
        this.sku = Objects.requireNonNull(sku, "sku");
        this.name = Objects.requireNonNull(name, "name");
        this.description = description == null ? Description.empty() : description;
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId");
        this.status = Objects.requireNonNull(status, "status");
        this.variants = new ArrayList<>();
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy");
        this.version = version;
        for (Variant variant : variants) {
            addVariantChecked(variant);
        }
    }

    /** A new product always starts as a DRAFT. */
    public static Product draft(ProductId id, Sku sku, ProductName name, Description description, CategoryId categoryId,
                                List<Variant> variants, String createdBy, Instant now) {
        return new Product(id, sku, name, description, categoryId, ProductStatus.DRAFT,
                variants == null ? List.of() : variants, now, now, createdBy, null);
    }

    /** Rebuild from storage. Only persistence adapters call this. */
    public static Product reconstitute(ProductId id, Sku sku, ProductName name, Description description,
                                       CategoryId categoryId, ProductStatus status, List<Variant> variants,
                                       Instant createdAt, Instant updatedAt, String createdBy, Long version) {
        return new Product(id, sku, name, description, categoryId, status, variants, createdAt, updatedAt,
                createdBy, version);
    }

    public void addVariant(Variant variant, Instant now) {
        requireNotArchived();
        addVariantChecked(variant);
        updatedAt = now;
    }

    public void updateDetails(ProductName newName, Description newDescription, CategoryId newCategory, Instant now) {
        requireNotArchived();
        this.name = Objects.requireNonNull(newName, "name");
        this.description = newDescription == null ? Description.empty() : newDescription;
        this.categoryId = Objects.requireNonNull(newCategory, "categoryId");
        updatedAt = now;
    }

    public void publish(Instant now) {
        requireNotArchived();
        if (status != ProductStatus.DRAFT) {
            throw new DomainException(ErrorCode.INVALID_STATE_TRANSITION, "Only a draft can be published");
        }
        if (variants.isEmpty()) {
            throw new DomainException(ErrorCode.PRODUCT_HAS_NO_VARIANTS, "Add at least one variant before publishing");
        }
        status = ProductStatus.PUBLISHED;
        updatedAt = now;
        pendingEvents.add(new ProductPublished(id.value(), sku.value(), now));
    }

    public void archive(Instant now) {
        requireNotArchived();
        status = ProductStatus.ARCHIVED;
        updatedAt = now;
        pendingEvents.add(new ProductArchived(id.value(), sku.value(), now));
    }

    /** Events recorded since the last call; the caller publishes them after a successful save. */
    public List<DomainEvent> pullEvents() {
        List<DomainEvent> events = List.copyOf(pendingEvents);
        pendingEvents.clear();
        return events;
    }

    private void addVariantChecked(Variant variant) {
        Objects.requireNonNull(variant, "variant");
        if (variants.size() >= MAX_VARIANTS) {
            throw new DomainException(ErrorCode.TOO_MANY_VARIANTS, "A product has at most " + MAX_VARIANTS + " variants");
        }
        if (variant.sku().equals(sku) || variants.stream().anyMatch(v -> v.sku().equals(variant.sku()))) {
            throw new DomainException(ErrorCode.DUPLICATE_VARIANT_SKU,
                    "Variant SKU " + variant.sku().value() + " is already used in this product");
        }
        variants.add(variant);
    }

    private void requireNotArchived() {
        if (status == ProductStatus.ARCHIVED) {
            throw new DomainException(ErrorCode.PRODUCT_ARCHIVED, "An archived product cannot change");
        }
    }

    public ProductId id() {
        return id;
    }

    public Sku sku() {
        return sku;
    }

    public ProductName name() {
        return name;
    }

    public Description description() {
        return description;
    }

    public CategoryId categoryId() {
        return categoryId;
    }

    public ProductStatus status() {
        return status;
    }

    public List<Variant> variants() {
        return List.copyOf(variants);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public String createdBy() {
        return createdBy;
    }

    /** Optimistic-locking token; null until first saved. */
    public Long version() {
        return version;
    }
}
