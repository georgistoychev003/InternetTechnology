package client;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

public class EncryptionUtilities {


    public static String getEncodedPublicKey() {
        // Method to get the public key in a transferable format
        return Base64.getEncoder().encodeToString(Client.getPublicKey().getEncoded());
    }

    public static PublicKey convertStringToPublicKey(String publicKeyStr) throws Exception {
        byte[] publicBytes = Base64.getDecoder().decode(publicKeyStr);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    public static SecretKey convertStringToSecretKey(String sessionKeyString) {
        byte[] decodedKey = Base64.getDecoder().decode(sessionKeyString);
        SecretKey originalKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        return originalKey;
    }

    public static String convertByteArrayKeyToString(byte[] key) {
        return Base64.getEncoder().encodeToString(key);
    }

    public static byte[] convertStringToByteArray(String encryptedString) {
        return Base64.getDecoder().decode(encryptedString);
    }

    public static SecretKey generateSessionKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        return keyGen.generateKey();
    }

    public static byte[] encryptSessionKey(SecretKey sessionKey, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(sessionKey.getEncoded());
    }

    public static byte[] decryptSessionKey(SecretKey sessionKey, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(sessionKey.getEncoded());
    }


    public static byte[] encryptMessage(String message, SecretKey sessionKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        // Generate IV
        byte[] iv = new byte[cipher.getBlockSize()];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        IvParameterSpec ivParams = new IvParameterSpec(iv);

        // Initialize cipher with IV
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(sessionKey.getEncoded(), "AES"), ivParams);

        // Encrypt the message
        byte[] encryptedMessage = cipher.doFinal(message.getBytes());

        // Combine IV and encrypted message
        byte[] messageWithIV = new byte[iv.length + encryptedMessage.length];
        System.arraycopy(iv, 0, messageWithIV, 0, iv.length);
        System.arraycopy(encryptedMessage, 0, messageWithIV, iv.length, encryptedMessage.length);

        return messageWithIV;
    }

    public static String decryptMessage(byte[] encryptedMessage, SecretKey sessionKey) {

        byte[] decryptedBytes;
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = Arrays.copyOfRange(encryptedMessage, 0, 16);
            IvParameterSpec ivParams = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, sessionKey, ivParams);
            decryptedBytes = cipher.doFinal(Arrays.copyOfRange(encryptedMessage, 16, encryptedMessage.length));
        } catch (NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException |
                 NoSuchPaddingException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}
