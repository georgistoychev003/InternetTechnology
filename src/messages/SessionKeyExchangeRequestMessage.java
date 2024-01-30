package messages;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SessionKeyExchangeRequestMessage extends Message {

    private String responseType = "SESSION_KEY_EXCHANGE_REQ";
    private String receiverUsername;
    private String encryptedSessionKey;

    public SessionKeyExchangeRequestMessage() {

    }

    public SessionKeyExchangeRequestMessage(String receiverUsername, String encryptedSessionKey) {
        this.receiverUsername = receiverUsername;
        this.encryptedSessionKey = encryptedSessionKey;
        setOverallData(determineMessageContents());
    }
    private String determineMessageContents() {
        String overallData = responseType;
        JsonNode node = getMapper().createObjectNode()
                .put("receiver", receiverUsername)
                .put("encrypted_session_key", encryptedSessionKey);

        overallData += " " + node.toString();
        return overallData;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getReceiverUsername() {
        return receiverUsername;
    }

    public void setReceiverUsername(String receiverUsername) {
        this.receiverUsername = receiverUsername;
    }

    public String getEncryptedSessionKey() {
        return encryptedSessionKey;
    }

    public void setEncryptedSessionKey(String encryptedSessionKey) {
        this.encryptedSessionKey = encryptedSessionKey;
    }
}
