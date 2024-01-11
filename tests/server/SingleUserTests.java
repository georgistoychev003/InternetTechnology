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
    private final static int max_delta_allowed_ms = 100; // TODO: ask what is this - gives an error first time we run T2

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
        s.close();
    }

    @Test
    void TC5_1_initialConnectionToServerReturnsWelcomeMessage() throws JsonProcessingException {
        String firstLine = receiveLineWithTimeout(in);
        WelcomeMessage welcome = Utils.messageToObject(firstLine);
        assertEquals(new WelcomeMessage("WELCOME {\"msg\":\"Welcome to the server 1.0\"}"), welcome);
    }

    @Test
    void TC5_2_validIdentMessageReturnsOkMessage() throws JsonProcessingException {
        receiveLineWithTimeout(in); //welcome message
        out.println((new LoginMessage("myname").getOverallData()));
        System.out.println((new LoginMessage("myname").getOverallData()));
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        System.out.println(serverResponse);
        String responseType = Utility.getResponseType(serverResponse);
        String status = Utility.extractParameterFromJson(serverResponse, "status");
        ResponseMessage resp = new ResponseMessage(responseType, status);
//        ResponseMessage resp = Utils.messageToObject(serverResponse); // TODO: ask ... Utils
        assertEquals("OK", resp.getStatus());
    }

//    @Test
//    void TC5_3_invalidJsonMessageReturnsParseError() throws JsonProcessingException {
//        receiveLineWithTimeout(in); //welcome message
//        out.println("LOGIN {\"}");
//        out.flush();
//        String serverResponse = receiveLineWithTimeout(in);
//        ParseError parseError = Utils.messageToObject(serverResponse);
//        assertNotNull(parseError);
//    }

    @Test
    void TC5_4_emptyJsonMessageReturnsError() throws JsonProcessingException {
        receiveLineWithTimeout(in); //welcome message
        out.println("LOGIN {\"username\": \"aa\"}");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        ResponseMessage loginResp = Utils.messageToObject(serverResponse);
        assertEquals(new ResponseMessage("ERROR", "5001"), loginResp);
    }

//    @Test
//    void TC5_5_pongWithoutPingReturnsErrorMessage() throws JsonProcessingException {
//        receiveLineWithTimeout(in); //welcome message
//        out.println(Utils.objectToMessage(new Pong()));
//        out.flush();
//        String serverResponse = receiveLineWithTimeout(in);
//        PongError pongError = Utils.messageToObject(serverResponse);
//        assertEquals(new PongError(8000), pongError);
//    }

    @Test
    void TC5_6_logInTwiceReturnsErrorMessage() throws JsonProcessingException {
        receiveLineWithTimeout(in); //welcome message
        out.println(Utils.objectToMessage(new LoginMessage("first")));
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        ResponseMessage loginResp = Utils.messageToObject(serverResponse);
        assertEquals("OK", loginResp.getStatus());

        out.println(Utils.objectToMessage(new LoginMessage("second")));
        out.flush();
        serverResponse = receiveLineWithTimeout(in);
        loginResp = Utils.messageToObject(serverResponse);
        assertEquals(new ResponseMessage("ERROR", "5002"), loginResp);
    }

//    @Test
//    void TC5_7_pingIsReceivedAtExpectedTime(TestReporter testReporter) throws JsonProcessingException {
//        receiveLineWithTimeout(in); //welcome message
//        out.println(Utils.objectToMessage(new LoginMessage("myname")));
//        out.flush();
//        receiveLineWithTimeout(in); //server response
//
//        //Make sure the test does not hang when no response is received by using assertTimeoutPreemptively
//        assertTimeoutPreemptively(ofMillis(ping_time_ms + ping_time_ms_delta_allowed), () -> {
//            Instant start = Instant.now();
//            String pingString = in.readLine();
//            Instant finish = Instant.now();
//
//            // Make sure the correct response is received
//            Ping ping = Utils.messageToObject(pingString);
//
//            assertNotNull(ping);
//
//            // Also make sure the response is not received too early
//            long timeElapsed = Duration.between(start, finish).toMillis();
//            testReporter.publishEntry("timeElapsed", String.valueOf(timeElapsed));
//            assertTrue(timeElapsed > ping_time_ms - ping_time_ms_delta_allowed);
//        });
//    }

    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), reader::readLine);
    }

}