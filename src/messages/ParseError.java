package messages;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import utils.Utility;

public class ParseError extends Message{

    private String responseType;
    private String error;

    public ParseError() {

    }

    public ParseError(String error) {
        this.responseType = "PARSE_ERROR";
        this.error = error;
        setOverallData(determineMessageContents(responseType, error));
    }
    private String determineMessageContents(String responseType, String error) {
        String overallData = responseType;
        overallData += " " + error;
        return overallData;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
//        setOverallData(determineMessageContents(responseType, status, code));
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
