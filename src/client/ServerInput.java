package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerInput implements Runnable{
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;


    public ServerInput(Socket socket) throws IOException {
        this.socket = socket;
        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        output = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            String serverResponse;
            while ((serverResponse = input.readLine()) != null) {
                System.out.println("Server: " + serverResponse);
                if (serverResponse.startsWith("PING")) {
                    sendToServer("PONG");
                } else if (serverResponse.startsWith("BYE_RESP")) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendToServer(String message) {
        output.println(message);
    }
}
