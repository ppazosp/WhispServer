package whisp.server;

import whisp.interfaces.ServerInterface;

import javax.net.ssl.SSLContext;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerApplication {

    private final static int SERVER_PORT = 1099;

    public static void main(String[] args) {
        try {
            System.setProperty("java.rmi.server.hostname", "localhost");
            SSLconfigurator sslConfigurator = new SSLconfigurator();
            sslConfigurator.genKeyCertificateServer();
            SSLContext sslContext = sslConfigurator.loadSSLContext("server.keystore", "password");

            SslRMIServerSocketFactory sslServerSocketFactory = new SslRMIServerSocketFactory(
                    sslContext, null, null, false);

            Registry registry = LocateRegistry.createRegistry(SERVER_PORT, null, sslServerSocketFactory);
            ServerInterface server = new Server();
            registry.rebind("MessagingServer", server);

            System.out.println("Server started, waiting for connections...");

        } catch (Exception e) {
            System.err.println("Error starting server");
            e.printStackTrace();
        }
    }
}
