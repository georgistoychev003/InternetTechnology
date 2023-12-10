package messages;

import com.fasterxml.jackson.databind.JsonNode;
import utils.Utility;

public class PrivateMessage extends Message {
    private String responseType;
    private String username;
    private String message;
    public PrivateMessage(String overallData) {
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

    public String getUsername() {
        return username;
    }

    public String getMessage() {
        return message;
    }

}
