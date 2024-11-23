package whisp.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface ServerInterface extends Remote {
    void registerClient(ClientInterface client) throws RemoteException;
    boolean sendRequest(String requestSender, String requestReceiver) throws RemoteException;
    ClientInterface getClient(String username) throws RemoteException;
    void requestAcepted(String requestSender, String requestReceiver) throws RemoteException;
    boolean login(String username, String password) throws RemoteException;
    String getSalt(String username) throws RemoteException;
    boolean checkUsernameAvailability(String username) throws RemoteException;
    String register(String username, String password, String salt) throws RemoteException;
    boolean validate(String username, int code) throws RemoteException;
    void changePassword(String username, String password, String salt) throws RemoteException;

}
