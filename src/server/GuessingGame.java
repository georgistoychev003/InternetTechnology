package server;

import messages.GameGuessMessage;
import messages.GameGuessResponseMessage;
import messages.Message;
import messages.ResponseMessage;

import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class GuessingGame implements Runnable {
    private static GuessingGame instance = null;
    private boolean gameInitiated;
    private int secretRandomNumber;
    private int minNumber = 1;
    private int maxNumber = 50;
    private ConcurrentHashMap<String, ClientHandler> participants;
    private final ReentrantLock lock = new ReentrantLock();

    private GuessingGame() {
        this.gameInitiated = false;
        this.participants = new ConcurrentHashMap<>();
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
                    secretRandomNumber = new Random().nextInt(maxNumber + 1);
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

    public Message checkGuess(String guess) {
        int number;
        try {
            number = Integer.parseInt(guess);
        } catch (IllegalArgumentException ex) {
            return new ResponseMessage("GAME_ERROR_RESP", "ERROR", "9002");
        }


        if (number > maxNumber || number < minNumber) {
            return new ResponseMessage("GAME_ERROR_RESP", "ERROR", "9003");
        }

        if (number > secretRandomNumber) {
            return new GameGuessResponseMessage("1");
        } else if (number < secretRandomNumber) {
            return new GameGuessResponseMessage("-1");
        } else {
            return new GameGuessResponseMessage("0");
        }
    }


    public void endGame() {
        gameInitiated = false;
        participants.clear();
    }

    public ConcurrentHashMap<String, ClientHandler> getParticipants() {
        return participants;
    }

    @Override
    public void run() {

    }
}
