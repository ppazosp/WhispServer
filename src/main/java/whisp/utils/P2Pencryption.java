package whisp.utils;

import javax.crypto.*;
import java.security.NoSuchAlgorithmException;

public class P2Pencryption {

    public static String generateKey() {
        //generar una clave secreta
        KeyGenerator keyGen = null;
        try {
            keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey secretKey = keyGen.generateKey();
            // Mostrar la clave generada en formato base64 para legibilidad
            final String encodedKey = java.util.Base64.getEncoder().encodeToString(secretKey.getEncoded());
            return encodedKey;
        } catch (NoSuchAlgorithmException e) {
            Logger.error("Critical error in hashing function");
            throw new IllegalStateException("This should never happen, something went horribly wrong", e);
        }
    }


}
