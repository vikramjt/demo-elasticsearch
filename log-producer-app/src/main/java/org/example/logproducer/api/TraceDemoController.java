package org.example.logproducer.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.Map;

@RestController
@RequestMapping("/api/demo")
public class TraceDemoController {

    private static final Logger log = LoggerFactory.getLogger(TraceDemoController.class);
    private final RestClient restClient;

    public TraceDemoController(RestClient restClient) {
        this.restClient = restClient;
    }

    @GetMapping("/call-main")
    public Map<String, Object> callMainService(@RequestParam(defaultValue = "hello-from-producer") String message) {
        log.info("Calling main service with message={}", message);

        Map<String, Object> downstreamResponse = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/internal/ping")
                        .queryParam("sourceService", "log-producer-app")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Map.class);

        log.info("Downstream response: {}", downstreamResponse);
        return Map.of(
                "status", "sent",
                "message", message,
                "downstreamResponse", downstreamResponse
        );
    }
}
