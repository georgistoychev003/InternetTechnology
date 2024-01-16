package messages;



import com.fasterxml.jackson.databind.JsonNode;

public class FileReceiveResponseMessage extends Message {
    private String responseType = "FILE_RECEIVE_RESP";
    private String response;

    public FileReceiveResponseMessage() {
    }

    public FileReceiveResponseMessage(String response) {
        this.response = response;
        setOverallData(determineMessageContents());
    }

    private String determineMessageContents() {
        String overallData = responseType;
        JsonNode node = getMapper().createObjectNode()
                .put("response", response);

        overallData += " " + node.toString();
        return overallData;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
