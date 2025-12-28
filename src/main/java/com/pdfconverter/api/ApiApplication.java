package com.pdfconverter.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application class for PDF to Image Converter API.
 */
@SpringBootApplication(scanBasePackages = "com.pdfconverter")
@EnableScheduling
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
        System.out.println("\n=== PDF to Image Converter API Started ===");
        System.out.println("API available at: http://localhost:8080");
        System.out.println("API documentation: http://localhost:8080/api/help");
        System.out.println("==========================================\n");
    }
}
