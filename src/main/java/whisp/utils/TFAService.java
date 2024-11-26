package whisp.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class TFAService {

    //*******************************************************************************************
    //* METHODS
    //*******************************************************************************************

    /**
     * Genera una clave secreta para autenticación en dos factores (TFA).
     *
     * @return la clave secreta generada como un {@link String}.
     */
    public static String generateSecretKey() {
        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }

    /**
     * Genera un código QR codificado en Base64 para la configuración de autenticación en dos factores.
     *
     * <p>
     *     El código QR contiene una URL OTP (One-Time Password) compatible con aplicaciones como Google Authenticator.
     * </p>
     *
     * @param secretKey la clave secreta generada para TFA.
     * @param username el nombre de usuario asociado a la clave.
     * @return una cadena codificada en Base64 que representa el código QR en formato PNG.
     * @throws IllegalStateException si ocurre un error durante la generación del código QR.
     */
    public static String generateQRCode(String secretKey, String username) {

        try {
            String issuer = "Whisp";
            String otpAuthURL = String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                    issuer, username, secretKey, issuer);

            BitMatrix bitMatrix = new MultiFormatWriter().encode(otpAuthURL, BarcodeFormat.QR_CODE, 300, 300);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());

        } catch (IOException | WriterException e) {
            Logger.error("Critical error creating QR");
            throw new IllegalStateException("This should never happen, something went horribly wrong", e);
        }
    }
}
