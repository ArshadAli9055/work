package dao;

import config.ConfigurationManager;
import config.DBConfig;
import config.Environment;
import config.EnvironmentManager;
import java.rmi.RemoteException;

public class BookDAOFactory {

    private static final DBConfig dbConfig;
    
    static {
        try {
            Environment env = EnvironmentManager.getCurrentEnvironment();
            dbConfig = ConfigurationManager.getInstance(env).getDbConfig();
        } catch (RemoteException e) {
            throw new RuntimeException("Error initializing ConfigurationManager", e);
        }
    }

    public static BookDAO createBookDAO() {
        String dbType = dbConfig.getProperty("type");
        if ("mysql".equalsIgnoreCase(dbType)) {
            return new MySQLBookDAO(dbConfig);
        } else if ("mongodb".equalsIgnoreCase(dbType)) {
            // return new MongoDBBookDAO(dbConfig); 
            throw new UnsupportedOperationException("MongoDB support is not implemented yet.");
        }
        else if("test".equalsIgnoreCase(dbType))
        {
            return new InMemoryBookDAO();
        }
        else {
            throw new UnsupportedOperationException("Unsupported database type: " + dbType);
        }
    }
}
