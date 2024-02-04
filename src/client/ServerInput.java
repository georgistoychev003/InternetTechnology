package client;

import com.fasterxml.jackson.databind.ObjectMapper;
import messages.*;
import messages.messagehandling.MessageHandler;
import messages.messagehandling.ResponseHandler;
import utils.Utility;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.security.PublicKey;
import java.util.*;

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
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResources();
        }
    }


    private void handleServerResponse(String response) throws Exception {
        String command = Utility.getResponseType(response);
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
                    if (!status.equals("OK")) {
                        code = Utility.extractParameterFromJson(response, "code");
                    }
                    ResponseMessage responseMessage = new ResponseMessage(responseType, status, code);
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
                    List<Map.Entry<String, Long>> gameResults = Utility.extractGameResultListFromJson(response, "results");
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
                case "SESSION_KEY_EXCHANGE_REQ" -> {
                    String sessionUsername = Utility.extractParameterFromJson(response, "username");
                    String encryptedSessionKey = Utility.extractParameterFromJson(response, "encryptedSessionKey");

                    SecretKey encryptedSecretKey = EncryptionUtilities.convertStringToSecretKey(encryptedSessionKey);
                    byte[] sessionKey = EncryptionUtilities.decryptSessionKey(encryptedSecretKey, Client.getPrivateKey());
                    String decryptedSessionKeyString = EncryptionUtilities.convertByteArrayKeyToString(sessionKey);

                    SecretKey decryptedSessionKey = EncryptionUtilities.convertStringToSecretKey(decryptedSessionKeyString);

                    Client.addSession(sessionUsername, decryptedSessionKey);
                }
                case "ENCRYPTED_MESSAGE" -> {
                    String senderUsername = Utility.extractParameterFromJson(response, "sender");
                    String encryptedMessage = Utility.extractParameterFromJson(response, "encryptedMessage");

                    byte[] encryptedMessageBytes = EncryptionUtilities.convertStringToByteArray(encryptedMessage);
                    String decriptedMessage = EncryptionUtilities.decryptMessage(encryptedMessageBytes, Client.getSessionHolder().get(senderUsername));

                    EncryptedMessage privateMessage = new EncryptedMessage(senderUsername, decriptedMessage);
                    System.out.println(MessageHandler.determineMessagePrintContents(privateMessage));
                }
                default -> System.out.println(command + " Response: " + response);
            }
        }
    }

    private void handleFileTransfer(FileReceiveResponseMessage fileReceiveResponse) {
        if (fileReceiveResponse.getResponse().equals("1")) {

            FileTransfer fileTransfer = Client.getFileTransfersMap().get(fileReceiveResponse.getSender());
            fileTransfer.setUuid(fileReceiveResponse.getUuid());
            Client.addFileTransferRequest(fileTransfer);
            Client.getFileTransfersMap().get(fileReceiveResponse.getSender()).initiateFileTransfer();
        } else if (fileReceiveResponse.getResponse().equals("-1")) {
            System.out.println("The receiver declined the file transfer request");
        }
    }


    private void handlePublicKeyResponse(PublicKeyResponseMessage response) throws Exception {
//        System.out.println("The public key of the recipient is: " + response.getPublicKey());
        PublicKey publicKey = EncryptionUtilities.convertStringToPublicKey(response.getPublicKey());

        SecretKey sessionKey = EncryptionUtilities.generateSessionKey();
        Client.addSession(response.getUsername(), sessionKey);
        byte[] encryptedSessionKey = EncryptionUtilities.encryptSessionKey(sessionKey, publicKey);

        // Prepare and send session key exchange request to server
        sendSessionKeyExchangeRequest(response.getUsername(), encryptedSessionKey);

        // Send the encrypted message
        sendEncryptedMessage(response.getUsername(), Client.retrieveEncryptedMessage(response.getUsername()));
        Client.removeEncryptedMessage(response.getUsername());
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
            encryptedMessage = EncryptionUtilities.convertByteArrayKeyToString(encryptedMessageBytes);
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
