package messages;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class EndGameMessage extends Message {
    private String responseType = "GAME_END";
    private String gameResults;

    public EndGameMessage() {

    }

    public EndGameMessage(String gameResults) {
        this.gameResults = gameResults;
        setOverallData(determineMessageContents());
    }

    private String determineMessageContents() {
        String overallData = responseType;
        ObjectNode node = getMapper().createObjectNode()
                .put("results", gameResults);

        overallData += " " + node.toString();
        return overallData;
    }

    public String getGameResults() {
        return gameResults;
    }

    public void setGameResults(String gameResults) {
        this.gameResults = gameResults;
        setOverallData(determineMessageContents()); // Update the overall data if game results change
    }
}
