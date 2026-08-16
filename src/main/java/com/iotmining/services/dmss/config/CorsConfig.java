package com.iotmining.services.dmss.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Same pattern as tenant-management-service/device-management-service: a
 * CorsConfigurationSource bean (not a standalone CorsFilter, which the
 * previous version of this class used) consumed by SecurityConfig via
 * ObjectProvider. @Primary is required either way - Spring MVC's
 * auto-registered mvcHandlerMappingIntrospector bean also implements
 * CorsConfigurationSource, so without it, SecurityConfig's
 * ObjectProvider<CorsConfigurationSource> finds two candidates and throws
 * NoUniqueBeanDefinitionException instead of picking one.
 */
@Configuration
public class CorsConfig {

    @Bean
    @Profile("dev")
    @Primary
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:3001",
                "http://localhost:3009",
                "http://localhost:3002"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
