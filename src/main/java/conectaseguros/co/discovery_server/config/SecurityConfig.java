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
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.HashMap;
import java.util.Map;

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
     * cookies, so CSRF attacks cannot be mounted.
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
     * the active Spring profile.
     *
     * <p>Optimized password encoding:
     * - BCrypt with a cost factor of 8 (default is 10). Performance difference:
     *   - Cost 10: ~200ms por hash
     *   - Cost 8: ~50ms por hash (4x más rápido)
     *   - The security difference is minimal for an internal service using HTTPS.
     *
     * <p>For production environments where safety is critical, maintain cost 10.
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
     * Optimized password encoder with multiple algorithms for gradual migration.
     *
     * The default encoder uses BCrypt with a cost factor of 8 (balance between security and performance).
     * In production, consider using Argon2 or Pbkdf2 for added safety.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        String defaultEncodingId = "bcrypt";
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        
        // BCrypt
        encoders.put("bcrypt", new BCryptPasswordEncoder(8));
        
        // Argon2
        encoders.put("argon2", Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        
        // PBKDF2 (estándar NIST)
        encoders.put("pbkdf2", Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8());

        return new DelegatingPasswordEncoder(defaultEncodingId, encoders);
    }
}