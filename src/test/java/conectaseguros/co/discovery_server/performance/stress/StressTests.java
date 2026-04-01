package conectaseguros.co.discovery_server.performance.stress;

import conectaseguros.co.discovery_server.performance.base.AbstractPerformanceTest;
import conectaseguros.co.discovery_server.performance.runner.PerformanceReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stress tests - burst loads and extreme conditions.
 */
@DisplayName("Stress Tests")
class StressTests extends AbstractPerformanceTest {

    @Nested
    @DisplayName("Burst Tests")
    class BurstTests {

        @Test
        @DisplayName("Rapid burst of 100 requests")
        void burst100Requests() throws InterruptedException, ExecutionException {
            int burstSize = 100;
            int poolSize = 20;
            
            try (ExecutorService executor = Executors.newFixedThreadPool(poolSize)) {
                List<Future<Long>> futures = new ArrayList<>();

                long startTime = System.currentTimeMillis();
                
                for (int i = 0; i < burstSize; i++) {
                    futures.add(executor.submit(StressTests.this::measureHealthCheck));
                }

                List<Long> latencies = collectResults(futures);
                
                long totalDuration = System.currentTimeMillis() - startTime;

                // Calculate statistics
                List<Long> validLatencies = latencies.stream().filter(l -> l >= 0).toList();
                long successCount = validLatencies.size();
                
                assertThat(successCount)
                    .as("At least 90% of burst requests should succeed")
                    .isGreaterThanOrEqualTo(90);

                var stats = PerformanceReport.calculateStats(validLatencies);
                stats.print("Burst 100 Requests");
                
                double throughput = (burstSize * 1000.0) / totalDuration;
                System.out.printf("  Throughput: %.2f req/s%n", throughput);
            }
        }

        @Test
        @DisplayName("Rapid burst of 200 requests")
        void burst200Requests() throws InterruptedException, ExecutionException {
            int burstSize = 200;
            int poolSize = 30;
            
            try (ExecutorService executor = Executors.newFixedThreadPool(poolSize)) {
                List<Future<Long>> futures = new ArrayList<>();

                long startTime = System.currentTimeMillis();
                
                for (int i = 0; i < burstSize; i++) {
                    futures.add(executor.submit(StressTests.this::measureHealthCheck));
                }

                List<Long> latencies = collectResults(futures);
                
                long totalDuration = System.currentTimeMillis() - startTime;

                List<Long> validLatencies = latencies.stream().filter(l -> l >= 0).toList();
                long successCount = validLatencies.size();
                
                assertThat(successCount)
                    .as("At least 85% of burst requests should succeed")
                    .isGreaterThanOrEqualTo(170);

                var stats = PerformanceReport.calculateStats(validLatencies);
                stats.print("Burst 200 Requests");
                
                double throughput = (burstSize * 1000.0) / totalDuration;
                System.out.printf("  Throughput: %.2f req/s%n", throughput);
            }
        }

        @Test
        @DisplayName("Rapid burst of 500 requests")
        void burst500Requests() throws InterruptedException, ExecutionException {
            int burstSize = 500;
            int poolSize = 50;
            
            try (ExecutorService executor = Executors.newFixedThreadPool(poolSize)) {
                List<Future<Long>> futures = new ArrayList<>();

                long startTime = System.currentTimeMillis();
                
                for (int i = 0; i < burstSize; i++) {
                    futures.add(executor.submit(StressTests.this::measureHealthCheck));
                }

                List<Long> latencies = collectResults(futures);
                
                long totalDuration = System.currentTimeMillis() - startTime;

                List<Long> validLatencies = latencies.stream().filter(l -> l >= 0).toList();
                long successCount = validLatencies.size();
                
                assertThat(successCount)
                    .as("At least 80% of burst requests should succeed")
                    .isGreaterThanOrEqualTo(400);

                var stats = PerformanceReport.calculateStats(validLatencies);
                stats.print("Burst 500 Requests");
                
                double throughput = (burstSize * 1000.0) / totalDuration;
                System.out.printf("  Throughput: %.2f req/s%n", throughput);
            }
        }
    }

    @Nested
    @DisplayName("Ramp-up Tests")
    class RampUpTests {

        @Test
        @DisplayName("Ramp up from 1 to 50 concurrent users over 10 seconds")
        void rampUp50Users() throws InterruptedException {
            int maxThreads = 50;
            int rampUpSeconds = 10;
            
            try (ExecutorService executor = Executors.newFixedThreadPool(maxThreads)) {
                AtomicInteger totalRequests = new AtomicInteger(0);
                AtomicInteger successRequests = new AtomicInteger(0);
                List<Long> latencies = new ArrayList<>();

                CountDownLatch startLatch = new CountDownLatch(1);
                CountDownLatch endLatch = new CountDownLatch(maxThreads);
                
                // Start threads gradually
                for (int i = 0; i < maxThreads; i++) {
                    final int threadNum = i;
                    executor.submit(() -> {
                        try {
                            startLatch.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        
                        // Each thread runs for rampUpSeconds
                        long endTime = System.currentTimeMillis() + (rampUpSeconds * 1000L);
                        
                        // Stagger thread start (ramp up effect)
                        try {
                            Thread.sleep(threadNum * 50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        
                        while (System.currentTimeMillis() < endTime) {
                            long start = System.currentTimeMillis();
                            try {
                                restTemplate.getForEntity(baseUrl + "/actuator/health", String.class);
                                latencies.add(System.currentTimeMillis() - start);
                                successRequests.incrementAndGet();
                                totalRequests.incrementAndGet();
                            } catch (Exception e) {
                                totalRequests.incrementAndGet();
                            }
                        }
                        
                        endLatch.countDown();
                    });
                }

                startLatch.countDown();
                boolean completed = endLatch.await(rampUpSeconds + 20, TimeUnit.SECONDS);
                assertThat(completed).isTrue();

                latencies.sort(Long::compareTo);
                var stats = PerformanceReport.calculateStats(latencies);
                stats.print("Ramp-up 50 Users");
                
                double throughput = totalRequests.get() / (double) rampUpSeconds;
                System.out.printf("  Throughput: %.2f req/s%n", throughput);
                
                double successRate = successRequests.get() / (double) totalRequests.get();
                assertThat(successRate)
                    .as("Success rate should be at least 90%")
                    .isGreaterThan(0.90);
            }
        }
    }

    /**
     * Measure health check execution time.
     */
    private long measureHealthCheck() {
        long start = System.currentTimeMillis();
        try {
            restTemplate.getForEntity(baseUrl + "/actuator/health", String.class);
            return System.currentTimeMillis() - start;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Collect results from futures with proper exception handling.
     */
    private List<Long> collectResults(List<Future<Long>> futures) 
            throws InterruptedException, ExecutionException {
        List<Long> results = new ArrayList<>();
        
        for (Future<Long> future : futures) {
            try {
                Long result = future.get(60, TimeUnit.SECONDS);
                results.add(result);
            } catch (TimeoutException e) {
                results.add(-1L);
            }
        }
        
        return results;
    }
}