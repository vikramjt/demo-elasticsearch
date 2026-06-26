package org.example.demoelasticsearch.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record DocumentUpsertRequest(
        @NotBlank String name,
        @NotBlank String category,
        @NotNull BigDecimal price,
        @NotNull Integer quantity,
        List<String> tags,
        Instant createdAt
) {
}
