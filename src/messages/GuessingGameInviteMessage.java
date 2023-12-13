package messages;

import com.fasterxml.jackson.databind.JsonNode;
import utils.Utility;

public class GuessingGameInviteMessage extends Message {

    private String responseType;
    private String username;

    public GuessingGameInviteMessage(String overallData) {
        super(overallData);
        determineMessageContents(overallData);
    }

    private void determineMessageContents(String overallData) {
        JsonNode responseNode = Utility.getMessageContents(overallData);
        responseType = responseNode.get("responseType").asText();
        if (responseNode.has("user")) {
            username = responseNode.get("user").asText();
        }
    }

    public String getUsername() {
        return username;
    }
}
