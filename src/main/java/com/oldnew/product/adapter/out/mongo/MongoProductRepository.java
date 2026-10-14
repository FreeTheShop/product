package com.oldnew.product.adapter.out.mongo;

import com.oldnew.product.domain.error.DomainException;
import com.oldnew.product.domain.error.ErrorCode;
import com.oldnew.product.domain.model.CategoryId;
import com.oldnew.product.domain.model.Description;
import com.oldnew.product.domain.model.Product;
import com.oldnew.product.domain.model.ProductId;
import com.oldnew.product.domain.model.ProductName;
import com.oldnew.product.domain.model.ProductStatus;
import com.oldnew.product.domain.model.Sku;
import com.oldnew.product.domain.model.Variant;
import com.oldnew.product.domain.port.ProductRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Adapter: stores Product aggregates; translates storage failures into domain errors. */
@Repository
class MongoProductRepository implements ProductRepository {

    private final SpringDataProductRepository repository;

    MongoProductRepository(SpringDataProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        return repository.findById(id.value()).map(MongoProductRepository::toDomain);
    }

    @Override
    public boolean existsBySku(Sku sku) {
        return repository.existsBySku(sku.value());
    }

    @Override
    public Product save(Product product) {
        try {
            return toDomain(repository.save(toDocument(product)));
        } catch (DuplicateKeyException e) {
            throw new DomainException(ErrorCode.SKU_ALREADY_EXISTS, "A product with this SKU already exists", e);
        } catch (OptimisticLockingFailureException e) {
            throw new DomainException(ErrorCode.VERSION_MISMATCH, "The product changed since you read it; reload and retry", e);
        }
    }

    static ProductDocument toDocument(Product p) {
        List<ProductDocument.VariantDocument> variants = p.variants().stream()
                .map(v -> new ProductDocument.VariantDocument(v.sku().value(), v.attributes()))
                .toList();
        return new ProductDocument(p.id().value(), p.sku().value(), p.name().value(), p.description().value(),
                p.categoryId().value(), p.status().name(), variants, p.createdAt(), p.updatedAt(), p.createdBy(),
                p.version());
    }

    static Product toDomain(ProductDocument d) {
        List<Variant> variants = d.variants() == null ? List.of() : d.variants().stream()
                .map(v -> new Variant(Sku.of(v.sku()), v.attributes()))
                .toList();
        return Product.reconstitute(new ProductId(d.id()), Sku.of(d.sku()), new ProductName(d.name()),
                new Description(d.description()), CategoryId.of(d.categoryId()), ProductStatus.valueOf(d.status()),
                variants, d.createdAt(), d.updatedAt(), d.createdBy(), d.version());
    }
}
