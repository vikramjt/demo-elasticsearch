package org.example.demoelasticsearch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "elasticsearch.index-init-enabled=false")
class DemoElasticSearchApplicationTests {

    @Test
    void contextLoads() {
    }

}
