package server;

import messages.GuessingGameInviteMessage;

public class ServerStateManager {

    private static ServerStateManager instance;

    private ServerStateManager() {

    }

    public static ServerStateManager getInstance(){
        if (instance == null){
            instance = new ServerStateManager();
        }
        return instance;
    }

    public boolean isUsernameValid(String username){
        return username != null && username.matches("^[A-Za-z0-9_]{3,14}$");
    }

    public boolean isUsernameAvailable(String username) {
        // Check if username is already used
        return !Server.isLoggedIn(username);
    }

    public String getLoginErrorCode(String username) {
        if (!isUsernameValid(username)) {
            return "5001"; // Invalid format or length
        }
        if (!isUsernameAvailable(username)) {
            return "5000"; // User already logged in
        }
        return "5002"; // User cannot login twice
    }


}
