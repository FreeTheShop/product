package com.oldnew.product.domain.error;

/** Stable error codes. Clients branch on these: never rename, only add and deprecate. */
public enum ErrorCode {
    VALIDATION_FAILED(ErrorKind.INVALID_INPUT),
    INVALID_PRODUCT_ID(ErrorKind.INVALID_INPUT),
    INVALID_SKU(ErrorKind.INVALID_INPUT),
    INVALID_NAME(ErrorKind.INVALID_INPUT),
    INVALID_DESCRIPTION(ErrorKind.INVALID_INPUT),
    INVALID_CATEGORY(ErrorKind.INVALID_INPUT),
    INVALID_VARIANT(ErrorKind.INVALID_INPUT),
    INVALID_CURSOR(ErrorKind.INVALID_INPUT),
    INVALID_VERSION(ErrorKind.INVALID_INPUT),
    PRODUCT_NOT_FOUND(ErrorKind.NOT_FOUND),
    SKU_ALREADY_EXISTS(ErrorKind.CONFLICT),
    DUPLICATE_VARIANT_SKU(ErrorKind.RULE_VIOLATION),
    TOO_MANY_VARIANTS(ErrorKind.RULE_VIOLATION),
    PRODUCT_HAS_NO_VARIANTS(ErrorKind.RULE_VIOLATION),
    PRODUCT_ARCHIVED(ErrorKind.RULE_VIOLATION),
    INVALID_STATE_TRANSITION(ErrorKind.RULE_VIOLATION),
    VERSION_MISMATCH(ErrorKind.STALE_VERSION),
    VERSION_REQUIRED(ErrorKind.VERSION_REQUIRED);

    private final ErrorKind kind;

    ErrorCode(ErrorKind kind) {
        this.kind = kind;
    }

    public ErrorKind kind() {
        return kind;
    }
}
