package messages;

import com.fasterxml.jackson.databind.JsonNode;
import utils.Utility;

public class WelcomeMessage extends Message{

    private String responseType;
    private String msg;
    private String version;
    public WelcomeMessage() {

    }
    public WelcomeMessage(String version) {
        responseType = "WELCOME";
        msg = "Welcome to the server " + version;
        setOverallData(determineMessageContents());
    }

    private String determineMessageContents() {
        String overallData = responseType;
        JsonNode node = getMapper().createObjectNode()
                .put("msg", msg);

        overallData += " " + node.toString();
        return overallData;
    }


    public void setMsg(String msg) {
        this.msg = msg;
    }
}
