package client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

public class FileTransfer {

    private String sender;
    private String receiver;
    private File file;
    private String uuid;
    private Socket fileTransferSocket;

    public FileTransfer(String sender, String receiver, File file) {
        this.sender = sender;
        this.receiver = receiver;
        this.file = file;
    }



    public void initiateFileTransfer() {
        new Thread(() -> {
            try {
                // Connect to the file transfer server
                fileTransferSocket = new Socket( "127.0.0.1", 1338);

                // Send the file content to the server
                sendFileContent();

                // Close the file transfer socket
                fileTransferSocket.close();
            } catch (IOException e) {
                System.err.println("Exception during file transfer initiation: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();

    }

    private void sendFileContent() {
        String filePath = file.getPath();
        try (FileInputStream fileInputStream = new FileInputStream(file);
             OutputStream outputStream = fileTransferSocket.getOutputStream()) {

            // Include sender/receiver indicator, UUID, and checksum in the file content
            outputStream.write('S');
            outputStream.write(uuid.getBytes());
            String fileExtension = Optional.of(filePath)
                    .filter(f -> f.contains("."))
                    .map(f -> f.substring(filePath.lastIndexOf(".") + 1)).orElse("");
            if (fileExtension.length() != 3) {
                fileExtension = "txt";
            }
            outputStream.write(fileExtension.getBytes());
            System.out.println("UUID:" + uuid);
            System.out.println("File ext:" + fileExtension);
            String checksum = calculateMD5Checksum(file);
            System.out.println("Checksum: " + checksum);
            System.out.println("Checksum length: " + checksum.length());
            outputStream.write(checksum.getBytes());

            long bytesTransferred = fileInputStream.transferTo(fileTransferSocket.getOutputStream());
            fileInputStream.close();
            System.out.println("File Transfer Complete. Bytes Transferred: " + bytesTransferred);
        } catch (IOException e) {
            System.err.println("Exception during file content transfer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String calculateMD5Checksum(File file) throws IOException {
        // TODO use a way to generate a checksum using the FileStream
        try (FileInputStream fis = new FileInputStream(file); FileChannel channel = fis.getChannel()) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            ByteBuffer buffer = ByteBuffer.allocate(8192);

            while (channel.read(buffer) > 0) {
                buffer.flip();
                md.update(buffer);
                buffer.clear();
            }

            byte[] hash = md.digest();

            return String.format("%032x", new BigInteger(1, hash));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }


    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String toString() {
        return "FileTransfer{" +
                "sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", file=" + file +
                ", uuid='" + uuid + '\'' +
                '}';
    }
}
