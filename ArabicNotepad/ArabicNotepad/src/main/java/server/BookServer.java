package server;

import bl.BookFacadeImpl;
import common.RemoteBookFacadeImpl;
import config.ConfigurationManager;
import config.ConfigurationManagerRemote;
import config.Environment;
import config.EnvironmentManager;
import dao.BookDAOFactory;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BookServer {
    private static final Logger logger = LoggerFactory.getLogger(BookServer.class);
    
    public static void main(String[] args) {
        Environment env = EnvironmentManager.getCurrentEnvironment();
        ConfigurationManagerRemote configManager = null;
        try {
            configManager = ConfigurationManager.getInstance(env);
        } catch (RemoteException ex) {
            java.util.logging.Logger.getLogger(BookServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try {
            var bookDAO = BookDAOFactory.createBookDAO();
            var localFacade = new BookFacadeImpl(bookDAO);
            var remoteFacade = new RemoteBookFacadeImpl(localFacade);
            
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("ConfigurationManager", configManager);
            logger.info("Server started and ConfigurationManager bound to registry.");
            registry.rebind("RemoteBookFacade", remoteFacade);
            logger.info("Server started and RemoteBookFacade bound to registry.");
            System.out.println("Server started...");           
        } catch (RemoteException e) {
            System.err.println("Server error: " + e.getMessage());
             logger.error("An error occurred while starting the server.", e);
        }
    }
}
