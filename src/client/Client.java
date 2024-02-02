package client;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.security.KeyPairGenerator;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class Client {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 1337;
    private static final int FILE_TRANSFER_PORT = 1338;

    private Socket socket;
    private static HashMap<String, FileTransfer> fileTransfersMap = new HashMap<>();
    private Scanner scanner;
    private static PrivateKey privateKey;
    private static PublicKey publicKey;
    private static HashMap<String, String> pendingEncryptedMessages = new HashMap<>();
    private static HashMap<String, SecretKey> sessionHolder = new HashMap<>();

    public Client() {
        scanner = new Scanner(System.in);
        generateKeyPair();
    }

    public void run() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);


            ClientInput clientInput = new ClientInput(socket);
            ServerInput serverInput = new ServerInput(socket);
            Thread serverThread = new Thread(serverInput);
            Thread clientThread = new Thread(clientInput);

            serverThread.start();
            clientThread.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Client is shutting down. Logging out...");
                if (clientInput.isLoggedIn()) {
                    clientInput.sendLogoutRequest();
                }
            }));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addFileTransferRequest(FileTransfer fileTransfer) {
        fileTransfersMap.put(fileTransfer.getSender(), fileTransfer);
    }
    public static void storeEncryptedMessage(String recipient, String message) {
        pendingEncryptedMessages.put(recipient, message);
    }

    public String retrieveEncryptedMessage(String recipient) {
        return pendingEncryptedMessages.get(recipient);
    }

    public static void setUUIDToFileTransfer(String uuid) {

    }
    private void generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
    // Method to get the public key in a transferable format
    public static String getEncodedPublicKey() {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public static HashMap<String, FileTransfer> getFileTransfersMap() {
        return fileTransfersMap;
    }

    public static HashMap<String, SecretKey> getSessionHolder() {
        return sessionHolder;
    }

    public static void addSession(String username, SecretKey sessionKey) {
        sessionHolder.put(username, sessionKey);
    }

    public static PrivateKey getPrivateKey() {
        return privateKey;
    }

    public static HashMap<String, String> getPendingEncryptedMessages() {
        return pendingEncryptedMessages;
    }

    //TODO : ask if this method should exist
//    private void closeSockets() {
//        try {
//            if (output != null) output.close();
//            if (input != null) input.close();
//            if (socket != null) socket.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public static void main(String[] args) {
        new Client().run();
    }
}