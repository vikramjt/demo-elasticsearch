package org.example.demoelasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@ConditionalOnProperty(prefix = "elasticsearch", name = "index-init-enabled", havingValue = "true", matchIfMissing = true)
public class SampleIndexInitializer {

    private static final Logger log = LoggerFactory.getLogger(SampleIndexInitializer.class);

    private final ElasticsearchClient elasticsearchClient;
    private final String sampleIndex;

    public SampleIndexInitializer(
            ElasticsearchClient elasticsearchClient,
            @Value("${elasticsearch.sample-index}") String sampleIndex
    ) {
        this.elasticsearchClient = elasticsearchClient;
        this.sampleIndex = sampleIndex;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeIndex() throws IOException {
        boolean exists = elasticsearchClient.indices().exists(ExistsRequest.of(builder -> builder.index(sampleIndex))).value();
        if (exists) {
            return;
        }

        elasticsearchClient.indices().create(builder -> builder
                .index(sampleIndex)
                .settings(settings -> settings.numberOfShards("1").numberOfReplicas("0"))
                .mappings(mapping -> mapping
                        .properties("name", field -> field.text(text -> text.fields("keyword", keyword -> keyword.keyword(k -> k))))
                        .properties("category", field -> field.keyword(keyword -> keyword))
                        .properties("price", field -> field.double_(number -> number))
                        .properties("quantity", field -> field.integer(number -> number))
                        .properties("tags", field -> field.keyword(keyword -> keyword))
                        .properties("createdAt", field -> field.date(date -> date))
                )
        );

        log.info("Created Elasticsearch index '{}'", sampleIndex);
    }
}
