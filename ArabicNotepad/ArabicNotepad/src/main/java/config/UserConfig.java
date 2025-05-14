package config;

import java.nio.file.Paths;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.ResourcePathResolver;

public class UserConfig extends BaseConfig {
    
    private static final String EXTERNAL_USER_PROPERTIES = Paths.get(System.getenv("APPDATA"), "ArabicNotepad", "config", "user.properties").toString();
    private static final String USERID_KEY = "userid";
    private static final Logger logger = LoggerFactory.getLogger(UserConfig.class);

  
    public UserConfig(Environment env) {
        super(getInternalPath(env), EXTERNAL_USER_PROPERTIES);
        ensureUserId();
    }

 
    private static String getInternalPath(Environment env) {
        return ResourcePathResolver.getPath(env, "user.properties");
    }


    private void ensureUserId() {
        String userId = getProperty(USERID_KEY);
        if (userId == null || userId.isEmpty()) {
            userId = generateUserId();
            setProperty(USERID_KEY, userId);
            logger.info("Generated new User ID: {}", userId);
        } else {
            logger.info("Existing User ID found: {}", userId);
        }
    }

  
    private String generateUserId() {
        return UUID.randomUUID().toString();
    }

    public String getUserId() {
        return getProperty(USERID_KEY);
    }
}
