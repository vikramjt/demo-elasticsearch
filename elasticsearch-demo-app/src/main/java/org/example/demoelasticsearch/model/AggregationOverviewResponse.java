package org.example.demoelasticsearch.model;

import java.util.Map;

public record AggregationOverviewResponse(
        long totalDocuments,
        Map<String, Long> categoryBuckets,
        double averagePrice,
        double minPrice,
        double maxPrice
) {
}
