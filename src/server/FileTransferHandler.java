package server;

import client.FileTransfer;

import java.io.*;
import java.net.Socket;
import java.util.UUID;

public class FileTransferHandler implements Runnable {
    private Socket senderSocket;
    private Socket receiverSocket;

    public FileTransferHandler(Socket senderSocket, Socket receiverSocket) {
        this.senderSocket = senderSocket;
        this.receiverSocket = receiverSocket;
    }

    @Override
    public void run() {
        try {
            System.out.println("File transfer connection established with: " + senderSocket.getInetAddress().getHostAddress());

            // Receive file transfer details
            DataInputStream dataInputStream = new DataInputStream(senderSocket.getInputStream());
            OutputStream outputStream = receiverSocket.getOutputStream();
            // Read the sender/receiver indicator
            byte indicator = dataInputStream.readByte();
            char indicatorChar = (char) indicator;

            // Read UUID
            byte[] uuidBytes = new byte[36];
            dataInputStream.readFully(uuidBytes);
            String uuid = new String(uuidBytes);

            // Read checksum
            byte[] checksumBytes = new byte[32];
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

            outputStream.write(indicator);
            outputStream.write(uuidBytes);
            outputStream.write(checksumBytes);

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

            long bytesTransferred = byteArrayInputStream.transferTo(receiverSocket.getOutputStream());
            System.out.println("Bytes transferred to receiver: " + bytesTransferred);

//            // Save the file
//            String fileName = uuid + ".txt";
//            try (FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {
//                fileOutputStream.write(byteArrayOutputStream.toByteArray());
//                System.out.println("File saved: " + fileName);
//            }

        } catch (IOException e) {
            System.err.println("Exception during file transfer handling: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (senderSocket != null && !senderSocket.isClosed()) {
                    senderSocket.close();
                    System.out.println("File transfer connection closed.");
                }
            } catch (IOException e) {
                System.err.println("Error closing file transfer socket: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
