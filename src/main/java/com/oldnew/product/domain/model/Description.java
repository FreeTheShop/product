package com.oldnew.product.domain.model;

import com.oldnew.product.domain.error.DomainException;
import com.oldnew.product.domain.error.ErrorCode;

/** Plain-text description, empty allowed. Rendering (and escaping) is the UI's job. */
public record Description(String value) {

    public static final int MAX_LENGTH = 5_000;

    public Description {
        value = value == null ? "" : value.strip();
        if (value.length() > MAX_LENGTH) {
            throw new DomainException(ErrorCode.INVALID_DESCRIPTION,
                    "Description must be at most " + MAX_LENGTH + " characters");
        }
    }

    public static Description empty() {
        return new Description("");
    }
}
