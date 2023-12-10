package messages;

import com.fasterxml.jackson.databind.JsonNode;
import utils.Utility;

public class ResponseMessage extends Message{

    private String responseType;
    private String status;
    private String code;
    public ResponseMessage(String overallData) {
        super(overallData);
        determineMessageContents(overallData);
    }

    private void determineMessageContents(String overallData) {
        JsonNode responseNode = Utility.getMessageContents(overallData);
        responseType = responseNode.get("responseType").asText();
        status = responseNode.get("status").asText();
        if (responseNode.has("code")){
            code = responseNode.get("code").asText();
        }
    }

    public String getResponseType() {
        return responseType;
    }

    public String getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

}
