package com.anonymous.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

// Paging for list endpoints: newest first, page size capped so a client can't pull a whole table
public final class PageUtil {

    public static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private PageUtil() {
    }

    public static Pageable newestFirst(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdDate"));
    }

}
