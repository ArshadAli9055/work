package ui;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ArabicNotepadClient extends Remote {
    void onRegisterClient(boolean result)throws RemoteException;
}
