package messages;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class PublicKeyResponseMessage extends ResponseMessage {

    private String responseType = "PUBLIC_KEY_RESP";
    private String status;
    private String publicKey; // Can be null in error scenarios
    private String errorCode;

    public PublicKeyResponseMessage(String status, String publicKey, String errorCode) {
        this.status = status;
        this.publicKey = publicKey;
        this.errorCode = errorCode;
        setOverallData(determineMessageContents());
    }

    private String determineMessageContents() {
        ObjectNode node = getMapper().createObjectNode();
        node.put("status", status);

        if ("OK".equals(status) && publicKey != null) {
            node.put("publicKey", publicKey);
        } else if ("ERROR".equals(status)) {
            node.put("code", errorCode != null ? errorCode : "");
        }

        return responseType + " " + node.toString();
    }

    // Getters and setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}
