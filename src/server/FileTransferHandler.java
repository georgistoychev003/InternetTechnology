package server;

import java.io.IOException;
import java.net.Socket;

public class FileTransferHandler implements Runnable {
    private Socket clientSocket;

    public FileTransferHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            System.out.println("File transfer connection established with: " + clientSocket.getInetAddress().getHostAddress());
        } catch (Exception e) {
            System.err.println("Exception during file transfer handling: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                    System.out.println("File transfer connection closed.");
                }
            } catch (IOException e) {
                System.err.println("Error closing file transfer socket: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
