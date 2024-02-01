package server;

import messages.GuessingGameInviteMessage;
import messages.Message;
import messages.ResponseMessage;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
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
        // Store the indicator, uuid and socket of each client that connects
        ConcurrentHashMap<Character, HashMap<String, Socket>> clients = new ConcurrentHashMap<>();
        clients.put('S', new HashMap<>());
        clients.put('R', new HashMap<>());
        try {
            while (!fileTransferServerSocket.isClosed()) {
                Socket clientSocket = fileTransferServerSocket.accept();
                DataInputStream dataInputStream1 = new DataInputStream(clientSocket.getInputStream());

                // Get the indicator and the uuid of the connected client
                char indicator = (char)dataInputStream1.readByte();
                byte[] uuidBytes = new byte[36];
                dataInputStream1.readFully(uuidBytes);
                String uuid = new String(uuidBytes);

                // Add connected client to client list
                HashMap<String, Socket> clientMap = clients.get(indicator);
                clientMap.put(uuid, clientSocket);
                clients.put(indicator, clientMap);

                // Get clients with opposite roles than the connected clients
                HashMap<String, Socket> otherClients = null;
                if (indicator == 'S') {
                    otherClients = clients.get('R');
                } else if (indicator == 'R') {
                    otherClients = clients.get('S');
                }

                // Try to find a matching pair of the client that connected to start the transfer between them
                if (otherClients != null) {
                    for (Map.Entry<String,Socket> entry : otherClients.entrySet()) {
                        if (entry.getKey().equals(uuid)){
                            System.out.println("Sender/Receiver match found with uuid: " +uuid);
                            FileTransferHandler fileTransferHandler = null;
                            if (indicator == 'S') {
                                fileTransferHandler = new FileTransferHandler(clientSocket, entry.getValue());
                            } else if (indicator == 'R') {
                                fileTransferHandler = new FileTransferHandler(entry.getValue(), clientSocket);
                            }
                            new Thread(fileTransferHandler).start();
                        }
                    }
                }

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

