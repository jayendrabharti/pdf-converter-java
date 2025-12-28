package com.pdfconverter.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pdfconverter.core.PdfRepairService;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check endpoint for monitoring.
 */
@RestController
@RequestMapping("/")
public class HealthCheckController {

    @Autowired
    private PdfRepairService repairService;

    @GetMapping("/health")
    public Map<String,Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "up");
        health.put("service", "PDF Converter API");
        
        // Repair tools availability
        Map<String, Boolean> repairTools = new HashMap<>();
        repairTools.put("enabled", repairService.isRepairEnabled());
        repairTools.put("qpdf", repairService.isQpdfAvailable());
        repairTools.put("ghostscript", repairService.isGhostscriptAvailable());
        health.put("repairTools", repairTools);
        
        // System info
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("processors", runtime.availableProcessors());
        systemInfo.put("freeMemoryMB", runtime.freeMemory() / (1024 * 1024));
        systemInfo.put("totalMemoryMB", runtime.totalMemory() / (1024 * 1024));
        systemInfo.put("maxMemoryMB", runtime.maxMemory() / (1024 * 1024));
        health.put("system", systemInfo);
        
        return health;
    }
}
