package com.oldnew.product.application.catalog;

import java.util.List;

/** One page of a keyset-paged list. {@code nextCursor} is null on the last page. */
public record CatalogPage<T>(List<T> items, String nextCursor) {
}
