package com.sudolife.config.security;

import com.sudolife.adapter.driving.rest.ratelimit.GenericApiRateLimitFilter;
import com.sudolife.adapter.driving.rest.ratelimit.HttpRequestOriginResolver;
import com.sudolife.application.service.ratelimit.ports.required.RateLimitBucketRegistry;
import com.sudolife.application.service.user.ports.required.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Clock;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String API_CONTENT_SECURITY_POLICY = "default-src 'none'; frame-ancestors 'none'";
    private static final String API_PERMISSIONS_POLICY = "accelerometer=(), camera=(), geolocation=(), gyroscope=(), "
            + "magnetometer=(), microphone=(), payment=(), usb=()";

    private final SecurityHeadersProperties securityHeadersProperties;

    public SecurityConfig(SecurityHeadersProperties securityHeadersProperties) {
        this.securityHeadersProperties = securityHeadersProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ObjectProvider<GenericApiRateLimitFilter> genericApiRateLimitFilter,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(this::configureSecurityHeaders)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/users/register").permitAll()
                        .requestMatchers("/api/users/login").permitAll()
                        .requestMatchers("/api/auth/password-recovery").permitAll()
                        .requestMatchers("/api/strava/callback").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/api/strava/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        genericApiRateLimitFilter.ifAvailable(filter -> http.addFilterAfter(filter, JwtAuthenticationFilter.class));

        return http.build();
    }

    private void configureSecurityHeaders(HeadersConfigurer<HttpSecurity> headers) {
        headers
                .contentTypeOptions(Customizer.withDefaults())
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                .contentSecurityPolicy(csp -> csp.policyDirectives(API_CONTENT_SECURITY_POLICY))
                .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", API_PERMISSIONS_POLICY));

        if (securityHeadersProperties.hstsEnabled()) {
            headers.httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(securityHeadersProperties.hstsMaxAge().toSeconds())
                    .includeSubDomains(securityHeadersProperties.hstsIncludeSubDomains()));
            return;
        }

        headers.httpStrictTransportSecurity(HeadersConfigurer.HstsConfig::disable);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins());
        configuration.setAllowedMethods(properties.allowedMethods());
        configuration.setAllowedHeaders(properties.allowedHeaders());
        configuration.setAllowCredentials(properties.allowCredentials());
        configuration.setMaxAge(properties.maxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    @ConditionalOnBean(RateLimitBucketRegistry.class)
    public GenericApiRateLimitFilter genericApiRateLimitFilter(RateLimitBucketRegistry bucketRegistry, HttpRequestOriginResolver originResolver) {
        return new GenericApiRateLimitFilter(bucketRegistry, originResolver);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return email -> userRepository.findByEmail(email)
                .map(user -> org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail().value())
                        .password(user.getPassword().value())
                        .authorities("USER")
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(email));
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
