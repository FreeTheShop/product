package com.oldnew.product.adapter.in.web;

import com.oldnew.product.domain.error.DomainException;
import com.oldnew.product.domain.error.ErrorCode;

/**
 * Optimistic concurrency over HTTP: the product version travels as a strong ETag ("3");
 * every change must send it back in If-Match, or it is refused with 428.
 */
final class EntityTags {

    private EntityTags() {
    }

    static long requireVersion(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new DomainException(ErrorCode.VERSION_REQUIRED, "Send the product's ETag in an If-Match header");
        }
        String tag = ifMatch.trim();
        if (tag.startsWith("W/")) {
            tag = tag.substring(2);
        }
        if (tag.length() >= 2 && tag.startsWith("\"") && tag.endsWith("\"")) {
            tag = tag.substring(1, tag.length() - 1);
        }
        try {
            long version = Long.parseLong(tag);
            if (version < 0) {
                throw new NumberFormatException("negative");
            }
            return version;
        } catch (NumberFormatException e) {
            throw new DomainException(ErrorCode.INVALID_VERSION, "If-Match must be the product's ETag, e.g. \"3\"", e);
        }
    }
}
