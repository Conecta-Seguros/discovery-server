package conectaseguros.co.discovery_server.performance.runner;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects and reports performance metrics.
 * Outputs results to console and optional CSV/JSON files.
 */
public class PerformanceReport {

    private final String testName;
    private final List<MetricEntry> metrics;
    private final LocalDateTime startTime;

    public PerformanceReport(String testName) {
        this.testName = testName;
        this.metrics = new ArrayList<>();
        this.startTime = LocalDateTime.now();
    }

    /**
     * Print summary to console.
     */
    public void printSummary() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("PERFORMANCE REPORT: " + testName);
        System.out.println("=".repeat(60));
        System.out.println("Start Time: " + startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        System.out.println("End Time: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        System.out.println("-".repeat(60));

        for (MetricEntry entry : metrics) {
            System.out.printf("  %-40s: %d %s%n", entry.name, entry.value, entry.unit);
        }

        System.out.println("=".repeat(60));
    }

    /**
     * Export metrics to CSV file.
     */
    public void exportToCsv(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("metric_name,value,unit,timestamp");
            for (MetricEntry entry : metrics) {
                writer.printf("%s,%d,%s,%d%n", entry.name, entry.value, entry.unit, entry.timestamp);
            }
            System.out.println("CSV report exported to: " + filename);
        } catch (IOException e) {
            System.err.println("Failed to export CSV: " + e.getMessage());
        }
    }

    /**
     * Export metrics to JSON file.
     */
    public void exportToJson(String filename) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"testName\": \"").append(testName).append("\",\n");
        json.append("  \"startTime\": \"").append(startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\",\n");
        json.append("  \"endTime\": \"").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\",\n");
        json.append("  \"metrics\": [\n");
        
        for (int i = 0; i < metrics.size(); i++) {
            MetricEntry entry = metrics.get(i);
            json.append("    {\"name\": \"").append(entry.name).append("\", \"value\": ").append(entry.value);
            json.append(", \"unit\": \"").append(entry.unit).append("\"}");
            if (i < metrics.size() - 1) json.append(",");
            json.append("\n");
        }
        
        json.append("  ]\n");
        json.append("}\n");

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.print(json.toString());
            System.out.println("JSON report exported to: " + filename);
        } catch (IOException e) {
            System.err.println("Failed to export JSON: " + e.getMessage());
        }
    }

    /**
     * Get summary statistics for a list of values.
     */
    @Contract("_ -> new")
    public static @NonNull SummaryStats calculateStats(@NonNull List<Long> values) {
        if (values.isEmpty()) {
            return new SummaryStats(0, 0, 0, 0, 0);
        }

        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        long sum = 0;

        for (long value : values) {
            min = Math.min(min, value);
            max = Math.max(max, value);
            sum += value;
        }

        long avg = sum / values.size();
        long median = calculateMedian(values);

        return new SummaryStats(min, max, avg, median, values.size());
    }

    private static long calculateMedian(@NonNull List<Long> sorted) {
        int size = sorted.size();
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2;
        }
        return sorted.get(size / 2);
    }

    private static class MetricEntry {
        final String name;
        final long value;
        final String unit;
        final long timestamp;

        MetricEntry(String name, long value, String unit, long timestamp) {
            this.name = name;
            this.value = value;
            this.unit = unit;
            this.timestamp = timestamp;
        }
    }

    /**
     * Summary statistics for a set of measurements.
     */
    public record SummaryStats(long min, long max, long avg, long median, long count) {
        public void print(String label) {
            System.out.printf("  %s - Min: %dms, Max: %dms, Avg: %dms, Median: %dms, Count: %d%n",
                label, min, max, avg, median, count);
        }
    }
}