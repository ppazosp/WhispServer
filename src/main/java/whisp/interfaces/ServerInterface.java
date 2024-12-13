package whisp.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote {
    void registerClient(ClientInterface client) throws RemoteException;
    boolean sendRequest(String requestSender, String requestReceiver) throws RemoteException;
    ClientInterface getClient(String username) throws RemoteException;
    void requestAccepted(String requestSender, String requestReceiver) throws RemoteException;
    boolean login(String username, String password) throws RemoteException;
    String getSalt(String username) throws RemoteException;
    boolean checkUsernameAvailability(String username) throws RemoteException;
    String register(String username, String password, String salt) throws RemoteException;
    boolean validate(String username, int code) throws RemoteException;
    void changePassword(String username, String oldPassword, String newPassword, String salt) throws RemoteException;
    void requestCancelled(String username, String senderName) throws RemoteException;
    void checkClientStatus(String clientUsername) throws RemoteException;
    boolean areFriends (String friend1, String friend2) throws RemoteException;
}
