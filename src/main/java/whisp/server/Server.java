package whisp.server;

import whisp.interfaces.ClientInterface;
import whisp.interfaces.ServerInterface;

import java.io.Serializable;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
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
            for (Map.Entry<String, ClientInterface> clientEntry : clients.entrySet()) {
                try {
                    clientEntry.getValue().ping();
                } catch (RemoteException e) {
                   disconnectClient(clientEntry.getKey());
                }
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    //llamar a esta funcion ante cualquier RemoteException por perdidad de conexion con los clientes
    public void disconnectClient(String clientUsername ) {
        try {
            System.out.println(clientUsername + " disconnected");
            ClientInterface deadClient = clients.get(clientUsername);
            clients.remove(clientUsername);
            for (ClientInterface otherClient : clients.values()) {
                if(dbManager.areFriends(otherClient.getUsername(), clientUsername)) otherClient.disconnectClient(deadClient);
            }

        } catch (RemoteException ex) {
            System.err.println("Error notifying disconnection");
        }
    }

    //TODO: funcion para comprobar que un usuario esta conectado


    @Override
    public boolean sendRequest(String requestSender, String requestReceiver) throws RemoteException {
        //comprobar que el ususario está conectado y y enviarle la solicitud
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

}



