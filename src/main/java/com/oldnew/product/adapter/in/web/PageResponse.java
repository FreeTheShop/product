package com.oldnew.product.adapter.in.web;

import java.util.List;

/** Keyset page. Pass {@code nextCursor} back as {@code cursor} for the next page; null means last page. */
public record PageResponse<T>(List<T> items, String nextCursor) {
}
