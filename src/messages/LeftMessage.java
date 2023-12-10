package messages;

import com.fasterxml.jackson.databind.JsonNode;
import utils.Utility;

public class LeftMessage extends Message{

    private String responseType;
    private String username;

    public LeftMessage(String overallData) {
        super(overallData);
        determineMessageContents(overallData);
    }

    private void determineMessageContents(String overallData) {
        JsonNode responseNode = Utility.getMessageContents(overallData);
        responseType = responseNode.get("responseType").asText();
        if (responseNode.has("username")){
            username = responseNode.get("username").asText();
        }
    }

    public String getUsername() {
        return username;
    }
}
