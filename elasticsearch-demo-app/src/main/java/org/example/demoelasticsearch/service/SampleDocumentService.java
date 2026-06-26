package org.example.demoelasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.NumberRangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import org.example.demoelasticsearch.model.AggregationOverviewResponse;
import org.example.demoelasticsearch.model.BarChartAggregationResponse;
import org.example.demoelasticsearch.model.BarChartData;
import org.example.demoelasticsearch.model.DocumentSearchCriteria;
import org.example.demoelasticsearch.model.DocumentUpsertRequest;
import org.example.demoelasticsearch.model.PagedResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SampleDocumentService {

    private final ElasticsearchClient elasticsearchClient;
    private final String sampleIndex;

    public SampleDocumentService(
            ElasticsearchClient elasticsearchClient,
            @Value("${elasticsearch.sample-index}") String sampleIndex
    ) {
        this.elasticsearchClient = elasticsearchClient;
        this.sampleIndex = sampleIndex;
    }

    public Map<String, Object> createDocument(DocumentUpsertRequest request) throws IOException {
        String documentId = UUID.randomUUID().toString();
        Map<String, Object> document = toDocument(request);
        IndexResponse response = elasticsearchClient.index(builder -> builder
                .index(sampleIndex)
                .id(documentId)
                .document(document)
                .refresh(Refresh.True)
        );
        document.put("id", response.id());
        return document;
    }

    public Map<String, Object> getDocument(String documentId) throws IOException {
        GetResponse<Map> response = elasticsearchClient.get(
                builder -> builder.index(sampleIndex).id(documentId),
                Map.class
        );
        if (!response.found()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found: " + documentId);
        }

        Map<String, Object> result = new HashMap<>(Objects.requireNonNull(response.source()));
        result.put("id", response.id());
        return result;
    }

    public Map<String, Object> updateDocument(String documentId, DocumentUpsertRequest request) throws IOException {
        Map<String, Object> document = toDocument(request);
        UpdateResponse<Map> response = elasticsearchClient.update(builder -> builder
                        .index(sampleIndex)
                        .id(documentId)
                        .doc(document)
                        .refresh(Refresh.True),
                Map.class
        );

        if (response.result() == Result.NotFound) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found: " + documentId);
        }

        Map<String, Object> result = getDocument(documentId);
        result.put("result", response.result().jsonValue());
        return result;
    }

    public void deleteDocument(String documentId) throws IOException {
        DeleteResponse response = elasticsearchClient.delete(builder -> builder
                .index(sampleIndex)
                .id(documentId)
                .refresh(Refresh.True)
        );
        if (response.result() == Result.NotFound) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found: " + documentId);
        }
    }

    public PagedResponse<Map<String, Object>> searchDocuments(DocumentSearchCriteria criteria, int page, int size) throws IOException {
        Query query = buildCriteriaQuery(criteria);
        SearchResponse<Map> response = elasticsearchClient.search(builder -> builder
                        .index(sampleIndex)
                        .query(query)
                        .from(page * size)
                        .size(size)
                        .sort(sort -> sort.field(field -> field.field("createdAt").order(SortOrder.Desc)))
                        .sort(sort -> sort.field(field -> field.field("_id").order(SortOrder.Asc))),
                Map.class
        );

        List<Map<String, Object>> items = response.hits().hits().stream()
                .map(this::mapHit)
                .toList();

        long total = response.hits().total() == null ? items.size() : response.hits().total().value();
        return new PagedResponse<>(items, total, page, size);
    }

    public AggregationOverviewResponse getAggregationOverview() throws IOException {
        SearchResponse<Void> response = elasticsearchClient.search(builder -> builder
                        .index(sampleIndex)
                        .size(0)
                        .aggregations("by_category", agg -> agg.terms(terms -> terms.field("category").size(10)))
                        .aggregations("avg_price", agg -> agg.avg(avg -> avg.field("price")))
                        .aggregations("price_stats", agg -> agg.stats(stats -> stats.field("price"))),
                Void.class
        );

        Map<String, Long> categoryBuckets = response.aggregations().get("by_category")
                .sterms()
                .buckets()
                .array()
                .stream()
                .collect(Collectors.toMap(
                        bucket -> bucket.key().stringValue(),
                        StringTermsBucket::docCount,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        double averagePrice = response.aggregations().get("avg_price").avg().value();
        var priceStats = response.aggregations().get("price_stats").stats();
        double minPrice = priceStats.min();
        double maxPrice = priceStats.max();

        long total = response.hits().total() == null ? 0L : response.hits().total().value();
        return new AggregationOverviewResponse(total, categoryBuckets, averagePrice, minPrice, maxPrice);
    }

    public String validateSampleIndex() throws IOException {
        GetIndexResponse response = elasticsearchClient.indices().get(builder -> builder.index(sampleIndex));
        if (response.result().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sample index not found");
        }
        return sampleIndex;
    }

    private Query buildCriteriaQuery(DocumentSearchCriteria criteria) {
        if (criteria == null) {
            return QueryBuilders.matchAll().build()._toQuery();
        }

        List<Query> filters = new ArrayList<>();
        if (criteria.queryText() != null && !criteria.queryText().isBlank()) {
            filters.add(QueryBuilders.multiMatch()
                    .query(criteria.queryText())
                    .fields("name", "tags", "category")
                    .build()
                    ._toQuery());
        }
        if (criteria.category() != null && !criteria.category().isBlank()) {
            filters.add(QueryBuilders.term()
                    .field("category")
                    .value(FieldValue.of(criteria.category()))
                    .build()
                    ._toQuery());
        }
        if (criteria.tags() != null && !criteria.tags().isEmpty()) {
            List<FieldValue> tags = criteria.tags().stream().map(FieldValue::of).toList();
            filters.add(QueryBuilders.terms()
                    .field("tags")
                    .terms(values -> values.value(tags))
                    .build()
                    ._toQuery());
        }
        if (criteria.minPrice() != null || criteria.maxPrice() != null) {
            NumberRangeQuery.Builder rangeBuilder = new NumberRangeQuery.Builder().field("price");
            if (criteria.minPrice() != null) {
                rangeBuilder.gte(criteria.minPrice().doubleValue());
            }
            if (criteria.maxPrice() != null) {
                rangeBuilder.lte(criteria.maxPrice().doubleValue());
            }
            NumberRangeQuery numberRangeQuery = rangeBuilder.build();
            filters.add(QueryBuilders.range(range -> range.number(numberRangeQuery)));
        }

        if (filters.isEmpty()) {
            return QueryBuilders.matchAll().build()._toQuery();
        }

        return QueryBuilders.bool().filter(filters).build()._toQuery();
    }

    private Map<String, Object> mapHit(Hit<Map> hit) {
        Map<String, Object> map = new HashMap<>();
        if (hit.source() != null) {
            map.putAll(hit.source());
        }
        map.put("id", hit.id());
        return map;
    }

    private Map<String, Object> toDocument(DocumentUpsertRequest request) {
        Map<String, Object> document = new HashMap<>();
        document.put("name", request.name());
        document.put("category", request.category());
        document.put("price", request.price().doubleValue());
        document.put("quantity", request.quantity());
        document.put("tags", request.tags() == null ? List.of() : request.tags());
        document.put("createdAt", request.createdAt() == null ? Instant.now() : request.createdAt());
        return document;
    }

    public BarChartAggregationResponse getBarChartByCategory(String metric) throws IOException {
        SearchResponse<Void> response = elasticsearchClient.search(builder -> builder
                        .index(sampleIndex)
                        .size(0)
                        .aggregations("by_category", agg -> agg.terms(terms -> terms.field("category").size(50))),
                Void.class
        );

        var categoryAgg = response.aggregations().get("by_category").sterms();
        List<BarChartData> data = new ArrayList<>();

        for (StringTermsBucket bucket : categoryAgg.buckets().array()) {
            String category = bucket.key().stringValue();
            double value = 0.0;

            if ("quantity".equalsIgnoreCase(metric)) {
                SearchResponse<Void> categorySearch = elasticsearchClient.search(builder -> builder
                                .index(sampleIndex)
                                .query(QueryBuilders.term().field("category").value(FieldValue.of(category)).build()._toQuery())
                                .size(0)
                                .aggregations("total_quantity", agg -> agg.sum(sum -> sum.field("quantity"))),
                        Void.class
                );
                value = categorySearch.aggregations().get("total_quantity").sum().value();
            } else if ("price".equalsIgnoreCase(metric)) {
                SearchResponse<Void> categorySearch = elasticsearchClient.search(builder -> builder
                                .index(sampleIndex)
                                .query(QueryBuilders.term().field("category").value(FieldValue.of(category)).build()._toQuery())
                                .size(0)
                                .aggregations("avg_price", agg -> agg.avg(avg -> avg.field("price"))),
                        Void.class
                );
                value = categorySearch.aggregations().get("avg_price").avg().value();
            }

            data.add(new BarChartData(category, value, metric));
        }

        long total = response.hits().total() == null ? 0L : response.hits().total().value();
        return new BarChartAggregationResponse("category_" + metric, metric, total, data);
    }
}
