package conectaseguros.co.discovery_server.performance.load;

import conectaseguros.co.discovery_server.performance.base.AbstractPerformanceTest;
import conectaseguros.co.discovery_server.performance.runner.PerformanceReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Load tests for concurrent request handling.
 */
@DisplayName("Concurrent Load Tests")
class ConcurrentLoadTests extends AbstractPerformanceTest {

    @Nested
    @DisplayName("Low Concurrency Tests")
    class LowConcurrencyTests {

        @Test
        @DisplayName("Handle 10 concurrent requests")
        void handle10ConcurrentRequests() throws InterruptedException, ExecutionException {
            int concurrentRequests = 10;
            
            try (ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests)) {
                List<Future<Long>> futures = new ArrayList<>();

                for (int i = 0; i < concurrentRequests; i++) {
                    futures.add(executor.submit(ConcurrentLoadTests.this::executeTimedHealthCheck));
                }

                List<Long> latencies = collectResults(futures);
                
                // Assert success rate
                long successCount = latencies.stream().filter(l -> l >= 0).count();
                assertThat(successCount)
                    .as("At least 90% of requests should succeed")
                    .isGreaterThanOrEqualTo((long) (concurrentRequests * 0.9));

                // Calculate statistics
                List<Long> validLatencies = latencies.stream().filter(l -> l >= 0).toList();
                var stats = PerformanceReport.calculateStats(validLatencies);
                stats.print("10 Concurrent Requests");

                // Assertions
                assertThat(stats.max())
                    .as("Max latency should be under 3 seconds")
                    .isLessThan(3000);
            }
        }

        @Test
        @DisplayName("Handle 25 concurrent requests")
        void handle25ConcurrentRequests() throws InterruptedException, ExecutionException {
            int concurrentRequests = 25;
            
            try (ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests)) {
                List<Future<Long>> futures = new ArrayList<>();

                for (int i = 0; i < concurrentRequests; i++) {
                    futures.add(executor.submit(ConcurrentLoadTests.this::executeTimedHealthCheck));
                }

                List<Long> latencies = collectResults(futures);
                
                long successCount = latencies.stream().filter(l -> l >= 0).count();
                assertThat(successCount)
                    .as("At least 90% of requests should succeed")
                    .isGreaterThanOrEqualTo((long) (concurrentRequests * 0.9));

                List<Long> validLatencies = latencies.stream().filter(l -> l >= 0).toList();
                var stats = PerformanceReport.calculateStats(validLatencies);
                stats.print("25 Concurrent Requests");
            }
        }
    }

    @Nested
    @DisplayName("Medium Concurrency Tests")
    class MediumConcurrencyTests {

        @Test
        @DisplayName("Handle 50 concurrent requests")
        void handle50ConcurrentRequests() throws InterruptedException, ExecutionException {
            int concurrentRequests = 50;
            int poolSize = Math.min(50, Runtime.getRuntime().availableProcessors() * 2);
            
            try (ExecutorService executor = Executors.newFixedThreadPool(poolSize)) {
                List<Future<Long>> futures = new ArrayList<>();

                for (int i = 0; i < concurrentRequests; i++) {
                    futures.add(executor.submit(ConcurrentLoadTests.this::executeTimedHealthCheck));
                }

                List<Long> latencies = collectResults(futures);
                
                long successCount = latencies.stream().filter(l -> l >= 0).count();
                assertThat(successCount)
                    .as("At least 85% of requests should succeed")
                    .isGreaterThanOrEqualTo((long) (concurrentRequests * 0.85));

                List<Long> validLatencies = latencies.stream().filter(l -> l >= 0).toList();
                var stats = PerformanceReport.calculateStats(validLatencies);
                stats.print("50 Concurrent Requests");
            }
        }

        @Test
        @DisplayName("Handle 100 concurrent requests")
        void handle100ConcurrentRequests() throws InterruptedException, ExecutionException {
            int concurrentRequests = 100;
            int poolSize = Math.min(100, Runtime.getRuntime().availableProcessors() * 2);
            
            try (ExecutorService executor = Executors.newFixedThreadPool(poolSize)) {
                List<Future<Long>> futures = new ArrayList<>();

                for (int i = 0; i < concurrentRequests; i++) {
                    futures.add(executor.submit(ConcurrentLoadTests.this::executeTimedHealthCheck));
                }

                List<Long> latencies = collectResults(futures);
                
                long successCount = latencies.stream().filter(l -> l >= 0).count();
                assertThat(successCount)
                    .as("At least 80% of requests should succeed")
                    .isGreaterThanOrEqualTo((long) (concurrentRequests * 0.8));

                List<Long> validLatencies = latencies.stream().filter(l -> l >= 0).toList();
                var stats = PerformanceReport.calculateStats(validLatencies);
                stats.print("100 Concurrent Requests");

                // Verify throughput
                assertThat(stats.avg())
                    .as("Average latency should be reasonable under load")
                    .isLessThan(2000);
            }
        }
    }

    @Nested
    @DisplayName("Sustained Load Tests")
    class SustainedLoadTests {

        @Test
        @DisplayName("Sustained load for 5 seconds with 5 threads")
        void sustainedLoad5Seconds() throws InterruptedException {
            int threadCount = 5;
            int durationSeconds = 5;
            
            try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
                AtomicInteger successCount = new AtomicInteger(0);
                AtomicInteger errorCount = new AtomicInteger(0);
                AtomicInteger requestCount = new AtomicInteger(0);
                ConcurrentHashMap<Long, Long> latencyMap = new ConcurrentHashMap<>();
                
                CountDownLatch startLatch = new CountDownLatch(1);
                CountDownLatch endLatch = new CountDownLatch(threadCount);

                for (int i = 0; i < threadCount; i++) {
                    executor.submit(new LoadWorker(
                        startLatch, endLatch, successCount, errorCount, 
                        requestCount, latencyMap, durationSeconds, baseUrl, restTemplate));
                }

                startLatch.countDown();
                boolean completed = endLatch.await(durationSeconds + 10, TimeUnit.SECONDS);
                assertThat(completed).as("Load test should complete within timeout").isTrue();

                List<Long> sortedLatencies = new ArrayList<>(latencyMap.values());
                sortedLatencies.sort(Long::compareTo);
                
                var stats = PerformanceReport.calculateStats(sortedLatencies);
                stats.print("Sustained Load (5s, 5 threads)");
                
                double requestsPerSecond = requestCount.get() / (double) durationSeconds;
                System.out.printf("  Throughput: %.2f req/s%n", requestsPerSecond);

                // Assertions
                double successRate = successCount.get() / (double) requestCount.get();
                assertThat(successRate)
                    .as("Success rate should be at least 95%")
                    .isGreaterThan(0.95);
            }
        }

        @Test
        @DisplayName("Sustained load for 10 seconds with 10 threads")
        void sustainedLoad10Seconds() throws InterruptedException {
            int threadCount = 10;
            int durationSeconds = 10;
            
            try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
                AtomicInteger successCount = new AtomicInteger(0);
                AtomicInteger errorCount = new AtomicInteger(0);
                AtomicInteger requestCount = new AtomicInteger(0);
                ConcurrentHashMap<Long, Long> latencyMap = new ConcurrentHashMap<>();
                
                CountDownLatch startLatch = new CountDownLatch(1);
                CountDownLatch endLatch = new CountDownLatch(threadCount);

                for (int i = 0; i < threadCount; i++) {
                    executor.submit(new LoadWorker(
                        startLatch, endLatch, successCount, errorCount, 
                        requestCount, latencyMap, durationSeconds, baseUrl, restTemplate));
                }

                startLatch.countDown();
                boolean completed = endLatch.await(durationSeconds + 15, TimeUnit.SECONDS);
                assertThat(completed).as("Load test should complete within timeout").isTrue();

                List<Long> sortedLatencies = new ArrayList<>(latencyMap.values());
                sortedLatencies.sort(Long::compareTo);
                
                var stats = PerformanceReport.calculateStats(sortedLatencies);
                stats.print("Sustained Load (10s, 10 threads)");
                
                double requestsPerSecond = requestCount.get() / (double) durationSeconds;
                System.out.printf("  Throughput: %.2f req/s%n", requestsPerSecond);

                // Assertions
                double successRate = successCount.get() / (double) requestCount.get();
                assertThat(successRate)
                    .as("Success rate should be at least 90%")
                    .isGreaterThan(0.90);
            }
        }
    }

    /**
     * Execute health check and return latency, or -1 if failed.
     */
    private long executeTimedHealthCheck() {
        long start = System.currentTimeMillis();
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/actuator/health", String.class);
            return response.getStatusCode() == HttpStatus.OK 
                ? System.currentTimeMillis() - start 
                : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Collect results from futures, handling exceptions properly.
     */
    private List<Long> collectResults(List<Future<Long>> futures) 
            throws InterruptedException, ExecutionException {
        List<Long> results = new ArrayList<>();
        
        for (Future<Long> future : futures) {
            try {
                Long result = future.get(30, TimeUnit.SECONDS);
                results.add(result);
            } catch (ExecutionException e) {
                results.add(-1L);
            } catch (TimeoutException e) {
                results.add(-1L);
            }
        }
        
        return results;
    }

    /**
     * Worker thread for sustained load testing.
     */
    private static class LoadWorker implements Runnable {
        private final CountDownLatch startLatch;
        private final CountDownLatch endLatch;
        private final AtomicInteger successCount;
        private final AtomicInteger errorCount;
        private final AtomicInteger requestCount;
        private final ConcurrentHashMap<Long, Long> latencyMap;
        private final int durationSeconds;
        private final String baseUrl;
        private final RestTemplate restTemplate;

        LoadWorker(CountDownLatch startLatch, CountDownLatch endLatch,
                   AtomicInteger successCount, AtomicInteger errorCount,
                   AtomicInteger requestCount, ConcurrentHashMap<Long, Long> latencyMap,
                   int durationSeconds, String baseUrl, RestTemplate restTemplate) {
            this.startLatch = startLatch;
            this.endLatch = endLatch;
            this.successCount = successCount;
            this.errorCount = errorCount;
            this.requestCount = requestCount;
            this.latencyMap = latencyMap;
            this.durationSeconds = durationSeconds;
            this.baseUrl = baseUrl;
            this.restTemplate = restTemplate;
        }

        @Override
        public void run() {
            try {
                startLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                endLatch.countDown();
                return;
            }

            long endTime = System.currentTimeMillis() + (durationSeconds * 1000L);
            int localSuccess = 0;
            int localError = 0;
            int localRequest = 0;

            while (System.currentTimeMillis() < endTime) {
                long start = System.currentTimeMillis();
                try {
                    ResponseEntity<String> response = restTemplate.getForEntity(
                        baseUrl + "/actuator/health", String.class);
                    
                    long latency = System.currentTimeMillis() - start;
                    localRequest++;
                    
                    if (response.getStatusCode() == HttpStatus.OK) {
                        localSuccess++;
                        latencyMap.put(start, latency);
                    } else {
                        localError++;
                    }
                } catch (Exception e) {
                    localError++;
                    localRequest++;
                }
            }

            successCount.addAndGet(localSuccess);
            errorCount.addAndGet(localError);
            requestCount.addAndGet(localRequest);
            endLatch.countDown();
        }
    }
}