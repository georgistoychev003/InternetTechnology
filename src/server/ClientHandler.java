package server;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private ClientFacade clientFacade =new ClientFacade(this);

    private String username;


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


    private void processClientMessage(String message)  {
        try {
            String command = Utility.getResponseType(message);

            switch (command) {
                case "LOGIN":
                    LoginMessage loginMessage = new LoginMessage(message);
                    ResponseMessage loginMessageToSend = clientFacade.handleLogin(loginMessage);
                    sendMessage(loginMessageToSend.getOverallData());
                    break;
                case "BROADCAST_REQ":
                    GlobalMessage broadcastMessage = new GlobalMessage(message);
                    ResponseMessage broadcastResponseToSend = clientFacade.handleBroadcastRequest(broadcastMessage);
                    sendMessage(broadcastResponseToSend.getOverallData());
                    break;
                case "BYE":
                    clientFacade.handleLogout();
                    this.handleLogout();
                    break;
                case "PONG":
                    receivedPong.set(true);
                    break;
//                case "PING":
//                    // This should not normally happen as server sends PING
//                    sendMessage("PONG_ERROR", "{\"code\": 8000}");
//                    break;
                case "CLIENT_LIST_REQ":
                    Message listRequestMessageToSend = clientFacade.handleClientListRequest();
                    sendMessage(listRequestMessageToSend.getOverallData());
                    break;
                case "PRIVATE_MESSAGE_REQ":
                    PrivateMessage privateMessage = new PrivateMessage(message);
                    Message privateMessageResponseToSend = clientFacade.handlePrivateMessageRequest(privateMessage);
                    sendMessage(privateMessageResponseToSend.getOverallData());
                    break;
                default:
                    sendMessage("UNKNOWN_COMMAND", "{}");
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
                    Message pingMessage = new Message("PING \"{}\"");
                    sendMessage(pingMessage.getOverallData());

                    // Wait for 3 seconds for PONG response
                    Thread.sleep(3000);
                    if (!receivedPong.get()) {
                        DisconnectMessage disconnectMessage = new DisconnectMessage("DSCN {\"reason\": 7000}");
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