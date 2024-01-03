package server;

import messages.Message;
import messages.ResponseMessage;

import java.util.Random;
import java.util.concurrent.*;

public class GuessingGame {
    private static GuessingGame instance = null;
    private boolean gameInitiated;
    private int secretRandomNumber;
    private ConcurrentHashMap<String, ClientHandler> participants;
    private final ScheduledExecutorService executorService;

    private GuessingGame() {
        this.gameInitiated = false;
        this.participants = new ConcurrentHashMap<>();
        this.executorService = Executors.newSingleThreadScheduledExecutor();
    }

    public void addParticipant(ClientHandler participant) {
        if (!participants.containsKey(participant.getUsername())) {
            participants.put(participant.getUsername(), participant);
        }
    }

    public boolean isGameInProgress() {
        return gameInitiated;
    }


    //we use singleton to ensure the requirement of only one game/instance is being initiated and ran at a time
    public static GuessingGame getInstance() {
        if (instance == null) {
            instance = new GuessingGame();
        }
        return instance;
    }

    public ResponseMessage createGame(ClientHandler initiator) {
        if (!gameInitiated) {
            gameInitiated = true;
            participants.clear();
            participants.put(initiator.getUsername(), initiator);
            return new ResponseMessage("GAME_CREATE_RES", "OK");
        }
        return new ResponseMessage("GAME_ERROR_RESP", "ERROR", "9000");
    }
    public ResponseMessage startGame(ClientHandler initiator) {
        if (!gameInitiated) {
            CountDownLatch countDownLatch = new CountDownLatch(1);

            try {
                countDownLatch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            if (participants.size() > 1) {
                secretRandomNumber = new Random().nextInt(50) + 1;
                return new ResponseMessage("GAME_START_RES", "OK");
            } else {
                endGame();
                return new ResponseMessage("GAME_ERROR_RESP", "ERROR", "9001");
            }
        }
        return new ResponseMessage("GAME_ERROR_RESP", "ERROR", "9000");
    }


    public void endGame() {
        gameInitiated = false;
        participants.clear();
    }
}
