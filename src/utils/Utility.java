package utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import messages.*;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Utility {
    private static ObjectMapper mapper = new ObjectMapper();

    public static JsonNode getMessageContents(String data) {
        String[] parts = data.split(" ", 2);
        //extract the first part-the command
        String responseType = parts[0];
        //check if there are two parts. If so,assign the second part (after the space) to jsonPart.
        // If there is only one part, then there is no JSON content and therefore jsonPart is set to an empty string.
        String jsonPart = parts.length > 1 ? parts[1] : "";
        JsonNode node;
        try {
            node = mapper.readTree(jsonPart);
            //add responseType to the node that is being returned
            ((ObjectNode) node).put("responseType", responseType);
        } catch (JsonProcessingException e) {
            System.out.println("Invalid JSON content: " + jsonPart);
            throw new RuntimeException(e);
        }

        return node;
    }


    public static String extractParameterFromJson(String data, String param) {
        JsonNode jsonNode = getMessageContents(data);
        JsonNode valueNode = jsonNode.get(param);
        return valueNode != null ? valueNode.asText() : null;
    }


    public static List<String> extractUserListFromJson(String data, String param) {
        JsonNode jsonNode = getMessageContents(data).get(param);
        String usersString = jsonNode.asText();
        try {
            return mapper.readValue(usersString, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Map.Entry<String, Long>> extractGameResultListFromJson(String data, String param) {
        JsonNode jsonNode = getMessageContents(data).get(param);
        try {
            String jsonArrayString = jsonNode.asText().replaceAll("^\"|\"$", "");

            // Convert the "time" field to Long if it's a valid long value, otherwise keep it as a string
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> rawList = objectMapper.readValue(jsonArrayString, new TypeReference<List<Map<String, Object>>>() {
            });

            List<Map.Entry<String, Long>> resultList = new ArrayList<>();
            for (Map<String, Object> entry : rawList) {
                String username = (String) entry.get("username");
                Object timeObject = entry.get("time");
                Long time = (timeObject instanceof Number) ? ((Number) timeObject).longValue() : null;
                resultList.add(Map.entry(username, time));
            }
            return resultList;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getResponseType(String data) {
        String[] parts = data.split(" ", 2);
        return parts[0];
    }

    public static ResponseMessage createResponseClass(String response) {
        String responseType = Utility.getResponseType(response);
        String status = Utility.extractParameterFromJson(response, "status");
        String code = "";
        if (status.equals("ERROR")) {
            code = Utility.extractParameterFromJson(response, "code");
        }
        return new ResponseMessage(responseType, status, code);
    }

    public static JoinedMessage createJoinedClass(String message) {
        String username = Utility.extractParameterFromJson(message, "username");
        return new JoinedMessage(username);
    }

    public static LoginMessage createLoginClass(String message) {
        String username = Utility.extractParameterFromJson(message, "username");
        return new LoginMessage(username);
    }

    public static GlobalMessage createGlobalMessageClass(String globalMessage) {
        String responseType = Utility.getResponseType(globalMessage);
        String username = Utility.extractParameterFromJson(globalMessage, "username");
        String message = Utility.extractParameterFromJson(globalMessage, "message");
        return new GlobalMessage(responseType, username, message);
    }

    public static ClientListMessage createClientListClass(String message) {
        String status = Utility.extractParameterFromJson(message, "status");
        List<String> clients = Utility.extractUserListFromJson(message, "users");
        return new ClientListMessage(status, clients);
    }

    public static PrivateMessage createPrivateMessageClass(String message) {
        String responseType = Utility.getResponseType(message);
        String sender = Utility.extractParameterFromJson(message, "sender");
        String receiver = Utility.extractParameterFromJson(message, "receiver");
        String prvMessage = Utility.extractParameterFromJson(message, "message");
        return new PrivateMessage(responseType, sender, receiver, prvMessage);
    }

    public static GuessingGameInviteMessage createGameInviteMessageClass(String message) {
        String user = Utility.extractParameterFromJson(message, "username");
        return new GuessingGameInviteMessage(user);
    }

    public static GameGuessResponseMessage createGameGuessResponseClass(String message) {
        String number = Utility.extractParameterFromJson(message, "number");
        return new GameGuessResponseMessage(number);
    }

    public static EndGameMessage createEndGameMessageClass(String message) {
        List<Map.Entry<String, Long>> results = Utility.extractGameResultListFromJson(message, "results");
        return new EndGameMessage(results);
    }

}
