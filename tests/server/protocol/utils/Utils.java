package server.protocol.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import messages.*;
import utils.Utility;

import java.util.*;

public class Utils {

    private final static ObjectMapper mapper = new ObjectMapper();
    private final static Map<Class<?>, ArrayList<String>> objToNameMapping = new HashMap<>();
    static {
        objToNameMapping.put(LoginMessage.class, new ArrayList<>(List.of("LOGIN")));
        objToNameMapping.put(ResponseMessage.class, new ArrayList<>(List.of("LOGIN_RESP", "BROADCAST_RESP")));
        objToNameMapping.put(GlobalMessage.class, new ArrayList<>(List.of("BROADCAST_REQ", "BROADCAST")));
        objToNameMapping.put(JoinedMessage.class, new ArrayList<>(List.of("JOINED")));
//        objToNameMapping.put(ParseError.class, "PARSE_ERROR");
//        objToNameMapping.put(Pong.class, "PONG");
//        objToNameMapping.put(PongError.class, "PONG_ERROR");
        objToNameMapping.put(WelcomeMessage.class, new ArrayList<>(List.of("WELCOME")));
//        objToNameMapping.put(Ping.class, "PING");
    }
//TODO : ask why it doesnt work
    public static String objectToMessage(Object object) throws JsonProcessingException {
        Class<?> clazz = object.getClass();
        String header = Utility.getResponseType(object.toString());
//        String header = objToNameMapping.get(clazz);
        if (header == null) {
            throw new RuntimeException("Cannot convert this class to a message");
        }
        String body = mapper.writeValueAsString(object);
        return header + " " + body;
    }

    public static <T> T messageToObject(String message) throws JsonProcessingException {
        String[] parts = message.split(" ", 2);
        if (parts.length > 2 || parts.length == 0) {
            throw new RuntimeException("Invalid message");
        }
        String header = parts[0];
        String body = "{}";
        if (parts.length == 2) {
            body = parts[1];
        }
        Class<?> clazz = getClass(header);
        Object obj = mapper.readValue(body, clazz);
        return (T) clazz.cast(obj);
    }

    private static Class<?> getClass(String header) {
        return objToNameMapping.entrySet().stream()
                .filter(e -> e.getValue().contains(header))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cannot find class belonging to header " + header));
    }
}
