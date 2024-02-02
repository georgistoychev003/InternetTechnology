package server;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.JsonNode;
import messages.*;
import org.json.JSONArray;
import org.json.JSONObject;
import utils.Utility;


public class ClientHandler implements Runnable {

    private Socket clientSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private AtomicBoolean running;
    private AtomicBoolean sentPing = new AtomicBoolean(false);
    private AtomicBoolean receivedPong = new AtomicBoolean(false);
    private ClientFacade clientFacade = new ClientFacade(this);
    private String username;
    private boolean guessedSecretNumber = false;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.running = new AtomicBoolean(true);
    }

    @Override
    public void run() {
        try {
            inputStream = clientSocket.getInputStream();
            outputStream = clientSocket.getOutputStream();

            startHeartbeat();

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));

            //TODO: fix that
            WelcomeMessage welcomeMessage = new WelcomeMessage(Server.VERSION);
            sendMessage(welcomeMessage.getOverallData());

            String line;
            while (running.get() && !clientSocket.isClosed() && (line = reader.readLine()) != null) {
                processClientMessage(line);
            }
        } catch (SocketException e) {
            System.out.println("Client disconnected: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }


    private void processClientMessage(String message) {
        System.out.println(message);
        try {
            String command = Utility.getResponseType(message);

            switch (command) {
                case "LOGIN":
                    String loginUsername = Utility.extractParameterFromJson(message, "username");
                    LoginMessage loginMessage = new LoginMessage(loginUsername);
                    ResponseMessage loginMessageToSend = clientFacade.handleLogin(loginMessage);
                    sendMessage(loginMessageToSend.getOverallData());
                    break;
                case "BROADCAST_REQ":
                    String broadcastUsername = Utility.extractParameterFromJson(message, "username");
                    String broadcastMessage = Utility.extractParameterFromJson(message, "message");
                    GlobalMessage globalMessage = new GlobalMessage(Utility.getResponseType(message), broadcastUsername, broadcastMessage);
                    ResponseMessage broadcastResponseToSend = clientFacade.handleBroadcastRequest(globalMessage);
                    sendMessage(broadcastResponseToSend.getOverallData());
                    break;
                case "BYE":
                    clientFacade.handleLogout();
                    this.handleLogout();
                    break;
                case "PONG":
                    if (sentPing.get()){
                        receivedPong.set(true);
                        sentPing.set(false);
                    } else {
                        sendMessage(new PongError("7001").getOverallData());
                    }
                    break;
                case "CLIENT_LIST_REQ":
                    Message listRequestMessageToSend = clientFacade.handleClientListRequest();
                    sendMessage(listRequestMessageToSend.getOverallData());
                    break;
                case "PRIVATE_MESSAGE_REQ":
                    String senderUsername = Utility.extractParameterFromJson(message, "sender");
                    String receiverUsername = Utility.extractParameterFromJson(message, "receiver");
                    String concreteMessage = Utility.extractParameterFromJson(message, "message");
                    PrivateMessage privateMessage = new PrivateMessage("PRIVATE_MESSAGE", senderUsername, receiverUsername, concreteMessage);
                    Message privateMessageResponseToSend = clientFacade.handlePrivateMessageRequest(privateMessage);
                    sendMessage(privateMessageResponseToSend.getOverallData());
                    break;
                case "GAME_CREATE_REQ":
                    new Thread(() -> handleGameCreateRequest(message)).start();
                    break;
                case "GAME_JOIN_REQ":
                    new Thread(() -> handleGameJoinRequest(message)).start();
                    break;
                case "GUESS_NUMBER":
                    Message guessMessage = clientFacade.handleGameGuess(message);
                    sendMessage(guessMessage.getOverallData());
                    break;
                case "FILE_TRANSFER_REQ":
                    String fileSenderUsername = Utility.extractParameterFromJson(message, "sender");
                    String fileReceiverUsername = Utility.extractParameterFromJson(message, "receiver");
                    String fileName = Utility.extractParameterFromJson(message, "fileName");
                    FileTransferREQMessage fileTransferREQMessage = new FileTransferREQMessage("FILE_TRANSFER_REQ", fileSenderUsername, fileReceiverUsername, fileName);
                    ResponseMessage fileTransferRequestMessage = clientFacade.handleFileTransferRequest(fileTransferREQMessage);
                    sendMessage(fileTransferRequestMessage.getOverallData());
                    break;
                case "FILE_RECEIVE_RESP":
                    Message fileReceiveResponseMessage = clientFacade.handleFileReceiveResponse(message);
                    if (fileReceiveResponseMessage instanceof ResponseMessage) {
                        sendMessage(fileReceiveResponseMessage.getOverallData());
                    }
                    break;
                case "PUBLIC_KEY":
                    String username = Utility.extractParameterFromJson(message, "username");
                    String publicKey = Utility.extractParameterFromJson(message, "publicKey");
                    handlePublicKey(username, publicKey);
                    break;
                case "PUBLIC_KEY_REQ":
                    String targetUsername = Utility.extractParameterFromJson(message, "targetUsername");
                    Message publicKeyResponse = clientFacade.handlePublicKeyRequest(targetUsername);
                    sendMessage(publicKeyResponse.getOverallData());
                    break;
                case "SESSION_KEY_EXCHANGE_REQ":
                    String usernameOfReceiver = Utility.extractParameterFromJson(message, "username");
                    String encryptedSessionKey = Utility.extractParameterFromJson(message, "encryptedSessionKey");
                    SessionKeyExchangeRequestMessage requestMessage = new SessionKeyExchangeRequestMessage(usernameOfReceiver, encryptedSessionKey);
                    clientFacade.handleSessionKeyExchange(requestMessage, this.username);
                    break;
                case "ENCRYPTED_MESSAGE_SEND_REQ":
                    String usernameOfEncryptedReceiver = Utility.extractParameterFromJson(message, "receiver");
                    String encryptedMessage = Utility.extractParameterFromJson(message, "encryptedMessage");
                    EncryptedMessageReq encryptedMessageReq = new EncryptedMessageReq(usernameOfEncryptedReceiver, encryptedMessage);
                    ResponseMessage encryptionMessageResponse = clientFacade.handleEncryptedMessage(encryptedMessageReq, this.username);
                    sendMessage(encryptionMessageResponse.getOverallData());
                    break;
                default:
                    System.out.println(message);
                    sendMessage("UNKNOWN_COMMAND");
                    break;
            }
        } catch (Exception e) {
            sendMessage("PARSE_ERROR", e.getMessage());
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean guessedSecretNumber() {
        return guessedSecretNumber;
    }

    public void setGuessedSecretNumber(boolean guessedSecretNumber) {
        this.guessedSecretNumber = guessedSecretNumber;
    }

    private void handleLogout() {
        running.set(false);
        // Close the connection
        closeConnection();
    }
    private void handlePublicKey(String username, String publicKey) {
        try {
            if (username != null && publicKey != null) {
                Server.storePublicKey(username, publicKey);
                System.out.println("Stored public key for user " + username);
            } else {
                System.out.println("Invalid public key data for user: " + username);
            }
        } catch (Exception e) {
            System.err.println("Error handling public key: " + e.getMessage());
        }
    }


    private void startHeartbeat() {
        new Thread(() -> {
            try {
                while (running.get()) {
                    Thread.sleep(10000); // Send ping every 10 seconds
                    receivedPong.set(false); // Reset the pong
                    sentPing.set(true);
                    Message pingMessage = new Message("PING");
                    sendMessage(pingMessage.getOverallData());

                    // Wait for 3 seconds for PONG response
                    Thread.sleep(3000);
                    if (!receivedPong.get()) {
                        DisconnectMessage disconnectMessage = new DisconnectMessage("7000");
                        sendMessage(disconnectMessage.getOverallData());
                        closeConnection();
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    protected void sendMessage(String command, String message) {
        System.out.println("Partial message:" + command + " " + message);
        try {
            if (!clientSocket.isClosed()) {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
                writer.write(command + " " + message);
                writer.newLine();
                writer.flush();
            }
        } catch (IOException e) {
            System.err.println("Error sending message to client: " + e.getMessage());
            closeConnection();
        }
    }

    protected void sendMessage(String wholeMessage) {
        System.out.println("Whole message:" + wholeMessage);
        try {
            if (!clientSocket.isClosed()) {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
                writer.write(wholeMessage);
                writer.newLine();
                writer.flush();
            }
        } catch (IOException e) {
            System.err.println("Error sending message to client: " + e.getMessage());
            closeConnection();
        }
    }
    private void handleGameCreateRequest(String message) {
        ResponseMessage gameCreateResponse = clientFacade.handleGameCreateRequest();
        sendMessage(gameCreateResponse.getOverallData());
        if (gameCreateResponse.getStatus().equals("OK")) {
            ResponseMessage gameStartResponse = clientFacade.handleGameStart();
            sendMessage(gameStartResponse.getOverallData());
        }
    }
    private void handleGameJoinRequest(String message) {
        ResponseMessage joinResponse = clientFacade.handleGameJoinRequest(this.username);
        sendMessage(joinResponse.getOverallData());
    }



    private void closeConnection() {
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (clientSocket != null) clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Thread.currentThread().interrupt(); // terminate client thread
        }
    }

}
