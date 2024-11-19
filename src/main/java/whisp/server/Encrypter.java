package whisp.server;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class Encrypter {

    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final String ALGORITHM_HASH = "PBKDF2WithHmacSHA256";

    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16]; // 16 bytes = 128 bits
        random.nextBytes(salt);
        return salt;
    }

    // Hashes a password using PBKDF2
    public static String getHashedPassword(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM_HASH);
        byte[] hashedPassword = factory.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hashedPassword);
    }

    // Returns the salt and hashed password
    public static String[] createHashPassword(String password) throws Exception {
        byte[] salt = generateSalt();
        String hashedPassword = getHashedPassword(password, salt);
        return new String[]{
                Base64.getEncoder().encodeToString(salt),
                hashedPassword
        };
    }


    private static final String ALGORITHM_ENCRYPT = "AES";
    private static final int KEY_SIZE = 256;

    public static String getKey(String username, String salt) throws Exception {
        char[] usernameChars = username.toCharArray();
        byte[] saltBytes = Base64.getDecoder().decode(salt);

        PBEKeySpec spec = new PBEKeySpec(usernameChars, saltBytes, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = keyFactory.generateSecret(spec).getEncoded();

        return Base64.getEncoder().encodeToString(keyBytes);
    }

    public static String encrypt(String data, String base64Key) throws Exception {
        SecretKey secretKey = decodeKey(base64Key);
        Cipher cipher = Cipher.getInstance(ALGORITHM_ENCRYPT);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public static String decrypt(String encryptedData, String base64Key) throws Exception {
        SecretKey secretKey = decodeKey(base64Key);
        Cipher cipher = Cipher.getInstance(ALGORITHM_ENCRYPT);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedBytes);
    }

    private static SecretKey decodeKey(String base64Key) {
        byte[] decodedKey = Base64.getDecoder().decode(base64Key);
        return new SecretKeySpec(decodedKey, ALGORITHM_ENCRYPT);
    }
}
