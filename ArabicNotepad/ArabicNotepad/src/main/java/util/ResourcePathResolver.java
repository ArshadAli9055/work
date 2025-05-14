package util;

import config.Environment;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourcePathResolver {
    private static final Logger logger = LoggerFactory.getLogger(ResourcePathResolver.class);
    
    public static String getPath(Environment env, String filename) {
        String fullPath = env.name().toLowerCase() + "/" + filename;
        
        try {
            URL resource = ResourcePathResolver.class.getClassLoader().getResource(fullPath);
            
            if (resource != null) {
                return fullPath;
            } else {
                System.err.println("Resource not found: " + fullPath);
                return null;
            }
        } catch (Exception e) {
            logger.error("Error: ResourcePathResolver: {}", e);
            return null;
        }
    }
}