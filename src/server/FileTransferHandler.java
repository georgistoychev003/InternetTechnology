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

            //Read fileExtension
            byte[] fileExtensionBytes = new byte[3];
            dataInputStream.readFully(fileExtensionBytes);
            String fileExtension = new String(fileExtensionBytes);

            // Read checksum
            byte[] checksumBytes = new byte[32];
            dataInputStream.readFully(checksumBytes);
            String checksum = new String(checksumBytes);


            System.out.println("Received File Transfer:");
            System.out.println("Checksum: " + checksum);
            System.out.println("File extension: " + fileExtension);

            outputStream.write(fileExtensionBytes);
            outputStream.write(checksumBytes);

            long bytesTransferred = dataInputStream.transferTo(receiverSocket.getOutputStream());
            System.out.println("Bytes transferred to receiver: " + bytesTransferred);

            // Signal that transfer finished
            outputStream.close();


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
