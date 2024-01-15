package server;

import com.fasterxml.jackson.core.JsonProcessingException;
import messages.GlobalMessage;
import messages.LoginMessage;
import messages.ResponseMessage;
import org.junit.jupiter.api.*;
import server.protocol.utils.Utils;
import utils.Utility;

import java.io.*;
import java.net.Socket;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;

class LineEndings {

    private static Properties props = new Properties();

    private Socket s;
    private BufferedReader in;
    private PrintWriter out;

    private final static int max_delta_allowed_ms = 1000;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = LineEndings.class.getResourceAsStream("testconfig.properties");
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
    void TC2_1_loginFollowedByBROADCASTWithWindowsLineEndingsReturnsOk() throws JsonProcessingException {
        receiveLineWithTimeout(in); //welcome message
        String message = new LoginMessage("myname") + "\r\n" +
                new GlobalMessage("BROADCAST_REQ","myname","a") + "\r\n";
        out.print(message);
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        ResponseMessage loginResp = Utility.createResponseClass(serverResponse);
        assertEquals("OK", loginResp.getStatus());

        serverResponse = receiveLineWithTimeout(in);
        ResponseMessage broadcastResp = Utility.createResponseClass(serverResponse);
        assertEquals("OK", broadcastResp.getStatus());
    }

    @Test
    void TC2_2_loginFollowedByBROADCASTWithLinuxLineEndingsReturnsOk() throws JsonProcessingException {
        receiveLineWithTimeout(in); //welcome message
        String message = new LoginMessage("myname") + "\n" +
                new GlobalMessage("BROADCAST_REQ","user2","a") + "\n";
        out.print(message);
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        ResponseMessage loginResp = Utility.createResponseClass(serverResponse);
        assertEquals("OK", loginResp.getStatus());

        serverResponse = receiveLineWithTimeout(in);
        ResponseMessage broadcastResp = Utility.createResponseClass(serverResponse);
        assertEquals("OK", broadcastResp.getStatus());
    }

    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), reader::readLine);
    }

}