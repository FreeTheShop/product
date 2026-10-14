package com.oldnew.product.application.catalog;

import java.util.List;

/**
 * What checkout-time services (Price, Validation, Client Core) need: does it exist, can it be sold,
 * which variant SKUs are valid. Includes ARCHIVED products so callers can refuse them explicitly.
 */
public record TransactionalProductView(String id, String sku, String status, boolean sellable,
                                       List<String> variantSkus, long version) {
}
