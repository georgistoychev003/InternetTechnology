package client;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerInput implements Runnable{
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
        }
        if (!jsonPart.isEmpty()) {
            JsonNode responseNode = mapper.readTree(jsonPart);
            if ("BROADCAST".equals(command) && responseNode.has("username") && responseNode.has("message")) {
                String username = responseNode.get("username").asText();
                String message = responseNode.get("message").asText();
                System.out.println("User \"" + username + "\" sent you a message: \"" + message + "\"");
            } else if ("LOGIN_RESP".equals(command) || "BYE_RESP".equals(command)) {
                handleLoginLogoutResponse(command, responseNode);
            } else {
                System.out.println(command + " Response: " + jsonPart);
            }
        } else {
            System.out.println("Server: " + command);
        }
    }

    private void handleLoginLogoutResponse(String command, JsonNode responseNode) {
        String status = responseNode.get("status").asText();
        if ("OK".equals(status)) {
            if ("LOGIN_RESP".equals(command)) {
                System.out.println("You just logged in successfully. Welcome!");
            } else if ("BYE_RESP".equals(command)) {
                System.out.println("You have successfully logged out. Bye do not come back!");
            }
        } else {
            System.out.println("Action failed: " + responseNode.get("code").asText());
        }
    }
    private void sendToServer(String message) {
        output.println(message);
    }
}
