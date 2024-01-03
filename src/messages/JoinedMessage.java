package messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import utils.Utility;

public class JoinedMessage extends Message{

    private String responseType = "JOINED";
    private String username;


    public JoinedMessage() {
    }
    @JsonCreator
    public JoinedMessage(@JsonProperty("username")String username) {
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

    public void setUsername(String username) {
        this.username = username;
    }
}
