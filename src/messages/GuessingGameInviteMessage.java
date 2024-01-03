package messages;

import com.fasterxml.jackson.databind.JsonNode;
import utils.Utility;

public class GuessingGameInviteMessage extends Message {

    private String responseType;
    private String username;
    public GuessingGameInviteMessage() {

    }

    public GuessingGameInviteMessage(String username) {
        this.responseType = "GAME_INVITE";
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
