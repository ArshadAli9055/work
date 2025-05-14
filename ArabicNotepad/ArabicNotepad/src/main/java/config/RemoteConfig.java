package config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class RemoteConfig {
    private static final String REMOTE_PROPERTIES_FILE = "remote/remote.properties";
    private static final Logger logger = LoggerFactory.getLogger(RemoteConfig.class);

    private final Properties properties = new Properties();

    public RemoteConfig() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(REMOTE_PROPERTIES_FILE)) {
            if (input == null) {
                throw new IllegalArgumentException("Remote properties file not found: " + REMOTE_PROPERTIES_FILE);
            }
            properties.load(input);
            logger.info("Loaded remote properties from {}", REMOTE_PROPERTIES_FILE);
        } catch (IOException | IllegalArgumentException e) {
            logger.error("Failed to load remote properties", e);
            throw new RuntimeException("Critical error loading remote configuration", e); // Fail fast
        }
    }

    public String getRmiHost() {
        return properties.getProperty("rmi.host", "localhost");
    }

    public int getRmiPort() {
        return parsePort(properties.getProperty("rmi.port", "1099"));
    }

    private int parsePort(String port) {
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException e) {
            logger.warn("Invalid RMI port '{}', defaulting to 1099", port);
            return 1099;
        }
    }
}
