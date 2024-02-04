package messages;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import utils.Utility;

public class ResponseMessage extends Message{

    private String responseType;
    private String status;
    private String code;

    public ResponseMessage() {

    }

    public ResponseMessage(String responseType, String status, String code) {
        this.responseType = responseType;
        this.status = status;
        this.code = code;
        setOverallData(determineMessageContents(responseType, status, code));
    }
    public ResponseMessage(String responseType, String status) {
        this(responseType, status, "");
    }

    private String determineMessageContents(String responseType, String status, String code) {
        String overallData = responseType;
        JsonNode node;
        if (code.isBlank() || code.isEmpty() || status.equals("OK")){
            node = getMapper().createObjectNode()
                    .put("status", status);
        } else {
            node = getMapper().createObjectNode()
                    .put("status", status)
                    .put("code", code);
        }

        overallData += " " + node.toString();
        return overallData;
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

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
