package conectaseguros.co.discovery_server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Chain 1 (Order 1): Actuator endpoints.
     *
     * <ul>
     *   <li>/actuator/health is public — required by Kubernetes liveness/readiness probes
     *       and load balancer health checks, which cannot send credentials.</li>
     *   <li>All other actuator endpoints (info, metrics, prometheus, env, …) require
     *       HTTP Basic authentication. /actuator/info is intentionally protected because
     *       the info endpoint can expose build metadata, Java runtime version, and OS
     *       details (via management.info.java/os contributors) that aid fingerprinting.</li>
     * </ul>
     *
     * Session policy is STATELESS: HTTP Basic sends credentials on every request,
     * so there is no need to create or maintain HttpSession objects.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) {
        return http
                .securityMatcher(EndpointRequest.toAnyEndpoint())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(EndpointRequest.to("health")).permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .contentTypeOptions(withDefaults())
                        .cacheControl(withDefaults())
                        .referrerPolicy(withDefaults())
                )
                .build();
    }

    /**
     * Configure security for the Eureka REST API and dashboard endpoints.
     *
     * <p>Requires HTTP Basic authentication for all requests; disables CSRF; enforces
     * stateless session management; and applies secure HTTP headers (deny framing,
     * set content-type options, enable cache-control, and apply the default referrer policy).
     * HSTS is intentionally omitted because TLS is terminated before the application.
     *
     * @param http the HttpSecurity to configure
     * @return the configured SecurityFilterChain that secures Eureka endpoints
     */
    @Bean
    @Order(2)
    public SecurityFilterChain eurekaSecurityFilterChain(HttpSecurity http) {
        return http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )
                .httpBasic(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .contentTypeOptions(withDefaults())
                        .cacheControl(withDefaults())
                        .referrerPolicy(withDefaults())
                )
                .build();
    }

    /**
         * Provides an in-memory {@link UserDetailsService} backed by credentials read from the
         * active Spring {@link Environment}.
         *
         * <p>Reads username from {@code spring.security.user.name} (default: {@code eureka})
         * and password from {@code spring.security.user.password}. The password is encoded with
         * BCrypt before creating the in-memory user.
         *
         * @param env the Spring Environment used to obtain security properties
         * @return an {@link InMemoryUserDetailsManager} containing a single user with role {@code EUREKA}
         * @throws IllegalStateException if {@code spring.security.user.password} is missing or blank
         */
    @Bean
    public UserDetailsService userDetailsService(Environment env) {
        String username = env.getProperty("spring.security.user.name", "eureka");
        String rawPassword = env.getProperty("spring.security.user.password", "");
        if (rawPassword.isBlank()) {
            throw new IllegalStateException(
                    "Discovery Server security misconfiguration: " +
                    "spring.security.user.password must be set and non-empty. " +
                    "Configure it in the active Spring profile (application-dev.properties, " +
                    "application-k8s.properties, etc.) or in the .env file.");
        }
        log.info("Configuring in-memory UserDetailsService for user '{}'", username);
        return new InMemoryUserDetailsManager(
                User.builder()
                        .username(username)
                        .password(passwordEncoder().encode(rawPassword))
                        .roles("EUREKA")
                        .build()
        );
    }

    /**
     * Create a PasswordEncoder that uses the BCrypt hashing algorithm.
     *
     * @return a PasswordEncoder configured to hash passwords with BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}