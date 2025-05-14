package config;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ConfigurationManager extends UnicastRemoteObject implements ConfigurationManagerRemote {
    private static ConfigurationManager instance;
    private DBConfig dbConfig;
    private LocalConfig localConfig;
    private UserConfig userConfig;
    private Environment currentEnvironment;

    private ConfigurationManager(Environment env) throws RemoteException {
        loadConfigurations(env);
    }

    public static synchronized ConfigurationManager getInstance(Environment env) throws RemoteException {
        if (instance == null) {
            instance = new ConfigurationManager(env);
        } else if (instance.currentEnvironment != env) {
            instance.loadConfigurations(env);
        }
        return instance;
    }

    public static synchronized ConfigurationManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ConfigurationManager is not initialized. Call getInstance(Environment) first.");
        }
        return instance;
    }

    private void loadConfigurations(Environment env) {
        this.currentEnvironment = env;
        this.dbConfig = new DBConfig(env);
        this.localConfig = new LocalConfig(env);
        this.userConfig = new UserConfig(env);
    }

    @Override
    public Environment getCurrentEnvironment() throws RemoteException {
        return currentEnvironment;
    }

    @Override
    public DBConfig getDbConfig() throws RemoteException {
        return dbConfig;
    }

    @Override
    public LocalConfig getLocalConfig() throws RemoteException {
        return localConfig;
    }

    @Override
    public UserConfig getUserConfig() throws RemoteException {
        return userConfig;
    }
}
