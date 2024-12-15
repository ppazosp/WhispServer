package whisp.utils;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

public class SSLConfigurator {

    //*******************************************************************************************
    //* CONSTANTS
    //*******************************************************************************************

    private static final String SERVER_KEYSTORE = "server.keystore";
    private static final String SERVER_TRUSTSTORE = "server.truststore";
    private static final String PASSWORD = "password";



    //*******************************************************************************************
    //* STATIC METHODS
    //*******************************************************************************************

    /**
     * Genera una clave y un certificado X.509 para el servidor.
     *
     * <p>
     *     Guarda el certificado y la clave en un KeyStore y exporta el certificado al TrustStore del cliente.
     * </p>
     */
    public void genKeyCertificateServer() {

        try {
            KeyStore trustStore = KeyStore.getInstance("JKS");
            try (FileInputStream trustStoreInput = new FileInputStream(SERVER_TRUSTSTORE)) {
                trustStore.load(trustStoreInput, PASSWORD.toCharArray());
            } catch (FileNotFoundException e) {
                // Si el TrustStore no existe, lo creamos vacío
                trustStore.load(null, PASSWORD.toCharArray());
            }

            // Verificar si ya existe el certificado en el TrustStore
            if (trustStore.containsAlias("rmiserver")) {
                System.out.println("El certificado ya existe en el TrustStore. Reutilizándolo.");
                return;
            }

            // Si no existe, generar clave y certificado del servidor
            KeyPair serverKeyPair = generateKeyPair();
            X509Certificate serverCert = generateCertificate(serverKeyPair, "CN=rmiserver, OU=Development, L=Santiago, C=ES");

            saveKeyStore(SERVER_KEYSTORE, "rmiserver", serverKeyPair.getPrivate(), serverCert, PASSWORD);

            saveTrustStore(SERVER_TRUSTSTORE, "rmiserver", serverCert, PASSWORD);

            System.out.println("Servidor KeyStore y TrustStore generados.");
        } catch (Exception e) {
            Logger.error("Error loading truststore from KeyStore");
            throw new IllegalStateException("This should never happen, something went horribly wrong", e);
        }
    }

    /**
     * Genera un par de claves RSA (pública y privada) de 2048 bits.
     *
     * @return un objeto {@link KeyPair} que contiene las claves generadas.
     * @throws IllegalStateException si ocurre un error al generar las claves.
     */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            Logger.error("Critical error generating key pair");
            throw new IllegalStateException("This should never happen, something went horribly wrong", e);
        }
    }

    /**
     * Genera un certificado X.509 firmado utilizando las claves proporcionadas.
     * El certificado es válido desde el día anterior a la fecha actual hasta un año en el futuro.
     *
     * @param keyPair el par de claves (pública y privada) para firmar el certificado.
     * @param subject el sujeto del certificado en formato DN (Distinguished Name).
     * @return el certificado X.509 generado.
     * @throws IllegalStateException si ocurre un error al generar el certificado.
     */
    public static X509Certificate generateCertificate(KeyPair keyPair, String subject) {
        try {
            Date startDate = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
            Date endDate = new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000);

            BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());
            X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    new X500Name(subject),
                    serialNumber,
                    startDate,
                    endDate,
                    new X500Name(subject),
                    keyPair.getPublic()
            );

            ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(keyPair.getPrivate());
            return new JcaX509CertificateConverter().getCertificate(certBuilder.build(contentSigner));
        } catch (Exception e) {
            Logger.error("Critical error generating certificates");
            throw new IllegalStateException("This should never happen, something went horribly wrong", e);
        }
    }

    /**
     * Guarda una clave privada y un certificado en un archivo KeyStore.
     *
     * @param keyStorePath la ruta del archivo KeyStore.
     * @param alias el alias bajo el cual se guardará la clave y el certificado.
     * @param privateKey la clave privada que se guardará.
     * @param certificate el certificado que se guardará.
     * @param password la contraseña para proteger el KeyStore.
     * @throws IllegalStateException si ocurre un error al guardar el KeyStore.
     */
    public static void saveKeyStore(String keyStorePath, String alias, PrivateKey privateKey, X509Certificate certificate, String password) {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, PASSWORD.toCharArray());
            keyStore.setKeyEntry(alias, privateKey, password.toCharArray(), new Certificate[]{certificate});
            keyStore.store(new FileOutputStream(keyStorePath), password.toCharArray());
        } catch (Exception e) {
            Logger.error("Critical error saving keystore");
            throw new IllegalStateException("This should never happen, something went horribly wrong", e);
        }
    }

    /**
     * Guarda un certificado en un archivo TrustStore.
     * Si el TrustStore no existe, se crea uno nuevo.
     *
     * @param trustStorePath la ruta del archivo TrustStore.
     * @param alias el alias bajo el cual se guardará el certificado.
     * @param certificate el certificado que se guardará.
     * @param password la contraseña para proteger el TrustStore.
     * @throws IllegalStateException si ocurre un error al guardar el TrustStore.
     */
    public static void saveTrustStore(String trustStorePath, String alias, X509Certificate certificate, String password) {
        try {
            KeyStore trustStore = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream(trustStorePath)) {
                trustStore.load(fis, password.toCharArray());
            } catch (FileNotFoundException e) {
                trustStore.load(null, PASSWORD.toCharArray());
            }
            trustStore.setCertificateEntry(alias, certificate);
            trustStore.store(new FileOutputStream(trustStorePath), password.toCharArray());
        } catch (Exception e) {
            Logger.error("Critical error saving truststore");
            throw new IllegalStateException("This should never happen, something went horribly wrong", e);
        }
    }

    /**
     * Carga un contexto SSL a partir de un KeyStore.
     *
     * @param keyStorePath la ruta del archivo KeyStore.
     * @param password la contraseña para acceder al KeyStore.
     * @return un objeto {@link SSLContext} configurado con el KeyStore proporcionado.
     * @throws IllegalStateException si ocurre un error al cargar el contexto SSL.
     */
    public SSLContext loadSSLContext(String keyStorePath, String password) {
        try {
            // Crear instancia de KeyStore
            KeyStore keyStore = KeyStore.getInstance("JKS");

            // Cargar el KeyStore desde el archivo
            try (FileInputStream fis = new FileInputStream(keyStorePath)) {
                keyStore.load(fis, password.toCharArray());
            }

            // Crear el KeyManagerFactory
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keyStore, password.toCharArray());

            // Configurar el SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);

            return sslContext;

        } catch (Exception e) {
            Logger.error("Error loading SSLContext from KeyStore");
            throw new IllegalStateException("This should never happen, something went horribly wrong", e);
        }
    }
}
