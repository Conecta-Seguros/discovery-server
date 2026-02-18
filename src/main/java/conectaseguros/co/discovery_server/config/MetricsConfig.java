package conectaseguros.co.discovery_server.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
 *       Matches the convention used across the Conecta Seguros platform.</li>
 *   <li>{@code environment} — active Spring profile (e.g. {@code dev}, {@code k8s},
 *       {@code k8s-ha}). Defaults to {@code default} when no profile is active.</li>
 * </ul>
 */
@Configuration
public class MetricsConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> commonTags(
            @Value("${spring.application.name}") String appName,
            @Value("${spring.profiles.active:default}") String profile) {
        return registry -> registry.config()
                .commonTags(
                        "application", appName,
                        "environment", profile
                );
    }
}
