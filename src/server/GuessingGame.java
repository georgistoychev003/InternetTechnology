package server;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class GuessingGame {
    private static GuessingGame instance = null;
    private boolean gameInProgress;
    private int secretRandomNumber;
    private ConcurrentHashMap<String, ClientHandler> participants;


    private GuessingGame() {
        this.gameInProgress = false;
        this.participants = new ConcurrentHashMap<>();
    }

    public void addParticipant(ClientHandler participant) {
        if (!participants.containsKey(participant.getUsername())) {
            participants.put(participant.getUsername(), participant);
        }
    }

    public boolean isGameInProgress() {
        return gameInProgress;
    }


    //we use singleton to ensure the requirement of only one game/instance is being initiated and ran at a time
    public static GuessingGame getInstance() {
        if (instance == null) {
            instance = new GuessingGame();
        }
        return instance;
    }

    public  boolean startGame(ClientHandler initiator) {
        if (!gameInProgress) {
            gameInProgress = true;
            secretRandomNumber = new Random().nextInt(50) + 1;
            participants.clear();
            participants.put(initiator.getUsername(), initiator);
            return true;
        }
        return false;
    }

    public void endGame() {
        gameInProgress = false;
        participants.clear();
    }
}
