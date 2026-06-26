package org.example.demoelasticsearch.api;

import org.example.demoelasticsearch.model.PagedResponse;
import org.example.demoelasticsearch.service.LogSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
public class LogSearchController {

    private final LogSearchService logSearchService;

    public LogSearchController(LogSearchService logSearchService) {
        this.logSearchService = logSearchService;
    }

    @GetMapping("/search")
    public PagedResponse<Map<String, Object>> searchLogs(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) throws IOException {
        return logSearchService.searchLogs(query, page, size);
    }

    @GetMapping("/traces/{traceId}")
    public PagedResponse<Map<String, Object>> searchTrace(
            @PathVariable String traceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) throws IOException {
        return logSearchService.searchByTraceId(traceId, page, size);
    }
}
