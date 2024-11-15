package whisp.interfaces;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote {
    void registerClient(ClientInterface client) throws RemoteException;
    boolean sendRequest(String requestSender, String requestReceiver) throws RemoteException;
    ClientInterface getClient(String username) throws RemoteException;
    void requestAcepted(String requestSender, String requestReceiver) throws RemoteException;
    boolean login(String username, String password) throws RemoteException;
    byte[] getSalt(String username) throws RemoteException;
}
