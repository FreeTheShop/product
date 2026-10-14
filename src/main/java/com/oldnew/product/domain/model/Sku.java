package com.oldnew.product.domain.model;

import com.oldnew.product.domain.error.DomainException;
import com.oldnew.product.domain.error.ErrorCode;

import java.util.Locale;
import java.util.regex.Pattern;

/** Stock-keeping unit: 3 to 40 of A-Z, 0-9 and '-', upper-case, starting with a letter or digit. */
public record Sku(String value) {

    private static final Pattern SHAPE = Pattern.compile("^[A-Z0-9][A-Z0-9-]{2,39}$");

    public Sku {
        if (value == null) {
            throw new DomainException(ErrorCode.INVALID_SKU, "SKU is required");
        }
        value = value.trim().toUpperCase(Locale.ROOT);
        if (!SHAPE.matcher(value).matches()) {
            throw new DomainException(ErrorCode.INVALID_SKU,
                    "SKU must be 3-40 characters of A-Z, 0-9 and '-', starting with a letter or digit");
        }
    }

    public static Sku of(String raw) {
        return new Sku(raw);
    }
}
