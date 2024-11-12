package whisp.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote {
    void registerClient(ClientInterface client) throws RemoteException;
}
