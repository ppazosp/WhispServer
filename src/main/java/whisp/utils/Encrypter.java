package whisp.utils;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public class Encrypter {

    //*******************************************************************************************
    //* CONSTANTS
    //*******************************************************************************************

    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final String ALGORITHM_ENCRYPT = "AES";



    //*******************************************************************************************
    //* METHODS
    //*******************************************************************************************

    public static String getKey(String key, String salt) {
        try{
            String rkey = new StringBuilder(key).reverse().toString();
            char[] usernameChars = rkey.toCharArray();
            byte[] saltBytes = Base64.getDecoder().decode(salt);
            PBEKeySpec spec = new PBEKeySpec(usernameChars, saltBytes, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = keyFactory.generateSecret(spec).getEncoded();

            return Base64.getEncoder().encodeToString(keyBytes);
        }catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            Logger.error("Critical error in hashing function");
            throw new IllegalStateException("This should never happen, something went horribly wrong", e);
        }
    }

    public static String encrypt(String data, String base64Key) {
        try{
            SecretKey secretKey = decodeKey(base64Key);
            Cipher cipher = Cipher.getInstance(ALGORITHM_ENCRYPT);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        }catch (NoSuchPaddingException | NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            Logger.error("Critical error in hashing function");
            throw new IllegalStateException("This should never happen, something went horribly wrong", e);
        }
    }

    public static String decrypt(String encryptedData, String base64Key) {
        try{
            SecretKey secretKey = decodeKey(base64Key);
            Cipher cipher = Cipher.getInstance(ALGORITHM_ENCRYPT);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decryptedBytes);
        }catch (NoSuchPaddingException | NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            Logger.error("Critical error in hashing function");
            throw new IllegalStateException("This should never happen, something went horribly wrong", e);
        }
    }

    private static SecretKey decodeKey(String base64Key) {
        byte[] decodedKey = Base64.getDecoder().decode(base64Key);
        return new SecretKeySpec(decodedKey, ALGORITHM_ENCRYPT);
    }

    public static String genAuthKey(String key, String salt) {
        String authKey = TFAService.generateSecretKey();
        String aesKey = Encrypter.getKey(new StringBuilder(key).reverse().toString(), salt);
        return  Encrypter.encrypt(authKey, aesKey);
    }
}
