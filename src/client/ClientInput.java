package client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import messages.FileReceiveResponseMessage;
import messages.FileTransferREQMessage;
import messages.GameGuessMessage;
import messages.PrivateMessage;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.UUID;

public class ClientInput implements Runnable {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private Scanner scanner;
    private ObjectMapper mapper;
    private Socket fileTransferSocket;
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
            } else if(userInput.startsWith("transfer file ")){
                handleFileTransfer(userInput);
            } else if (userInput.startsWith("file receive response ")) {
                handleFileReceiveResponseCommand(userInput);
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
    private void handleFileTransfer(String userInput) {
        String[] parts = userInput.split(" ", 4);
        if (parts.length < 4) {
            System.out.println("Invalid command. Use: transfer file to <username> <fileName>");
            return;
        }

        String fileReceiverUsername = parts[2];
        String fileName = parts[3];

        sendFileTransferRequest(fileReceiverUsername, fileName);
    }

    private void handleFileReceiveResponseCommand(String userInput) {
        // Expecting input in the format: "file receive response <Yes/No>"
        String[] parts = userInput.split(" ", 4);
        if (parts.length < 4) {
            System.out.println("Invalid command. Use: file receive response <Yes/No>");
            return;
        }
        String response = parts[3].trim().toLowerCase();
        if (!response.equalsIgnoreCase("yes") && !response.equalsIgnoreCase("no")) {
            System.out.println("Invalid response. Please respond with 'Yes' or 'No'.");
            return;
        }
        String responseCode = response.equalsIgnoreCase("yes") ? "1" : "-1";
        sendFileReceiveResponse(responseCode);

        if (response.equalsIgnoreCase("yes")) {
            initiateFileTransfer();
        }
    }

    private void sendFileReceiveResponse(String responseCode) {
        FileReceiveResponseMessage responseMessage = new FileReceiveResponseMessage(responseCode);
        sendToServer(responseMessage.getOverallData());
    }

    private void initiateFileTransfer() {
        try {
            // Connect to the file transfer server
            fileTransferSocket = new Socket( "127.0.0.1", 1338);

            // Send the file content to the server
            sendFileContent();

            // Close the file transfer socket
            fileTransferSocket.close();
        } catch (IOException e) {
            System.err.println("Exception during file transfer initiation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendFileContent() {
        String filePath = "src/file.txt";
        try (FileInputStream fileInputStream = new FileInputStream(filePath);
             OutputStream outputStream = fileTransferSocket.getOutputStream()) {

            // Include sender/receiver indicator, UUID, and checksum in the file content
            outputStream.write('S');
            outputStream.write(UUID.randomUUID().toString().getBytes());
            String checksum = calculateMD5Checksum(filePath);
            System.out.println("Checksum: " + checksum);
            System.out.println("Checksum length: " + checksum.length());
            outputStream.write(checksum.getBytes());

            long bytesTransferred = fileInputStream.transferTo(fileTransferSocket.getOutputStream());

            System.out.println("File Transfer Complete. Bytes Transferred: " + bytesTransferred);
        } catch (IOException e) {
            System.err.println("Exception during file content transfer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String calculateMD5Checksum(String filePath) throws IOException {
        byte[] data = Files.readAllBytes(Paths.get(filePath));
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        String checksum = new BigInteger(1, hash).toString(16);
        return checksum;
    }

    private void sendFileTransferRequest(String receiver, String fileName) {
        FileTransferREQMessage fileTransferREQMessage = new FileTransferREQMessage("FILE_TRANSFER_REQ", receiver, fileName);
        sendToServer(fileTransferREQMessage.getOverallData());
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
        System.out.println("transfer file <username> <filename> - request to transfer a file to a user by providing the recepeint's username and a filename");
        System.out.println("file receive response <yes/no> - accept/decline a file that a user wants to transfer to you");
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

    protected void sendLogoutRequest() {
        if (username != null) {
            sendToServer("BYE", null);
        }
    }

    private void sendListRequest() {
        sendToServer("CLIENT_LIST_REQ", null);
    }
    public boolean isLoggedIn() {
        return username != null;
    }

    @Override
    public void run() {
        try {
            handleUserInput();
        } finally {
            sendLogoutRequest();
        }
    }


}
