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
        if (responseNode.has("username")){
            username = responseNode.get("username").asText();
        }
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