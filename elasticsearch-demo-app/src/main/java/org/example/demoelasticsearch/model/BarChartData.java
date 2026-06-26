package org.example.demoelasticsearch.model;

public record BarChartData(
        String category,
        double value,
        String valueType
) {
}
