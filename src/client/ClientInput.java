package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClientInput implements Runnable{
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private Scanner scanner;


    public ClientInput(Socket socket) throws IOException {
        this.socket = socket;
        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        output = new PrintWriter(socket.getOutputStream(), true);
        scanner = new Scanner(System.in);
    }

    private void handleUserInput() {
        while (true) {
            String helpOptions = scanner.nextLine();
            if (helpOptions.startsWith("login ")) {
                String username = helpOptions.substring(6);
                sendToServer("LOGIN {\"username\":\"" + username + "\"}");
            } else if (helpOptions.startsWith("message ")) {
                String message = helpOptions.substring(8);
                sendToServer("BROADCAST_REQ {\"message\":\"" + message + "\"}");
            } else if (helpOptions.equalsIgnoreCase("logout")) {
                sendToServer("BYE");
                break;
            } else if (helpOptions.equalsIgnoreCase("help")) {
                showHelpMenu();
            } else {
                System.out.println("This command is not valid, please type help to get the valid list of commands!");
            }
        }
    }

    private void showHelpMenu() {
        System.out.println("Commands:");
        System.out.println("login <username> - Login to the server");
        System.out.println("<message> - Send a broadcast message");
        System.out.println("<logout> - Logout from the server");
    }
    private void sendToServer(String message) {
        output.println(message);
    }

    @Override
    public void run() {
        handleUserInput();
        try {
            String clientResponse;
            while ((clientResponse = input.readLine()).contains("BROADCAST")) {
                System.out.println("Client: " + clientResponse);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
