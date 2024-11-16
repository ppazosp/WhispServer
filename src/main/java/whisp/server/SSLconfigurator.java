package whisp.server;

import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

public class SSLconfigurator {
    private static final String SERVER_KEYSTORE = "server.keystore";
    private static final String SERVER_TRUSTSTORE = "server.truststore";
    private static final String CLIENT_KEYSTORE_PREFIX = "client";
    private static final String CLIENT_TRUSTSTORE = "client.truststore";
    private static final String PASSWORD = "password";

    public void genKeyCertificateServer(){
        //Generar clave y certificado del servidor
        KeyPair serverKeyPair = generateKeyPair();
        X509Certificate serverCert = generateCertificate(serverKeyPair,"CN=rmiserver, OU=Development, L=Santiago, C=ES" );

        //Guardar el certificado y clave del server en su keystore
        saveKeyStore(SERVER_KEYSTORE, "rmiserver", serverKeyPair.getPrivate(), serverCert, PASSWORD);
        //Exportar el certificado del servidor
        String serverCertPath = "rmiserver.crt";
        exportCertificate(serverCert, serverCertPath);
    }

}
