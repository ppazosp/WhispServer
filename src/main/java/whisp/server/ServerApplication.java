package whisp.server;

import whisp.interfaces.ServerInterface;
import whisp.utils.SSLConfigurator;

import javax.net.ssl.SSLContext;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.io.*;
import java.nio.file.Paths;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerApplication {

    //*******************************************************************************************
    //* CONSTANTS
    //*******************************************************************************************

    private final static int SERVER_PORT = 1099;



    //*******************************************************************************************
    //* MAIN METHODS
    //*******************************************************************************************

    /**
     * Main del programa, define las propiedas a usar en JavaRMI, crea el Registry del servidor
     * y configura el contexto SSL.
     *
     * @param args argumentos por línea de comandos
     *  */
    public static void main(String[] args) {
        try {
            String filePath = Paths.get( "ips.conf").toString();
            String serverIp = readIpFromFile(filePath);
            System.out.println("Server IP: " + serverIp);

            System.setProperty("java.rmi.server.hostname", serverIp);
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
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Lee el archivo ips.conf y extrae las direcciones IP.
     *
     * @param filePath Ruta al archivo ips.conf.
     * @return Un array de cadenas con las IPs del servidor y del cliente.
     * @throws IOException Si ocurre un error al leer el archivo.
     */
    public static String readIpFromFile(String filePath) throws IOException {
        String serverIp = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("SERVER_IP")) {
                    serverIp = line.split("=")[1].trim();
                }
            }
        }

        if (serverIp == null) {
            throw new IOException("Missing SERVER_IP or CLIENT_IP in the file.");
        }

        return serverIp;
    }
}
