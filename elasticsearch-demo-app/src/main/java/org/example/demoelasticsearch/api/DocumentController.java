package org.example.demoelasticsearch.api;

import jakarta.validation.Valid;
import org.example.demoelasticsearch.model.AggregationOverviewResponse;
import org.example.demoelasticsearch.model.BarChartAggregationResponse;
import org.example.demoelasticsearch.model.DocumentSearchCriteria;
import org.example.demoelasticsearch.model.DocumentUpsertRequest;
import org.example.demoelasticsearch.model.PagedResponse;
import org.example.demoelasticsearch.service.SampleDocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final SampleDocumentService sampleDocumentService;

    public DocumentController(SampleDocumentService sampleDocumentService) {
        this.sampleDocumentService = sampleDocumentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createDocument(@Valid @RequestBody DocumentUpsertRequest request) throws IOException {
        return sampleDocumentService.createDocument(request);
    }

    @GetMapping("/{documentId}")
    public Map<String, Object> getDocument(@PathVariable String documentId) throws IOException {
        return sampleDocumentService.getDocument(documentId);
    }

    @PutMapping("/{documentId}")
    public Map<String, Object> updateDocument(@PathVariable String documentId, @Valid @RequestBody DocumentUpsertRequest request)
            throws IOException {
        return sampleDocumentService.updateDocument(documentId, request);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(@PathVariable String documentId) throws IOException {
        sampleDocumentService.deleteDocument(documentId);
    }

    @PostMapping("/search")
    public PagedResponse<Map<String, Object>> searchDocuments(
            @RequestBody(required = false) DocumentSearchCriteria criteria,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) throws IOException {
        return sampleDocumentService.searchDocuments(criteria, page, size);
    }

    @GetMapping("/aggregations/overview")
    public AggregationOverviewResponse getAggregationOverview() throws IOException {
        return sampleDocumentService.getAggregationOverview();
    }

    @GetMapping("/aggregations/bar-chart")
    public BarChartAggregationResponse getBarChartAggregation(
            @RequestParam(defaultValue = "quantity") String metric
    ) throws IOException {
        return sampleDocumentService.getBarChartByCategory(metric);
    }

    @GetMapping("/index")
    public Map<String, String> getSampleIndex() throws IOException {
        return Map.of("sampleIndex", sampleDocumentService.validateSampleIndex());
    }
}
