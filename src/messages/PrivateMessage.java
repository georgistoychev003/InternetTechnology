package messages;

import com.fasterxml.jackson.databind.JsonNode;

public class PrivateMessage extends Message {
    private String responseType;
    private String sender;
    private String receiver;
    private String message;

    public PrivateMessage() {

    }
    public PrivateMessage(String responseType, String sender, String receiver, String message) {
        this.responseType = responseType;
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        setOverallData(determineMessageContents());
    }

    private String determineMessageContents() {
        String overallData = responseType;
        JsonNode node = getMapper().createObjectNode()
                    .put("sender", sender)
                    .put("receiver", receiver)
                    .put("message", message);


        overallData += " " + node.toString();
        return overallData;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getMessage() {
        return message;
    }


    public void setMessage(String message) {
        this.message = message;
    }
}
