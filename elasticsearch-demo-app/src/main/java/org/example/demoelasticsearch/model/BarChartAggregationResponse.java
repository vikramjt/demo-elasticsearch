package org.example.demoelasticsearch.model;

import java.util.List;

public record BarChartAggregationResponse(
        String aggregationType,
        String metric,
        long totalDocuments,
        List<BarChartData> data
) {
}
