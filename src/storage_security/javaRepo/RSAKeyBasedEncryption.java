package storage_security.javaRepo;

import javax.crypto.Cipher;
import java.security.*;
import java.util.Base64;

public class RSAKeyBasedEncryption {
    private static KeyPair keyPair;

    static {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize RSA keys", e);
        }
    }

    public static String encrypt(String data) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
        byte[] encryptedData = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    public static String decrypt(String encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
        byte[] decodedData = Base64.getDecoder().decode(encryptedData);
        return new String(cipher.doFinal(decodedData));
    }

    public static void main(String[] args) throws Exception {
        String sensitiveData = "SensitiveData123";

        // Encrypt the data
        String encryptedData = encrypt(sensitiveData);
        System.out.println("Encrypted Data: " + encryptedData);

        // Decrypt the data
        String decryptedData = decrypt(encryptedData);
        System.out.println("Decrypted Data: " + decryptedData);
    }
}