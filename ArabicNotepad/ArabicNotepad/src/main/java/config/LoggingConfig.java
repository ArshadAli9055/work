package config;

import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.ResourcePathResolver;


public class LoggingConfig extends BaseConfig {

    private static final String EXTERNAL_LOGGING_PROPERTIES = Paths.get(System.getenv("APPDATA"), "ArabicNotepad", "config", "logging.properties").toString();

    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    private static final Logger logger = LoggerFactory.getLogger(LoggingConfig.class);

   
    public LoggingConfig(Environment env) {
        super(getInternalPath(env), EXTERNAL_LOGGING_PROPERTIES);
        configureLogging();
    }

    private static String getInternalPath(Environment env) {
        return ResourcePathResolver.getPath(env, "logging.properties");
    }

    private void configureLogging() {
        String framework = getProperty("logging.framework");
        if ("logback".equalsIgnoreCase(framework)) {
            logger.info("Using Logback for logging.");
            // Logback automatically configures itself using logback.xml in the classpath
        } else {
            logger.warn("Unsupported logging framework specified: {}. Defaulting to Logback.", framework);   
        }
    }
}
