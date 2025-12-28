package com.pdfconverter.core;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

/**
 * Spring configuration for core PDF processing components.
 */
@Configuration
public class CoreBeanConfiguration {

    @Bean
    public MetadataGenerator metadataGenerator() {
        return new MetadataGenerator();
    }
}
