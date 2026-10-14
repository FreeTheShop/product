package com.oldnew.product.domain.model;

import com.oldnew.product.domain.error.DomainException;
import com.oldnew.product.domain.error.ErrorCode;

public record ProductName(String value) {

    public static final int MAX_LENGTH = 200;

    public ProductName {
        if (value == null || value.isBlank()) {
            throw new DomainException(ErrorCode.INVALID_NAME, "Name is required");
        }
        value = value.strip();
        if (value.length() > MAX_LENGTH) {
            throw new DomainException(ErrorCode.INVALID_NAME, "Name must be at most " + MAX_LENGTH + " characters");
        }
    }
}
