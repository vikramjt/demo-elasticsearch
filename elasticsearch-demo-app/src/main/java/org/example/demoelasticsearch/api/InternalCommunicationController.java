package org.example.demoelasticsearch.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/internal")
public class InternalCommunicationController {

    private static final Logger log = LoggerFactory.getLogger(InternalCommunicationController.class);

    @GetMapping("/ping")
    public Map<String, String> ping(@RequestParam(defaultValue = "unknown") String sourceService) {
        log.info("Received inter-service ping from {}", sourceService);
        return Map.of("status", "ok", "sourceService", sourceService);
    }
}
