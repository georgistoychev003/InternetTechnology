package server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import messages.*;
import utils.Utility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class GuessingGame implements Runnable {
    private static GuessingGame instance = null;
    private boolean gameInitiated;
    private boolean gameStarted = false;
    private int secretRandomNumber;
    private int minNumber = 1;
    private int maxNumber = 50;
    private long gameStartTime;
    private ConcurrentHashMap<String, Long> guessTimes;
    private ConcurrentHashMap<String, ClientHandler> participants;
    private ConcurrentHashMap<String, Boolean> usersCorrectlyGuessed;
    private ScheduledExecutorService gameTimer;
    private final ReentrantLock lock = new ReentrantLock();
    private final ObjectMapper mapper = new ObjectMapper();

    private GuessingGame() {
        this.gameInitiated = false;
        this.participants = new ConcurrentHashMap<>();
        this.guessTimes = new ConcurrentHashMap<>();
        this.usersCorrectlyGuessed = new ConcurrentHashMap<>();
        gameTimer = Executors.newSingleThreadScheduledExecutor();
    }

    public void addParticipant(ClientHandler participant) {
        if (!participants.containsKey(participant.getUsername()) && !gameStarted) {
            participants.put(participant.getUsername(), participant);
//            usersCorrectlyGuessed.put(participant.getUsername(), false);
        }
    }

    public boolean isGameInitiated() {
        return gameInitiated;
    }
    public boolean isGameInProgress() {
        return gameStarted;
    }


    //we use singleton to ensure the requirement of only one game/instance is being initiated and ran at a time
    public static GuessingGame getInstance() {
        if (instance == null) {
            instance = new GuessingGame();
            new Thread(instance).start();
        }
        return instance;
    }

    public ResponseMessage createGame(ClientHandler initiator) {
        if (!gameInitiated) {
            gameInitiated = true;
            participants.clear();
            participants.put(initiator.getUsername(), initiator);
            return new ResponseMessage("GAME_CREATE_RESP", "OK");
        }
        return new ResponseMessage("GAME_ERROR_RESP", "ERROR", "9000");
    }
    public ResponseMessage startGame(ClientHandler initiator) {
        System.out.println("is initiated: " + gameInitiated);
        if (gameInitiated) {
            lock.lock();
            try {
                CountDownLatch countDownLatch = new CountDownLatch(1);

                try {
                    countDownLatch.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                if (participants.size() > 1) {
                    gameStartTime = System.currentTimeMillis();
                    gameTimer.schedule(this::endGame, 2, TimeUnit.MINUTES);
                    secretRandomNumber = new Random().nextInt(maxNumber + 1);
                    gameStarted = true;
                    return new ResponseMessage("GAME_START_RESP", "OK");
                } else {
                    endGame();
                    return new ResponseMessage("GAME_ERROR_RESP", "ERROR", "9001");
                }
            } finally {
                lock.unlock();
            }
        }
        return new ResponseMessage("GAME_ERROR_RESP", "ERROR", "9009");
    }

    public Message checkGuess(String guess, String username) {
        if (!gameStarted || !gameInitiated) {
            return new ResponseMessage("GAME_ERROR_RESP", "ERROR", "9007");
        }
        System.out.println(username);
        if (participants.get(username).guessedSecretNumber()) {
            return new ResponseMessage("GAME_ERROR_RESP", "ERROR", "9010");
        }
        int number;
        try {
            number = Integer.parseInt(guess);
        } catch (IllegalArgumentException ex) {
            return new ResponseMessage("GAME_ERROR_RESP", "ERROR", "9002");
        }

        if (number > maxNumber || number < minNumber) {
            return new ResponseMessage("GAME_ERROR_RESP", "ERROR", "9003");
        }

        if (number == secretRandomNumber) {
            long timeTaken = System.currentTimeMillis() - gameStartTime;
            guessTimes.put(username, timeTaken);
//            usersCorrectlyGuessed.put(username, true);
            participants.get(username).setGuessedSecretNumber(true);
            if (allParticipantsGuessedCorrectly()) {
                endGame();
            }
            return new GameGuessResponseMessage("0");
        } else if (number < secretRandomNumber) {
            return new GameGuessResponseMessage("-1");
        } else {
            return new GameGuessResponseMessage("1");
        }
    }

    private boolean allParticipantsGuessedCorrectly() {
        return guessTimes.size() == participants.size();
    }


    public void endGame() {
        // Sort the guessTimes map by values (time taken)
        List<Map.Entry<String, Long>> sortedGuesses = new ArrayList<>(guessTimes.entrySet());
        sortedGuesses.sort(Map.Entry.comparingByValue());
        EndGameMessage endGameMessage = new EndGameMessage(sortedGuesses);
        // Broadcast results only to the participants of the game
        participants.forEach((username, clientHandler) -> {
            clientHandler.sendMessage(endGameMessage.getOverallData());
        });

        // Reset the game state
        gameInitiated = false;
        gameStarted = false;
        participants.clear();
        guessTimes.clear();
    }



    public ConcurrentHashMap<String, ClientHandler> getParticipants() {
        return participants;
    }

    @Override
    public void run() {

    }
}
