package com.oldnew.product.domain.model;

import com.oldnew.product.domain.error.DomainException;
import com.oldnew.product.domain.error.ErrorCode;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Product identity: a UUIDv7 (RFC 9562). Time-ordered, so newer products sort last, which keeps the
 * keyset-paged catalog stable and index-friendly; random enough that ids cannot be guessed.
 */
public record ProductId(String value) implements Comparable<ProductId> {

    private static final Pattern SHAPE =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    public ProductId {
        if (value == null || !SHAPE.matcher(value).matches()) {
            throw new DomainException(ErrorCode.INVALID_PRODUCT_ID, "Product id is not valid");
        }
    }

    public static ProductId of(String raw) {
        return new ProductId(raw == null ? null : raw.trim().toLowerCase(java.util.Locale.ROOT));
    }

    public static ProductId newId(Clock clock) {
        long millis = clock.millis();
        byte[] random = new byte[10];
        RANDOM.nextBytes(random);
        long msb = (millis << 16) | 0x7000L | ((random[0] & 0x0FL) << 8) | (random[1] & 0xFFL);
        long lsb = 0x8000000000000000L | ((random[2] & 0x3FL) << 56);
        for (int i = 3; i < 10; i++) {
            lsb |= (random[i] & 0xFFL) << (8 * (9 - i));
        }
        return new ProductId(new UUID(msb, lsb).toString());
    }

    @Override
    public int compareTo(ProductId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
