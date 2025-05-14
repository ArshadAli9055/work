package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class EnvironmentManager {
    private static final String ENV_PROPERTY = "app.env";
    private static final String CONFIG_FILE = "system.properties";

    public static Environment getCurrentEnvironment() {
        String env = getEnvironmentFromFile();

        if (env == null) {
            env = System.getProperty(ENV_PROPERTY);
        }
        if (env == null) {
            env = System.getenv(ENV_PROPERTY);
        }
        if (env != null) {
            return switch (env.toUpperCase()) {
                case "PRODUCTION" -> Environment.PRODUCTION;
                case "TESTING" -> Environment.TESTING;
                case "REMOTE" -> Environment.REMOTE;
                case "DEVELOPMENT" -> Environment.DEVELOPMENT;
                default -> Environment.DEVELOPMENT;
            };
        }
        return Environment.DEVELOPMENT;
    }

    private static String getEnvironmentFromFile() {
    Properties properties = new Properties();
    try (InputStream inputStream = EnvironmentManager.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
        if (inputStream != null) {
            properties.load(inputStream);
            return properties.getProperty(ENV_PROPERTY);
        } else {
            System.err.println("Could not find " + CONFIG_FILE + " in the classpath.");
        }
    } catch (IOException e) {
        System.err.println("Could not load system.properties: " + e.getMessage());
    }
    return null;
}

}