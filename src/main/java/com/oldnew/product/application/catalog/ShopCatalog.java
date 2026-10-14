package com.oldnew.product.application.catalog;

import com.oldnew.product.domain.error.DomainException;
import com.oldnew.product.domain.error.ErrorCode;
import com.oldnew.product.domain.model.CategoryId;
import com.oldnew.product.domain.model.ProductId;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * Queries (Core Shop): browse the published catalog. Public, high volume, read-only.
 * Paging is keyset-based (no skip), so page 1000 costs the same as page 1.
 */
@Service
public class ShopCatalog {

    public static final int MAX_PAGE_SIZE = 100;

    private final CatalogReader reader;

    public ShopCatalog(CatalogReader reader) {
        this.reader = reader;
    }

    public CatalogPage<ShopProductView> list(String category, String cursor, int size) {
        int limit = Math.clamp(size, 1, MAX_PAGE_SIZE);
        CategoryId categoryId = category == null || category.isBlank() ? null : CategoryId.of(category);
        ProductId after = decodeCursor(cursor);
        List<ShopProductView> rows = reader.findPublished(categoryId, after, limit + 1);
        if (rows.size() <= limit) {
            return new CatalogPage<>(rows, null);
        }
        List<ShopProductView> page = rows.subList(0, limit);
        return new CatalogPage<>(List.copyOf(page), encodeCursor(page.getLast().id()));
    }

    public ShopProductView get(ProductId id) {
        return reader.findPublishedById(id)
                .orElseThrow(() -> new DomainException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found"));
    }

    /** The cursor is opaque to clients: base64url of the last id, so its format can change later. */
    static String encodeCursor(String lastId) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(lastId.getBytes(StandardCharsets.UTF_8));
    }

    static ProductId decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return ProductId.of(new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8));
        } catch (IllegalArgumentException | DomainException e) {
            throw new DomainException(ErrorCode.INVALID_CURSOR, "The cursor is not valid; start again without it", e);
        }
    }
}
