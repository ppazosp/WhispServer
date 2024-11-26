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
    private static final String ALGORITHM_HASHING = "PBKDF2WithHmacSHA256";



    //*******************************************************************************************
    //* METHODS
    //*******************************************************************************************

    /**
     * Genera una clave derivada utilizando {@link Encrypter#ALGORITHM_HASHING} como algoritmo.
     *
     * @param key la clave base (normalmente un nombre de usuario).
     * @param salt el salt codificado en Base64.
     * @return la clave derivada codificada en Base64.
     * @throws IllegalStateException si ocurre un error crítico en el algoritmo de generación de la clave.
     */
    public static String getKey(String key, String salt) {
        try{
            String rkey = new StringBuilder(key).reverse().toString();
            char[] usernameChars = rkey.toCharArray();
            byte[] saltBytes = Base64.getDecoder().decode(salt);
            PBEKeySpec spec = new PBEKeySpec(usernameChars, saltBytes, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM_HASHING);
            byte[] keyBytes = keyFactory.generateSecret(spec).getEncoded();

            return Base64.getEncoder().encodeToString(keyBytes);
        }catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            Logger.error("Critical error in hashing function");
            throw new IllegalStateException("This should never happen, something went horribly wrong", e);
        }
    }

    /**
     * Cifra datos utilizando el algoritmo {@link Encrypter#ALGORITHM_ENCRYPT} y una clave codificada en Base64.
     *
     * @param data los datos que se desean cifrar.
     * @param base64Key la clave AES codificada en Base64.
     * @return los datos cifrados codificados en Base64.
     * @throws IllegalStateException si ocurre un error crítico durante el cifrado.
     */
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

    /**
     * Descifra datos previamente cifrados utilizando el algoritmo {@link Encrypter#ALGORITHM_ENCRYPT} y una clave codificada en Base64.
     *
     * @param encryptedData los datos cifrados codificados en Base64.
     * @param base64Key la clave AES codificada en Base64.
     * @return los datos descifrados como texto plano.
     * @throws IllegalStateException si ocurre un error crítico durante el descifrado.
     */
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

    /**
     * Decodifica una clave AES codificada en Base64 y la convierte en un objeto {@link SecretKey}.
     *
     * @param base64Key la clave AES codificada en Base64.
     * @return un objeto {@link SecretKey} para su uso en cifrado/descifrado.
     */
    private static SecretKey decodeKey(String base64Key) {
        byte[] decodedKey = Base64.getDecoder().decode(base64Key);
        return new SecretKeySpec(decodedKey, ALGORITHM_ENCRYPT);
    }

    /**
     * Genera una clave de autentificación cifrada para el usuario.
     *
     * <p>
     *     Se genera una clave secreta para la autenticación en dos factores,
     *     que luego es cifrada utilizando una clave AES derivada para poder guardarla en la Base de Datos.
     * </p>
     *
     * @param key la clave base para generar la clave AES (normalmente un nombre de usuario).
     * @param salt la sal utilizada para derivar la clave AES.
     * @return la clave de autenticación cifrada, codificada en Base64.
     */
    public static String genAuthKey(String key, String salt) {
        String authKey = TFAService.generateSecretKey();
        String aesKey = Encrypter.getKey(new StringBuilder(key).reverse().toString(), salt);
        return  Encrypter.encrypt(authKey, aesKey);
    }
}
