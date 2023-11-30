package messages;

public abstract class Message {

    private final String overallData;

    public Message(String overallData) {
        this.overallData = overallData;
    }

    public String getOverallData() {
        return overallData;
    }
}
