package org.example.demoelasticsearch.model;

import java.math.BigDecimal;
import java.util.List;

public record DocumentSearchCriteria(
        String queryText,
        String category,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        List<String> tags
) {
}
