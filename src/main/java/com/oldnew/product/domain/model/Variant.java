package com.oldnew.product.domain.model;

import com.oldnew.product.domain.error.DomainException;
import com.oldnew.product.domain.error.ErrorCode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** A sellable variant (e.g. size 42, colour red) with its own SKU. Immutable. */
public record Variant(Sku sku, Map<String, String> attributes) {

    public static final int MAX_ATTRIBUTES = 20;
    public static final int MAX_KEY_LENGTH = 50;
    public static final int MAX_VALUE_LENGTH = 200;

    public Variant {
        Objects.requireNonNull(sku, "sku");
        Map<String, String> copy = new LinkedHashMap<>();
        if (attributes != null) {
            if (attributes.size() > MAX_ATTRIBUTES) {
                throw new DomainException(ErrorCode.INVALID_VARIANT, "A variant has at most " + MAX_ATTRIBUTES + " attributes");
            }
            attributes.forEach((key, value) -> {
                if (key == null || key.isBlank() || key.length() > MAX_KEY_LENGTH
                        || key.startsWith("$") || key.contains(".")) {
                    throw new DomainException(ErrorCode.INVALID_VARIANT,
                            "Attribute names must be 1-" + MAX_KEY_LENGTH + " characters without '$' or '.'");
                }
                if (value == null || value.length() > MAX_VALUE_LENGTH) {
                    throw new DomainException(ErrorCode.INVALID_VARIANT,
                            "Attribute values must be at most " + MAX_VALUE_LENGTH + " characters");
                }
                copy.put(key.strip(), value.strip());
            });
        }
        attributes = Collections.unmodifiableMap(copy);
    }
}
