package encryption_decryption.javaRepo;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class AESExample {
    public static void main(String[] args) throws Exception {
        // Generate a Key for AES
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128); // 128-bit AES
        SecretKey secretKey = keyGenerator.generateKey();

        // Encrypt
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        String plainText = "Hello, World!";
        byte[] encryptedText = cipher.doFinal(plainText.getBytes());
        System.out.println("Encrypted: " + new String(encryptedText));

        // Decrypt
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedText = cipher.doFinal(encryptedText);
        System.out.println("Decrypted: " + new String(decryptedText));
    }
}

