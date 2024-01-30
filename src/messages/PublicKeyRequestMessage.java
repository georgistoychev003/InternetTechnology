package messages;

import com.fasterxml.jackson.databind.JsonNode;

public class PublicKeyRequestMessage extends Message {

    private String responseType = "PUBLIC_KEY_REQ";
    private String targetUsername; // Username of the recipient whose public key is being requested

    public PublicKeyRequestMessage(String targetUsername) {
        this.targetUsername = targetUsername;
        setOverallData(determineMessageContents());
    }

    private String determineMessageContents() {
        String overallData = responseType;
        JsonNode node = getMapper().createObjectNode()
                .put("targetUsername", targetUsername);

        overallData += " " + node.toString();
        return overallData;
    }


    public String getTargetUsername() {
        return targetUsername;
    }

    public void setTargetUsername(String targetUsername) {
        this.targetUsername = targetUsername;
    }
}
