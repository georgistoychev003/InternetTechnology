package messages;

public class ResponseHandler {

    public static String determineResponseMessagePrint(ResponseMessage responseMessage) {
        if (responseMessage.getStatus().equals("OK")) {
            return switch (responseMessage.getResponseType()) {
                case "LOGIN_RESP" -> "You just logged in successfully. Welcome!";
                case "BYE_RESP" -> "You have successfully logged out. Bye do not come back!";
                case "BROADCAST_RESP" -> "Broadcast message sent successfully.";
                case "PRIVATE_MESSAGE_RESP" -> "Private message sent successfully.";
                case "GAME_CREATE_RESP" -> "Game lobby created successfully. Starting game in 10 seconds.";
                case "GAME_JOIN_RESP" -> "You successfully joined the game.";
                case "GAME_START_RESP" -> "Game is starting. Guessing range is 1 - 50. Good luck!";
                case "FILE_TRANSFER_RESP" -> "Your file transfer request was sent to the desired recipient. The recipient can accept or decline your request!";
                case "SESSION_KEY_EXCHANGE_RESP" -> "Your session key was successfully distributed to the recipient";
                default -> "Response message could not be determined";
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
                    "You need to be logged in to perform this action! Please log in and try again!";
            case "7000" -> "You were disconnected from the server, due to an internal error";
            case "7001" -> "Internal server error: sending pong without receiving ping";
            case "8000" -> "Server error occurred";
            case "8001" -> "You cannot send a file to a non-existent user";
            case "8002" -> "You cannot send a file to yourself";
            case "8003" -> "Please provide a valid response when accepting/rejecting a file transfer";
            case "8004" -> "The UUID provided when accepting the file transfer, is not valid ";
            case "8005" -> "You cannot accept a file from a non-existing user";
            case "9000" -> "You cannot start a game after one is already created";
            case "9001" -> "Not enough players to start the game";
            case "9002" -> "Invalid guess number format";
            case "9003" -> "Guess out of range";
            case "9004" -> "You cannot join a game after it has started";
            case "9005" -> "You cannot make a guess when you are not logged in";
            case "9006" -> "You cannot make a guess if you are not part of the game";
            case "9007" -> "You cannot make a guess when game hasn’t started";
            case "9008" -> "You cannot join the game twice";
            case "9009" -> "Cannot start a game before creating one";
            case "9010" -> "You have already guessed the number";
            case "11000" -> "User not found or user has no public key.";
            case "11001" -> "User not found during session key exchange";
            default -> "Action failed with error code: " + responseMessage.getCode();
        };
    }
}
