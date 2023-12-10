package client;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import messages.*;
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
        if (response.startsWith("PING")) {
            sendToServer("PONG");
        } else if (command.equals("WELCOME")) {
            System.out.println("Hey, you just established connection with the server! To get an overview of all possible commands type 'help' in the console!");
        } else {
            switch (command) {
                case "JOINED" -> {
                    JoinedMessage joinedMessage = new JoinedMessage(response);
                    System.out.println(MessageHandler.determineMessagePrintContents(joinedMessage));
                }
                case "LEFT" -> {
                    LeftMessage leftMessage = new LeftMessage(response);
                    System.out.println(MessageHandler.determineMessagePrintContents(leftMessage));
                }
                case "BROADCAST" -> {
                    GlobalMessage globalMessage = new GlobalMessage(response);
                    System.out.println(MessageHandler.determineMessagePrintContents(globalMessage));
                }
                case "LOGIN_RESP", "BYE_RESP", "BROADCAST_RESP", "PRIVATE_MESSAGE_RESP" -> {
                    ResponseMessage responseMessage = new ResponseMessage(response);
                    System.out.println(ResponseHandler.determineResponseMessagePrint(responseMessage));
                }
                case "CLIENT_LIST_RESP" -> {
                    Message message = MessageHandler.handleConnectedClientsResponseStatus(response);
                    System.out.println(MessageHandler.handlePrintOfConnectedClients(message));
                }
                case "PRIVATE_MESSAGE" -> {
                    PrivateMessage privateMessage = new PrivateMessage(response);
                    System.out.println(MessageHandler.determineMessagePrintContents(privateMessage));
                }
                default -> System.out.println(command + " Response: " + response);
            }
        }
    }


    private void sendToServer(String message) {
        output.println(message);
    }
}
