package com.team.revaluation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map /pdfs/** URL to the pdf-storage folder in your project root
        registry.addResourceHandler("/pdfs/**")
                .addResourceLocations("file:pdf-storage/");
    }
}