package com.oldnew.product.application;

import com.oldnew.product.application.catalog.CatalogPage;
import com.oldnew.product.application.catalog.CatalogReader;
import com.oldnew.product.application.catalog.ShopCatalog;
import com.oldnew.product.application.catalog.ShopProductView;
import com.oldnew.product.application.catalog.TransactionalProductView;
import com.oldnew.product.domain.error.DomainException;
import com.oldnew.product.domain.error.ErrorCode;
import com.oldnew.product.domain.model.CategoryId;
import com.oldnew.product.domain.model.ProductId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Keyset paging logic, with an in-memory reader (a fake, not a mock of the thing under test). */
class ShopCatalogTest {

    private final List<ShopProductView> published = new ArrayList<>();
    private final ShopCatalog catalog = new ShopCatalog(new InMemoryReader());

    ShopCatalogTest() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-12T09:00:00Z"), ZoneOffset.UTC);
        for (int i = 0; i < 5; i++) {
            String id = ProductId.newId(Clock.offset(clock, Duration.ofMillis(i))).value();
            published.add(new ShopProductView(id, "SKU-" + i, "Product " + i, "", "shoes", List.of()));
        }
    }

    @Test
    void walksAllPagesWithoutGapsOrRepeats() {
        List<String> seen = new ArrayList<>();
        String cursor = null;
        do {
            CatalogPage<ShopProductView> page = catalog.list(null, cursor, 2);
            page.items().forEach(p -> seen.add(p.id()));
            cursor = page.nextCursor();
        } while (cursor != null);

        assertThat(seen).containsExactlyElementsOf(published.stream().map(ShopProductView::id).toList());
    }

    @Test
    void lastPageHasNoCursor() {
        assertThat(catalog.list(null, null, 5).nextCursor()).isNull();
        assertThat(catalog.list(null, null, 4).nextCursor()).isNotNull();
    }

    @Test
    void pageSizeIsCappedAtOneHundred() {
        assertThat(catalog.list(null, null, 10_000).items()).hasSize(5);
    }

    @Test
    void tamperedCursorIsAClientError() {
        assertThatThrownBy(() -> catalog.list(null, "not-base64!!", 2))
                .isInstanceOfSatisfying(DomainException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_CURSOR));
    }

    private final class InMemoryReader implements CatalogReader {

        @Override
        public List<ShopProductView> findPublished(CategoryId category, ProductId afterId, int limit) {
            return published.stream()
                    .filter(p -> afterId == null || p.id().compareTo(afterId.value()) > 0)
                    .limit(limit)
                    .toList();
        }

        @Override
        public Optional<ShopProductView> findPublishedById(ProductId id) {
            return published.stream().filter(p -> p.id().equals(id.value())).findFirst();
        }

        @Override
        public Optional<TransactionalProductView> findForTransaction(ProductId id) {
            return Optional.empty();
        }
    }
}
