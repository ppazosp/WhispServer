package whisp.server;

import org.apache.commons.logging.Log;
import whisp.utils.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DBManager {

    //*******************************************************************************************
    //* ATTRIBUTES
    //*******************************************************************************************

    private static final String DB_URL = "x";
    private static final String DB_USER = "x";
    private static final String DB_PASSWORD = "x";

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    //*******************************************************************************************
    //* QUERY METHODS
    //*******************************************************************************************

    /**
     * Verifica si un nombre de usuario ya está en uso en la base de datos.
     *
     * @param username el nombre de usuario a verificar.
     * @return {@code true} si el nombre de usuario ya está tomado, {@code false} en caso contrario.
     */
    public boolean isUsernameTaken(String username){

        String query = "SELECT COUNT(*) FROM \"user\" WHERE username = ?";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);

            ResultSet rs = stmt.executeQuery();

            boolean val = false;

            if (rs.next()){
                val = rs.getInt(1) > 0;
            }

            Logger.info("Query completed correctly");

            return val;

        } catch (SQLException e) {
            Logger.error("Check database connection");
            throw new IllegalStateException("Stop using Eduroam", e);
        }
    }

    /**
     * Obtiene de la base de datos la lista de amigos de un usuario específico.
     *
     * @param username el nombre de usuario para el que se buscan los amigos.
     * @return un objeto {@link List} con los nombres de los amigos del usuario.
     */
    public List<String> getFriends(String username) {

        ArrayList<String> friends = new ArrayList<>();

        String query =
                "SELECT friend2 AS friend_name FROM friendship WHERE friend1 = ? " +
                "UNION " +
                "SELECT friend1 AS friend_name FROM friendship WHERE friend2 = ?";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, username);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String friendName = rs.getString("friend_name");
                friends.add(friendName);
            }

            Logger.info("Query completed correctly");

        } catch (SQLException e) {
            Logger.error("Check database connection");
            throw new IllegalStateException("Stop using Eduroam", e);
        }
        return friends;
    }

    /**
     * Verifica si dos usuarios son amigos en la base de datos.
     *
     * @param client1 el nombre del primer usuario.
     * @param client2 el nombre del segundo usuario.
     * @return {@code true} si los usuarios son amigos, {@code false} en caso contrario.
     */
    public boolean areFriends(String client1, String client2) {

        String query = "SELECT COUNT(*) FROM friendship WHERE (friend1 = ? AND friend2 = ?) OR (friend1 = ? AND friend2 = ?)";
        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, client1);
            stmt.setString(2, client2);
            stmt.setString(3, client2);
            stmt.setString(4, client1);

            ResultSet rs = stmt.executeQuery();

            boolean val = false;

            if(rs.next()){
                val =rs.getInt(1) > 0;
            }
            Logger.info("Query completed correctly");

            return val;

        } catch (SQLException e) {
            Logger.error("Check database connection");
            throw new IllegalStateException("Stop using Eduroam", e);
        }
    }

    /**
     * Añade una nueva amistad entre dos usuarios a la base de datos.
     *
     * @param requestSender el usuario que envió la solicitud de amistad.
     * @param requestReceiver el usuario que aceptó la solicitud de amistad.
     */
    public void addFriend(String requestSender, String requestReceiver) {

        String query = "INSERT INTO friendship (friend1, friend2) VALUES (?, ?)";
        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, requestSender);
            stmt.setString(2, requestReceiver);
            stmt.executeUpdate();

            Logger.info("Query completed correctly");

        } catch (SQLException e) {
            Logger.error("Check database connection");
            throw new IllegalStateException("Stop using Eduroam", e);
        }
    }

    /**
     * Verifica las credenciales de inicio de sesión de un usuario en la base de datos.
     *
     * @param username el nombre de usuario.
     * @param password la contraseña del usuario.
     * @return {@code true} si las credenciales son correctas, {@code false} en caso contrario.
     */
    public boolean checkLogin(String username, String password) {

        String query = "SELECT COUNT(*) FROM \"user\" WHERE username = ? AND password = ?";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();

            boolean val = false;
            if(rs.next()) {
                val =  rs.getInt(1) > 0;
            }

            Logger.info("Query completed correctly");

            return val;

        } catch (SQLException e) {
            Logger.error("Check database connection");
            throw new IllegalStateException("Stop using Eduroam", e);
        }
    }

    /**
     * Obtiene de la base de datos el salt asociado a un nombre de usuario.
     *
     * @param username el nombre de usuario.
     * @return el salt asociada al usuario, o null si no se encuentra.
     */
    public String getSalt (String username){

        String query = "SELECT salt FROM \"user\" WHERE username = ?";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);

            ResultSet rs = stmt.executeQuery();

            String salt = "";

            if(rs.next()){
                salt = rs.getString("salt");
            }

            Logger.info("Query completed correctly");

            return salt;

        } catch (SQLException e) {
            Logger.error("Check database connection");
            throw new IllegalStateException("Stop using Eduroam", e);
        }
    }

    /**
     * Registra un nuevo usuario en la base de datos con su información de autenticación.
     *
     * @param username el nombre del usuario.
     * @param password la contraseña del usuario.
     * @param authKey la clave de autenticación generada para el usuario.
     * @param salt la sal asociada al usuario.
     */
    public void register(String username, String password, String authKey, String salt){

        String query = "INSERT INTO \"user\" (username, password, auth_key, salt) VALUES (?, ?, ?, ?)";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, authKey);
            stmt.setString(4, salt);

            stmt.executeUpdate();

            Logger.info("Query completed correctly");

        } catch (SQLException e) {
            Logger.error("Check database connection");
            throw new IllegalStateException("Stop using Eduroam", e);
        }
    }

    /**
     * Cambia la contraseña de un usuario y actualiza su salt asociado en la base de datos.
     *
     * @param username el nombre del usuario.
     * @param oldPassword la vieja contraseña
     * @param newPassword la nueva contraseña.
     * @param salt la nueva sal asociada.
     */
    public void changePassword(String username, String oldPassword, String newPassword, String salt){

        String query = "UPDATE \"user\" SET password = ?, salt = ? WHERE username = ? and password = ?";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, oldPassword);
            stmt.setString(2, salt);
            stmt.setString(3, username);
            stmt.setString(4, newPassword);

            stmt.executeUpdate();

            Logger.info("Query completed correctly");

        } catch (SQLException e) {
            Logger.error("Check database connection");
            throw new IllegalStateException("Stop using Eduroam", e);
        }
    }

    /**
     * Obtiene de la base de datos la clave de autenticación asociada a un nombre de usuario.
     *
     * @param username el nombre de usuario.
     * @return la clave de autenticación, o null si no se encuentra.
     */
    public String getAuthKey(String username){

        String query = "SELECT auth_key FROM \"user\" WHERE username = ?";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);

            ResultSet rs = stmt.executeQuery();

            String authKey = "";

            if(rs.next()){
                authKey = rs.getString(1).trim();
            }

            Logger.info("Query completed correctly");

            return authKey;

        } catch (SQLException e) {
            Logger.error("Check database connection");
            throw new IllegalStateException("Stop using Eduroam", e);
        }
    }

    /**
     * Añade una nueva solicitud de amistad a la base de datos.
     *
     * @param requestSender el nombre del usuario que envía la solicitud.
     * @param requestReceiver el nombre del usuario que recibe la solicitud.
     */
    public void addFriendRequest(String requestSender, String requestReceiver) {

        String query = "INSERT INTO pending_request (receiver_user, sender_user) VALUES (?, ?)";
        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, requestReceiver); // El usuario que recibe la solicitud
            stmt.setString(2, requestSender);  // El usuario que envía la solicitud

            stmt.executeUpdate();

            Logger.info("Query completed correctly");

        } catch (SQLException e) {
            Logger.error("Check database connection");
            throw new IllegalStateException("Stop using Eduroam", e);
        }

    }

    /**
     * Obtiene todas las solicitudes de amistad recibidas pendientes en la base de datos para un usuario.
     *
     * @param requestReceiver el nombre del usuario que recibe las solicitudes.
     * @return una lista de nombres de los usuarios que han enviado las solicitudes de amistad.
     */
    public ArrayList<String> getReceivedFriendRequests(String requestReceiver) {

        ArrayList<String> requests = new ArrayList<>();
        String query = "SELECT sender_user FROM pending_request WHERE receiver_user = ?";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, requestReceiver);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                requests.add(rs.getString("sender_user"));
            }

            Logger.info("Query completed correctly");

        } catch (SQLException e) {
            Logger.error("Check database connection");
            throw new IllegalStateException("Stop using Eduroam", e);
        }
        return requests;
    }

    /**
     * Obtiene todas las solicitudes de amistad enviadas pendientes en la base de datos para un usuario.
     *
     * @param requestReceiver el nombre del usuario que recibe las solicitudes.
     * @return una lista de nombres de los usuarios que han recibido las solicitudes de amistad.
     */
    public ArrayList<String> getSentFriendRequests(String requestReceiver) {

        ArrayList<String> requests = new ArrayList<>();
        String query = "SELECT sender_user FROM pending_request WHERE sender_user = ?";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, requestReceiver);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                requests.add(rs.getString("sender_user"));
            }

            Logger.info("Query completed correctly");

        } catch (SQLException e) {
            Logger.error("Check database connection");
            throw new IllegalStateException("Stop using Eduroam", e);
        }
        return requests;
    }

    /**
     * Elimina una solicitud de amistad específica de la base de datos.
     *
     * @param requestSender el nombre del usuario que envió la solicitud.
     * @param requestReceiver el nombre del usuario que recibió la solicitud.
     */
    public void deleteFriendRequest(String requestSender, String requestReceiver) {

        String query = "DELETE FROM pending_request WHERE sender_user = ? AND receiver_user = ?";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, requestSender);
            stmt.setString(2, requestReceiver);

            stmt.executeUpdate();

            Logger.info("Query completed correctly");

        } catch (SQLException e) {
            Logger.error("Check database connection");
            throw new IllegalStateException("Stop using Eduroam", e);
        }
    }

}
