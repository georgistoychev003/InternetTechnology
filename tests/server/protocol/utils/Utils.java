package server.protocol.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import messages.*;
import utils.Utility;

import java.util.*;

public class Utils {

    private final static ObjectMapper mapper = new ObjectMapper();

    public static String objectToMessage(Object object) throws JsonProcessingException {
        String header = Utility.getResponseType(object.toString());
        if (header == null) {
            throw new RuntimeException("Cannot convert this class to a message");
        }
        String body = mapper.writeValueAsString(object);
        return header + " " + body;
    }


}
