package client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 1337;

    private Socket socket;
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



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