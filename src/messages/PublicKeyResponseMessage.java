package messages;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class PublicKeyResponseMessage extends Message {

    private String responseType = "PUBLIC_KEY_RESP";
    private String username;
    private String publicKey; // Can be null in error scenarios

    public PublicKeyResponseMessage(String username, String publicKey) {
        this.username = username;
        this.publicKey = publicKey;
        setOverallData(determineMessageContents());
    }

    private String determineMessageContents() {
        String overallData = responseType;
        JsonNode node = getMapper().createObjectNode()
                .put("username", username)
                .put("publicKey", publicKey);


        overallData += " " + node.toString();
        return overallData;
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

}
