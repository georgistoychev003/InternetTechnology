package server;

import com.fasterxml.jackson.databind.JsonNode;
import messages.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

public class ClientFacade {

    private ClientInputManager clientInputManager = ClientInputManager.getInstance();
    private ClientHandler clientHandler;

    public ClientFacade(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
    }



    public ResponseMessage handleLogin(LoginMessage loginMessage) {
        String attemptedUsername = loginMessage.getUsername();

        if (clientInputManager.isUsernameValid(attemptedUsername) && clientInputManager.isUsernameAvailable(attemptedUsername) &&
        clientHandler.getUsername() == null) {
            // Add username to a global list of logged-in users
            Server.addLoggedInUser(attemptedUsername, clientHandler);
            clientHandler.setUsername(attemptedUsername);

            // Broadcast to all users that a new user has joined
            String joinMessage = String.format("{\"username\":\"%s\"}", attemptedUsername);
            JoinedMessage joinedMessage = new JoinedMessage("JOINED " + joinMessage);
            Server.broadcastMessage(joinedMessage, attemptedUsername);

            //Return message that needs to be sent
            return new ResponseMessage("LOGIN_RESP {\"status\":\"OK\"}");
        } else {
            String errorCode = clientInputManager.getLoginErrorCode(attemptedUsername);
            return new ResponseMessage("LOGIN_RESP {\"status\":\"ERROR\", \"code\":" + errorCode + "}");
        }
    }

    public ResponseMessage handleBroadcastRequest(GlobalMessage globalMessage) {
        if (clientHandler.getUsername() == null) {
            // User is not logged in
            return new ResponseMessage("BROADCAST_RESP {\"status\":\"ERROR\", \"code\":6000}");
        }

//        JSONObject jsonObj = new JSONObject(jsonPart);
//        String message = jsonObj.getString("message");
        String message = globalMessage.getMessage();


        String bodyOfMessage = String.format("{\"username\":\"%s\",\"message\":\"%s\"}", clientHandler.getUsername(), message);
        GlobalMessage messageToBroadcast = new GlobalMessage("BROADCAST " + bodyOfMessage);
        // Send broadcast message to all other clients, excluding the one who initiates it
        Server.broadcastMessage(messageToBroadcast, clientHandler.getUsername());


        // Send confirmation back to the sender
        return new ResponseMessage("BROADCAST_RESP {\"status\":\"OK\"}");
    }

    public void handleLogout() {
        // Remove the user from the list of logged-in users
        Server.removeLoggedInUser(clientHandler.getUsername());

        // Notify other clients that this user has left, excluding the user himself who left
        LeftMessage leftMessage = new LeftMessage("LEFT {\"username\":\"" + clientHandler.getUsername() + "\"}");
        Server.broadcastMessage(leftMessage, clientHandler.getUsername());
    }

    public Message handleClientListRequest() {
        if (clientHandler.getUsername() == null) {
            return new ResponseMessage("CLIENT_LIST_RESP {\"status\":\"ERROR\", \"code\":6000}");
        }

        String[] usersArray = Server.getLoggedInUsers().keySet().toArray(new String[0]);
        String usersJsonArray = new JSONArray(usersArray).toString();
        ClientListMessage clientListMessage = new ClientListMessage("CLIENT_LIST_RESP {\"status\":\"OK\", \"users\":" + usersJsonArray + "}");
//        JSONObject response = new JSONObject();
//        response.put("status", "OK");
//        response.put("users", new JSONArray(usersArray));
        return clientListMessage;
    }

    public Message handlePrivateMessageRequest(PrivateMessage privateMess) {
        if (clientHandler.getUsername() == null) {
            // User is not logged in
            return new ResponseMessage("PRIVATE_MESSAGE_RESP {\"status\":\"ERROR\", \"code\":6000}");
        }

        ClientHandler privateMessageHandler = Server.getLoggedInUsers().get(privateMess.getUsername());
        if (privateMessageHandler != null) {
            // User exists so we send the private message

            PrivateMessage privateMessage = new PrivateMessage("PRIVATE_MESSAGE {\"username\":\"" + privateMess.getUsername() + "\",\"message\":\"" + privateMess.getMessage() + "\"}");
            privateMessageHandler.sendMessage(privateMessage.getOverallData());
            return new ResponseMessage("PRIVATE_MESSAGE_RESP {\"status\":\"OK\"}");
        } else {
            // Target user not found
            return new ResponseMessage("PRIVATE_MESSAGE_RESP {\"status\":\"ERROR\", \"code\":1000}");
        }
    }

    public ResponseMessage handleGameCreateRequest() {
        GuessingGame game = GuessingGame.getInstance();
        if (game.startGame(this.clientHandler)) {
            Server.broadcastGameInvite(this.clientHandler.getUsername());
            return new ResponseMessage("GAME_CREATE_RES {\"status\":\"OK\"}");
        } else {
            return new ResponseMessage("GAME_ERROR_RESP {\"status\":\"ERROR\", \"code\":9000}");
        }
    }

    public ResponseMessage handleGameJoinRequest(String username) {
        GuessingGame game = GuessingGame.getInstance();
        if (game.isGameInProgress()) {
            game.addParticipant(clientHandler); // Add client to participants
            return new ResponseMessage("GAME_JOIN_RES {\"status\":\"OK\"}");
        } else {
            return new ResponseMessage("GAME_ERROR_RESP {\"status\":\"ERROR\", \"code\":9001}");
        }
    }


}
