package messages;

import com.fasterxml.jackson.databind.JsonNode;

public class PongError extends Message {

    private String responseType;
    private String code;


    public PongError(String code) {
        this.responseType = "PONG_ERROR";
        this.code = code;
        setOverallData(determineMessageBody());
    }

    private String determineMessageBody() {
        String overallData = responseType;
        JsonNode node = getMapper().createObjectNode()
                .put("code", code);

        overallData += " " + node.toString();
        return overallData;
    }
}
