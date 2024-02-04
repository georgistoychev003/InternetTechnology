package messages;

import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class Message {

    private String overallData;
    private ObjectMapper mapper = new ObjectMapper();

    public Message() {

    }

    public Message(String oneliner) {
        this.overallData = oneliner;
    }


    public String getOverallData() {
        return overallData;
    }

    @Override
    public String toString() {
        return overallData;
    }

    public void setOverallData(String overallData) {
        this.overallData = overallData;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }
}
