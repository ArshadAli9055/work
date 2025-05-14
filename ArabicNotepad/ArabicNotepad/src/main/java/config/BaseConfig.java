package config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseConfig {
    protected Properties properties = new Properties();
    protected Logger logger = LoggerFactory.getLogger(this.getClass());
    protected String internalConfigPath;
    protected String externalConfigPath;


    public BaseConfig(String internalConfigPath, String externalConfigPath) {
        this.internalConfigPath = internalConfigPath;
        this.externalConfigPath = externalConfigPath;
        loadProperties();
    }


    private void loadProperties() {
        Environment env = EnvironmentManager.getCurrentEnvironment();
        if (env == Environment.PRODUCTION) {
            if (!loadFromExternal()) {
                if (loadFromInternal()) {
                    createExternal();
                }
            }
        } else {
            loadFromInternal();
        }
    }


    protected boolean loadFromExternal() {
        File externalFile = new File(externalConfigPath);
        if (externalFile.exists()) {
            try (InputStream input = new FileInputStream(externalFile)) {
                properties.load(input);
                logger.info("Loaded external properties from {}", externalConfigPath);
                return true;
            } catch (IOException e) {
                logger.error("Failed to load external properties from {}", externalConfigPath, e);
            }
        } else {
            logger.warn("External properties file {} does not exist.", externalConfigPath);
        }
        return false;
    }

    protected boolean loadFromInternal() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(internalConfigPath)) {
            if (input == null) {
                logger.error("Internal properties file {} not found.", internalConfigPath);
                return false;
            }
            properties.load(input);
            logger.info("Loaded internal properties from {}", internalConfigPath);
            return true;
        } catch (IOException e) {
            logger.error("Failed to load internal properties from {}", internalConfigPath, e);
            return false;
        }
    }

    protected void createExternal() {
        try {
            Path path = Paths.get(externalConfigPath);
            Files.createDirectories(path.getParent());

            try (OutputStream output = new FileOutputStream(externalConfigPath)) {
                properties.store(output, "");
                logger.info("Created external properties file at {}", externalConfigPath);
            }
        } catch (IOException e) {
            logger.error("Failed to create external properties file at {}", externalConfigPath, e);
        }
    }

  
    protected void saveExternal() {
        Environment env = EnvironmentManager.getCurrentEnvironment();
        if (env == Environment.PRODUCTION) {
            try (OutputStream output = new FileOutputStream(externalConfigPath)) {
                properties.store(output, "");
                logger.info("Saved properties to external file {}", externalConfigPath);
            } catch (IOException e) {
                logger.error("Failed to save properties to external file {}", externalConfigPath, e);
            }
        }
    }

  
    public String getProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            logger.error("Property '{}' not found. This is a critical error.", key);
            throw new IllegalStateException("Missing property: " + key);
        }
        return value;
    }

 
    public void setProperty(String key, String value) {
        if (EnvironmentManager.getCurrentEnvironment() == Environment.PRODUCTION && isReadOnlyKey(key)) {
            throw new UnsupportedOperationException("Cannot modify read-only property in production: " + key);
        }
        properties.setProperty(key, value);
        saveExternal();
    }

    private boolean isReadOnlyKey(String key) {
        return key.equals("storage.path"); // To make storage.path read-only in production. Just add more if need be
    }
}
