package whisp.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class TFAService {
    public static String generateSecretKey() {
        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }

    public static String generateQRCode(String secretKey, String username) {

        try {
            String issuer = "Whisp";
            String otpAuthURL = String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                    issuer, username, secretKey, issuer);

            BitMatrix bitMatrix = new MultiFormatWriter().encode(otpAuthURL, BarcodeFormat.QR_CODE, 300, 300);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        }catch (Exception e){
            Logger.error("Cannot generate QR");
        }

        return "";
    }
}
