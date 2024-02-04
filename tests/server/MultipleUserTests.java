package server;

import com.fasterxml.jackson.core.JsonProcessingException;
import messages.*;
import org.junit.jupiter.api.*;
import utils.Utility;


import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;
//@TestMethodOrder(MethodOrderer.MethodName.class)
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MultipleUserTests {

    private static Properties props = new Properties();

    private Socket socketUser1, socketUser2, socketUser3;
    private BufferedReader inUser1, inUser2, inUser3;
    private PrintWriter outUser1, outUser2, outUser3;

    private final static int max_delta_allowed_ms = 1000000;
    private boolean gameIsStarted = false;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = MultipleUserTests.class.getResourceAsStream("testconfig.properties");
        props.load(in);
        in.close();
    }

    @BeforeEach
    void setup() throws IOException {
        socketUser1 = new Socket(props.getProperty("host"), Integer.parseInt(props.getProperty("port")));
        inUser1 = new BufferedReader(new InputStreamReader(socketUser1.getInputStream()));
        outUser1 = new PrintWriter(socketUser1.getOutputStream(), true);

        socketUser2 = new Socket(props.getProperty("host"), Integer.parseInt(props.getProperty("port")));
        inUser2 = new BufferedReader(new InputStreamReader(socketUser2.getInputStream()));
        outUser2 = new PrintWriter(socketUser2.getOutputStream(), true);

        socketUser3 = new Socket(props.getProperty("host"), Integer.parseInt(props.getProperty("port")));
        inUser3 = new BufferedReader(new InputStreamReader(socketUser3.getInputStream()));
        outUser3 = new PrintWriter(socketUser3.getOutputStream(), true);

    }

    @AfterEach
    void cleanup() throws IOException {
        outUser1.println("BYE");
        outUser2.println("BYE");
        outUser3.println("BYE");
    }


    @Test
    /* This test is expected to fail with the given NodeJS server because the JOINED is not implemented.
       Make sure the test works when implementing your own server in Java
     */
    void TC3_1_joinedIsReceivedByOtherUserWhenUserConnects() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //WELCOME
        receiveLineWithTimeout(inUser2); //WELCOME

        // Connect user1
        outUser1.println(new LoginMessage("user1"));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user2
        outUser2.println(new LoginMessage("user2"));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK

        //JOINED is received by user1 when user2 connects
        String resIdent = receiveLineWithTimeout(inUser1);
        JoinedMessage joined = Utility.createJoinedClass(resIdent);

        assertEquals(new JoinedMessage("user2").getOverallData(),joined.getOverallData());
    }

    @Test
    /* This test is expected to fail with the given NodeJS server because the JOINED is not implemented.
       Make sure the test works when implementing your own server in Java
     */
    void TC3_2_broadcastMessageIsReceivedByOtherConnectedClients() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //WELCOME
        receiveLineWithTimeout(inUser2); //WELCOME

        // Connect user1
        outUser1.println(new LoginMessage("user1"));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user2
        outUser2.println(new LoginMessage("user2"));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED

        //send BROADCAST from user 1
        outUser1.println(new GlobalMessage("BROADCAST_REQ","user1","messagefromuser1"));

        outUser1.flush();
        String fromUser1 = receiveLineWithTimeout(inUser1);
        ResponseMessage broadcastResp1 = Utility.createResponseClass(fromUser1);

        assertEquals("OK", broadcastResp1.getStatus());

        String fromUser2 = receiveLineWithTimeout(inUser2);
        GlobalMessage broadcast2 = Utility.createGlobalMessageClass(fromUser2);

        assertEquals(new GlobalMessage("BROADCAST","user1","messagefromuser1").getOverallData(), broadcast2.getOverallData());

        //send BROADCAST from user 2
        outUser2.println(new GlobalMessage("BROADCAST_REQ","user2","messagefromuser2"));
        outUser2.flush();
        fromUser2 = receiveLineWithTimeout(inUser2);
        ResponseMessage broadcastResp2 = Utility.createResponseClass(fromUser2);
        assertEquals("OK", broadcastResp2.getStatus());

        fromUser1 = receiveLineWithTimeout(inUser1);
        GlobalMessage broadcast1 = Utility.createGlobalMessageClass(fromUser1);

        assertEquals(new GlobalMessage("BROADCAST", "user2","messagefromuser2").getOverallData(), broadcast1.getOverallData());
    }

    @Test
    void TC3_3_loginMessageWithAlreadyConnectedUsernameReturnsError() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //welcome message
        receiveLineWithTimeout(inUser2); //welcome message

        // Connect user 1
        outUser1.println(new LoginMessage("user1"));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect using same username
        outUser2.println(new LoginMessage("user1"));
        outUser2.flush();
        String resUser2 = receiveLineWithTimeout(inUser2);
        ResponseMessage loginResp = Utility.createResponseClass(resUser2);
        assertEquals(new ResponseMessage("LOGIN_RESP","ERROR", "5000").getOverallData(), loginResp.getOverallData());
    }

    @Test
    void TC3_4_requestClientListWIth2LoggedUsersReturnsItWithTheListedUsers() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //WELCOME
        receiveLineWithTimeout(inUser2); //WELCOME

        // Connect user1
        outUser1.println(new LoginMessage("user1"));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user2
        outUser2.println(new LoginMessage("user2"));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED

        outUser1.println(new ClientListMessage("CLIENT_LIST_REQ"));
        outUser1.flush();

        String userList = receiveLineWithTimeout(inUser1);
        ClientListMessage clientListMessage = Utility.createClientListClass(userList);

        assertEquals(new ClientListMessage("OK", List.of("user1", "user2")).getOverallData(), clientListMessage.getOverallData());
    }

    @Test
    void TC3_6_sendAPrivateMessageToALoggedInUser() {
        receiveLineWithTimeout(inUser1); //WELCOME
        receiveLineWithTimeout(inUser2); //WELCOME

        // Connect user1
        outUser1.println(new LoginMessage("user1"));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user2
        outUser2.println(new LoginMessage("user2"));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED

        outUser1.println(new PrivateMessage("PRIVATE_MESSAGE_REQ", "user1", "user2", "hello bro!"));
        outUser1.flush();

        String successMess = receiveLineWithTimeout(inUser1);
        String prvMess = receiveLineWithTimeout(inUser2);
        PrivateMessage privateMessage = Utility.createPrivateMessageClass(prvMess);
        ResponseMessage responseMessage = Utility.createResponseClass(successMess);

        assertEquals("OK", responseMessage.getStatus());
        assertEquals(new PrivateMessage("PRIVATE_MESSAGE", "user1", "user2", "hello bro!").getOverallData(), privateMessage.getOverallData());
    }

    @Test
    void TC3_7_sendAPrivateMessageToNonExistentUserGivesError() {
        connectTwoClients();

        outUser1.println(new PrivateMessage("PRIVATE_MESSAGE_REQ", "user1", "nouser", "I know you wont receive this message!"));
        outUser1.flush();

        String errorMess = receiveLineWithTimeout(inUser1);
        ResponseMessage responseMessage = Utility.createResponseClass(errorMess);

        assertEquals(new ResponseMessage("PRIVATE_MESSAGE_RESP", "ERROR", "1000").getOverallData(), responseMessage.getOverallData());
    }

    @Test
    void TC3_8_guessingGameCompleteFlow() {
        connectTwoClients();

        outUser1.println("GAME_CREATE_REQ");
        outUser1.flush();

        String createResp = receiveLineWithTimeout(inUser1); // game created OK
        String inviteMess = receiveLineWithTimeout(inUser2); // invitation to join game
        ResponseMessage createResponse = Utility.createResponseClass(createResp);
        GuessingGameInviteMessage inviteMessage = Utility.createGameInviteMessageClass(inviteMess);

        assertEquals(new ResponseMessage("GAME_CREATE_RESP", "OK").getOverallData(), createResponse.getOverallData());
        assertEquals(new GuessingGameInviteMessage("user1").getOverallData(), inviteMessage.getOverallData());

        outUser2.println("GAME_JOIN_REQ");
        outUser2.flush();

        String joinResp = receiveLineWithTimeout(inUser2); // Successfully joined game
        ResponseMessage joinResponse = Utility.createResponseClass(joinResp);

        assertEquals("OK", joinResponse.getStatus());

        receiveLineWithTimeout(inUser1); // PING
        String gameStartResp1 = receiveLineWithTimeout(inUser1); // game started
        receiveLineWithTimeout(inUser2); // PING
        String gameStartResp2 = receiveLineWithTimeout(inUser2); // game started
        ResponseMessage gameStartResponse1 = Utility.createResponseClass(gameStartResp1);
        ResponseMessage gameStartResponse2 = Utility.createResponseClass(gameStartResp2);

        assertTrue(gameStartResponse1.getStatus().equals("OK") && gameStartResponse2.getStatus().equals("OK") ); //

        int counter = 1;
        outUser1.println(new GameGuessMessage(String.valueOf(counter)));
        String gameGuessResp = receiveLineWithTimeout(inUser1); // guess status
        GameGuessResponseMessage guessResponseMessage = Utility.createGameGuessResponseClass(gameGuessResp);
        while (!guessResponseMessage.getNumber().equals("0")) {
            counter++;

            outUser1.println(new GameGuessMessage(String.valueOf(counter)));

            gameGuessResp = receiveLineWithTimeout(inUser1); // guess status
            guessResponseMessage = Utility.createGameGuessResponseClass(gameGuessResp);
        }

        assertEquals(new GameGuessResponseMessage("0").getOverallData(), guessResponseMessage.getOverallData());

        outUser2.println(new GameGuessMessage(String.valueOf(counter))); // player 2 guesses after that
        receiveLineWithTimeout(inUser2);
        String gameEnd = receiveLineWithTimeout(inUser1); // game end score
        System.out.println(gameEnd);
        EndGameMessage endGameMessage = Utility.createEndGameMessageClass(gameEnd);

        assertEquals("user1", endGameMessage.getWinner());
    }

    @Test
    void TC3_9_tryingToCreateAGameAfterOneIsAlreadyCreatedReturnsError() {
        connectTwoClients();

        outUser1.println("GAME_CREATE_REQ");
        outUser1.flush();

        receiveLineWithTimeout(inUser1); // game created OK
        receiveLineWithTimeout(inUser2); // invitation to join game
//        createAndStartGame();

        outUser2.println("GAME_CREATE_REQ");
        outUser2.flush();

        String errorResp = receiveLineWithTimeout(inUser2); // Game error
        ResponseMessage errorResponse = Utility.createResponseClass(errorResp);

        assertEquals(new ResponseMessage("GAME_ERROR_RESP", "ERROR", "9000").getOverallData(), errorResponse.getOverallData());
        receiveLineWithTimeout(inUser1); // game not started
    }

    @Test
    void TC3_010_tryingToStartGameWith1PlayerReturnsError() {
        connectTwoClients();

        outUser1.println("GAME_CREATE_REQ");
        outUser1.flush();

        receiveLineWithTimeout(inUser1); // game created OK

        receiveLineWithTimeout(inUser1); // PING
        receiveLineWithTimeout(inUser1); // game end
        String errorResp = receiveLineWithTimeout(inUser1); // game not started
        ResponseMessage errorResponse = Utility.createResponseClass(errorResp);

        assertEquals(new ResponseMessage("GAME_ERROR_RESP", "ERROR", "9001").getOverallData(), errorResponse.getOverallData());
    }

    @Test
    void TC3_011_tryingToGuessWithInvalidFormatReturnsError() {
        createAndStartGame();

        outUser1.println(new GameGuessMessage("#"));
        String gameGuessResp = receiveLineWithTimeout(inUser1); // guess status
        ResponseMessage errorResp = Utility.createResponseClass(gameGuessResp);

        assertEquals(new ResponseMessage("GAME_ERROR_RESP", "ERROR", "9002").getOverallData(), errorResp.getOverallData());

        guessNumbers();
    }

    @Test
    void TC3_012_tryingToGuessWithNumberOutOfRangeGivesError() {
        createAndStartGame();

        outUser1.println(new GameGuessMessage("51"));
        String gameGuessResp = receiveLineWithTimeout(inUser1); // guess status
        ResponseMessage errorResp = Utility.createResponseClass(gameGuessResp);

        assertEquals(new ResponseMessage("GAME_ERROR_RESP", "ERROR", "9003").getOverallData(), errorResp.getOverallData());

        guessNumbers();
    }

    @Test
    void TC3_013_tryingToJoinGameAfterItHasStartedGivesError() {
        createAndStartGame();

        // Connect user3
        receiveLineWithTimeout(inUser3); //WELCOME
        outUser3.println(new LoginMessage("user3"));
        outUser3.flush();
        receiveLineWithTimeout(inUser3); //PING
        receiveLineWithTimeout(inUser3); //OK
        receiveLineWithTimeout(inUser1); // user3 joined
        receiveLineWithTimeout(inUser2); // user3 joined


        outUser3.println("GAME_JOIN_REQ");
        outUser3.flush();
        String errorResp = receiveLineWithTimeout(inUser3); // join error status
        ResponseMessage errorResponse = Utility.createResponseClass(errorResp);

        assertEquals(new ResponseMessage("GAME_ERROR_RESP", "ERROR", "9004").getOverallData(), errorResponse.getOverallData());
        guessNumbers();
    }

    @Test
    void TC3_014_tryingToMakeGuessWithoutLoggingInGivesError() {
        createAndStartGame();

        receiveLineWithTimeout(inUser3); //WELCOME
        outUser3.println(new GameGuessMessage("5"));
        outUser3.flush();
        receiveLineWithTimeout(inUser3); //PING
        String errorResp = receiveLineWithTimeout(inUser3); // guess error
        ResponseMessage errorResponse = Utility.createResponseClass(errorResp);

        assertEquals(new ResponseMessage("GAME_ERROR_RESP", "ERROR", "9005").getOverallData(), errorResponse.getOverallData());

        guessNumbers();
    }

    @Test
    void TC3_015_tryingToMakeGuessWhenNotPartOfTheGameGivesError() {
        createAndStartGame();

        // Connect user3
        receiveLineWithTimeout(inUser3); //WELCOME
        outUser3.println(new LoginMessage("user3"));
        outUser3.flush();
        receiveLineWithTimeout(inUser3); //PING
        receiveLineWithTimeout(inUser3); //OK
        receiveLineWithTimeout(inUser1); // user3 joined
        receiveLineWithTimeout(inUser2); // user3 joined


        outUser3.println(new GameGuessMessage("5"));
        outUser3.flush();
        String errorResp = receiveLineWithTimeout(inUser3); // guess error status
        ResponseMessage errorResponse = Utility.createResponseClass(errorResp);

        assertEquals(new ResponseMessage("GAME_ERROR_RESP", "ERROR", "9006").getOverallData(), errorResponse.getOverallData());

        guessNumbers();
    }

    @Test
    void TC3_016_tryingToMakeGuessWhenGameHasNotStartedGivesError() {
        connectTwoClients();

        outUser1.println("GAME_CREATE_REQ");
        outUser1.flush();

        receiveLineWithTimeout(inUser1); // game created OK
        receiveLineWithTimeout(inUser2); // invitation to join game

        outUser2.println("GAME_JOIN_REQ");
        outUser2.flush();
        receiveLineWithTimeout(inUser2); // game join success


        outUser2.println(new GameGuessMessage("5"));
        outUser2.flush();
        String errorResp = receiveLineWithTimeout(inUser2); // guess error status
        ResponseMessage errorResponse = Utility.createResponseClass(errorResp);

        assertEquals(new ResponseMessage("GAME_ERROR_RESP", "ERROR", "9007").getOverallData(), errorResponse.getOverallData());

        receiveLineWithTimeout(inUser1); // PING
        receiveLineWithTimeout(inUser1); // game started
        receiveLineWithTimeout(inUser2); // PING
        receiveLineWithTimeout(inUser2); // game started

        guessNumbers();
    }

    @Test
    void TC3_017_tryingToJoinGameTwiceGivesError() {
        createAndStartGame();

        outUser2.println("GAME_JOIN_REQ");
        outUser2.flush();
        String errorResp = receiveLineWithTimeout(inUser2); // game join error
        ResponseMessage errorResponse = Utility.createResponseClass(errorResp);

        assertEquals(new ResponseMessage("GAME_ERROR_RESP", "ERROR", "9008").getOverallData(), errorResponse.getOverallData());

        guessNumbers();
    }

    @Test
    void TC3_018_tryingToMakeGuessesAfterGuessingTheNumberReturnsError() {
        createAndStartGame();

        int counter = 1;
        outUser1.println(new GameGuessMessage(String.valueOf(counter)));
        String gameGuessResp = receiveLineWithTimeout(inUser1); // guess status
        GameGuessResponseMessage guessResponseMessage = Utility.createGameGuessResponseClass(gameGuessResp);
        while (!guessResponseMessage.getNumber().equals("0")) {
            counter++;

            outUser1.println(new GameGuessMessage(String.valueOf(counter)));

            gameGuessResp = receiveLineWithTimeout(inUser1); // guess status
            guessResponseMessage = Utility.createGameGuessResponseClass(gameGuessResp);
        }

        outUser1.println(new GameGuessMessage(String.valueOf(counter))); // trying to guess again
        String guessResp = receiveLineWithTimeout(inUser1); // guess error
        ResponseMessage guessResponse = Utility.createResponseClass(guessResp);

        assertEquals(new ResponseMessage("GAME_ERROR_RESP", "ERROR", "9010").getOverallData(), guessResponse.getOverallData());

        outUser2.println(new GameGuessMessage(String.valueOf(counter))); // player 2 guesses after that
    }


    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), reader::readLine);
    }

    private void connectTwoClients() {
        receiveLineWithTimeout(inUser1); //WELCOME
        receiveLineWithTimeout(inUser2); //WELCOME

//        generateRandomString(4);
        // Connect user1
        outUser1.println(new LoginMessage("user1"));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user2
        outUser2.println(new LoginMessage("user2"));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED
    }

    private void generateRandomString(int i) {
    }

    private void guessNumbers() {
        int counter = 1;
        outUser1.println(new GameGuessMessage(String.valueOf(counter)));
        String gameGuessResp = receiveLineWithTimeout(inUser1); // guess status
        GameGuessResponseMessage guessResponseMessage = Utility.createGameGuessResponseClass(gameGuessResp);
        while (!guessResponseMessage.getNumber().equals("0")) {
            counter++;

            outUser1.println(new GameGuessMessage(String.valueOf(counter)));

            gameGuessResp = receiveLineWithTimeout(inUser1); // guess status
            guessResponseMessage = Utility.createGameGuessResponseClass(gameGuessResp);
        }
        outUser2.println(new GameGuessMessage(String.valueOf(counter))); // player 2 guesses after that
    }

    private void createAndStartGame() {
        if (!gameIsStarted) {
            connectTwoClients();

            outUser1.println("GAME_CREATE_REQ");
            outUser1.flush();

            receiveLineWithTimeout(inUser1); // game created OK
            receiveLineWithTimeout(inUser2); // invitation to join game

            outUser2.println("GAME_JOIN_REQ");
            outUser2.flush();
            receiveLineWithTimeout(inUser2); // game join success

            receiveLineWithTimeout(inUser1); // PING
            receiveLineWithTimeout(inUser1); // game started
            receiveLineWithTimeout(inUser2); // PING
            receiveLineWithTimeout(inUser2); // game started
            gameIsStarted = true;
        }
    }

}