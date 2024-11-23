package whisp.server;

import whisp.interfaces.ServerInterface;

import javax.net.ssl.SSLContext;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerApplication {

    private final static int SERVER_PORT = 1099;

    public static void main(String[] args) {
        try {
            System.setProperty("java.rmi.server.hostname", "localhost");
            System.setProperty("https.protocols", "TLSv1.2,TLSv1.3");
            System.setProperty("javax.rmi.ssl.server.enabledProtocols", "TLSv1.2,TLSv1.3");
            System.setProperty("javax.net.ssl.keyStore", "server.keystore");
            System.setProperty("javax.net.ssl.keyStorePassword", "password");
            System.setProperty("javax.net.ssl.trustStore", "server.truststore");
            System.setProperty("javax.net.ssl.trustStorePassword", "password");
            SSLConfigurator sslConfigurator = new SSLConfigurator();
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
        }
    }
}
