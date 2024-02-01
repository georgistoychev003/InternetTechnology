package messages;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SessionKeyExchangeRequestMessage extends Message {

    private String responseType = "SESSION_KEY_EXCHANGE_REQ";
    private String username;
    private String encryptedSessionKey;

    public SessionKeyExchangeRequestMessage() {

    }

    public SessionKeyExchangeRequestMessage(String username, String encryptedSessionKey) {
        this.username = username;
        this.encryptedSessionKey = encryptedSessionKey;
        setOverallData(determineMessageContents());
    }
    private String determineMessageContents() {
        String overallData = responseType;
        JsonNode node = getMapper().createObjectNode()
                .put("username", username)
                .put("encryptedSessionKey", encryptedSessionKey);

        overallData += " " + node.toString();
        return overallData;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEncryptedSessionKey() {
        return encryptedSessionKey;
    }

    public void setEncryptedSessionKey(String encryptedSessionKey) {
        this.encryptedSessionKey = encryptedSessionKey;
    }
}
