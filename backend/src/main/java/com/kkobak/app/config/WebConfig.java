package com.kkobak.app.config;

import com.kkobak.app.media.StorageProperties;
import java.nio.file.Path;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final String[] allowedOrigins;
    private final StorageProperties storage;

    public WebConfig(@Value("${app.cors.allowed-origins}") String origins, StorageProperties storage) {
        this.allowedOrigins = Arrays.stream(origins.split(",")).map(String::trim).filter(value -> !value.isBlank()).toArray(String[]::new);
        this.storage = storage;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins(allowedOrigins).allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS").allowedHeaders("*");
        registry.addMapping("/uploads/**").allowedOrigins(allowedOrigins).allowedMethods("GET", "OPTIONS").allowedHeaders("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**").addResourceLocations(Path.of(storage.localRoot()).toAbsolutePath().normalize().toUri().toString());
    }
}
