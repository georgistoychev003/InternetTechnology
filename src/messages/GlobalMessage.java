package messages;

import com.fasterxml.jackson.databind.JsonNode;
import utils.Utility;

public class GlobalMessage extends Message{

    private String responseType;
    private String username;
    private String message;
    public GlobalMessage(String overallData) {
        super(overallData);
        determineMessageContents(overallData);
    }

    private void determineMessageContents(String overallData) {
        JsonNode responseNode = Utility.getMessageContents(overallData);
        responseType = responseNode.get("responseType").asText();
        username = responseNode.get("username").asText();
        if (responseNode.has("message")){
            message = responseNode.get("message").asText();
        }
    }

    @Override
    public String toString() {
        if (responseType.equals("BROADCAST")){
            return "Global: " + username + " -> " + message;
        } else if (responseType.equals("LEFT")) {
            return  "A user with username \"" + username + "\" just left the chat/disconnected.";
        }
        return "error";
    }
}
