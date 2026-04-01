package conectaseguros.co.discovery_server.performance.memory;

import conectaseguros.co.discovery_server.performance.base.AbstractPerformanceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Memory stability tests - verify no memory leaks under load.
 */
@DisplayName("Memory Stability Tests")
class MemoryStabilityTests extends AbstractPerformanceTest {

    @Nested
    @DisplayName("Memory Usage Tests")
    class MemoryUsageTests {

        @Test
        @DisplayName("Memory stable after 500 requests")
        void memoryStableAfter500Requests() throws InterruptedException {
            int totalRequests = 500;
            int poolSize = 10;
            
            // Force GC before test
            System.gc();
            Runtime runtime = Runtime.getRuntime();
            
            long usedMemoryBefore = runtime.totalMemory() - runtime.freeMemory();
            System.out.println("Memory before: " + (usedMemoryBefore / 1024 / 1024) + " MB");

            try (ExecutorService executor = Executors.newFixedThreadPool(poolSize)) {
                CountDownLatch latch = new CountDownLatch(totalRequests);
                AtomicInteger successCount = new AtomicInteger(0);
                AtomicInteger errorCount = new AtomicInteger(0);

                for (int i = 0; i < totalRequests; i++) {
                    executor.submit(() -> {
                        try {
                            restTemplate.getForEntity(baseUrl + "/actuator/health", String.class);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                        }
                        latch.countDown();
                    });
                }

                boolean completed = latch.await(60, TimeUnit.SECONDS);
                assertThat(completed).as("All requests should complete").isTrue();
                
                // Force GC after test
                System.gc();
                
                // Wait a bit for GC to complete
                Thread.sleep(1000);
                System.gc();
                
                long usedMemoryAfter = runtime.totalMemory() - runtime.freeMemory();
                long memoryIncrease = usedMemoryAfter - usedMemoryBefore;
                
                System.out.println("Memory after: " + (usedMemoryAfter / 1024 / 1024) + " MB");
                System.out.println("Memory increase: " + (memoryIncrease / 1024 / 1024) + " MB");
                System.out.println("Success: " + successCount.get() + ", Errors: " + errorCount.get());

                // Memory should not increase by more than 100MB
                assertThat(memoryIncrease)
                    .as("Memory increase should be less than 100MB")
                    .isLessThan(100 * 1024 * 1024);
            }
        }

        @Test
        @DisplayName("Memory stable after 1000 requests")
        void memoryStableAfter1000Requests() throws InterruptedException {
            int totalRequests = 1000;
            int poolSize = 20;
            
            System.gc();
            Runtime runtime = Runtime.getRuntime();
            
            long usedMemoryBefore = runtime.totalMemory() - runtime.freeMemory();
            System.out.println("Memory before: " + (usedMemoryBefore / 1024 / 1024) + " MB");

            try (ExecutorService executor = Executors.newFixedThreadPool(poolSize)) {
                CountDownLatch latch = new CountDownLatch(totalRequests);
                AtomicInteger successCount = new AtomicInteger(0);
                AtomicInteger errorCount = new AtomicInteger(0);

                for (int i = 0; i < totalRequests; i++) {
                    executor.submit(() -> {
                        try {
                            restTemplate.getForEntity(baseUrl + "/actuator/health", String.class);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                        }
                        latch.countDown();
                    });
                }

                boolean completed = latch.await(120, TimeUnit.SECONDS);
                assertThat(completed).as("All requests should complete").isTrue();
                
                System.gc();
                Thread.sleep(1000);
                System.gc();
                
                long usedMemoryAfter = runtime.totalMemory() - runtime.freeMemory();
                long memoryIncrease = usedMemoryAfter - usedMemoryBefore;
                
                System.out.println("Memory after: " + (usedMemoryAfter / 1024 / 1024) + " MB");
                System.out.println("Memory increase: " + (memoryIncrease / 1024 / 1024) + " MB");
                System.out.println("Success: " + successCount.get() + ", Errors: " + errorCount.get());

                // Memory should not increase by more than 150MB
                assertThat(memoryIncrease)
                    .as("Memory increase should be less than 150MB")
                    .isLessThan(150 * 1024 * 1024);
            }
        }

        @Test
        @DisplayName("Multiple GC cycles show stable memory")
        void multipleGcCyclesStable() throws InterruptedException {
            int requestsPerCycle = 100;
            int cycles = 5;
            
            Runtime runtime = Runtime.getRuntime();
            List<Long> memoryReadings = new ArrayList<>();

            for (int cycle = 0; cycle < cycles; cycle++) {
                System.gc();
                Thread.sleep(500);
                
                long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                memoryReadings.add(usedMemory);
                System.out.println("Cycle " + (cycle + 1) + " memory: " + (usedMemory / 1024 / 1024) + " MB");

                // Execute requests
                try (ExecutorService executor = Executors.newFixedThreadPool(10)) {
                    CountDownLatch latch = new CountDownLatch(requestsPerCycle);
                    
                    for (int i = 0; i < requestsPerCycle; i++) {
                        executor.submit(() -> {
                            try {
                                restTemplate.getForEntity(baseUrl + "/actuator/health", String.class);
                            } catch (Exception e) {
                                // Ignore
                            }
                            latch.countDown();
                        });
                    }
                    
                    latch.await(30, TimeUnit.SECONDS);
                }
            }

            // Calculate memory trend
            long firstReading = memoryReadings.get(0);
            long lastReading = memoryReadings.get(memoryReadings.size() - 1);
            long memoryGrowth = lastReading - firstReading;
            
            System.out.println("Memory growth over " + cycles + " cycles: " + 
                (memoryGrowth / 1024 / 1024) + " MB");

            // Memory should not grow significantly (allow up to 50MB growth)
            assertThat(memoryGrowth)
                .as("Memory should not grow significantly across cycles")
                .isLessThan(50 * 1024 * 1024);
        }
    }

    @Nested
    @DisplayName("Connection Pool Tests")
    class ConnectionPoolTests {

        @Test
        @DisplayName("No connection leaks under sustained load")
        void noConnectionLeaks() throws InterruptedException {
            int totalRequests = 200;
            int poolSize = 5;
            
            try (ExecutorService executor = Executors.newFixedThreadPool(poolSize)) {
                CountDownLatch latch = new CountDownLatch(totalRequests);
                List<Long> latencies = new ArrayList<>();

                for (int i = 0; i < totalRequests; i++) {
                    executor.submit(() -> {
                        try {
                            long start = System.currentTimeMillis();
                            restTemplate.getForEntity(baseUrl + "/actuator/health", String.class);
                            latencies.add(System.currentTimeMillis() - start);
                        } catch (Exception e) {
                            latencies.add(-1L);
                        }
                        latch.countDown();
                    });
                }

                boolean completed = latch.await(60, TimeUnit.SECONDS);
                assertThat(completed).isTrue();

                long successCount = latencies.stream().filter(l -> l >= 0).count();
                double successRate = successCount / (double) totalRequests;
                
                System.out.println("Success rate: " + (String.format("%.2f", successRate * 100)) + "%");
                
                // Should have reasonable success rate (90% is acceptable under heavy load)
                assertThat(successRate)
                    .as("Success rate should be above 90% under sustained load")
                    .isGreaterThan(0.90);
            }
        }
    }
}