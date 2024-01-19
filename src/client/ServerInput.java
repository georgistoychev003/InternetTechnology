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
import java.net.SocketException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.databind.type.LogicalType.Map;

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
        } catch (SocketException e) {
            System.out.println("Connection to server lost.");
            // we may ask Gerralt if we have to try and reconnect again if the server reruns
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeResources();
        }
    }


    private void handleServerResponse(String response) throws IOException {
        String command = Utility.getResponseType(response);
        String[] responseParts = response.split(" ", 2);
        String body = (responseParts.length > 1) ? responseParts[1] : "";
        if (response.startsWith("PING")) {
            sendToServer("PONG");
        } else if (command.equals("WELCOME")) {
            System.out.println("Hey, you just established connection with the server! To get an overview of all possible commands type 'help' in the console!");
        } else {
            switch (command) {
                case "JOINED" -> {
                    String joinedUsername = Utility.extractParameterFromJson(response, "username");
                    JoinedMessage joinedMessage = new JoinedMessage(joinedUsername);
//                    JoinedMessage joinedMessage = mapper.readValue(body, JoinedMessage.class);
                    System.out.println(MessageHandler.determineMessagePrintContents(joinedMessage));
                }
                case "LEFT" -> {
                    String leftUsername = Utility.extractParameterFromJson(response, "username");
                    LeftMessage leftMessage = new LeftMessage(leftUsername);
//                    LeftMessage leftMessage = mapper.readValue(body, LeftMessage.class);
                    System.out.println(MessageHandler.determineMessagePrintContents(leftMessage));
                }
                case "BROADCAST" -> {
                  //  String body = response.split(" ", 2)[1]; // FIXME: Dont do this for empty bodies
//                    ChatMessage message = mapper.readValue(body, ChatMessage.class);
                    String broadcastUsername = Utility.extractParameterFromJson(response, "username");
                    String broadcastMessage = Utility.extractParameterFromJson(response, "message");
                    GlobalMessage globalMessage = new GlobalMessage(Utility.getResponseType(response), broadcastUsername, broadcastMessage);
//                    GlobalMessage globalMessage = mapper.readValue(body, GlobalMessage.class);
                    System.out.println(MessageHandler.determineMessagePrintContents(globalMessage));
                }
                case "LOGIN_RESP", "BYE_RESP", "BROADCAST_RESP", "PRIVATE_MESSAGE_RESP", "GAME_CREATE_RESP",
                        "GAME_START_RESP", "GAME_JOIN_RESP", "GAME_ERROR_RESP", "FILE_TRANSFER_RESP" -> {
                    String responseType = Utility.getResponseType(response);
                    String status = Utility.extractParameterFromJson(response, "status");
                    //If status is OK, the code is empty
                    String code = "";
                    if (!status.equals("OK")){
                        code = Utility.extractParameterFromJson(response, "code");
                    }
                    ResponseMessage responseMessage = new ResponseMessage(responseType, status, code);
//                    ResponseMessage responseMessage = mapper.readValue(body, ResponseMessage.class);
//                    responseMessage.setResponseType(responseType);
                    System.out.println(ResponseHandler.determineResponseMessagePrint(responseMessage));
                }
                case "CLIENT_LIST_RESP" -> {
                    Message message = MessageHandler.handleConnectedClientsResponseStatus(response);
                    System.out.println(MessageHandler.handlePrintOfConnectedClients(message));
                }
                case "PRIVATE_MESSAGE" -> {
                    String senderUsername = Utility.extractParameterFromJson(response, "sender");
                    String receiverUsername = Utility.extractParameterFromJson(response, "receiver");
                    String concreteMessage = Utility.extractParameterFromJson(response, "message");
                    PrivateMessage privateMessage = new PrivateMessage("PRIVATE_MESSAGE", senderUsername, receiverUsername, concreteMessage);
//                    PrivateMessage privateMessage = mapper.readValue(body, PrivateMessage.class);
                    System.out.println(MessageHandler.determineMessagePrintContents(privateMessage));
                }
                case "GAME_INVITE" -> {
                    String gameInitiator = Utility.extractParameterFromJson(response, "username");
                    GuessingGameInviteMessage gameInviteMessage = new GuessingGameInviteMessage(gameInitiator);
                    System.out.println(MessageHandler.determineMessagePrintContents(gameInviteMessage));
                }
                case "GUESS_RESP" -> {
                    String gameGuessResponse = Utility.extractParameterFromJson(response, "number");
                    GameGuessResponseMessage guessResponseMessage = new GameGuessResponseMessage(gameGuessResponse);
                    System.out.println(MessageHandler.determineMessagePrintContents(guessResponseMessage));
                }
                case "GAME_END" -> {
                    List<Map.Entry<String,Long>> gameResults = Utility.extractGameResultListFromJson(response, "results");
                    EndGameMessage endGameMessage = new EndGameMessage(gameResults);
                    System.out.println(MessageHandler.determineMessagePrintContents(endGameMessage));
                }
                case "FILE_RECEIVE_RESP" -> {
                    FileReceiveResponseMessage fileReceiveResponse = mapper.readValue(body, FileReceiveResponseMessage.class);
                    System.out.println("Response: " + fileReceiveResponse);
                }
                default -> System.out.println(command + " Response: " + response);
            }
        }
    }


    private void sendToServer(String message) {
        output.println(message);
    }

    private void closeResources() {
        try {
            if (input != null) {
                input.close();
            }
            if (output != null) {
                output.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
