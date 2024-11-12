package whisp.server;

import whisp.interfaces.ServerInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerApplication {

    static String registryURL;
    private final static int SERVER_PORT = 1099;


    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.createRegistry(SERVER_PORT);
            ServerInterface server = new Server();
            registry.rebind("MessagingServer", server);

            System.out.println("Server started, waiting for connections...");

            while (true) {
                Thread.sleep(5000);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
