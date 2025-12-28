package com.pdfconverter.api.dto;

import java.util.Map;

/**
 * DTO for API information and help.
 */
public class ApiInfo {
    private String name;
    private String version;
    private Map<String, EndpointInfo> endpoints;

    public ApiInfo() {
    }

    public ApiInfo(String name, String version, Map<String, EndpointInfo> endpoints) {
        this.name = name;
        this.version = version;
        this.endpoints = endpoints;
    }

    public static class EndpointInfo {
        private String description;
        private Map<String, String> parameters;

        public EndpointInfo(String description, Map<String, String> parameters) {
            this.description = description;
            this.parameters = parameters;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Map<String, String> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, String> parameters) {
            this.parameters = parameters;
        }
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, EndpointInfo> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Map<String, EndpointInfo> endpoints) {
        this.endpoints = endpoints;
    }
}
