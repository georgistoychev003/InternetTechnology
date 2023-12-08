package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final int PORT = 1337;
    private ServerSocket serverSocket;
    private static ConcurrentHashMap<String, ClientHandler> loggedInUsers = new ConcurrentHashMap<>();
    public static final String VERSION = "1.5";  //ask gerralt if we really need a version

    public void startServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start(); // Handle each client connection in a new thread
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            stopServer();
        }
    }

    public void stopServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static boolean isLoggedIn(String username) {
        return loggedInUsers.containsKey(username);
    }

    public static void addLoggedInUser(String username, ClientHandler handler) {
        loggedInUsers.put(username, handler);
    }


    public static void removeLoggedInUser(String username) {
        loggedInUsers.remove(username);
    }


    public static void broadcastMessage(String command, String message, String senderUsername) {
        loggedInUsers.forEach((username, clientHandler) -> {
            if (!username.equals(senderUsername)) {
                clientHandler.sendMessage(command, message);
            }
        });
    }

    public static ClientHandler getClientHandlerByUsername(String username) {
        return loggedInUsers.get(username);
    }


    public static void main(String[] args) {
        Server server = new Server();
        server.startServer();
    }

    public static ConcurrentHashMap<String, ClientHandler> getLoggedInUsers() {
        return loggedInUsers;
    }
}

