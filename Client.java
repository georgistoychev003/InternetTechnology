import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 1337;

    private Socket socket;
    private PrintWriter output;
    private BufferedReader input;
    private Scanner scanner;

    public Client() {
        scanner = new Scanner(System.in);
    }

    public void run() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            output = new PrintWriter(socket.getOutputStream(), true);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            new Thread(this::listenToServer).start();
            handleUserInput();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleUserInput() {
        while (true) {
            String helpOptions = scanner.nextLine();
            if (helpOptions.startsWith("login ")) {
                String username = helpOptions.substring(6);
                sendToServer("LOGIN {\"username\":\"" + username + "\"}");
            } else if (helpOptions.startsWith("message ")) {
                String message = helpOptions.substring(8);
                sendToServer("BROADCAST_REQ {\"message\":\"" + message + "\"}");
            } else if (helpOptions.equalsIgnoreCase("logout")) {
                sendToServer("BYE");
                break;
            } else if (helpOptions.equalsIgnoreCase("help")) {
                showHelpMenu();
            } else {
                System.out.println("This command is not valid, please type help to get the valid list of commands!");
            }
        }
    }


    private void sendToServer(String message) {
        output.println(message);
    }

    private void listenToServer() {
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
        } finally {
            closeSockets();
        }
    }

    private void showHelpMenu() {
        System.out.println("Commands:");
        System.out.println("login <username> - Login to the server");
        System.out.println("<message> - Send a broadcast message");
        System.out.println("<logout> - Logout from the server");
    }

    private void closeSockets() {
        try {
            if (output != null) output.close();
            if (input != null) input.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Client().run();
    }
}