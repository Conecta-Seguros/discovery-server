package conectaseguros.co.discovery_server.config;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Loads environment variables from a {@code .env} file at application startup.
 *
 * <p>This configuration bridges the gap between local development — where sensitive values
 * live in a {@code .env} file — and production — where they are injected as OS-level
 * environment variables by Docker or Kubernetes. In both cases the variables are available
 * to Spring's {@code ${…}} placeholder resolver without any additional wiring.
 *
 * <p>Behaviour by environment:
 * <ul>
 *   <li><b>Local development:</b> reads {@code .env} from the working directory and promotes
 *       each entry to a Java system property, making variables such as {@code ${EUREKA_USERNAME}}
 *       and {@code ${EUREKA_PASSWORD}} resolvable in {@code application-dev.properties}.</li>
 *   <li><b>Docker / Kubernetes:</b> the {@code .env} file is absent or empty
 *       ({@code ignoreIfMissing}). OS-level environment variables are already visible to
 *       Spring Boot through its {@code SystemEnvironmentPropertySource}, so no additional
 *       action is taken.</li>
 * </ul>
 *
 * <p><b>Security:</b> the {@code .env} file is listed in {@code .gitignore} and must never
 * be committed to version control. Use {@code .env.template} as the reference for required
 * variables.
 */
@Slf4j
@Configuration
public class DotenvConfig {

    /**
     * Creates a {@link Dotenv} instance that loads variables from the {@code .env} file
     * and promotes them to Java system properties.
     *
     * <p>Configuration flags:
     * <ul>
     *   <li>{@code ignoreIfMissing} — silently skips loading when no {@code .env} file is
     *       found; production environments rely on OS-level env vars and must not fail here.</li>
     *   <li>{@code ignoreIfMalformed} — tolerates malformed entries (e.g. missing {@code =})
     *       instead of throwing, preventing startup failures due to formatting errors.</li>
     *   <li>{@code systemProperties} — copies each loaded variable into
     *       {@link System#setProperty(String, String)}, making it available to Spring's
     *       {@code PropertySourcesPlaceholderConfigurer} for {@code ${…}} resolution. Without
     *       this flag the {@code .env} values are loaded into the {@link Dotenv} object but
     *       remain invisible to Spring property injection.</li>
     * </ul>
     *
     * @return a configured {@link Dotenv} instance; retained in the context so that other
     *         components can call {@link Dotenv#get(String)} directly if needed
     */
    @Bean
    public Dotenv dotenv() {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .ignoreIfMalformed()
                .systemProperties()
                .load();
        log.debug("Dotenv loaded — .env variables promoted to system properties");
        return dotenv;
    }
}
