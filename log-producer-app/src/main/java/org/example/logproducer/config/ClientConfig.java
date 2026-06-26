package org.example.logproducer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientConfig {

    @Bean
    public RestClient restClient(
            @Value("${downstream.base-url:http://localhost:8080}") String downstreamBaseUrl
    ) {
        return RestClient.builder().baseUrl(downstreamBaseUrl).build();
    }
}
