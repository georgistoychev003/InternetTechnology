package messages;

import com.fasterxml.jackson.databind.JsonNode;
import utils.Utility;

public class DisconnectMessage extends Message{

    private String reason;
    public DisconnectMessage(String overallData) {
        super(overallData);
        determineMessageBody(overallData);
    }

    private void determineMessageBody(String overallData) {
        JsonNode jsonNode = Utility.getMessageContents(overallData);
        if (jsonNode.has("reason")){
            reason = jsonNode.get("reason").asText();
        }
    }


}
