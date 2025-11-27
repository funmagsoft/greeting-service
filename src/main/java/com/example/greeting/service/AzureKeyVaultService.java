package com.example.greeting.service;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Serwis do obsługi Azure Key Vault.
 * Używa Workload Identity (DefaultAzureCredential) do autoryzacji.
 */
@Service
public class AzureKeyVaultService {
    
    private static final Logger log = LoggerFactory.getLogger(AzureKeyVaultService.class);
    
    private final SecretClient secretClient;
    private final String keyVaultUrl;
    
    public AzureKeyVaultService(
            @Value("${azure.keyvault.url:https://hycomcminternal-kv.vault.azure.net}") String keyVaultUrl) {
        this.keyVaultUrl = keyVaultUrl;
        log.info("Inicjalizacja AzureKeyVaultService z URL: {}", keyVaultUrl);
        
        try {
            this.secretClient = new SecretClientBuilder()
                    .vaultUrl(keyVaultUrl)
                    .credential(new DefaultAzureCredentialBuilder().build())
                    .buildClient();
            log.info("✅ SecretClient zainicjalizowany pomyślnie");
        } catch (Exception e) {
            log.error("❌ Błąd inicjalizacji SecretClient: {}", e.getMessage());
            throw new RuntimeException("Nie można zainicjalizować Key Vault client", e);
        }
    }
    
    /**
     * Pobiera sekret z Key Vault.
     * @param secretName nazwa sekretu
     * @return wartość sekretu
     */
    public String getSecret(String secretName) {
        try {
            log.debug("Pobieranie sekretu: {}", secretName);
            String secretValue = secretClient.getSecret(secretName).getValue();
            log.info("✅ Sekret '{}' pobrany pomyślnie", secretName);
            return secretValue;
        } catch (Exception e) {
            log.error("❌ Błąd pobierania sekretu '{}': {}", secretName, e.getMessage());
            throw new RuntimeException("Nie można pobrać sekretu: " + secretName, e);
        }
    }
    
    /**
     * Sprawdza czy połączenie z Key Vault działa.
     * @return true jeśli połączenie działa
     */
    public boolean isHealthy() {
        try {
            // Próba listowania sekretów (nie pobieramy wartości)
            secretClient.listPropertiesOfSecrets().stream().findFirst();
            log.info("✅ Key Vault health check OK");
            return true;
        } catch (Exception e) {
            log.error("❌ Key Vault health check FAILED: {}", e.getMessage());
            return false;
        }
    }
    
    public String getKeyVaultUrl() {
        return keyVaultUrl;
    }
}

