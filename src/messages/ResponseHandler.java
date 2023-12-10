package messages;

public class ResponseHandler {

    public static String determineResponseMessagePrint(ResponseMessage responseMessage) {
        if (responseMessage.getStatus().equals("OK")) {
            return switch (responseMessage.getResponseType()) {
                case "LOGIN_RESP" -> "You just logged in successfully. Welcome!";
                case "BYE_RESP" -> "You have successfully logged out. Bye do not come back!";
                case "BROADCAST_RESP" -> "Broadcast message sent successfully.";
                case "PRIVATE_MESSAGE_RESP" -> "Private message sent successfully.";
                default -> "error";
            };
        } else {
            return getCodeMeaning(responseMessage);
        }
    }

    private static String getCodeMeaning(ResponseMessage responseMessage) {
        return switch (responseMessage.getCode()) {
            case "5000" -> "User with the given username is already logged in.";
            case "5001" ->
                    "Sorry, the username you provided is of invalid format or length. Please try again with a different username.";
            case "5002" -> "A user cannot login twice. You can logout and try to log in again!";
            case "6000" ->
                    "You tried to send a message without being logged in. Your message was not processed! Please log in and then try to send a broadcast message!";
            case "7000", "7001" -> "You were disconnected from the server, due to an internal error";
            case "8000" -> "Server error occurred";
            default -> "Action failed with error code: " + responseMessage.getCode();
        };
    }
}
