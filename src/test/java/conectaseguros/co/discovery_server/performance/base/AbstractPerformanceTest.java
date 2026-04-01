package conectaseguros.co.discovery_server.performance.base;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for all performance tests.
 * Provides common setup and utility methods.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.security.user.name=eureka-test",
    "spring.security.user.password=test-password",
    "eureka.client.register-with-eureka=false",
    "eureka.client.fetch-registry=false"
})
public abstract class AbstractPerformanceTest {

    @Value("${local.server.port}")
    protected int port;

    protected String baseUrl;
    protected HttpHeaders authHeaders;
    protected RestTemplate restTemplate;

    @BeforeEach
    void setUpBase() {
        baseUrl = "http://localhost:" + port;
        restTemplate = new RestTemplate();
        
        String auth = "eureka-test:test-password";
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        authHeaders = new HttpHeaders();
        authHeaders.set("Authorization", "Basic " + encodedAuth);
    }

    /**
     * Execute a GET request and measure execution time.
     * 
     * @param path the endpoint path
     * @return the execution time in milliseconds
     */
    protected long measureGet(String path) {
        long startTime = System.currentTimeMillis();
        restTemplate.getForEntity(baseUrl + path, String.class);
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Assert that the response time is within acceptable limits.
     * 
     * @param executionTimeMs the measured execution time
     * @param maxAllowedMs the maximum allowed time in milliseconds
     * @param message the assertion message
     */
    protected void assertResponseTime(long executionTimeMs, long maxAllowedMs, String message) {
        assertThat(executionTimeMs)
            .as(message)
            .isLessThan(maxAllowedMs);
    }
}