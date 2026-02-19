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
     * Chain 2 (Order 2): Eureka REST API and dashboard.
     *
     * <p>All requests require HTTP Basic authentication. This covers:
     * <ul>
     *   <li>/eureka/** — service registration and discovery calls from microservice clients.</li>
     *   <li>/ and /lastn — the Eureka web dashboard.</li>
     * </ul>
     *
     * <p>CSRF is disabled globally: with STATELESS session policy there are no session
     * cookies, so CSRF attacks cannot be mounted. The previous approach of
     * {@code ignoringRequestMatchers("/eureka/**")} was incomplete because the dashboard
     * UI also issues POST and PUT requests.
     *
     * <p>HSTS is intentionally omitted: TLS is terminated at the ingress/load balancer;
     * the application only sees plain HTTP internally.
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
     * Provides an in-memory {@link UserDetailsService} backed by credentials defined in
     * the active Spring profile ({@code application-dev.properties},
     * {@code application-k8s.properties}, etc.).
     *
     * <p>Passwords are encoded with BCrypt at startup so they are never stored in plain
     * text in memory or logged by the framework.
     */
    @Bean
    public UserDetailsService userDetailsService(Environment env) {
        String username = env.getProperty("spring.security.user.name", "eureka");
        String rawPassword = env.getProperty("spring.security.user.password", "");
        if (username.isBlank()) {
            throw new IllegalStateException(
                    "Discovery Server security misconfiguration: " +
                    "spring.security.user.name must be set and non-empty. " +
                    "Configure it in the active Spring profile (application-dev.properties, " +
                    "application-k8s.properties, etc.) or in the .env file.");
        }
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
     * BCrypt is the industry-standard adaptive password hashing algorithm.
     * It applies a random salt per hash and its cost factor increases hash time
     * proportionally to resist brute-force attacks on stolen hashes.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
