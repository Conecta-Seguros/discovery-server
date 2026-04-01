package conectaseguros.co.discovery_server.performance.api;

import conectaseguros.co.discovery_server.performance.base.AbstractPerformanceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance tests for Health endpoint.
 */
@DisplayName("Health Endpoint Performance Tests")
class HealthApiTests extends AbstractPerformanceTest {

    @Nested
    @DisplayName("Basic Health Check Tests")
    class BasicTests {

        @Test
        @DisplayName("Health endpoint responds within 500ms")
        void healthRespondsQuickly() {
            long executionTime = measureGet("/actuator/health");
            assertResponseTime(executionTime, 500, "Health check should respond quickly");
        }

        @Test
        @DisplayName("Health endpoint returns 200 OK")
        void healthReturnsOk() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/actuator/health", String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("Liveness probe responds within 500ms")
        void livenessRespondsQuickly() {
            long executionTime = measureGet("/actuator/health/liveness");
            assertResponseTime(executionTime, 500, "Liveness should respond quickly");
        }

        @Test
        @DisplayName("Readiness probe responds within 500ms")
        void readinessRespondsQuickly() {
            long executionTime = measureGet("/actuator/health/readiness");
            assertResponseTime(executionTime, 500, "Readiness should respond quickly");
        }
    }

    @Nested
    @DisplayName("Consistency Tests")
    class ConsistencyTests {

        @RepeatedTest(50)
        @DisplayName("Health endpoint is consistent - responds under 1 second")
        void healthIsConsistent() {
            long executionTime = measureGet("/actuator/health");
            assertResponseTime(executionTime, 1000, "Health check should be consistent");
        }
    }

    @Nested
    @DisplayName("Throughput Tests")
    class ThroughputTests {

        @Test
        @DisplayName("Can handle 10 sequential requests quickly")
        void handle10SequentialRequests() {
            List<Long> executionTimes = new ArrayList<>();
            
            for (int i = 0; i < 10; i++) {
                long startTime = System.currentTimeMillis();
                try {
                    ResponseEntity<String> response = restTemplate.getForEntity(
                        baseUrl + "/actuator/health", String.class);
                    executionTimes.add(System.currentTimeMillis() - startTime);
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                } catch (RestClientException e) {
                    throw new AssertionError("Request " + i + " failed: " + e.getMessage());
                }
            }

            // Calculate statistics
            long total = executionTimes.stream().mapToLong(Long::longValue).sum();
            long avg = total / executionTimes.size();
            long max = executionTimes.stream().mapToLong(Long::longValue).max().orElse(0);

            System.out.printf("10 sequential requests - Avg: %dms, Max: %dms%n", avg, max);
            
            assertThat(avg).as("Average response time should be under 500ms").isLessThan(500);
            assertThat(max).as("Max response time should be under 2 seconds").isLessThan(2000);
        }
    }
}