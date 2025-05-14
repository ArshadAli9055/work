package config;

import java.nio.file.Paths;
import util.ResourcePathResolver;

public class DBConfig extends BaseConfig {
    
    public DBConfig(Environment env) {
        super(getInternalPath(env), getExternalPath());
    }

    private static String getInternalPath(Environment env) {
        return ResourcePathResolver.getPath(env, "db.properties");
    }


    private static String getExternalPath() {
        return Paths.get(System.getenv("APPDATA"), "ArabicNotepad", "config", "db.properties").toString();
    }

    // To Add any DB-specific methods here when/if necessary later
}
