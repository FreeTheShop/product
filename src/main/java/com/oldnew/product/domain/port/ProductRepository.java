package com.oldnew.product.domain.port;

import com.oldnew.product.domain.model.Product;
import com.oldnew.product.domain.model.ProductId;
import com.oldnew.product.domain.model.Sku;

import java.util.Optional;

/**
 * Write-side port for the Product aggregate.
 * {@link #save} throws {@code DomainException(SKU_ALREADY_EXISTS)} on a duplicate SKU and
 * {@code DomainException(VERSION_MISMATCH)} when someone else saved first.
 */
public interface ProductRepository {

    Optional<Product> findById(ProductId id);

    boolean existsBySku(Sku sku);

    Product save(Product product);
}
