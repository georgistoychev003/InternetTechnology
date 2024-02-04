package messages;

public class PingMessage extends Message{
    private String responseType;

    public PingMessage() {
        this.responseType = "PING";
        setOverallData(responseType);
    }


    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }
}
