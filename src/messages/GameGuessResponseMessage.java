package messages;

import com.fasterxml.jackson.databind.JsonNode;

public class GameGuessResponseMessage extends Message{

    private String responseType;
    private String number;
    public GameGuessResponseMessage() {

    }

    public GameGuessResponseMessage(String number) {
        this.responseType = "GUESS_RESP";
        this.number = number;
        setOverallData(determineMessageContents());
    }

    private String determineMessageContents() {
        String overallData = responseType;
        JsonNode node = getMapper().createObjectNode()
                .put("number", number);

        overallData += " " + node.toString();
        return overallData;
    }

    public String getResponseType() {
        return responseType;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }
}
