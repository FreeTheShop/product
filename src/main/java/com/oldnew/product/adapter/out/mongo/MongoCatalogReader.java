package com.oldnew.product.adapter.out.mongo;

import com.oldnew.product.application.catalog.CatalogReader;
import com.oldnew.product.application.catalog.ShopProductView;
import com.oldnew.product.application.catalog.TransactionalProductView;
import com.oldnew.product.domain.model.CategoryId;
import com.oldnew.product.domain.model.ProductId;
import com.oldnew.product.domain.model.ProductStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Read models straight from MongoDB. Every query here is backed by an index in {@link MongoCollectionSetup}:
 * <ul>
 *     <li>list: {status, categoryId, _id} or {status, _id}, keyset on _id (no skip)</li>
 *     <li>by id: _id</li>
 * </ul>
 * Projections leave out audit fields the readers must not see.
 */
@Component
class MongoCatalogReader implements CatalogReader {

    private final MongoTemplate mongo;

    MongoCatalogReader(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public List<ShopProductView> findPublished(CategoryId category, ProductId afterId, int limit) {
        Criteria criteria = Criteria.where("status").is(ProductStatus.PUBLISHED.name());
        if (category != null) {
            criteria = criteria.and("categoryId").is(category.value());
        }
        if (afterId != null) {
            criteria = criteria.and("id").gt(afterId.value());
        }
        Query query = new Query(criteria).with(Sort.by(Sort.Direction.ASC, "id")).limit(limit);
        shopFields(query);
        return mongo.find(query, ProductDocument.class).stream().map(MongoCatalogReader::toShopView).toList();
    }

    @Override
    public Optional<ShopProductView> findPublishedById(ProductId id) {
        Query query = new Query(Criteria.where("id").is(id.value()).and("status").is(ProductStatus.PUBLISHED.name()));
        shopFields(query);
        return Optional.ofNullable(mongo.findOne(query, ProductDocument.class)).map(MongoCatalogReader::toShopView);
    }

    @Override
    public Optional<TransactionalProductView> findForTransaction(ProductId id) {
        Query query = new Query(Criteria.where("id").is(id.value()));
        query.fields().include("sku", "status", "variants.sku", "version");
        return Optional.ofNullable(mongo.findOne(query, ProductDocument.class)).map(d -> new TransactionalProductView(
                d.id(), d.sku(), d.status(), ProductStatus.PUBLISHED.name().equals(d.status()),
                d.variants() == null ? List.of() : d.variants().stream().map(ProductDocument.VariantDocument::sku).toList(),
                d.version() == null ? 0 : d.version()));
    }

    private static void shopFields(Query query) {
        query.fields().include("sku", "name", "description", "categoryId", "variants");
    }

    private static ShopProductView toShopView(ProductDocument d) {
        return new ShopProductView(d.id(), d.sku(), d.name(), d.description(), d.categoryId(),
                d.variants() == null ? List.of() : d.variants().stream()
                        .map(v -> new ShopProductView.Variant(v.sku(), v.attributes())).toList());
    }
}
