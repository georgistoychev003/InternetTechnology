package client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import messages.GameGuessMessage;
import messages.PrivateMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClientInput implements Runnable {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private Scanner scanner;
    private ObjectMapper mapper;
    private String username;

    public ClientInput(Socket socket) throws IOException {
        this.socket = socket;
        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        output = new PrintWriter(socket.getOutputStream(), true);
        scanner = new Scanner(System.in);
        mapper = new ObjectMapper();
        this.username = null;
    }

    private void handleUserInput() {
        while (true) {
            String userInput = scanner.nextLine();
            if (userInput.startsWith("login ")) {
                handleLoginCommand(userInput);
            } else if (userInput.startsWith("message ")) {
               handleMessageCommand(userInput);
            } else if (userInput.equalsIgnoreCase("list")) {
                sendListRequest();

            } else if (userInput.startsWith("private ")) {
                handlePrivateMessageCommand(userInput);
            } else if (userInput.startsWith("game create")) {
                handleGameCreation();
            } else if (userInput.startsWith("game join")) {
                handleGameJoin();
            } else if (userInput.startsWith("game guess ")) {
                handleGameGuess(userInput);
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


    private void handleLoginCommand(String userInput) {
        int beginIndex = "login ".length();
        String username = userInput.substring(beginIndex);
        sendLoginRequest(username);
    }

    private void handleMessageCommand(String userInput) {
        int beginIndex = "message ".length();
        String message = userInput.substring(beginIndex);
        sendBroadcastRequest(message);
    }

    private void handlePrivateMessageCommand(String userInput) {
        //split the input into three segments: the command ("private"), the target username, and the message itself
        //this is what we expect according to our protocol
        String[] parts = userInput.split(" ", 3);
        if (parts.length < 3) {
            System.out.println("Invalid command. Use: private <username> <message>");
            return;
        }
        //extracting rhe desired username and message
        String senderUsername = username;
        String receiverUsername = parts[1];
        String message = parts[2];
        sendPrivateMessageRequest(senderUsername, receiverUsername, message);
    }

    private void sendPrivateMessageRequest(String sender, String receiver, String message) {
        PrivateMessage privateMessage = new PrivateMessage("PRIVATE_MESSAGE_REQ", sender, receiver, message);
        sendToServer(privateMessage.getOverallData());
    }


    private void handleGameCreation() {
        sendToServer("GAME_CREATE_REQ", null);
    }

    private void handleGameJoin() {
        sendToServer("GAME_JOIN_REQ", null);
    }

    private void handleGameGuess(String input) {
        String[] parts = input.split(" ", 3);
        String number = parts[2];
        GameGuessMessage gameGuessMessage = new GameGuessMessage(number);
        sendToServer(gameGuessMessage.getOverallData());

    }

    private void showHelpMenu() {
        System.out.println("Commands:");
        System.out.println("login <username> - Login to the server");
        System.out.println("list - Show logged in users");
        System.out.println("message <message> - Send a broadcast message");
        System.out.println("private <username> <message> - Send a private message");
        System.out.println("game create - Creates a number guessing game");
        System.out.println("game join - Join the number guessing game");
        System.out.println("game guess <number> - Attempt to guess the secret number in the Guessing Game");
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

    private void sendToServer(String message) {
        output.println(message);
    }

    private void sendLoginRequest(String username) {
        ObjectNode loginNode = mapper.createObjectNode();
        loginNode.put("username", username);
        this.username = username;
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

    private void sendListRequest() {
        sendToServer("CLIENT_LIST_REQ", null);
    }

    @Override
    public void run() {
        handleUserInput();
    }
}
