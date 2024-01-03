package messages;

import com.fasterxml.jackson.databind.JsonNode;
import utils.Utility;

public class GlobalMessage extends Message{

    private String responseType;
    private String username;
    private String message;

    public GlobalMessage() {

    }
    public GlobalMessage(String responseType,String username, String message) {
        this.responseType = responseType;
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

    public void setUsername(String username) {
        this.username = username;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
