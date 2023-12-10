package messages;

public class Message {

    private final String overallData;

    public Message(String overallData) {
        this.overallData = overallData;
    }

    public String getOverallData() {
        return overallData;
    }

    @Override
    public String toString() {
        return "Message{" +
                "overallData='" + overallData + '\'' +
                '}';
    }
}
