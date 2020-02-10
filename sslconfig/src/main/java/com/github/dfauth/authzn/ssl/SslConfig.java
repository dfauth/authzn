package com.github.dfauth.authzn.ssl;

import akka.http.javadsl.ConnectionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SslConfig {

    private static final Logger logger = LoggerFactory.getLogger(SslConfig.class);

    private final String certFile;
    private final String password;
    private final List<String> cipherSuites;

    public SslConfig() {
        this("test.p12", "password", Collections.emptyList());
    }

    public SslConfig(String certFile, String password, List<String> cipherSuites) {
        this.certFile = certFile;
        this.password = password;
        this.cipherSuites = cipherSuites;
    }

    public ConnectionContext getConnectionContext(){
        try {
            return useTLS();
        } catch (RuntimeException e) {
            return useDefault();
        }
    }

    public ConnectionContext useDefault() {
        return ConnectionContext.noEncryption();
    }

    public ConnectionContext useTLS() {
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            InputStream keystore = getClass().getClassLoader().getResourceAsStream(certFile);

            assert keystore != null;
            ks.load(keystore, password.toCharArray());

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(ks, password.toCharArray());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);

            Set<String> enabledCipherSuites = cipherSuites.stream().map(e -> e.trim()).filter(e -> !e.equals("")).collect(Collectors.toSet());
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
            // ConnectionContext.https(sslContext, Optional.empty(), Optional.of(enabledCipherSuites), Optional.empty(), Optional.empty());
            return ConnectionContext.https(sslContext);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (CertificateException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (KeyStoreException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (UnrecoverableKeyException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (KeyManagementException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
