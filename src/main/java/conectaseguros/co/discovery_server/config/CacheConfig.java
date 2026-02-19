package conectaseguros.co.discovery_server.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configures Caffeine as the Spring Cache provider.
 *
 * <p>Eureka's internal response cache (controlled via {@code eureka.server.response-cache-*}
 * properties) is separate from Spring Cache. This configuration provides the Spring Cache
 * abstraction so that any {@code @Cacheable} annotation in the application layer is backed
 * by Caffeine rather than the default no-op or JDK concurrent cache.
 *
 * <p>Cache settings:
 * <ul>
 *   <li>TTL: 10 minutes — aligned with the platform-wide caching TTL defined in CLAUDE.md.</li>
 *   <li>Max size: 500 entries — sufficient for a service registry.</li>
 *   <li>{@code recordStats()} — exposes Caffeine hit/miss/eviction counters to Micrometer
 *       automatically when the {@code caffeine.jcache} bridge is on the classpath.</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(500)
                        .recordStats()
        );
        return manager;
    }
}
