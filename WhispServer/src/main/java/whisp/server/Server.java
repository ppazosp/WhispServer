package whisp.server;

import whisp.server.ClientInterface;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

public class Server implements ServerInterface {
    private static final int RMIPortnumber = 1099;
    static String registryURL;
    private final static int SERVER_PORT = 8888;

    List<ClientInterface> clients = new ArrayList<ClientInterface>();

    @Override
    public void registerClient(ClientInterface client) throws RemoteException {
        clients.add(client);

        System.out.println("Registered Client: " + client);
    }
}

