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

    public String getCode() {
        switch (code) {
            case "5000":
                return "User with the given username is already logged in.";
            case "5001":
                return "Sorry, the username you provided is of invalid format or length. Please try again with a different username.";
            case "5002":
                return "A user cannot login twice. You can logout and try to log in again!";
            case "6000":
                return "You tried to send a message without being logged in. Your message was not processed! Please log in and then try to send a broadcast message!";
            case "7000", "7001":
                return "You were disconnected from the server, due to an internal error";
            case "8000":
                return "Server error occurred";
            default:
                return "Action failed with error code: " + code;
        }
    }

    @Override
    public String toString() {
        if (status.equals("OK")) {
            if (responseType.equals("LOGIN_RESP")) {
                return "You just logged in successfully. Welcome!";
            } else if (responseType.equals("BYE_RESP")) {
                return "You have successfully logged out. Bye do not come back!";
            } else if (responseType.equals("BROADCAST_RESP")) {
                return "Broadcast message sent successfully.";
            }
            return "error";
        } else {
            return getCode();
        }
    }
}
