package messages;

import com.fasterxml.jackson.databind.JsonNode;
import utils.Utility;

public class JoinedMessage extends Message{

    private String responseType;
    private String username;

    public JoinedMessage(String username) {
        this.responseType = "JOINED";
        this.username = username;
        setOverallData(determineMessageContents());
    }

    private String determineMessageContents() {
        String overallData = responseType;
        JsonNode node = getMapper().createObjectNode()
                .put("username", username);

        overallData += " " + node.toString();
        return overallData;
    }

    public String getUsername() {
        return username;
    }
}
