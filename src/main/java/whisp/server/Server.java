package whisp.server;

import whisp.interfaces.ClientInterface;
import whisp.interfaces.ServerInterface;

import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Server extends UnicastRemoteObject implements ServerInterface {

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
        client.receiveActiveClients(clientFriendHashMap);
        for (ClientInterface c : clients.values()) {
            if(dbManager.areFriends(c.getUsername(), client.getUsername())) c.receiveNewClient(client);
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
            //stack
        }
    }
}

