package com.applyflow.jobcopilot.shared.infrastructure.security;

import com.applyflow.jobcopilot.shared.infrastructure.ratelimit.RateLimitFilter;
import com.applyflow.jobcopilot.shared.infrastructure.web.CorrelationIdFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SecurityProperties securityProperties,
                                                   CorrelationIdFilter correlationIdFilter,
                                                   RateLimitFilter rateLimitFilter,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   ActuatorTokenAuthenticationFilter actuatorTokenAuthenticationFilter,
                                                   RestAuthenticationEntryPoint authEntryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> {
                    headers.frameOptions(frame -> frame.deny());
                    headers.contentTypeOptions(Customizer.withDefaults());
                    headers.referrerPolicy(ref -> ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                    headers.contentSecurityPolicy(csp -> csp.policyDirectives(securityProperties.getHeaders().resolveCspPolicy()));
                    headers.permissionsPolicy(pp -> pp.policy("camera=(), microphone=(), geolocation=(), payment=()"));
                    if (securityProperties.getHeaders().isHstsEnabled()) {
                        headers.httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000));
                    }
                })
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authEntryPoint).accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/prometheus", "/actuator/metrics", "/actuator/metrics/**").hasRole("ADMIN")
                        .requestMatchers("/actuator/**").denyAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/v1/auth/me").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/v1/**").hasAnyRole("USER", "ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitFilter, CorrelationIdFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, CorrelationIdFilter.class)
                .addFilterAfter(actuatorTokenAuthenticationFilter, JwtAuthenticationFilter.class);
        return http.build();
    }
}
