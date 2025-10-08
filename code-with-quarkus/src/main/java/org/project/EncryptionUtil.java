package org.project;

import io.quarkus.logging.Log;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class EncryptionUtil {

    private static final String SECRET_KEY = "MySuperSecretKey"; // 16 chars for AES
    private static final String ALGORITHM = "AES/ECB/PKCS5Padding";

    private static SecretKeySpec getKeySpec() {
        return new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
    }

    /**
     * Encrypts the given input string using AES and returns Base64 encoded string.
     */
    public static String encrypt(String input) throws Exception {
        Log.debug("Encrypting input: " + input);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, getKeySpec());
        byte[] encryptedBytes = cipher.doFinal(input.getBytes());
        String encrypted = Base64.getEncoder().encodeToString(encryptedBytes);
        Log.debug("Generated encrypted string: " + encrypted);
        return encrypted;
    }

    /**
     * Decrypts a Base64 encoded AES encrypted string back to plain text.
     */
    public static String decrypt(String encryptedInput) throws Exception {
        Log.debug("Decrypting input: " + encryptedInput);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, getKeySpec());
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedInput);
        String decrypted = new String(cipher.doFinal(decodedBytes));
        Log.debug("Decrypted result: " + decrypted);
        return decrypted;
    }

    /**
     * Utility method to validate encryption-decryption consistency.
     */
    public static boolean verifyEncryption(String original, String encrypted) {
        try {
            return original.equals(decrypt(encrypted));
        } catch (Exception e) {
            Log.error("Encryption verification failed", e);
            return false;
        }
    }
}
