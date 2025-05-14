package config;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ConfigurationManagerRemote extends Remote {
    Environment getCurrentEnvironment() throws RemoteException;
    DBConfig getDbConfig() throws RemoteException;
    LocalConfig getLocalConfig() throws RemoteException;
    UserConfig getUserConfig() throws RemoteException;
}
