package messages;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class EndGameMessage extends Message {
    private String responseType = "GAME_END";
    private List<Map.Entry<String, Long>> gameResults;
    private ObjectMapper mapper = new ObjectMapper();

    public EndGameMessage() {

    }

    public EndGameMessage(List<Map.Entry<String, Long>> gameResults) {
        this.gameResults = gameResults;
        setOverallData(determineMessageContents());
    }

    private String determineMessageContents() {
        String overallData = responseType;
        JsonNode[] resultArray = new JsonNode[gameResults.size()];
        int count = 0;
        for (Map.Entry<String, Long> entry : gameResults) {

            JsonNode node = mapper.createObjectNode()
                    .put("username", entry.getKey())
                    .put("time", entry.getValue());
            resultArray[count] = node;
            count++;
        }
        ObjectNode node = getMapper().createObjectNode()
                .put("results", Arrays.toString(resultArray));
        overallData += " " + node.toString();
        return overallData;
    }

    public List<Map.Entry<String, Long>>  getGameResults() {
        return gameResults;
    }

    public void setGameResults(List<Map.Entry<String, Long>>  gameResults) {
        this.gameResults = gameResults;
        setOverallData(determineMessageContents()); // Update the overall data if game results change
    }

    public String getWinner() {
        return gameResults.get(0).getKey();
    }
}
