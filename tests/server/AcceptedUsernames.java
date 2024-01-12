package server;

import com.fasterxml.jackson.core.JsonProcessingException;
import messages.LoginMessage;
import messages.ResponseMessage;
import org.junit.jupiter.api.*;
import server.protocol.utils.Utils;

import java.io.*;
import java.net.Socket;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;

class AcceptedUsernames {

    private static Properties props = new Properties();

    private Socket s;
    private BufferedReader in;
    private PrintWriter out;
    private final static int max_delta_allowed_ms = 100;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = AcceptedUsernames.class.getResourceAsStream("testconfig.properties");
        props.load(in);
        in.close();
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
    void TC1_1_userNameWithThreeCharactersIsAccepted() throws JsonProcessingException {
        receiveLineWithTimeout(in); //welcome message
        out.println(Utils.objectToMessage(new LoginMessage("mym")));
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        ResponseMessage loginResp = Utils.messageToObject(serverResponse);
        assertEquals("OK", loginResp.getStatus());
    }

    @Test
    void TC1_2_userNameWithTwoCharactersReturnsError() throws JsonProcessingException {
        receiveLineWithTimeout(in); //welcome message
        out.println(Utils.objectToMessage(new LoginMessage("my")));
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        ResponseMessage loginResp = Utils.messageToObject(serverResponse);
        assertEquals(new ResponseMessage("ERROR","5001"),loginResp, "Too short username accepted: " + serverResponse);
    }

    @Test
    void TC1_3_userNameWith14CharactersIsAccepted() throws JsonProcessingException {
        receiveLineWithTimeout(in); //welcome message
        out.println(Utils.objectToMessage(new LoginMessage("abcdefghijklmn")));
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        ResponseMessage loginResp = Utils.messageToObject(serverResponse);
        assertEquals("OK", loginResp.getStatus());
    }

    @Test
    void TC1_4_userNameWith15CharectersReturnsError() throws JsonProcessingException {
        receiveLineWithTimeout(in); //welcome message
        out.println(Utils.objectToMessage(new LoginMessage("abcdefghijklmop")));
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        ResponseMessage loginResp = Utils.messageToObject(serverResponse);
        assertEquals(new ResponseMessage("ERROR","5001"), loginResp, "Too long username accepted: " + serverResponse);
    }

    @Test
    void TC1_5_userNameWithStarReturnsError() throws JsonProcessingException {
        receiveLineWithTimeout(in); //welcome message
        out.println(Utils.objectToMessage(new LoginMessage("*a*")));
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        ResponseMessage loginResp = Utils.messageToObject(serverResponse);
        assertEquals(new ResponseMessage("ERROR","5001"), loginResp, "Wrong character accepted");
    }

    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), reader::readLine);
    }

}