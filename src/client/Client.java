package client;

import java.io.*;
import java.net.Socket;
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

    public Client() {
        scanner = new Scanner(System.in);
    }

    public void run() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);

            ServerInput serverInput = new ServerInput(socket);
            ClientInput clientInput = new ClientInput(socket);
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

    public static void setUUIDToFileTransfer(String uuid) {

    }

    public static HashMap<String, FileTransfer> getFileTransfersMap() {
        return fileTransfersMap;
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