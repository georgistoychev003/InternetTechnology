package server;

import messages.GuessingGameInviteMessage;
import messages.Message;
import messages.ResponseMessage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final int PORT = 1337;
    private static final int FILE_TRANSFER_PORT = 1338;
    private ServerSocket serverSocket;
    private ServerSocket fileTransferServerSocket;
    private static ConcurrentHashMap<String, ClientHandler> loggedInUsers = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, String> publicKeys = new ConcurrentHashMap<>();
    public static final String VERSION = "1.0";

    public void startServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            fileTransferServerSocket = new ServerSocket(FILE_TRANSFER_PORT);
            System.out.println("Server started on port " + PORT);
            System.out.println("File Transfer Server started on port " + FILE_TRANSFER_PORT);

            // Separate thread for handling file transfer connections
            new Thread(this::handleFileTransferConnections).start();

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

    private void handleFileTransferConnections() {
        List<FileTransferHandler> connectedClients = new ArrayList<>();
        try {
            while (!fileTransferServerSocket.isClosed()) {
                Socket receiverSocket = fileTransferServerSocket.accept();
                Socket senderSocket = fileTransferServerSocket.accept();
                System.out.println("File transfer connection accepted 1: " + senderSocket.getInetAddress().getHostAddress());
                System.out.println("File transfer connection accepted 2: " + receiverSocket.getInetAddress().getHostAddress());
                System.out.println("File transfer connection accepted 1: " + senderSocket.getLocalPort());
                System.out.println("File transfer connection accepted 1: " + senderSocket.getInetAddress().getHostName());
                System.out.println("File transfer connection accepted 2: " + receiverSocket.getLocalPort());
                System.out.println("File transfer connection accepted 2: " + receiverSocket.getInetAddress().getHostName());
                FileTransferHandler fileTransferHandler = new FileTransferHandler(senderSocket, receiverSocket);
                new Thread(fileTransferHandler).start();
            }
        } catch (IOException e) {
            System.out.println("An error occurred in the file transfer server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stopServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (fileTransferServerSocket != null && !fileTransferServerSocket.isClosed()) {
                fileTransferServerSocket.close();
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


    public static void broadcastMessage(Message message, String senderUsername) {
        loggedInUsers.forEach((username, clientHandler) -> {
            if (!username.equals(senderUsername)) {
                clientHandler.sendMessage(message.getOverallData());
            }
        });
    }


    public static void broadcastGameInvite(String initiatorUsername) {
        GuessingGameInviteMessage inviteMessage = new GuessingGameInviteMessage(initiatorUsername);
        loggedInUsers.forEach((username, clientHandler) -> {
            if (!username.equals(initiatorUsername)) {
                clientHandler.sendMessage(inviteMessage.getOverallData());
            }
        });
    }


    public static void broadcastGameStart(String initiatorUsername, GuessingGame game, ResponseMessage message){
        game.getParticipants().forEach((username, clientHandler) -> {
            if (!username.equals(initiatorUsername)) {
                clientHandler.sendMessage(message.getOverallData());
            }
        });
    }
    public static void storePublicKey(String username, String publicKey) {
        publicKeys.put(username, publicKey);
    }

    public static String getPublicKey(String username) {
        return publicKeys.get(username);
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

