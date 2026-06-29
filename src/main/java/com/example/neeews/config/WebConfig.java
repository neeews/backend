package com.example.neeews.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.image.storage-path}")
    private String imageStoragePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = Paths.get(imageStoragePath).toAbsolutePath().toString();
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + absolutePath + "/");
    }
}
