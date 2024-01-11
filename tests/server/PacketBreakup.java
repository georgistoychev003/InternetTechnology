package server;

import com.fasterxml.jackson.core.JsonProcessingException;
import messages.ResponseMessage;
import org.junit.jupiter.api.*;
import server.protocol.utils.Utils;

import java.io.*;
import java.net.Socket;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;

class PacketBreakup {

    private static Properties props = new Properties();

    private Socket s;
    private BufferedReader in;
    private PrintWriter out;

    private final static int max_delta_allowed_ms = 100;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = PacketBreakup.class.getResourceAsStream("testconfig.properties");
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
    void TC4_1_identFollowedByBroadcastWithMultipleFlushReturnsOk() throws JsonProcessingException {
        receiveLineWithTimeout(in); //welcome message
        out.print("LOGIN {\"username\":\"m");
        out.flush();
        out.print("yname\"}\r\nBROAD");
        out.flush();
        out.print("BROADCAST_REQ {\"message\":\"a\"}\r\n");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        ResponseMessage loginResp = Utils.messageToObject(serverResponse);
        assertEquals("OK", loginResp.getStatus());
        serverResponse = receiveLineWithTimeout(in);
        ResponseMessage broadcastResp = Utils.messageToObject(serverResponse);
        assertEquals("OK", broadcastResp.getStatus());
    }

    private String receiveLineWithTimeout(BufferedReader reader){
        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), reader::readLine);
    }

}