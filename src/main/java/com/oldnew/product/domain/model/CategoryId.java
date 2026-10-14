package com.oldnew.product.domain.model;

import com.oldnew.product.domain.error.DomainException;
import com.oldnew.product.domain.error.ErrorCode;

import java.util.Locale;
import java.util.regex.Pattern;

/** Category slug, e.g. "mens-shoes". Categories get their own model later; for now a validated reference. */
public record CategoryId(String value) {

    private static final Pattern SHAPE = Pattern.compile("^[a-z0-9][a-z0-9-]{0,63}$");

    public CategoryId {
        if (value == null) {
            throw new DomainException(ErrorCode.INVALID_CATEGORY, "Category is required");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (!SHAPE.matcher(value).matches()) {
            throw new DomainException(ErrorCode.INVALID_CATEGORY, "Category must be 1-64 of a-z, 0-9 and '-'");
        }
    }

    public static CategoryId of(String raw) {
        return new CategoryId(raw);
    }
}
