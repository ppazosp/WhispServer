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
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

public class SSLConfigurator {
    private static final String SERVER_KEYSTORE = "server.keystore";
    private static final String SERVER_TRUSTSTORE = "server.truststore";
    private static final String CLIENT_KEYSTORE_PREFIX = "client";
    private static final String CLIENT_TRUSTSTORE = "client.truststore";
    private static final String PASSWORD = "password";

    public void genKeyCertificateServer() {
        // Generar clave y certificado del servidor
        KeyPair serverKeyPair = generateKeyPair();
        X509Certificate serverCert = generateCertificate(serverKeyPair, "CN=rmiserver, OU=Development, L=Santiago, C=ES");

        // Guardar el certificado y clave del servidor en su KeyStore
        saveKeyStore(SERVER_KEYSTORE, "rmiserver", serverKeyPair.getPrivate(), serverCert, PASSWORD);

        // Exportar el certificado del servidor al TrustStore del cliente
        saveTrustStore(SERVER_TRUSTSTORE, "rmiserver", serverCert, PASSWORD);

        System.out.println("Servidor KeyStore y TrustStore generados.");
    }

    public void genKeyCertificateClient(String clientName) {
        // Generar clave y certificado para el cliente
        KeyPair clientKeyPair = generateKeyPair();
        String clientSubject = "CN=" + clientName + ", OU=Clients, L=Santiago, C=ES";
        X509Certificate clientCert = generateCertificate(clientKeyPair, clientSubject);

        // Guardar el certificado y clave del cliente en su propio KeyStore
        saveKeyStore(CLIENT_KEYSTORE_PREFIX + "_" + clientName + ".keystore", clientName, clientKeyPair.getPrivate(), clientCert, PASSWORD);

        // Exportar el certificado del cliente al TrustStore del servidor
        saveTrustStore(CLIENT_TRUSTSTORE, clientName, clientCert, PASSWORD);

        System.out.println("Cliente KeyStore y TrustStore generados para: " + clientName);
    }

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

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
            e.printStackTrace();
        }
        return null;
    }

    public static void saveKeyStore(String keyStorePath, String alias, PrivateKey privateKey, X509Certificate certificate, String password) {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, null);
            keyStore.setKeyEntry(alias, privateKey, password.toCharArray(), new Certificate[]{certificate});
            keyStore.store(new FileOutputStream(keyStorePath), password.toCharArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveTrustStore(String trustStorePath, String alias, X509Certificate certificate, String password) {
        try {
            KeyStore trustStore = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream(trustStorePath)) {
                trustStore.load(fis, password.toCharArray());
            } catch (Exception e) {
                // Si el archivo no existe, inicializar uno nuevo
                trustStore.load(null, null);
            }
            trustStore.setCertificateEntry(alias, certificate);
            trustStore.store(new FileOutputStream(trustStorePath), password.toCharArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
            e.printStackTrace();
            throw new RuntimeException("Error loading SSLContext from KeyStore", e);
        }
    }
}
