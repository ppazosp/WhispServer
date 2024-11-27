package whisp.server;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import whisp.utils.Logger;
import whisp.interfaces.ClientInterface;
import whisp.interfaces.ServerInterface;
import whisp.utils.Encrypter;
import whisp.utils.TFAService;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Server extends UnicastRemoteObject implements ServerInterface, Serializable {

    //*******************************************************************************************
    //* ATTRIBUTES
    //*******************************************************************************************

    DBManager dbManager;
    HashMap<String, ClientInterface> clients = new HashMap<>();



    //*******************************************************************************************
    //* CONSTRUCTOR
    //*******************************************************************************************

    protected Server() throws RemoteException {
        super();
        dbManager = new DBManager();
        checkIfAlive();
    }



    //*******************************************************************************************
    //* OVERRIDE METHODS
    //*******************************************************************************************

    /**
     * Registra la conexion de un cliente.
     *
     * <p>
     *      Busca en la base de datos los amigos del cliente y llama la función {@link ClientInterface#receiveActiveClients(HashMap)}
     *      para enviarle de vuelta los que estén conectados.
     * </p>
     *
     * <p>
     *     Busca en la base de datos los solicitudes pendientes del cliente y llama a la funcón {@link ClientInterface#receiveRequests(List, List)}
     *     para enviarselas de vuelta.
     * </p>
     *
     * @param client cliente que se conecta
     * @throws RemoteException si ocurre un error de comunicación remota
     *  */
    @Override
    public void registerClient(ClientInterface client) throws RemoteException {
        Logger.info("Trying to register user...");

        Logger.info("Fetching friends...");
        List<String> clientFriendsList = dbManager.getFriends(client.getUsername());
        HashMap<String, ClientInterface> clientFriendHashMap = new HashMap<>();

        for (String friend : clientFriendsList) {
            if(clients.containsKey(friend)) {
                clientFriendHashMap.put(friend, clients.get(friend));
            }
        }

        Logger.info("Sending back friends connected...");
        try{
            client.receiveActiveClients(clientFriendHashMap);

        } catch (RemoteException e) {
            System.err.println("Error sending active clients");
        }

        Logger.info("Fetching requests...");
        List<String> clientReceivedRequestsList = dbManager.getReceivedFriendRequests(client.getUsername());
        List<String> clientSentRequestsList = dbManager.getSentFriendRequests(client.getUsername());

        Logger.info("Sending back requests...");
        try {
            client.receiveRequests(clientSentRequestsList, clientReceivedRequestsList);
        } catch (RemoteException e) {
            System.err.println("Error sending friend requests");
        }

        Logger.info("Sending connected info to other clients...");
        for (ClientInterface c : clients.values()) {
            if(dbManager.areFriends(c.getUsername(), client.getUsername())){
                c.receiveNewClient(client);
            }
        }

        Logger.info("Saving client reference on server...");
        clients.put(client.getUsername(), client);

        Logger.info(client.getUsername() + " connected successfully");
    }

    /**
     * Envía una solicitud de amistad de un usuario a otro pasando por la Base de Datos.
     * <p>
     *      Si el receptor de la solicitud está conectado, se le notifica.
     * </p>
     *
     *
     * @param requestSender el nombre del usuario que envía la solicitud.
     * @param requestReceiver el nombre del usuario que recibe la solicitud.
     * @throws RemoteException si ocurre un error remoto.
     */
    @Override
    public void sendRequest(String requestSender, String requestReceiver) throws RemoteException {
        Logger.info("Request received on server, saving it on database...");
        dbManager.addFriendRequest(requestSender, requestReceiver);

        if(clients.containsKey(requestReceiver)) {
            Logger.info("Sending request to user...");
            clients.get(requestReceiver).receiveFriendRequest(requestSender);
        }
    }

    /**
     * Obtiene la interfaz remota de un cliente conectado por su nombre de usuario.
     *
     * @param username el nombre del usuario del cliente.
     * @return la interfaz remota del cliente o null si no está conectado.
     * @throws RemoteException si ocurre un error remoto.
     */
    @Override
    public ClientInterface getClient(String username) throws RemoteException {
        return clients.get(username);
    }

    /**
     * Maneja la aceptación de una solicitud de amistad entre dos usuarios.
     *
     * <p>
     *     Elimina la solicitud de la Base de Datos y guarda la nueva relación de amistad.
     * </p>
     *
     * <p>
     *     Si el remitente está conectado, se le notifica con el nuevo cliente amigo.
     * </p>
     *
     * @param requestSender el nombre del usuario que envió la solicitud.
     * @param requestReceiver el nombre del usuario que aceptó la solicitud.
     * @throws RemoteException si ocurre un error remoto.
     */
    @Override
    public void requestAccepted(String requestSender, String requestReceiver) throws RemoteException {

        Logger.info("Accept received, adding friends row...");
        dbManager.addFriend(requestSender, requestReceiver);
        Logger.info(requestReceiver + requestReceiver + " are now friends");

        if(clients.containsKey(requestSender)) {
            Logger.info("Notifying sender...");
            clients.get(requestSender).receiveNewClient(clients.get(requestReceiver));
        }

        if(clients.containsKey(requestReceiver)) {
            Logger.info("Notifying receiver...");
            clients.get(requestReceiver).receiveNewClient(clients.get(requestSender));
        }


        Logger.info("Deleting request row...");
        dbManager.deleteFriendRequest(requestSender, requestReceiver);
    }

    /**
     * Cancela una solicitud de amistad enviada a un usuario.
     * Si el remitente de la solicitud está conectado, se le notifica.
     *
     * @param username el nombre del usuario que recibió la solicitud.
     * @param senderName el nombre del usuario que envió la solicitud.
     * @throws RemoteException si ocurre un error remoto.
     */
    @Override
    public void requestCancelled(String username, String senderName) throws RemoteException {

        dbManager.deleteFriendRequest(senderName, username);
        if(clients.containsKey(senderName)) {
            clients.get(senderName).receiveRequestCancelled(username);
        }
    }

    /**
     * Verifica las credenciales de inicio de sesión de un usuario.
     *
     * @param username el nombre del usuario.
     * @param password la contraseña del usuario.
     * @return {@code true} si las credenciales son correctas, {@code false} en caso contrario.
     * @throws RemoteException si ocurre un error remoto.
     */
    @Override
    public boolean login(String username, String password) throws RemoteException {
        Logger.info("Login credentials received, checking them...");
        return dbManager.checkLogin(username, password);
    }

    /**
     * Obtiene el salt de la Base de Datos asociada a un usuario para poder hashear su contraseña antes de enviarla.
     *
     * @param username el nombre del usuario.
     * @return el salt del usuario.
     * @throws RemoteException si ocurre un error remoto.
     */
    @Override
    public String getSalt(String username) throws RemoteException {
        return dbManager.getSalt(username);
    }

    /**
     * Verifica si un nombre de usuario está disponible (no hay otro igual) en la Base de Datos.
     *
     * @param username el nombre de usuario a verificar.
     * @return true si el nombre de usuario está disponible, false si ya está tomado.
     * @throws RemoteException si ocurre un error remoto.
     */
    @Override
    public boolean checkUsernameAvailability(String username) throws RemoteException {
        Logger.info("Username received, checking availability...");
        return dbManager.isUsernameTaken(username);
    }

    /**
     * Registra un nuevo usuario en la Base de Datos.
     *
     * <p>
     *     Antes de ello, genera también una clave de autenticación cifrada específica para el usuario.
     * </p>
     *
     * @param username el nombre del usuario.
     * @param password la contraseña del usuario.
     * @param salt el salt asociado al usuario.
     * @return un código QR para la autenticación de dos factores.
     * @throws RemoteException si ocurre un error remoto.
     */
    @Override
    public String register(String username, String password, String salt) throws RemoteException {

        Logger.info("Registration credentials received, registering...");

        Logger.info("Creating auth key...");
        String authKey = Encrypter.genAuthKey(username, salt);

        Logger.info("Saving credentials on database...");
        dbManager.register(username, password, authKey, salt);

        return TFAService.generateQRCode(authKey, username);
    }


    /**
     * Valida un código de autenticación para un usuario.
     *
     * @param username el nombre del usuario.
     * @param code el código de autenticación a validar.
     * @return {@code true} si el código es válido, {@code false} en caso contrario.
     * @throws RemoteException si ocurre un error remoto.
     */
    @Override
    public boolean validate(String username, int code) throws RemoteException {

        Logger.info("Validaton credentials received, fetching database for more...");
        String salt = dbManager.getSalt(username);
        String encryptedAuthKey = dbManager.getAuthKey(username);

        Logger.info("Decrypting authKey and checking validation...");
        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        String authKey = Encrypter.decrypt(encryptedAuthKey, Encrypter.getKey(username, salt));
        return gAuth.authorize(authKey, code);
    }

    /**
     * Cambia la contraseña de un usuario.
     *
     * @param username el nombre del usuario.
     * @param password la nueva contraseña del usuario.
     * @param salt el nuevo salt del usuario
     * @throws RemoteException si ocurre un error remoto.
     */
    @Override
    public void changePassword(String username, String password, String salt) throws RemoteException {
        Logger.info("New password change petition received, proceeding to complete it...");
        dbManager.changePassword(username, password, salt);
    }

    // CARGARME ESTA MIERDA
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

    /**
     * Desconecta un cliente del sistema.
     *
     * <p>
     *     Notifica a otros clientes que sean amigos del cliente desconectado.
     * </p>
     *
     * <p>
     *     Si ocurre un error al notificar otros clientes, los desconecta también de forma recursiva.
     * </p>
     *
     * <p>
     *     Es necesario que la función sea {@code synchronized} para que solo un hilo modifique el HashMap de
     *     clientes conectados a la vez.
     * </p>
     *
     * @param clientUsername el nombre del usuario del cliente a desconectar.
     */
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

}



