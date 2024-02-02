package messages;

import com.fasterxml.jackson.databind.JsonNode;
import utils.Utility;

public class EncryptedMessageReq extends Message{

    private String responseType;
    private String receiver;
    private String encryptedMessage;

    public EncryptedMessageReq() {

    }
    public EncryptedMessageReq(String receiver, String encryptedMessage) {
        this.responseType = "ENCRYPTED_MESSAGE_SEND_REQ";
        this.receiver = receiver;
        this.encryptedMessage = encryptedMessage;
        setOverallData(determineMessageContents());
    }


    private String determineMessageContents() {
        String overallData = responseType;
        JsonNode node = getMapper().createObjectNode()
                .put("receiver", receiver)
                .put("encryptedMessage", encryptedMessage);


        overallData += " " + node.toString();
        return overallData;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getEncryptedMessage() {
        return encryptedMessage;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public void setEncryptedMessage(String encryptedMessage) {
        this.encryptedMessage = encryptedMessage;
    }
}
