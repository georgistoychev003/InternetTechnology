package client;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import messages.GlobalMessage;
import messages.ResponseMessage;
import utils.Utility;

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
        String command = Utility.getResponseType(response);
        if (response.equals("PING")) {
            sendToServer("PONG");
        } else if (command.equals("WELCOME")) {
            System.out.println("Hey, you just established connection with the server!");
        } else {
            if ("BROADCAST".equals(command)) {
                GlobalMessage globalMessage = new GlobalMessage(response);
                System.out.println(globalMessage);
            } else if ("LOGIN_RESP".equals(command) || "BYE_RESP".equals(command)) {
                ResponseMessage responseMessage = new ResponseMessage(response);
                System.out.println(responseMessage);
            } else if ("BROADCAST_RESP".equals(command)) {
                ResponseMessage responseMessage = new ResponseMessage(response);
                System.out.println(responseMessage);
            } else if ("LEFT".equals(command)) {
                GlobalMessage globalMessage = new GlobalMessage(response);
                System.out.println(globalMessage);
            } else {
                System.out.println(command + " Response: " + response);
            }
        }
    }

    private void sendToServer(String message) {
        output.println(message);
    }
}
