package client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
    private ObjectMapper mapper;


    public ClientInput(Socket socket) throws IOException {
        this.socket = socket;
        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        output = new PrintWriter(socket.getOutputStream(), true);
        scanner = new Scanner(System.in);
        mapper = new ObjectMapper();
    }

    private void handleUserInput() {
        while (true) {
            String userInput = scanner.nextLine();
            if (userInput.startsWith("login ")) {
                // We subtract the command from the input
                int beginIndex = "login ".length();
                String username = userInput.substring(beginIndex);
                sendLoginRequest(username);
            } else if (userInput.startsWith("message ")) {
                int beginIndex = "message ".length();
                String message = userInput.substring(beginIndex);
                sendBroadcastRequest(message);
            } else if (userInput.equalsIgnoreCase("logout")) {
                sendLogoutRequest();
                break;
            } else if (userInput.equalsIgnoreCase("help")) {
                showHelpMenu();
            } else {
                System.out.println("Invalid command, please type 'help' for a list of commands.");
            }
        }
    }

    private void showHelpMenu() {
        System.out.println("Commands:");
        System.out.println("login <username> - Login to the server");
        System.out.println("message <message> - Send a broadcast message");
        System.out.println("logout - Logout from the server");
    }
    private void sendToServer(String command, ObjectNode node) {
        if (node != null) {
            String json = node.toString();
            output.println(command + " " + json);
        } else {
            output.println(command);
        }
    }
    private void sendLoginRequest(String username) {
        ObjectNode loginNode = mapper.createObjectNode();
        loginNode.put("username", username);
        sendToServer("LOGIN", loginNode);
    }

    private void sendBroadcastRequest(String message) {
        ObjectNode messageNode = mapper.createObjectNode();
        messageNode.put("message", message);
        sendToServer("BROADCAST_REQ", messageNode);
    }

    private void sendLogoutRequest() {
        sendToServer("BYE", null);
    }

    @Override
    public void run() {
        handleUserInput();
    }
}
