package messages;

import com.fasterxml.jackson.databind.JsonNode;
import utils.Utility;

public class LoginMessage extends Message{

    private String responseType = "LOGIN";
    private String username;

    public LoginMessage() {

    }
    public LoginMessage(String username) {
        this.responseType = "LOGIN";
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

    public void setUsername(String username) {
        this.username = username;
    }
}
