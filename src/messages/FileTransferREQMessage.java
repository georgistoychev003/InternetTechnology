package messages;


import com.fasterxml.jackson.databind.JsonNode;
import utils.Utility;

public class FileTransferREQMessage extends Message{

    private String responseType;
    private String sender;
    private String receiver;
    private String fileName;

    public FileTransferREQMessage() {

    }
    public FileTransferREQMessage(String responseType,String sender, String receiver, String fileName) {
        this.responseType = responseType;
        this.sender = sender;
        this.receiver = receiver;
        this.fileName = fileName;
        setOverallData(determineMessageContents());
    }


    private String determineMessageContents() {
        String overallData = responseType;
        JsonNode node = getMapper().createObjectNode()
                .put("sender", sender)
                .put("receiver", receiver)
                .put("fileName", fileName);


        overallData += " " + node.toString();
        return overallData;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getFileName() {
        return fileName;
    }


    public void setFileName(String message) {
        this.fileName = fileName;
    }
}
