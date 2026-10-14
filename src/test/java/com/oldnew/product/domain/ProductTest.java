package com.oldnew.product.domain;

import com.oldnew.product.domain.error.DomainException;
import com.oldnew.product.domain.error.ErrorCode;
import com.oldnew.product.domain.event.ProductArchived;
import com.oldnew.product.domain.event.ProductPublished;
import com.oldnew.product.domain.model.CategoryId;
import com.oldnew.product.domain.model.Description;
import com.oldnew.product.domain.model.Product;
import com.oldnew.product.domain.model.ProductId;
import com.oldnew.product.domain.model.ProductName;
import com.oldnew.product.domain.model.ProductStatus;
import com.oldnew.product.domain.model.Sku;
import com.oldnew.product.domain.model.Variant;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-12T09:00:00Z"), ZoneOffset.UTC);
    private static final Instant NOW = CLOCK.instant();

    private static Product draft() {
        return Product.draft(ProductId.newId(CLOCK), Sku.of("RUN-1"), new ProductName("Runner"),
                new Description("Light"), CategoryId.of("shoes"), List.of(), "user-1", NOW);
    }

    private static Variant variant(String sku) {
        return new Variant(Sku.of(sku), Map.of("size", "42"));
    }

    private static void assertFails(ErrorCode expected, ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(DomainException.class,
                e -> assertThat(e.code()).isEqualTo(expected));
    }

    @Test
    void newProductIsADraftWithoutEvents() {
        Product product = draft();

        assertThat(product.status()).isEqualTo(ProductStatus.DRAFT);
        assertThat(product.pullEvents()).isEmpty();
        assertThat(product.version()).isNull();
    }

    @Test
    void cannotPublishWithoutAVariant() {
        assertFails(ErrorCode.PRODUCT_HAS_NO_VARIANTS, () -> draft().publish(NOW));
    }

    @Test
    void publishingADraftWithAVariantRecordsAnEvent() {
        Product product = draft();
        product.addVariant(variant("RUN-1-42"), NOW);

        product.publish(NOW);

        assertThat(product.status()).isEqualTo(ProductStatus.PUBLISHED);
        assertThat(product.pullEvents()).singleElement().isInstanceOf(ProductPublished.class);
        assertThat(product.pullEvents()).as("events are handed out once").isEmpty();
    }

    @Test
    void cannotPublishTwice() {
        Product product = draft();
        product.addVariant(variant("RUN-1-42"), NOW);
        product.publish(NOW);

        assertFails(ErrorCode.INVALID_STATE_TRANSITION, () -> product.publish(NOW));
    }

    @Test
    void variantSkusAreUniqueAndDifferFromTheProductSku() {
        Product product = draft();
        product.addVariant(variant("RUN-1-42"), NOW);

        assertFails(ErrorCode.DUPLICATE_VARIANT_SKU, () -> product.addVariant(variant("run-1-42"), NOW));
        assertFails(ErrorCode.DUPLICATE_VARIANT_SKU, () -> product.addVariant(variant("RUN-1"), NOW));
    }

    @Test
    void atMostOneHundredVariants() {
        Product product = draft();
        for (int i = 0; i < Product.MAX_VARIANTS; i++) {
            product.addVariant(variant("RUN-V" + i), NOW);
        }

        assertFails(ErrorCode.TOO_MANY_VARIANTS, () -> product.addVariant(variant("RUN-V100"), NOW));
    }

    @Test
    void archivedProductsAreFrozen() {
        Product product = draft();
        product.archive(NOW);

        assertThat(product.pullEvents()).singleElement().isInstanceOf(ProductArchived.class);
        assertFails(ErrorCode.PRODUCT_ARCHIVED, () -> product.addVariant(variant("RUN-1-43"), NOW));
        assertFails(ErrorCode.PRODUCT_ARCHIVED, () -> product.publish(NOW));
        assertFails(ErrorCode.PRODUCT_ARCHIVED, () -> product.archive(NOW));
    }

    @Test
    void variantAttributesCannotSmuggleMongoOperators() {
        assertFails(ErrorCode.INVALID_VARIANT, () -> new Variant(Sku.of("X-1-A"), Map.of("$where", "1")));
        assertFails(ErrorCode.INVALID_VARIANT, () -> new Variant(Sku.of("X-1-A"), Map.of("a.b", "1")));
    }
}
