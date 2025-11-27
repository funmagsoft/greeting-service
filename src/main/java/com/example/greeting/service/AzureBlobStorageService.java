package com.example.greeting.service;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Serwis do obsługi Azure Blob Storage.
 * Używa Workload Identity (DefaultAzureCredential) do autoryzacji.
 */
@Service
public class AzureBlobStorageService {
    
    private static final Logger log = LoggerFactory.getLogger(AzureBlobStorageService.class);
    
    private final BlobContainerClient containerClient;
    private final String accountName;
    private final String containerName;
    
    public AzureBlobStorageService(
            @Value("${azure.storage.account-name:hycomcminternal}") String accountName,
            @Value("${azure.storage.container-name:test-container-dev}") String containerName) {
        
        this.accountName = accountName;
        this.containerName = containerName;
        
        String endpoint = String.format("https://%s.blob.core.windows.net", accountName);
        log.info("Inicjalizacja AzureBlobStorageService - endpoint: {}, container: {}", endpoint, containerName);
        
        try {
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .endpoint(endpoint)
                    .credential(new DefaultAzureCredentialBuilder().build())
                    .buildClient();
            
            this.containerClient = blobServiceClient.getBlobContainerClient(containerName);
            log.info("✅ BlobContainerClient zainicjalizowany pomyślnie");
        } catch (Exception e) {
            log.error("❌ Błąd inicjalizacji BlobContainerClient: {}", e.getMessage());
            throw new RuntimeException("Nie można zainicjalizować Blob Storage client", e);
        }
    }
    
    /**
     * Upload tekstu do blob storage.
     * @param blobName nazwa pliku blob
     * @param content zawartość tekstowa
     */
    public void uploadBlob(String blobName, String content) {
        try {
            log.debug("Uploading blob: {}", blobName);
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            blobClient.upload(new ByteArrayInputStream(data), data.length, true);
            log.info("✅ Blob '{}' uploaded pomyślnie ({} bytes)", blobName, data.length);
        } catch (Exception e) {
            log.error("❌ Błąd uploadu blob '{}': {}", blobName, e.getMessage());
            throw new RuntimeException("Nie można uploadować blob: " + blobName, e);
        }
    }
    
    /**
     * Download blob jako tekst.
     * @param blobName nazwa pliku blob
     * @return zawartość tekstowa
     */
    public String downloadBlob(String blobName) {
        try {
            log.debug("Downloading blob: {}", blobName);
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            String content = blobClient.downloadContent().toString();
            log.info("✅ Blob '{}' downloaded pomyślnie ({} bytes)", blobName, content.length());
            return content;
        } catch (Exception e) {
            log.error("❌ Błąd downloadu blob '{}': {}", blobName, e.getMessage());
            throw new RuntimeException("Nie można pobrać blob: " + blobName, e);
        }
    }
    
    /**
     * Lista wszystkich blobów w kontenerze.
     * @return lista nazw blobów
     */
    public List<String> listBlobs() {
        try {
            log.debug("Listowanie blobów w kontenerze: {}", containerName);
            List<String> blobNames = new ArrayList<>();
            for (BlobItem blobItem : containerClient.listBlobs()) {
                blobNames.add(blobItem.getName());
            }
            log.info("✅ Znaleziono {} blobów w kontenerze", blobNames.size());
            return blobNames;
        } catch (Exception e) {
            log.error("❌ Błąd listowania blobów: {}", e.getMessage());
            throw new RuntimeException("Nie można wylistować blobów", e);
        }
    }
    
    /**
     * Sprawdza czy blob istnieje.
     * @param blobName nazwa blob
     * @return true jeśli istnieje
     */
    public boolean blobExists(String blobName) {
        try {
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            boolean exists = blobClient.exists();
            log.debug("Blob '{}' exists: {}", blobName, exists);
            return exists;
        } catch (Exception e) {
            log.error("❌ Błąd sprawdzania istnienia blob '{}': {}", blobName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Usuwa blob.
     * @param blobName nazwa blob
     */
    public void deleteBlob(String blobName) {
        try {
            log.debug("Usuwanie blob: {}", blobName);
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            blobClient.delete();
            log.info("✅ Blob '{}' usunięty pomyślnie", blobName);
        } catch (Exception e) {
            log.error("❌ Błąd usuwania blob '{}': {}", blobName, e.getMessage());
            throw new RuntimeException("Nie można usunąć blob: " + blobName, e);
        }
    }
    
    /**
     * Health check dla Blob Storage.
     * @return true jeśli połączenie działa
     */
    public boolean isHealthy() {
        try {
            // Próba pobrania properties kontenera
            containerClient.getProperties();
            log.info("✅ Blob Storage health check OK");
            return true;
        } catch (Exception e) {
            log.error("❌ Blob Storage health check FAILED: {}", e.getMessage());
            return false;
        }
    }
    
    public String getAccountName() {
        return accountName;
    }
    
    public String getContainerName() {
        return containerName;
    }
}

