package whisp.server;

import whisp.interfaces.ServerInterface;

import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerApplication {

    private final static int SERVER_PORT = 1099;

    public static void main(String[] args) {
        try {
            //TODO: conseguir la ip dinamicamnete
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            //imprime la host addres
            System.out.println("Host address: " + hostAddress);
            System.setProperty("java.rmi.server.hostname", hostAddress);
            Registry registry = LocateRegistry.createRegistry(SERVER_PORT);
            ServerInterface server = new Server();
            registry.rebind("MessagingServer", server);

            System.out.println("Server started, waiting for connections...");

        } catch (Exception e) {
            System.err.println("Error starting server");
        }
    }
}
