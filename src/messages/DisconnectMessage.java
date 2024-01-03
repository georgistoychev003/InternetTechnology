package messages;

import com.fasterxml.jackson.databind.JsonNode;
import utils.Utility;

public class DisconnectMessage extends Message{
    private String responseType;
    private String reason;

    public DisconnectMessage() {

    }
    public DisconnectMessage(String reason) {
        this.responseType = "DSCN";
        this.reason = reason;
        setOverallData(determineMessageBody());
    }

    private String determineMessageBody() {
        String overallData = responseType;
        JsonNode node = getMapper().createObjectNode()
                .put("reason", reason);

        overallData += " " + node.toString();
        return overallData;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
