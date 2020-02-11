package com.github.dfauth.authzn.ssl;

import org.bouncycastle.crypto.CryptoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import static org.testng.Assert.assertTrue;

public class CertTestCase {

    private static final Logger logger = LoggerFactory.getLogger(CertTestCase.class);

    @Test
    public void testIt() {
        try {
            new X509CertificateGenerator("server.CA-signed.p12", "password", "1", false).createCertificate("127.0.0.1", 365, "testAlias", new FileOutputStream("target/test.p12"), "password");
            assertTrue(new File("target/test.p12").exists());
        } catch (InvalidKeyException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (KeyStoreException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (NoSuchProviderException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (CertificateException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (UnrecoverableKeyException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (SignatureException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (CryptoException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
