package com.oldnew.product.domain.error;

/** Failure kinds. The web adapter maps each to an HTTP status; the domain never sees HTTP. */
public enum ErrorKind {
    INVALID_INPUT,
    NOT_FOUND,
    CONFLICT,
    RULE_VIOLATION,
    /** The caller's copy is stale (optimistic concurrency). */
    STALE_VERSION,
    /** The caller must say which version it is changing. */
    VERSION_REQUIRED
}
