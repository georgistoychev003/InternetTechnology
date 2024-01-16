package server;

import java.io.*;
import java.net.Socket;
import java.util.UUID;

public class FileTransferHandler implements Runnable {
    private Socket clientSocket;

    public FileTransferHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            System.out.println("File transfer connection established with: " + clientSocket.getInetAddress().getHostAddress());

            // Receive file transfer details
            DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());

            // Read the sender/receiver indicator
            byte indicator = dataInputStream.readByte();
            char indicatorChar = (char) indicator;

            // Read UUID
            byte[] uuidBytes = new byte[30];
            dataInputStream.readFully(uuidBytes);
            String uuid = new String(uuidBytes);

            // Read checksum
            byte[] checksumBytes = new byte[38];
            dataInputStream.readFully(checksumBytes);
            String checksum = new String(checksumBytes);

            // Read file contents
            // we need to fix this for larger files
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = dataInputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }

            System.out.println("Received File Transfer:");
            System.out.println("Sender/Receiver Indicator: " + indicatorChar);
            System.out.println("UUID: " + uuid);
            System.out.println("Checksum: " + checksum);

            // Save the file
            String fileName = uuid + ".txt";
            try (FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {
                fileOutputStream.write(byteArrayOutputStream.toByteArray());
                System.out.println("File saved: " + fileName);
            }

        } catch (IOException e) {
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
