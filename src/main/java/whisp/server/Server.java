package whisp.server;

import whisp.interfaces.ClientInterface;
import whisp.interfaces.ServerInterface;

import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class Server extends UnicastRemoteObject implements ServerInterface {
    private static final int RMIPortnumber = 1099;
    static String registryURL;
    private final static int SERVER_PORT = 8888;

    List<ClientInterface> clients = new ArrayList<ClientInterface>();

    protected Server() throws RemoteException {
    }

    @Override
    public void registerClient(ClientInterface client) throws RemoteException {
        clients.add(client);

        System.out.println("Registered Client: " + client);
    }
}

