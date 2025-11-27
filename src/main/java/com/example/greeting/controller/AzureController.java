package com.example.greeting.controller;

import com.example.greeting.service.AzureBlobStorageService;
import com.example.greeting.service.AzureKeyVaultService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kontroler do testowania integracji z Azure (Key Vault i Blob Storage).
 */
@RestController
@RequestMapping("/azure")
public class AzureController {
    
    private final AzureKeyVaultService keyVaultService;
    private final AzureBlobStorageService blobStorageService;
    
    public AzureController(
            AzureKeyVaultService keyVaultService,
            AzureBlobStorageService blobStorageService) {
        this.keyVaultService = keyVaultService;
        this.blobStorageService = blobStorageService;
    }
    
    /**
     * Health check - sprawdza połączenie z Key Vault i Blob Storage.
     * GET /azure/health
     */
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("service", "greeting-service");
        
        // Key Vault status
        Map<String, Object> kvStatus = new HashMap<>();
        kvStatus.put("url", keyVaultService.getKeyVaultUrl());
        kvStatus.put("healthy", keyVaultService.isHealthy());
        health.put("keyVault", kvStatus);
        
        // Blob Storage status
        Map<String, Object> blobStatus = new HashMap<>();
        blobStatus.put("accountName", blobStorageService.getAccountName());
        blobStatus.put("containerName", blobStorageService.getContainerName());
        blobStatus.put("healthy", blobStorageService.isHealthy());
        health.put("blobStorage", blobStatus);
        
        boolean allHealthy = keyVaultService.isHealthy() && blobStorageService.isHealthy();
        health.put("status", allHealthy ? "UP" : "DEGRADED");
        
        return allHealthy ? ResponseEntity.ok(health) : ResponseEntity.status(503).body(health);
    }
    
    /**
     * Pobiera sekret z Key Vault.
     * GET /azure/secrets/{secretName}
     */
    @GetMapping(value = "/secrets/{secretName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> getSecret(@PathVariable String secretName) {
        try {
            String secretValue = keyVaultService.getSecret(secretName);
            Map<String, String> response = new HashMap<>();
            response.put("secretName", secretName);
            response.put("value", secretValue);
            response.put("message", "Sekret pobrany pomyślnie");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("secretName", secretName);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Testowy endpoint do sprawdzenia sekretów DB (masking hasła).
     * GET /azure/config
     */
    @GetMapping(value = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> getConfig() {
        try {
            String dbUser = keyVaultService.getSecret("greeting-db-user");
            String dbPassword = keyVaultService.getSecret("greeting-db-password");
            
            Map<String, String> config = new HashMap<>();
            config.put("dbUser", dbUser);
            config.put("dbPassword", maskPassword(dbPassword)); // Maskowanie hasła
            config.put("message", "Konfiguracja pobrana z Key Vault");
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Upload tekstu do Blob Storage.
     * POST /azure/blobs/{blobName}
     * Body: plain text content
     */
    @PostMapping(value = "/blobs/{blobName}", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> uploadBlob(
            @PathVariable String blobName,
            @RequestBody String content) {
        try {
            blobStorageService.uploadBlob(blobName, content);
            Map<String, String> response = new HashMap<>();
            response.put("blobName", blobName);
            response.put("size", String.valueOf(content.length()));
            response.put("message", "Blob uploaded pomyślnie");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("blobName", blobName);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Download blob z Blob Storage.
     * GET /azure/blobs/{blobName}
     */
    @GetMapping(value = "/blobs/{blobName}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> downloadBlob(@PathVariable String blobName) {
        try {
            String content = blobStorageService.downloadBlob(blobName);
            return ResponseEntity.ok(content);
        } catch (Exception e) {
            return ResponseEntity.status(404).body("Blob not found: " + e.getMessage());
        }
    }
    
    /**
     * Lista wszystkich blobów w kontenerze.
     * GET /azure/blobs
     */
    @GetMapping(value = "/blobs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> listBlobs() {
        try {
            List<String> blobs = blobStorageService.listBlobs();
            Map<String, Object> response = new HashMap<>();
            response.put("containerName", blobStorageService.getContainerName());
            response.put("count", blobs.size());
            response.put("blobs", blobs);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Usuwa blob.
     * DELETE /azure/blobs/{blobName}
     */
    @DeleteMapping(value = "/blobs/{blobName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> deleteBlob(@PathVariable String blobName) {
        try {
            blobStorageService.deleteBlob(blobName);
            Map<String, String> response = new HashMap<>();
            response.put("blobName", blobName);
            response.put("message", "Blob usunięty pomyślnie");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("blobName", blobName);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Maskuje hasło (pokazuje tylko pierwsze 2 znaki).
     */
    private String maskPassword(String password) {
        if (password == null || password.length() <= 2) {
            return "***";
        }
        return password.substring(0, 2) + "***";
    }
}

