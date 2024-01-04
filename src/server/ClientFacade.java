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

        ClientHandler privateMessageHandler = Server.getLoggedInUsers().get(privateMess.getUsername());
        if (privateMessageHandler != null) {
            // User exists so we send the private message
            PrivateMessage privateMessage = new PrivateMessage(privateMess.getUsername(), privateMess.getMessage());
            privateMessageHandler.sendMessage(privateMessage.getOverallData());
            return new ResponseMessage("PRIVATE_MESSAGE_RESP", "OK");
        } else {
            // Target user not found
            return new ResponseMessage("PRIVATE_MESSAGE_RESP", "ERROR", "1000");
        }
    }

    public ResponseMessage handleGameCreateRequest() {
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
        if (getGuessingGame().isGameInProgress()) {
            getGuessingGame().addParticipant(clientHandler); // Add client to participants
            return new ResponseMessage("GAME_JOIN_RESP", "OK");
        } else {
            return new ResponseMessage("GAME_ERROR_RESP", "ERROR", "9001");
        }
    }

    public Message handleGameGuess(String message) {
        String number = Utility.extractParameterFromJson(message, "number");
        return getGuessingGame().checkGuess(number);
    }

    private GuessingGame getGuessingGame() {
        return GuessingGame.getInstance();
    }
}
