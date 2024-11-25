package whisp.server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DBManager {
    private static final String DB_URL = "jdbc:postgresql://aws-0-eu-west-3.pooler.supabase.com:6543/postgres";
    private static final String DB_USER = "postgres.cdsycjhyadjvjrhdwtut";
    private static final String DB_PASSWORD = "UT3y6PVcnCx2TxgT";

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public boolean isUsernameTaken(String username){
        String query = "SELECT COUNT(*) FROM \"user\" WHERE username = ?";
        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println("Error checking username availability: " + e.getMessage());
        }

        return false;
    }

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

        } catch (SQLException e) {
            System.err.println("Error recovering friends for " + username);
        }
        return friends;
    }

    public boolean areFriends(String client1, String client2) {
        String query = "SELECT COUNT(*) FROM friendship WHERE (friend1 = ? AND friend2 = ?) OR (friend1 = ? AND friend2 = ?)";
        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, client1);
            stmt.setString(2, client2);
            stmt.setString(3, client2);
            stmt.setString(4, client1);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking friendship status between " + client1 + " and " + client2);
        }
        return false;
    }

    public void addFriend(String requestSender, String requestReceiver) {
        String query = "INSERT INTO friendship (friend1, friend2) VALUES (?, ?)";
        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, requestSender);
            stmt.setString(2, requestReceiver);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error adding friendship between " + requestSender + " and " + requestReceiver);
        }
    }

    public boolean checkLogin(String username, String password) {
        String query = "SELECT COUNT(*) FROM \"user\" WHERE username = ? AND password = ?";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking login for " + username + " " + e.getMessage());
        }
        return false;
    }

    public String getSalt (String username){
        String query = "SELECT salt FROM \"user\" WHERE username = ?";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("salt");
            }
        } catch (SQLException e) {
            System.err.println("Error checking login for " + username + " " + e.getMessage());
        }
        return null;
    }

    public void register(String username, String password, String authKey, String salt){
        String query = "INSERT INTO \"user\" (username, password, auth_key, salt) VALUES (?, ?, ?, ?)";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, authKey);
            stmt.setString(4, salt);

            stmt.executeUpdate();
            System.out.println("User registered successfully: " + username);

        } catch (SQLException e) {
            System.err.println("Error registering user: " + username + " - " + e.getMessage());
        }
    }

    public void changePassword(String username, String password, String salt){
        String query = "UPDATE \"user\" SET password = ?, salt = ? WHERE username = ?";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, password);
            stmt.setString(2, salt);
            stmt.setString(3, username);

            stmt.executeUpdate();
            System.out.println("Password successfully updated for user: " + username);

        } catch (SQLException e) {
            System.err.println("Error updating password for user: " + username + " - " + e.getMessage());
        }
    }

    public String getAuthKey(String username){
        String query = "SELECT auth_key FROM \"user\" WHERE username = ?";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString(1).trim();
            }
        } catch (SQLException e) {
            System.err.println("Error getting auth key for " + username + " " + e.getMessage());
        }
        return null;
    }

    public void addFriendRequest(String requestSender, String requestReceiver) {
        String query = "INSERT INTO pending_request (receiver_user, sender_user) VALUES (?, ?)";
        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, requestReceiver); // El usuario que recibe la solicitud
            stmt.setString(2, requestSender);  // El usuario que envía la solicitud

            stmt.executeUpdate();
            System.out.println("Friend request sent from " + requestSender + " to " + requestReceiver);

        } catch (SQLException e) {
            System.err.println("Error adding friend request from " + requestSender + " to " + requestReceiver + ": " + e.getMessage());
        }

    }

    public ArrayList<String> getFriendRequests(String requestReceiver) {
        ArrayList<String> requests = new ArrayList<>();
        String query = "SELECT sender_user FROM pending_request WHERE receiver_user = ?";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, requestReceiver);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                requests.add(rs.getString("sender_user"));
            }

        } catch (SQLException e) {
            System.err.println("Error getting friend requests for " + requestReceiver + ": " + e.getMessage());
        }
        return requests;
    }

    public void deleteFriendRequest(String requestSender, String requestReceiver) {
        String query = "DELETE FROM pending_request WHERE sender_user = ? AND receiver_user = ?";
        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, requestSender);
            stmt.setString(2, requestReceiver);

            stmt.executeUpdate();
            System.out.println("Friend request from " + requestSender + " to " + requestReceiver + " deleted");

        } catch (SQLException e) {
            System.err.println("Error deleting friend request from " + requestSender + " to " + requestReceiver + ": " + e.getMessage());
        }
    }


}
