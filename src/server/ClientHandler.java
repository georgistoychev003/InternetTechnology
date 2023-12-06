package server;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONArray;
import org.json.JSONObject;


public class ClientHandler implements Runnable {

    private Socket clientSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private AtomicBoolean running;
    private AtomicBoolean receivedPong = new AtomicBoolean(false);

    private String username;


    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.running = new AtomicBoolean(true);
    }

    @Override
    public void run() {
        try {
            inputStream = clientSocket.getInputStream();
            outputStream = clientSocket.getOutputStream();

            startHeartbeat();


            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));

            sendMessage("WELCOME", "{\"msg\":\"Welcome to the server " + Server.VERSION + "\"}");

            String line;
            while (running.get() && (line = reader.readLine()) != null) {
                processClientMessage(writer, line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    private void processClientMessage(BufferedWriter writer, String message) throws IOException {
        try {
            String[] parts = message.split(" ", 2);
            String command = parts[0];
            String jsonPart = parts.length > 1 ? parts[1] : "";

            switch (command) {
                case "LOGIN":
                    handleLogin(jsonPart);
                    break;
                case "BROADCAST_REQ":
                    handleBroadcastRequest(jsonPart);
                    break;
                case "BYE":
                    handleLogout();
                    break;
                case "PONG":
                    receivedPong.set(true);
                    break;
                case "PING":
                    // This should not normally happen as server sends PING
                    sendMessage("PONG_ERROR", "{\"code\": 8000}");
                    break;
                case "CLIENT_LIST_REQ":
                    handleClientListRequest();
                    break;
                default:
                    sendMessage("UNKNOWN_COMMAND", "{}");
                    break;
            }
        } catch (Exception e) {
            sendMessage("PARSE_ERROR", "{}");
        }
    }

    private void handleLogin(String jsonPart) throws IOException {
        JSONObject jsonObj = new JSONObject(jsonPart);
        String attemptedUsername = jsonObj.getString("username");

        if (isUsernameValid(attemptedUsername) && isUsernameAvailable(attemptedUsername)) {
            this.username = attemptedUsername;
            // Add username to a global list of logged-in users
            Server.addLoggedInUser(username, this);
            sendMessage("LOGIN_RESP", "{\"status\":\"OK\"}");

            // Broadcast to all users that a new user has joined
            String joinMessage = String.format("{\"username\":\"%s\"}", username);
            Server.broadcastMessage("JOINED", joinMessage, this.username);

        } else {
            String errorCode = getLoginErrorCode(attemptedUsername);
            sendMessage("LOGIN_RESP", "{\"status\":\"ERROR\", \"code\":" + errorCode + "}");
        }
    }


    private void handleBroadcastRequest(String jsonPart) throws IOException {
        if (username == null) {
            // User is not logged in
            sendMessage("BROADCAST_RESP", "{\"status\":\"ERROR\", \"code\":6000}");
            return;
        }

        JSONObject jsonObj = new JSONObject(jsonPart);
        String message = jsonObj.getString("message");


        String broadcastMessage = String.format("{\"username\":\"%s\",\"message\":\"%s\"}", username, message);

        // Send broadcast message to all other clients, excluding the one who initiates it
        Server.broadcastMessage("BROADCAST", broadcastMessage, this.username);


        // Send confirmation back to the sender
        sendMessage("BROADCAST_RESP", "{\"status\":\"OK\"}");
    }


    private void handleLogout() {
        // Remove the user from the list of logged-in users
        Server.removeLoggedInUser(username);

        // Notify other clients that this user has left, excluding the user himself who joined
        Server.broadcastMessage("LEFT", "{\"username\":\"" + username + "\"}", this.username);

        // Set running to false so the loop in the run method will terminate
        running.set(false);

        // Close the connection
        closeConnection();
    }

    private void startHeartbeat() {
        new Thread(() -> {
            try {
                while (running.get()) {
                    Thread.sleep(10000); // Send ping every 10 seconds
                    receivedPong.set(false); // Reset the pong
                    sendMessage("PING", "{}");

                    // Wait for 3 seconds for PONG response
                    Thread.sleep(3000);

                    if (!receivedPong.get()) {
                        sendMessage("DSCN", "{\"reason\": 7000}");
                        closeConnection();
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }


    protected void sendMessage(String command, String message) {
        try {
            if (!clientSocket.isClosed()) {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
                writer.write(command + " " + message);
                writer.newLine();
                writer.flush();
            }
        } catch (IOException e) {
            System.err.println("Error sending message to client: " + e.getMessage());
            closeConnection();
        }
    }



    private boolean isUsernameValid(String username) {
        return username != null && username.matches("^[A-Za-z0-9_]{3,14}$");
    }

    private boolean isUsernameAvailable(String username) {
        // Check if username is already used
        return !Server.isLoggedIn(username);
    }

    private String getLoginErrorCode(String username) {
        if (!isUsernameValid(username)) {
            return "5001"; // Invalid format or length
        }
        if (!isUsernameAvailable(username)) {
            return "5000"; // User already logged in
        }
        return "5002"; // User cannot login twice
    }

    private void handleClientListRequest() throws IOException {
        if (username == null) {
            sendMessage("CLIENT_LIST_RESP", "{\"status\":\"ERROR\", \"code\":6000}");
            return;
        }

        String[] usersArray = Server.getLoggedInUsers().keySet().toArray(new String[0]);
        JSONObject response = new JSONObject();
        response.put("status", "OK");
        response.put("users", new JSONArray(usersArray));
        sendMessage("CLIENT_LIST_RESP", response.toString());
    }


    private void closeConnection() {
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (clientSocket != null) clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        Thread.currentThread().interrupt(); // terminate client thread
    }
    }

}
