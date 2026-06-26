package org.example.demoelasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.example.demoelasticsearch.model.PagedResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LogSearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final String logsIndexPattern;

    public LogSearchService(
            ElasticsearchClient elasticsearchClient,
            @Value("${elasticsearch.logs-index-pattern}") String logsIndexPattern
    ) {
        this.elasticsearchClient = elasticsearchClient;
        this.logsIndexPattern = logsIndexPattern;
    }

    public PagedResponse<Map<String, Object>> searchLogs(String queryText, int page, int size) throws IOException {
        Query query = queryText == null || queryText.isBlank()
                ? QueryBuilders.matchAll().build()._toQuery()
                : QueryBuilders.queryString().query(queryText).defaultField("message").build()._toQuery();
        return executeQuery(query, page, size);
    }

    public PagedResponse<Map<String, Object>> searchByTraceId(String traceId, int page, int size) throws IOException {
        Query query = QueryBuilders.term().field("traceId").value(traceId).build()._toQuery();
        return executeQuery(query, page, size);
    }

    private PagedResponse<Map<String, Object>> executeQuery(Query query, int page, int size) throws IOException {
        SearchResponse<Map> response = elasticsearchClient.search(builder -> builder
                        .index(logsIndexPattern)
                        .query(query)
                        .from(page * size)
                        .size(size)
                        .sort(sort -> sort.field(field -> field.field("@timestamp").order(SortOrder.Desc))),
                Map.class
        );

        List<Map<String, Object>> items = response.hits().hits().stream()
                .map(this::toMap)
                .toList();
        long total = response.hits().total() == null ? items.size() : response.hits().total().value();
        return new PagedResponse<>(items, total, page, size);
    }

    private Map<String, Object> toMap(Hit<Map> hit) {
        Map<String, Object> value = new HashMap<>();
        if (hit.source() != null) {
            value.putAll(hit.source());
        }
        value.put("_id", hit.id());
        return value;
    }
}
