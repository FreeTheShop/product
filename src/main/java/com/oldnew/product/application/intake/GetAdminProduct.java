package com.oldnew.product.application.intake;

import com.oldnew.product.domain.error.DomainException;
import com.oldnew.product.domain.error.ErrorCode;
import com.oldnew.product.domain.model.ProductId;
import com.oldnew.product.domain.port.ProductRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/** Query (Intake): any product in any status, with its version for the next change. */
@Service
public class GetAdminProduct {

    private final ProductRepository products;

    public GetAdminProduct(ProductRepository products) {
        this.products = products;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CATALOG_MANAGER')")
    public AdminProductView handle(ProductId id) {
        return products.findById(id).map(AdminProductView::of)
                .orElseThrow(() -> new DomainException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found"));
    }
}
