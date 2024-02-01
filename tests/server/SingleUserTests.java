package server;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.*;
import server.protocol.utils.Utils;
import messages.*;
import utils.Utility;

import java.io.*;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;

class SingleUserTests {

    private static Properties props = new Properties();
    private static int ping_time_ms;
    private static int ping_time_ms_delta_allowed;
    private final static int max_delta_allowed_ms = 10000;

    private Socket s;
    private BufferedReader in;
    private PrintWriter out;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = SingleUserTests.class.getResourceAsStream("testconfig.properties");
        props.load(in);
        in.close();

        ping_time_ms = Integer.parseInt(props.getProperty("ping_time_ms", "10000"));
        ping_time_ms_delta_allowed = Integer.parseInt(props.getProperty("ping_time_ms_delta_allowed", "100"));
    }

    @BeforeEach
    void setup() throws IOException {
        s = new Socket(props.getProperty("host"), Integer.parseInt(props.getProperty("port")));
        in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        out = new PrintWriter(s.getOutputStream(), true);
    }

    @AfterEach
    void cleanup() throws IOException {
        out.println("BYE");
        s.close();
    }

    @Test
    void TC5_1_initialConnectionToServerReturnsWelcomeMessage() {
        String firstLine = receiveLineWithTimeout(in);
        assertEquals(new WelcomeMessage("1.0").getOverallData(), firstLine);
    }

    @Test
    void TC5_2_validIdentMessageReturnsOkMessage() throws JsonProcessingException {
        receiveLineWithTimeout(in); //welcome message
        out.println((new LoginMessage("myname").getOverallData()));
        System.out.println((new LoginMessage("myname").getOverallData()));
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        System.out.println(serverResponse);
        ResponseMessage resp = Utility.createResponseClass(serverResponse);
//        ResponseMessage resp = Utils.messageToObject(serverResponse); // TODO: ask ... Utils
        assertEquals("OK", resp.getStatus());
    }

    @Test
    void TC5_3_invalidJsonMessageReturnsParseError() throws JsonProcessingException {
        receiveLineWithTimeout(in); //welcome message
        out.println("LOGIN {\"}");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        String error = serverResponse.split(" ", 2)[1];
        ParseError parseError = new ParseError(error);
        assertNotNull(parseError.getOverallData());
    }

    @Test
    void TC5_4_emptyJsonMessageReturnsError() {
        receiveLineWithTimeout(in); //welcome message
        out.println("LOGIN {\"username\": \"aa\"}");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        ResponseMessage resp = Utility.createResponseClass(serverResponse);
//        ResponseMessage loginResp = Utils.messageToObject(serverResponse);
        assertEquals(new ResponseMessage("LOGIN_RESP", "ERROR", "5001").getOverallData(), resp.getOverallData());
    }

    @Test
    void TC5_5_pongWithoutPingReturnsErrorMessage() throws JsonProcessingException {
        receiveLineWithTimeout(in); //welcome message
        out.println("PONG");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        assertEquals(new PongError("7001").getOverallData(), serverResponse);
    }

    @Test
    void TC5_6_logInTwiceReturnsErrorMessage() throws JsonProcessingException {
        receiveLineWithTimeout(in); //welcome message
        out.println(new LoginMessage("first").getOverallData());
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        ResponseMessage loginResp = Utility.createResponseClass(serverResponse);
        assertEquals("OK", loginResp.getStatus());

        out.println(new LoginMessage("second").getOverallData());
        out.flush();
        serverResponse = receiveLineWithTimeout(in);
        loginResp = Utility.createResponseClass(serverResponse);
        assertEquals(new ResponseMessage("LOGIN_RESP", "ERROR", "5002").getOverallData(), loginResp.getOverallData());
    }

    @Test
    void TC5_7_pingIsReceivedAtExpectedTime(TestReporter testReporter) throws JsonProcessingException {
        receiveLineWithTimeout(in); //welcome message
        Instant start = Instant.now();
        out.println(Utils.objectToMessage(new LoginMessage("myname")));
        out.flush();
        receiveLineWithTimeout(in); //server response

        //Make sure the test does not hang when no response is received by using assertTimeoutPreemptively
        assertTimeoutPreemptively(ofMillis(ping_time_ms + ping_time_ms_delta_allowed), () -> {
            String pingString = in.readLine();
            Instant finish = Instant.now();

            // Make sure the correct response is received
//            Ping ping = Utils.messageToObject(pingString);
            System.out.println(pingString);

            assertNotNull(pingString);

            // Also make sure the response is not received too early
            long timeElapsed = Duration.between(start, finish).toMillis();
            testReporter.publishEntry("timeElapsed", String.valueOf(timeElapsed));
            assertTrue(timeElapsed > ping_time_ms - ping_time_ms_delta_allowed - 300);
        });
    }

    @Test
    void TC5_8_requestingClientListWhenNotLoggedInReturnsError() throws JsonProcessingException {
        receiveLineWithTimeout(in); //welcome message
        out.println(new ClientListMessage("CLIENT_LIST_REQ"));
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        ResponseMessage clientListResp = Utility.createResponseClass(serverResponse);
        assertEquals(new ResponseMessage("CLIENT_LIST_RESP", "ERROR", "6000").getOverallData(), clientListResp.getOverallData());

    }

    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), reader::readLine);
    }

}