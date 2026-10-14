package com.oldnew.product.application.catalog;

import com.oldnew.product.domain.model.CategoryId;
import com.oldnew.product.domain.model.ProductId;

import java.util.List;
import java.util.Optional;

/**
 * Read-side port (CQRS-lite): queries go straight to read models, bypassing the aggregate.
 * Implemented by the Mongo adapter with projections and index-backed keyset paging.
 */
public interface CatalogReader {

    /** Published products after {@code afterId} (exclusive), oldest first, at most {@code limit}. */
    List<ShopProductView> findPublished(CategoryId category, ProductId afterId, int limit);

    Optional<ShopProductView> findPublishedById(ProductId id);

    Optional<TransactionalProductView> findForTransaction(ProductId id);
}
