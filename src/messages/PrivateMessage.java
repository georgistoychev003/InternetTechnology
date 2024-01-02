package messages;

import com.fasterxml.jackson.databind.JsonNode;
import utils.Utility;

public class PrivateMessage extends Message {
    private String responseType;
    private String username;
    private String message;
    public PrivateMessage(String username, String message) {
        this.responseType = "PRIVATE_MESSAGE";
        this.username = username;
        this.message = message;
        setOverallData(determineMessageContents());
    }

    private String determineMessageContents() {
        String overallData = responseType;
        JsonNode node = getMapper().createObjectNode()
                    .put("username", username)
                    .put("message", message);


        overallData += " " + node.toString();
        return overallData;
    }

    public String getUsername() {
        return username;
    }

    public String getMessage() {
        return message;
    }

}
