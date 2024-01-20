package messages;



import com.fasterxml.jackson.databind.JsonNode;

public class FileReceiveResponseMessage extends Message {
    private String responseType = "FILE_RECEIVE_RESP";
    private String sender;
    private String response;
    private String uuid;

    public FileReceiveResponseMessage() {
    }

    public FileReceiveResponseMessage(String sender, String response, String uuid) {
        this.response = response;
        this.sender = sender;
        this.uuid = uuid;
        setOverallData(determineMessageContents());
    }

    private String determineMessageContents() {
        String overallData = responseType;
        JsonNode node = getMapper().createObjectNode()
                .put("sender", sender)
                .put("response", response)
                .put("uuid", uuid);

        overallData += " " + node.toString();
        return overallData;
    }

    public String getResponse() {
        return response;
    }

    public String getSender() {
        return sender;
    }

    public String getUuid() {
        return uuid;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
