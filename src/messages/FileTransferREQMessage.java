package messages;


import com.fasterxml.jackson.databind.JsonNode;
import utils.Utility;

public class FileTransferREQMessage extends Message{

    private String responseType;
    private String username;
    private String fileName;

    public FileTransferREQMessage() {

    }
    public FileTransferREQMessage(String responseType,String username, String fileName) {
        this.responseType = responseType;
        this.username = username;
        this.fileName = fileName;
        setOverallData(determineMessageContents());
    }


    private String determineMessageContents() {
        String overallData = responseType;
        JsonNode node = getMapper().createObjectNode()
                .put("username", username)
                .put("fileName", fileName);


        overallData += " " + node.toString();
        return overallData;
    }

    public String getUsername() {
        return username;
    }

    public String getFileName() {
        return fileName;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setFileName(String message) {
        this.fileName = fileName;
    }
}
