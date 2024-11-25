package whisp.server;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import whisp.Logger;
import whisp.interfaces.ClientInterface;
import whisp.interfaces.ServerInterface;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Server extends UnicastRemoteObject implements ServerInterface, Serializable {

    DBManager dbManager;
    HashMap<String, ClientInterface> clients = new HashMap<>();

    protected Server() throws RemoteException {
        super();
        dbManager = new DBManager();
        checkIfAlive();
    }

    @Override
    public void registerClient(ClientInterface client) throws RemoteException {
        List<String> clientFriendsList = dbManager.getFriends(client.getUsername());
        HashMap<String, ClientInterface> clientFriendHashMap = new HashMap<>();
        List<String> clientRequestsList = dbManager.getFriendRequests(client.getUsername());

        //imprimir la lista de amigos como un arraylist
        for (String friend : clientFriendsList) {
            System.out.println(friend);
        }

        for (String friend : clientFriendsList) {
            if(clients.containsKey(friend)) {
                clientFriendHashMap.put(friend, clients.get(friend));

            }
        }
        try{
            client.receiveActiveClients(clientFriendHashMap);

        } catch (RemoteException e) {
            System.err.println("Error sending active clients");
        }
        for (ClientInterface c : clients.values()) {
            if(dbManager.areFriends(c.getUsername(), client.getUsername())){
                c.receiveNewClient(client);
            }
        }
        //Solicitudes pendientes
        try {
            client.receiveBDrequests(clientRequestsList);
        } catch (RemoteException e) {
            System.err.println("Error sending friend requests");
        }

        //añadir el cliente a la lista de clientes
        clients.put(client.getUsername(), client);

        System.out.println(client.getUsername() + " connected");
    }

    public void checkIfAlive() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

        executor.scheduleAtFixedRate(() -> {
            try {
                Logger.info("Number of clients connected: " + clients.values().size());
                for (Map.Entry<String, ClientInterface> clientEntry : clients.entrySet()) {
                    try {
                        clientEntry.getValue().ping();
                    } catch (RemoteException e) {
                        disconnectClient(clientEntry.getKey());
                        break;
                    }
                }
            }catch (Exception e){
                Logger.error("Error en checkIfAlive");
                e.printStackTrace();
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    //llamar a esta funcion ante cualquier RemoteException por perdidad de conexion con los clientes
    public synchronized void disconnectClient(String clientUsername) {
        System.out.println(clientUsername + " disconnected");
        ClientInterface deadClient = clients.get(clientUsername);

        clients.remove(clientUsername);

        String extraClientToDisconnect = "";

        for (Map.Entry<String, ClientInterface> entry : clients.entrySet()) {
            ClientInterface otherClient = entry.getValue();
            String otherClientUsername = entry.getKey();

            try {
                if (dbManager.areFriends(otherClientUsername, clientUsername)) {
                    otherClient.disconnectClient(deadClient);
                }
            } catch (Exception e) {
                extraClientToDisconnect = otherClientUsername;
            }
        }

        if (!extraClientToDisconnect.isEmpty()){
            disconnectClient(extraClientToDisconnect);
        }
    }

    @Override
    public void sendRequest(String requestSender, String requestReceiver) throws RemoteException {
        dbManager.addFriendRequest(requestSender, requestReceiver);
        if(clients.containsKey(requestReceiver)) {
            clients.get(requestReceiver).receiveFriendRequest(requestSender);
        }
    }

    @Override
    public ClientInterface getClient(String username) throws RemoteException {
        return clients.get(username);
    }

    @Override
    public void requestAcepted(String requestSender, String requestReceiver) throws RemoteException {

        dbManager.addFriend(requestSender, requestReceiver);
        //si el cliente está conectado se le envía el cliente que ha aceptado la solicitud
        if(clients.containsKey(requestSender)) {
            clients.get(requestSender).receiveNewClient(clients.get(requestReceiver));
        }
        dbManager.deleteFriendRequest(requestSender, requestReceiver);
    }

    @Override
    public boolean login(String username, String password) throws RemoteException {
        return dbManager.checkLogin(username, password);
    }

    @Override
    public String getSalt(String username) throws RemoteException {
        return dbManager.getSalt(username);
    }

    @Override
    public boolean checkUsernameAvailability(String username) throws RemoteException {
        return dbManager.isUsernameTaken(username);
    }

    @Override
    public String register(String username, String password, String salt) throws RemoteException {
        try {
            String authKey = TFAService.generateSecretKey();
            String aesKey = Encrypter.getKey(new StringBuilder(username).reverse().toString(), salt);
            String encryptedAuthKey = Encrypter.encrypt(authKey, aesKey);

            dbManager.register(username, password, encryptedAuthKey, salt);

            return TFAService.generateQRCode(authKey, username);
        }catch (Exception e){
            Logger.error("Could not register " + username);
            e.printStackTrace();
        }

        return "";
    }

    @Override
    public boolean validate(String username, int code) throws RemoteException {
        try {
            GoogleAuthenticator gAuth = new GoogleAuthenticator();
            String salt = dbManager.getSalt(username);
            String encryptedAuthKey = dbManager.getAuthKey(username);

            String authKey = Encrypter.decrypt(encryptedAuthKey, Encrypter.getKey(new StringBuilder(username).reverse().toString(), salt));
            return gAuth.authorize(authKey, code);

        }catch (Exception e){
            Logger.error("Could not validate user " + username);
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public void changePassword(String username, String password, String salt) throws RemoteException {
        dbManager.changePassword(username, password, salt);
    }




}



