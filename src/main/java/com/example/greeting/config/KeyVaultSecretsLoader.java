package com.example.greeting.config;

import com.example.greeting.service.AzureKeyVaultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Loads secrets from Key Vault on application startup.
 * Runs after application context is fully initialized.
 */
@Component
public class KeyVaultSecretsLoader implements ApplicationRunner {
    
    private static final Logger log = LoggerFactory.getLogger(KeyVaultSecretsLoader.class);
    
    private final AzureKeyVaultService keyVaultService;
    
    public KeyVaultSecretsLoader(AzureKeyVaultService keyVaultService) {
        this.keyVaultService = keyVaultService;
    }
    
    @Override
    public void run(ApplicationArguments args) {
        log.info("=== Loading secrets from Key Vault ===");
        
        try {
            // Read greeting-db-username
            String dbUsername = keyVaultService.getSecret("greeting-db-username");
            log.debug("greeting-db-username: {}", dbUsername);
            log.info("✅ greeting-db-username loaded successfully (length: {})", dbUsername.length());
            
            // Read greeting-db-password
            String dbPassword = keyVaultService.getSecret("greeting-db-password");
            log.debug("greeting-db-password: {}", dbPassword);
            log.info("✅ greeting-db-password loaded successfully (length: {})", dbPassword.length());
            
            log.info("=== All secrets loaded successfully ===");
            
        } catch (Exception e) {
            log.error("❌ Failed to load secrets from Key Vault: {}", e.getMessage(), e);
            throw new RuntimeException("Cannot start application - Key Vault secrets are not accessible", e);
        }
    }
}

