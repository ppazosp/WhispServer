package whisp.server;

import whisp.Logger;
import whisp.interfaces.ClientInterface;
import whisp.interfaces.ServerInterface;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public boolean sendRequest(String requestSender, String requestReceiver) throws RemoteException {
        if(clients.containsKey(requestReceiver)) {
            clients.get(requestReceiver).receiveFriendRequest(requestSender);
            return true;
        }
        return false;
    }

    @Override
    public ClientInterface getClient(String username) throws RemoteException {
        return clients.get(username);
    }

    @Override
    public void requestAcepted(String requestSender, String requestReceiver) {
        dbManager.addFriend(requestSender, requestReceiver);
        try {
            clients.get(requestSender).receiveNewClient(clients.get(requestReceiver));
        } catch (RemoteException e) {
            System.err.println("Error sending new friend notification");
        }
    }

    @Override
    public boolean login(String username, String password) throws RemoteException {
        return dbManager.checkLogin(username, password);
    }

    @Override
    public byte[] getSalt(String username) throws RemoteException {
        return dbManager.getSalt(username);
    }

    @Override
    public String register(String username, String password, String salt) throws RemoteException {
        try {
            String authKey = TFAService.generateSecretKey();
            String hashedAuthKey = Encrypter.getHashedPassword(authKey, Base64.getDecoder().decode(salt));

            dbManager.register(username, password, hashedAuthKey, salt);

            return TFAService.generateQRCode(authKey, username);
        }catch (Exception e){
            Logger.error("Could not register " + username);
        }

        return "";
    }

    @Override
    public void changePassword(String username, String password, String salt) throws RemoteException {
        dbManager.changePassword(username, password, salt);
    }


}



