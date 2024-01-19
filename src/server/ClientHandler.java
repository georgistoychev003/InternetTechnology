package server;

import java.io.*;
import java.net.Socket;
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

            WelcomeMessage welcomeMessage = new WelcomeMessage("WELCOME {\"msg\":\"Welcome to the server " + Server.VERSION + "\"}");
            sendMessage(welcomeMessage.getOverallData());

            String line;
            while (running.get() && (line = reader.readLine()) != null) {
                processClientMessage(line);
            }
        } catch (Exception e) {
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
                    receivedPong.set(true);
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
                    String fileReceiverUsername = Utility.extractParameterFromJson(message, "username");
                    String fileName = Utility.extractParameterFromJson(message, "fileName");
                    FileTransferREQMessage fileTransferREQMessage = new FileTransferREQMessage("FILE_TRANSFER_REQ", fileReceiverUsername, fileName);
                    ResponseMessage fileTransferRequestMessage = clientFacade.handleFileTransferRequest(fileTransferREQMessage);
                    sendMessage(fileTransferRequestMessage.getOverallData());
                    break;
                case "FILE_RECEIVE_RESP":
                    handleFileReceiveResponse(message);
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

    private void startHeartbeat() {
        new Thread(() -> {
            try {
                while (running.get()) {
                    Thread.sleep(10000); // Send ping every 10 seconds
                    receivedPong.set(false); // Reset the pong
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

    private void handleFileReceiveResponse(String message) {
        String response = Utility.extractParameterFromJson(message, "response");
        FileReceiveResponseMessage responseMessage = new FileReceiveResponseMessage(response);
        sendMessage(responseMessage.getOverallData());
        if ("1".equals(response)) {
            System.out.println("File transfer accepted by " + username);
        } else if ("-1".equals(response)) {
            System.out.println("File transfer rejected by " + username);
        } else {
            System.out.println("Invalid file transfer response received from " + username);
        }
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
