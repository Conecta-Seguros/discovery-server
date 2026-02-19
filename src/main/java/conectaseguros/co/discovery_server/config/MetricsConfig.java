package conectaseguros.co.discovery_server.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Applies Micrometer common tags to every meter registered in this application.
 *
 * <p>Common tags ensure all metrics exported to Prometheus carry consistent labels,
 * enabling Grafana dashboards to filter and aggregate by service name and active
 * environment without modifying individual metric definitions.
 *
 * <p>Tags applied:
 * <ul>
 *   <li>{@code application} — Spring application name (e.g. {@code discovery-server}).
 *       Matches the convention used across the Caicedo Seguros platform.</li>
 *   <li>{@code environment} — comma-joined active Spring profiles (e.g. {@code dev},
 *       {@code k8s,debug}). Falls back to {@code default} when no profile is active.
 *       Uses {@link Environment#getActiveProfiles()} rather than
 *       {@code @Value("${spring.profiles.active}")} to correctly handle multiple
 *       simultaneous profiles and cases where the property is not explicitly set in
 *       the Spring {@code Environment} (e.g. when activated via JVM system property).</li>
 * </ul>
 */
@Configuration
public class MetricsConfig {

    /**
     * Configure Micrometer common tags to apply to every meter registry.
     *
     * @param appName the application name from spring.application.name used for the "application" tag
     * @param profile the active Spring profile (defaults to "default") used for the "environment" tag
     * @return a MeterRegistryCustomizer that adds "application" and "environment" common tags to all meters
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> commonTags(
            @Value("${spring.application.name}") String appName,
            Environment environment) {
        String[] active = environment.getActiveProfiles();
        String profiles = active.length > 0 ? String.join(",", active) : "default";
        return registry -> registry.config()
                .commonTags(
                        "application", appName,
                        "environment", profiles
                );
    }
}