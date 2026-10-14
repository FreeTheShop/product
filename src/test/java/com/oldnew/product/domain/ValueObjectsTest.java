package com.oldnew.product.domain;

import com.oldnew.product.domain.error.DomainException;
import com.oldnew.product.domain.model.CategoryId;
import com.oldnew.product.domain.model.Description;
import com.oldnew.product.domain.model.ProductId;
import com.oldnew.product.domain.model.ProductName;
import com.oldnew.product.domain.model.Sku;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValueObjectsTest {

    @Test
    void skuIsNormalisedToUpperCase() {
        assertThat(Sku.of(" run-42 ").value()).isEqualTo("RUN-42");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "AB", "-AB", "A B", "A$B", "A.B"})
    void malformedSkusAreRejected(String raw) {
        assertThatThrownBy(() -> Sku.of(raw)).isInstanceOf(DomainException.class);
    }

    @Test
    void productIdsAreVersion7AndTimeOrdered() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-12T09:00:00Z"), ZoneOffset.UTC);
        ProductId first = ProductId.newId(clock);
        ProductId second = ProductId.newId(Clock.offset(clock, Duration.ofMillis(1)));

        assertThat(first.value().charAt(14)).isEqualTo('7');
        assertThat(first).isLessThan(second);
    }

    @Test
    void productIdRejectsAnythingButUuidV7() {
        assertThatThrownBy(() -> ProductId.of("123")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> ProductId.of("{\"$ne\":null}")).isInstanceOf(DomainException.class);
    }

    @Test
    void nameAndDescriptionLimits() {
        assertThatThrownBy(() -> new ProductName("  ")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new ProductName("x".repeat(201))).isInstanceOf(DomainException.class);
        assertThat(new Description(null).value()).isEmpty();
        assertThatThrownBy(() -> new Description("x".repeat(5001))).isInstanceOf(DomainException.class);
    }

    @Test
    void categoryIsASlug() {
        assertThat(CategoryId.of("Running-Shoes").value()).isEqualTo("running-shoes");
        assertThatThrownBy(() -> CategoryId.of("shoes/../admin")).isInstanceOf(DomainException.class);
    }
}
