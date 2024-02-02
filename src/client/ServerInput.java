package client;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import messages.*;
import utils.Utility;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

import static com.fasterxml.jackson.databind.type.LogicalType.Map;

public class ServerInput implements Runnable {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private ObjectMapper mapper;
    private Socket fileTransferSocket;

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
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResources();
        }
    }


    private void handleServerResponse(String response) throws Exception {
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
                        "GAME_START_RESP", "GAME_JOIN_RESP", "GAME_ERROR_RESP", "FILE_TRANSFER_RESP", "SESSION_KEY_EXCHANGE_RESP",
                        "ENCRYPTED_MESSAGE_SEND_RESP" -> {
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
                case "FILE_RECEIVE_REQ" -> {
                    String fileSender = Utility.extractParameterFromJson(response, "sender");
                    String fileReceiver = Utility.extractParameterFromJson(response, "receiver");
                    String fileName = Utility.extractParameterFromJson(response, "fileName");
                    FileTransferREQMessage fileTransferREQMessage = new FileTransferREQMessage("FILE_RECEIVE_REQ", fileSender, fileReceiver, fileName);
                    System.out.println(fileSender + " wants to send you file named \"" + fileName + "\"");
                    System.out.println("Type help to see how to respond to that request");
                }
                case "FILE_RECEIVE_RESP" -> {
                    String myUsername = Utility.extractParameterFromJson(response, "sender");
                    String fileResp = Utility.extractParameterFromJson(response, "response");
                    String uuid = Utility.extractParameterFromJson(response, "uuid");
                    FileReceiveResponseMessage fileReceiveResponse = new FileReceiveResponseMessage(myUsername, fileResp, uuid);
                    handleFileTransfer(fileReceiveResponse);
                }
                case "PUBLIC_KEY_RESP" -> {
                    String publicKeyUsername = Utility.extractParameterFromJson(response, "username");
                    String publicKey = Utility.extractParameterFromJson(response, "publicKey");
                    PublicKeyResponseMessage keyResponseMessage = new PublicKeyResponseMessage(publicKeyUsername, publicKey);
                    handlePublicKeyResponse(keyResponseMessage);
                }
                case "ENCRYPTED_MESSAGE" -> {
                    String senderUsername = Utility.extractParameterFromJson(response, "sender");
                    String encryptedMessage = Utility.extractParameterFromJson(response, "encryptedMessage");
                    EncryptedMessage privateMessage = new EncryptedMessage(senderUsername, encryptedMessage);
                    System.out.println(MessageHandler.determineMessagePrintContents(privateMessage));
                }
                default -> System.out.println(command + " Response: " + response);
            }
        }
    }

    private void handleFileTransfer(FileReceiveResponseMessage fileReceiveResponse) {
        if (fileReceiveResponse.getResponse().equals("1")){

            FileTransfer fileTransfer = Client.getFileTransfersMap().get(fileReceiveResponse.getSender());
            fileTransfer.setUuid(fileReceiveResponse.getUuid());
            Client.addFileTransferRequest(fileTransfer);
            Client.getFileTransfersMap().get(fileReceiveResponse.getSender()).initiateFileTransfer();
        }
    }

//    private void handlePublicKeyResponse(String response) {
//        String status = Utility.extractParameterFromJson(response, "status");
//        if ("OK".equals(status)) {
//            String publicKey = Utility.extractParameterFromJson(response, "publicKey");
//            System.out.println("The public key of the recipient is: " + publicKey);
//        } else {
//            String responseType = Utility.getResponseType(response);
//            String errorCode = Utility.extractParameterFromJson(response, "code");
//            ResponseMessage errorResponse = new ResponseMessage(responseType, "ERROR", errorCode);
//            System.out.println(ResponseHandler.determineResponseMessagePrint(errorResponse));
//        }
//    }

    private void handlePublicKeyResponse(PublicKeyResponseMessage response) throws Exception {
        System.out.println("The public key of the recipient is: " + response.getPublicKey());
        PublicKey publicKey = EncryptionUtilities.convertStringToPublicKey(response.getPublicKey());

        SecretKey sessionKey = EncryptionUtilities.generateSessionKey();
        Client.addSession(response.getUsername(), sessionKey);
        byte[] encryptedSessionKey = EncryptionUtilities.encryptSessionKey(sessionKey, publicKey);


        // Prepare and send session key exchange request to server
        sendSessionKeyExchangeRequest(response.getUsername(), encryptedSessionKey);

        sendEncryptedMessage(response.getUsername(), Client.getPendingEncryptedMessages().get(response.getUsername()));
    }



    private void sendSessionKeyExchangeRequest(String receiverUsername, byte[] encryptedSessionKey) {
        String encryptedKeyStr = Base64.getEncoder().encodeToString(encryptedSessionKey);
        SessionKeyExchangeRequestMessage requestMessage = new SessionKeyExchangeRequestMessage(receiverUsername, encryptedKeyStr);

        sendToServer(requestMessage.getOverallData());
    }

    private void sendEncryptedMessage(String receiverUsername, String message) {
        String encryptedMessage;
        try {
            byte[] encryptedMessageBytes = EncryptionUtilities.encryptMessage(message, Client.getSessionHolder().get(receiverUsername));
            encryptedMessage = new String(encryptedMessageBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        EncryptedMessageReq encryptedMessageReq = new EncryptedMessageReq(receiverUsername, encryptedMessage);
        sendToServer(encryptedMessageReq.getOverallData());
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
