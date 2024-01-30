package messages;

import com.fasterxml.jackson.databind.JsonNode;

public class PublicKeyMessage extends Message {

    private String responseType = "PUBLIC_KEY";
    private String username; // Username of the client sending the public key
    private String publicKey;

    public PublicKeyMessage() {

    }

    public PublicKeyMessage(String username, String publicKey) {
        this.username = username;
        this.publicKey = publicKey;
        setOverallData(determineMessageContents());
    }

    private String determineMessageContents() {
        String overallData = responseType;
        JsonNode node = getMapper().createObjectNode()
                .put("username", username)
                .put("public_key", publicKey);

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

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
}
