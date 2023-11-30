package client;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerInput implements Runnable {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private ObjectMapper mapper;


    public ServerInput(Socket socket) throws IOException {
        this.socket = socket;
        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        output = new PrintWriter(socket.getOutputStream(), true);
        mapper = new ObjectMapper();
    }


    @Override
    public void run() {
        try {
            String serverResponse;
            while ((serverResponse = input.readLine()) != null) {
                handleServerResponse(serverResponse);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleServerResponse(String response) throws IOException {
        // Split the response into command and JSON parts based on spaces
        String[] parts = response.split(" ", 2);
        //extract the first part-the command
        String command = parts[0];
        //check if there are two parts. If so,assign the second part (after the space) to jsonPart.
        // If there is only one part, then there is no JSON content and therefore jsonPart is set to an empty string.
        String jsonPart = parts.length > 1 ? parts[1] : "";
        if (response.equals("PING")) {
            sendToServer("PONG");
        } else if (command.equals("WELCOME")) {
            System.out.println("Hey, you just established connection with the server!");
        } else if (!jsonPart.isEmpty()) {
            JsonNode responseNode = mapper.readTree(jsonPart);
            if ("BROADCAST".equals(command) && responseNode.has("username") && responseNode.has("message")) {
                String username = responseNode.get("username").asText();
                String message = responseNode.get("message").asText();
                System.out.println("User \"" + username + "\" sent you a message: \"" + message + "\"");
            } else if ("LOGIN_RESP".equals(command) || "BYE_RESP".equals(command)) {
                handleLoginLogoutResponse(command, responseNode);
            } else if ("BROADCAST_RESP".equals(command)) {
                handleBroadcastResponse(responseNode);

            } else if ("LEFT".equals(command) && responseNode.has("username")) {
                String username = responseNode.get("username").asText();
                System.out.println("A user with username \"" + username + "\" just left the chat/disconnected.");
            } else {
                System.out.println(command + " Response: " + jsonPart);
            }
        } else {
            System.out.println("Server: " + command);
        }
    }

    private void handleLoginLogoutResponse(String command, JsonNode responseNode) {
        String status = responseNode.get("status").asText();
        if (status.equals("OK")) {
            if (command.equals("LOGIN_RESP")) {
                System.out.println("You just logged in successfully. Welcome!");
            } else if (command.equals("BYE_RESP")) {
                System.out.println("You have successfully logged out. Bye!");
            }
        } else {
            int errorCode = responseNode.get("code").asInt();
            switch (errorCode) {
                case 5000:
                    System.out.println("User with the given username is already logged in.");
                    break;
                case 5001:
                    System.out.println("Sorry, the username you provided is of invalid format or length. Please try again with a different username.");
                    break;
                case 5002:
                    System.out.println("A user cannot login twice. You can logout and try to log in again!");
                    break;
                default:
                    System.out.println("Action failed with error code: " + errorCode);
                    break;
            }
        }
    }

    private void handleBroadcastResponse(JsonNode responseNode) {
        String status = responseNode.get("status").asText();
        if (status.equals("ERROR")) {
            int errorCode = responseNode.get("code").asInt();
            if (errorCode == 6000) {
                System.out.println("You tried to send a message without being logged in. Your message was not processed! Please log in and then try to send a broadcast message!");
            }
        } else {
            System.out.println("Broadcast message sent successfully.");
        }
    }

    private void sendToServer(String message) {
        output.println(message);
    }
}
