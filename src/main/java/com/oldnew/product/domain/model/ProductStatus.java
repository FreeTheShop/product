package com.oldnew.product.domain.model;

/** DRAFT -> PUBLISHED -> ARCHIVED, and DRAFT -> ARCHIVED. Nothing comes back from ARCHIVED. */
public enum ProductStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}
