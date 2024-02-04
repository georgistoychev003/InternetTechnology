package messages.messagehandling;

import com.fasterxml.jackson.databind.JsonNode;
import messages.*;
import utils.Utility;

import java.util.List;
import java.util.Map;

public class MessageHandler {

    public static String determineMessagePrintContents(Message message) {
        if (message instanceof GlobalMessage){
            return "Global: " + ((GlobalMessage) message).getUsername() + " -> " + ((GlobalMessage) message).getMessage();
        } else if (message instanceof JoinedMessage) {
            return "A user with username: " + ((JoinedMessage) message).getUsername()+ " just joined the chatroom";
        } else if (message instanceof LeftMessage) {
            return  "A user with username \"" + ((LeftMessage) message).getUsername() + "\" just left the chat/disconnected.";
        } else if (message instanceof PrivateMessage){
            return "Private: " + ((PrivateMessage) message).getSender() + " -> " + ((PrivateMessage) message).getMessage();
        } else if (message instanceof GuessingGameInviteMessage) {
            return "Game Invite: " + ((GuessingGameInviteMessage) message).getUsername() + " has invited you to join the guessing game. Use 'game join' command to join the game";
        } else if (message instanceof GameGuessResponseMessage) {
            return determineGuessStatus((GameGuessResponseMessage) message);
        } else if (message instanceof EndGameMessage) {
            return determineEndGameMessage((EndGameMessage) message);
        } else if (message instanceof EncryptedMessage) {
            return "Secret message: " + ((EncryptedMessage) message).getSender() + " -> " + ((EncryptedMessage) message).getEncryptedMessage();
        }
        return "error";
    }

    private static String determineEndGameMessage(EndGameMessage message) {
        if (message.getGameResults().isEmpty()){
            return "Game ended! There were no winners!";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("*-----Game results-----*\n");
        int count = 1;
        for (Map.Entry<String, Long> result : message.getGameResults()) {
            sb.append(count).append(") ").append(result.getKey()).append(" -> ").append(result.getValue()).append(" ms\n");
            count++;
        }
        sb.append("*----------------------*\n");

        return sb.toString();
    }

    private static String determineGuessStatus(GameGuessResponseMessage message) {
        if (message.getNumber().equals("1")){
            return "The secret number is lower than your guess";
        } else if (message.getNumber().equals("-1")) {
            return "The secret number is higher than your guess";
        } else {
            return "You guessed the secret number";
        }
    }

    public static Message handleConnectedClientsResponseStatus(String response) {
        String responseStatus = Utility.extractParameterFromJson(response, "status");
        if (responseStatus.equals("OK")){
            List<String> users = Utility.extractUserListFromJson(response, "users");
            return new ClientListMessage(responseStatus, users);
        } else {
            String responseType = Utility.getResponseType(response);
            String code = Utility.extractParameterFromJson(response, "code");
            return new ResponseMessage(responseType, responseStatus, code);
        }
    }

    public static String handlePrintOfConnectedClients(Message message){
        StringBuilder sb = new StringBuilder();
        if (message instanceof ClientListMessage){
            List<String> users = ((ClientListMessage) message).getUsers();
            if (!users.isEmpty()) {
                sb.append("********** List Of Connected Clients **********");
                for (String name : users) {
                    sb.append("\n-->>: ").append(name);
                }
            } else {
                sb.append("Error: Invalid or missing user information in the response.");
            }
        } else if (message instanceof ResponseMessage){
            String errorCode = ((ResponseMessage) message).getCode();
            if (errorCode.equals("6000")) {
                sb.append("Sorry, you are not logged in, thus you cannot request the list of connected clients. Login and try again.");
            } else {
                sb.append("Error: ").append(errorCode);
            }
        }

        return sb.toString();
    }
}
