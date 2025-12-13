package com.backend.cookshare.authentication.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = RestTemplateConfig.class)
class RestTemplateConfigTest {

    @Autowired
    private RestTemplate restTemplate;

    @Test
    void restTemplateBean_ShouldNotBeNull() {
        assertNotNull(restTemplate, "RestTemplate bean should be created");
    }

    @Test
    void restTemplateBean_ShouldBeInstanceOfRestTemplate() {
        assertTrue(restTemplate instanceof RestTemplate, "Bean should be instance of RestTemplate");
    }
}
