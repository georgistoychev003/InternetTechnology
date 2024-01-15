package messages;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import utils.Utility;

import java.util.ArrayList;
import java.util.List;

public class ClientListMessage extends Message{

    private String responseType;
    private String status;
    private List<String> users;

    public ClientListMessage() {

    }
    public ClientListMessage(String status, List<String> users) {
        this.responseType = "CLIENT_LIST_RESP";
        this.status = status;
        this.users = users;
        setOverallData(determineMessageContents());
    }

    public ClientListMessage(String responseType) {
        this.responseType = responseType;
        setOverallData(responseType);
    }

    private String determineMessageContents() {
       String usersJsonArray = convertUsersListToJsonArray(users);

        JsonNode node = getMapper().createObjectNode()
                .put("status", status)
                .put("users", usersJsonArray);

        return responseType + " " + node.toString();
    }

    private String convertUsersListToJsonArray(List<String> users) {
        try {
            return new ObjectMapper().writeValueAsString(users);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getUsers() {
        return users;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }
}
