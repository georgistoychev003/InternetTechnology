package messages;

import com.fasterxml.jackson.databind.JsonNode;
import utils.Utility;

public class MessageHandler {

    public static String determineMessagePrintContents(Message message) {
        if (message instanceof GlobalMessage){
            return "Global: " + ((GlobalMessage) message).getUsername() + " -> " + ((GlobalMessage) message).getMessage();
        } else if (message instanceof JoinedMessage) {
            return "A user with username: " + ((JoinedMessage) message).getUsername()+ " just joined the chatroom";
        } else if (message instanceof LeftMessage) {
            return  "A user with username \"" + ((LeftMessage) message).getUsername() + "\" just left the chat/disconnected.";
        } else if (message instanceof PrivateMessage){
            return "Private: " + ((PrivateMessage) message).getUsername() + "-> " + ((PrivateMessage) message).getMessage();
        }
        return "error";
    }

    public static Message handleConnectedClientsResponseStatus(String response) {
        String responseStatus = Utility.getMessageContents(response).get("status").asText();
        if (responseStatus.equals("OK")){
            return new ClientListMessage(response);
        } else {
            return new ResponseMessage(response);
        }
    }

    public static String handlePrintOfConnectedClients(Message message){
        StringBuilder sb = new StringBuilder();
        if (message instanceof ClientListMessage){
            JsonNode users = ((ClientListMessage) message).getUsers();
            if (users.isArray()) {
                sb.append("********** List Of Connected Clients **********");
                for (JsonNode userNode : users) {
                    sb.append("\n-->>: ").append(userNode.asText());
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
