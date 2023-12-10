package messages;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import utils.Utility;

import java.util.ArrayList;

public class ClientListMessage extends Message{

    private String responseType;
    private String status;
    private JsonNode users;
    public ClientListMessage(String overallData) {
        super(overallData);
        determineMessageContents(overallData);
    }

    private void determineMessageContents(String overallData) {
        JsonNode responseNode = Utility.getMessageContents(overallData);
        responseType = responseNode.get("responseType").asText();
        if (responseNode.has("status")){
            status = responseNode.get("status").asText();
        }
        if (responseNode.has("users")){
           String usersJsonArray = responseNode.get("users").toString();
            try {
                this.users = new ObjectMapper().readTree(usersJsonArray);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public JsonNode getUsers() {
        return users;
    }
}
