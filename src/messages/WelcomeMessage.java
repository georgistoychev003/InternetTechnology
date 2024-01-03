package messages;

import com.fasterxml.jackson.databind.JsonNode;
import utils.Utility;

public class WelcomeMessage extends Message{

    private String msg;
    public WelcomeMessage() {

    }
    public WelcomeMessage(String overallData) {
       setOverallData(overallData);
        determineMessageBody(overallData);
    }

    private void determineMessageBody(String overallData) {
        JsonNode jsonNode = Utility.getMessageContents(overallData);
        if (jsonNode.has("msg")){
            msg = jsonNode.get("msg").asText();
        }
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
