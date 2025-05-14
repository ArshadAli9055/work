package config;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.ResourcePathResolver;


public class LocalConfig extends BaseConfig {
    
    private static final String EXTERNAL_LOCAL_PROPERTIES = Paths.get(System.getenv("APPDATA"), "ArabicNotepad", "config", "local.properties").toString();
    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    private static final Logger logger = LoggerFactory.getLogger(LocalConfig.class);

 
    public LocalConfig(Environment env) {
        super(getInternalPath(env), EXTERNAL_LOCAL_PROPERTIES);
        setDefaultsIfNecessary(env);
        createStorageDirectory();
    }
    
   
    private static String getInternalPath(Environment env) {
        return ResourcePathResolver.getPath(env, "local.properties");
    }
    
    private void setDefaultsIfNecessary(Environment env) {
        if (env == Environment.PRODUCTION) {
            if (getProperty("current.path") == null || getProperty("current.path").equals("null")) {
                setProperty("current.path", "null");
            }
            if (getProperty("storage.path") == null || getProperty("storage.path").isEmpty()) {
                String oneDrivePath = detectOneDrivePath();
                if (oneDrivePath != null) {
                    setProperty("storage.path", oneDrivePath);
                } else {
                    setProperty("storage.path", getDefaultStoragePath());
                }
            }
        }
        // For other environments, I just need to retain existing or use internal defaults
    }

 
    private String detectOneDrivePath() {
        String userHome = System.getProperty("user.home");
        Path oneDrivePath = Paths.get(userHome, "OneDrive", "Documents", "ArabicNotepad", "Exported");
        File oneDriveDir = oneDrivePath.toFile();
        if (oneDriveDir.exists() && oneDriveDir.isDirectory()) {
            logger.info("OneDrive path detected: {}", oneDriveDir.getAbsolutePath());
            return oneDriveDir.getAbsolutePath();
        }
        return null;
    }


    private String getDefaultStoragePath() {
        return Paths.get(System.getProperty("user.home"), "Documents", "ArabicNotepad", "Exported").toString();
    }

    public String getStoragePath() {
        return getProperty("storage.path");
    }

    public String getCurrentPath() {
        return getProperty("current.path");
    }

    public void setCurrentPath(String path) {
        setProperty("current.path", path);
    }


    public final void createStorageDirectory() {
        String storagePath = getStoragePath();
        if (storagePath == null || storagePath.equals("null")) {
            logger.warn("Storage path is not set. Skipping creation.");
            return;
        }
        Path storageDirPath = Paths.get(storagePath);
        File storageDir = storageDirPath.toFile();
        if (!storageDir.exists()) {
            if (storageDir.mkdirs()) {
                logger.info("Created storage directory at {}", storageDir.getAbsolutePath());
            } else {
                logger.error("Failed to create storage directory at {}", storageDir.getAbsolutePath());
            }
        } else {
            logger.info("Storage directory already exists at {}", storageDir.getAbsolutePath());
        }
    }
      
}
