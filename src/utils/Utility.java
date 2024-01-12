package utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import messages.ResponseMessage;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Utility {
    private static ObjectMapper mapper = new ObjectMapper();
    public static JsonNode getMessageContents(String data){
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
            ((ObjectNode)node).put("responseType", responseType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return node;
    }

//    public static String extractParameterFromJson(String data, String param){
//        JsonNode jsonNode = getMessageContents(data);
//        return jsonNode.get(param).asText();
//    }
    public static String extractParameterFromJson(String data, String param){
        JsonNode jsonNode = getMessageContents(data);
        JsonNode valueNode = jsonNode.get(param);
        return valueNode != null ? valueNode.asText() : null;
    }


    public static List<String> extractUserListFromJson(String data, String param){
        JsonNode jsonNode = getMessageContents(data).get(param);
        String usersString = jsonNode.asText();
        try {
            return mapper.readValue(usersString, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Map.Entry<String,Long>> extractGameResultListFromJson(String data, String param){
        JsonNode jsonNode = getMessageContents(data).get(param);
        try {
            String jsonArrayString = jsonNode.asText().replaceAll("^\"|\"$", "");

            // Convert the "time" field to Long if it's a valid long value, otherwise keep it as a string
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> rawList = objectMapper.readValue(jsonArrayString, new TypeReference<List<Map<String, Object>>>() {});

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

    public static String getResponseType(String data){
        String[] parts = data.split(" ", 2);
        return parts[0];
    }


}
