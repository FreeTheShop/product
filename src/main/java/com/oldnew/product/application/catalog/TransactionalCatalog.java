package com.oldnew.product.application.catalog;

import com.oldnew.product.domain.error.DomainException;
import com.oldnew.product.domain.error.ErrorCode;
import com.oldnew.product.domain.model.ProductId;
import org.springframework.stereotype.Service;

/** Query (Core Transactional): service-to-service lookups at checkout time. */
@Service
public class TransactionalCatalog {

    private final CatalogReader reader;

    public TransactionalCatalog(CatalogReader reader) {
        this.reader = reader;
    }

    public TransactionalProductView get(ProductId id) {
        return reader.findForTransaction(id)
                .orElseThrow(() -> new DomainException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found"));
    }
}
