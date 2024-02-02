package messages;

import com.fasterxml.jackson.databind.JsonNode;
import utils.Utility;

public class EncryptedMessage extends Message{

    private String responseType;
    private String sender;
    private String encryptedMessage;

    public EncryptedMessage() {

    }
    public EncryptedMessage(String sender, String encryptedMessage) {
        this.responseType = "ENCRYPTED_MESSAGE";
        this.sender = sender;
        this.encryptedMessage = encryptedMessage;
        setOverallData(determineMessageContents());
    }


    private String determineMessageContents() {
        String overallData = responseType;
        JsonNode node = getMapper().createObjectNode()
                .put("sender", sender)
                .put("encryptedMessage", encryptedMessage);


        overallData += " " + node.toString();
        return overallData;
    }

    public String getSender() {
        return sender;
    }

    public String getEncryptedMessage() {
        return encryptedMessage;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setEncryptedMessage(String encryptedMessage) {
        this.encryptedMessage = encryptedMessage;
    }
}
