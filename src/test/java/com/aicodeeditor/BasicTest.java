package com.aicodeeditor;

import com.aicodeeditor.core.PrivilegeManager;
import com.aicodeeditor.core.ConfigurationManager;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic tests for core functionality
 */
public class BasicTest {
    
    @Test
    public void testPrivilegeManager() {
        // Test that privilege manager can be instantiated
        assertDoesNotThrow(() -> {
            PrivilegeManager.hasAdminPrivileges();
        });
    }
    
    @Test
    public void testConfigurationManager() {
        // Test that configuration manager can be instantiated
        assertDoesNotThrow(() -> {
            ConfigurationManager config = ConfigurationManager.getInstance();
            assertNotNull(config);
        });
    }
    
    @Test
    public void testConfigurationDefaults() {
        ConfigurationManager config = ConfigurationManager.getInstance();
        config.initialize();
        
        // Test default values
        assertEquals("http://localhost:11434", config.getString("ollama.host", ""));
        assertEquals("llama3.2:7b", config.getString("ollama.model", ""));
        assertTrue(config.getBoolean("sandbox.enabled", false));
        assertTrue(config.getBoolean("web.enabled", false));
        assertTrue(config.getBoolean("dependencies.autoInstall", false));
    }
}
