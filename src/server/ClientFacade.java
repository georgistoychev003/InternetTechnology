package server;

import messages.*;
import org.json.JSONArray;
import utils.Utility;

import java.util.List;


public class ClientFacade {

    private ServerStateManager serverStateManager = ServerStateManager.getInstance();
    private ClientHandler clientHandler;

    public ClientFacade(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
    }

    public ResponseMessage handleLogin(LoginMessage loginMessage) {
        String attemptedUsername = loginMessage.getUsername();

        if (serverStateManager.isUsernameValid(attemptedUsername) && serverStateManager.isUsernameAvailable(attemptedUsername) &&
        clientHandler.getUsername() == null) {
            // Add username to a global list of logged-in users
            Server.addLoggedInUser(attemptedUsername, clientHandler);
            clientHandler.setUsername(attemptedUsername);

            // Broadcast to all users that a new user has joined
            JoinedMessage joinedMessage = new JoinedMessage(attemptedUsername);
            System.out.println("Joined mess: " + joinedMessage.getOverallData());
            Server.broadcastMessage(joinedMessage, attemptedUsername);

            //Return message that needs to be sent
            return new ResponseMessage("LOGIN_RESP", "OK");
        } else {
            String errorCode = serverStateManager.getLoginErrorCode(attemptedUsername);
            return new ResponseMessage("LOGIN_RESP", "ERROR",  errorCode);
        }
    }

    public ResponseMessage handleBroadcastRequest(GlobalMessage globalMessage) {
        if (clientHandler.getUsername() == null) {
            // User is not logged in
            return new ResponseMessage("BROADCAST_RESP", "ERROR", "6000");
        }

        String message = globalMessage.getMessage();
        GlobalMessage messageToBroadcast = new GlobalMessage("BROADCAST",clientHandler.getUsername(), message);
        // Send broadcast message to all other clients, excluding the one who initiates it
        Server.broadcastMessage(messageToBroadcast, clientHandler.getUsername());

        // Send confirmation back to the sender
        return new ResponseMessage("BROADCAST_RESP", "OK");
    }

    public void handleLogout() {
        // Remove the user from the list of logged-in users
        Server.removeLoggedInUser(clientHandler.getUsername());

        // Notify other clients that this user has left, excluding the user himself who left
        LeftMessage leftMessage = new LeftMessage(clientHandler.getUsername());
        Server.broadcastMessage(leftMessage, clientHandler.getUsername());
    }

    public Message handleClientListRequest() {
        if (clientHandler.getUsername() == null) {
            return new ResponseMessage("CLIENT_LIST_RESP", "ERROR", "6000");
        }

        List<String> usersNameList = Server.getLoggedInUsers().keySet().stream().toList();


        return new ClientListMessage("OK", usersNameList);
    }

    public Message handlePrivateMessageRequest(PrivateMessage privateMess) {
        if (clientHandler.getUsername() == null) {
            // User is not logged in
            return new ResponseMessage("PRIVATE_MESSAGE_RESP", "ERROR", "6000");
        }

        ClientHandler privateMessageHandler = Server.getLoggedInUsers().get(privateMess.getReceiver());
        if (privateMessageHandler != null) {
            // User exists so we send the private message
            PrivateMessage privateMessage = new PrivateMessage("PRIVATE_MESSAGE", privateMess.getSender(), privateMess.getReceiver(), privateMess.getMessage());
            privateMessageHandler.sendMessage(privateMessage.getOverallData());
            return new ResponseMessage("PRIVATE_MESSAGE_RESP", "OK");
        } else {
            // Target user not found
            return new ResponseMessage("PRIVATE_MESSAGE_RESP", "ERROR", "1000");
        }
    }

    public Message handleFileReceiveResponse(String message) {
        String fileSender = Utility.extractParameterFromJson(message, "sender");
        String response = Utility.extractParameterFromJson(message, "response");
        String uuid = Utility.extractParameterFromJson(message, "uuid");

        assert response != null;
        if (!response.equals("1") && !response.equals("-1")) {
            // Invalid response
            return new ResponseMessage("FILE_TRANSFER_RESP", "ERROR", "8003");
        }

        if (response.equals("1") && uuid.length() != 36) {
            // Invalid UUID sent
            return new ResponseMessage("FILE_TRANSFER_RESP", "ERROR", "8004");
        }

        FileReceiveResponseMessage responseMessage = new FileReceiveResponseMessage(fileSender,response, uuid);
        ClientHandler recipientHandler = Server.getLoggedInUsers().get(responseMessage.getSender());
        if (recipientHandler != null) {
            recipientHandler.sendMessage(responseMessage.getOverallData());
        } else  {
            return new ResponseMessage("FILE_TRANSFER_RESP", "ERROR", "8005");
        }
        return responseMessage;
    }

    public ResponseMessage handleGameCreateRequest() {
        if (clientHandler.getUsername() == null) {
            // User is not logged in
            return new ResponseMessage("GAME_ERROR_RESP", "ERROR", "6000");
        }

        GuessingGame game = getGuessingGame();
        ResponseMessage responseMessage = game.createGame(this.clientHandler);
        if (responseMessage.getStatus().equals("OK")){
            Server.broadcastGameInvite(this.clientHandler.getUsername());
        }
        return responseMessage;
    }

    public ResponseMessage handleGameStart() {

        System.out.println("Currently in handleGameStart method");
        ResponseMessage startResponse = getGuessingGame().startGame(this.clientHandler);
        Server.broadcastGameStart(this.clientHandler.getUsername(), getGuessingGame(), startResponse);
        return startResponse;
    }

    public ResponseMessage handleGameJoinRequest(String username) {
        if (clientHandler.getUsername() == null) {
            // User is not logged in
            return new ResponseMessage("GAME_ERROR_RESP", "ERROR", "6000");
        }

        if (getGuessingGame().getParticipants().containsKey(username)) {
            return new ResponseMessage("GAME_ERROR_RESP", "ERROR", "9008");
        }

        if (getGuessingGame().isGameInProgress()) {
            return new ResponseMessage("GAME_ERROR_RESP", "ERROR", "9004");
        }

        if (getGuessingGame().isGameInitiated()) {
            getGuessingGame().addParticipant(clientHandler); // Add client to participants
            return new ResponseMessage("GAME_JOIN_RESP", "OK");
        } else {
            return new ResponseMessage("GAME_ERROR_RESP", "ERROR", "9001");
        }
    }

    public ResponseMessage handleFileTransferRequest(FileTransferREQMessage fileTransferREQMessage) {
        String senderUsername = clientHandler.getUsername();
        String receiverUsername = fileTransferREQMessage.getReceiver();

        // Check if the sender is logged in
        if (senderUsername == null) {
            return new ResponseMessage("FILE_TRANSFER_RESP", "ERROR", "6000"); // User not logged in
        }

        // Check if the recipient exists and is different from the sender
        ClientHandler receiverHandler = Server.getLoggedInUsers().get(receiverUsername);
        if (receiverHandler == null) {
            return new ResponseMessage("FILE_TRANSFER_RESP", "ERROR", "8001"); // Recipient not found
        }
        if (senderUsername.equals(receiverUsername)) {
            return new ResponseMessage("FILE_TRANSFER_RESP", "ERROR", "8002"); // Sender is the same as recipient
        }


        FileTransferREQMessage fileReceiveReqMessage = new FileTransferREQMessage("FILE_RECEIVE_REQ", senderUsername, receiverUsername, fileTransferREQMessage.getFileName());
        receiverHandler.sendMessage(fileReceiveReqMessage.getOverallData());


        return new ResponseMessage("FILE_TRANSFER_RESP", "OK");
    }
//    public Message handleGameGuess(String message) {
//        String number = Utility.extractParameterFromJson(message, "number");
//        return getGuessingGame().checkGuess(number);

//    }

    public Message handleGameGuess(String message) {
        if (clientHandler.getUsername() == null) {
            // User is not logged in
            return new ResponseMessage("GAME_ERROR_RESP", "ERROR", "9005");
        }

        String number = Utility.extractParameterFromJson(message, "number");
        String username = clientHandler.getUsername();
        if (!getGuessingGame().getParticipants().containsKey(username)){
            return new ResponseMessage("GAME_ERROR_RESP", "ERROR", "9006");
        }
        return getGuessingGame().checkGuess(number, username);
    }

    public Message handlePublicKeyRequest(String targetUsername) {
        String publicKey = Server.getPublicKey(targetUsername);
        if (publicKey != null) {
            // Successful response with public key
            return new PublicKeyResponseMessage(targetUsername, publicKey);
        } else {
            // Error response if user not found or user has no public key
            //TODO : the stated case above has 2 error codes according to protocol
            return new ResponseMessage("ENCRYPTED_MESSAGE_SEND_RESP", "ERROR", "11000");
        }
    }

    public void handleSessionKeyExchange(SessionKeyExchangeRequestMessage requestMessage, String senderUsername) {
        String receiverUsername = requestMessage.getUsername();
        String encryptedSessionKey = requestMessage.getEncryptedSessionKey();

        ClientHandler receiverHandler = Server.getLoggedInUsers().get(receiverUsername);
        if (receiverHandler != null) {
            // Forward the encrypted session key to the intended recipient
            SessionKeyExchangeRequestMessage forwardMessage = new SessionKeyExchangeRequestMessage(senderUsername, encryptedSessionKey);
            receiverHandler.sendMessage(forwardMessage.getOverallData());

            // Respond to the sender with a success message
            ResponseMessage responseToSender = new ResponseMessage("SESSION_KEY_EXCHANGE_RESP", "OK");
            clientHandler.sendMessage(responseToSender.getOverallData());
        } else {
            // Recipient not found
            ResponseMessage errorResponse = new ResponseMessage("SESSION_KEY_EXCHANGE_RESP", "ERROR", "11001");
            clientHandler.sendMessage(errorResponse.getOverallData());
        }
    }




    private GuessingGame getGuessingGame() {
        return GuessingGame.getInstance();
    }

}
