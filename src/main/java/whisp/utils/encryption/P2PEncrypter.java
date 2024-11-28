package whisp.utils.encryption;

import whisp.utils.Logger;

import javax.crypto.*;
import java.security.NoSuchAlgorithmException;

public class P2PEncrypter {

    //*******************************************************************************************
    //* STATIC METHODS
    //*******************************************************************************************

    /**
     * Genera una clave secreta AES de 256 bits y la devuelve en formato Base64.
     * <p>
     *
     * @return una cadena que representa la clave secreta codificada en Base64.
     * @throws IllegalStateException si ocurre un error crítico al generar la clave.
     */
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
