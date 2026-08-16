package com.iotmining.services.dmss.config;

import com.iotmining.services.dmss.security.LiteJwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final LiteJwtAuthenticationFilter liteJwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            ObjectProvider<CorsConfigurationSource> corsConfigurationSource) throws Exception {

        // CorsConfig defines a dev-only CorsConfigurationSource bean (@Primary, so it
        // wins over Spring MVC's own mvcHandlerMappingIntrospector) - same pattern as
        // tenant-management-service/device-management-service. Without one - the
        // default in prod, where Nginx/the gateway already adds CORS headers for this
        // service's routes - CORS stays disabled here to avoid duplicate
        // Access-Control-Allow-Origin headers.
        CorsConfigurationSource corsSource = corsConfigurationSource.getIfAvailable();
        if (corsSource != null) {
            http.cors(cors -> cors.configurationSource(corsSource));
        } else {
            http.cors(AbstractHttpConfigurer::disable);
        }

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Preflight requests never carry an Authorization header - without this,
                        // anyRequest().authenticated() below rejects them with 401/403 before
                        // CORS headers ever get added to the response.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(liteJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
