package org.example.demoelasticsearch.model;

import java.util.List;

public record PagedResponse<T>(
        List<T> items,
        long totalElements,
        int page,
        int size
) {
}
