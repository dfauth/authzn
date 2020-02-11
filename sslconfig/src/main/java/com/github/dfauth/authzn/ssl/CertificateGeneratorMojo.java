package com.github.dfauth.authzn.ssl;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.bouncycastle.crypto.CryptoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

@Mojo(name = "certgen", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class CertificateGeneratorMojo extends AbstractMojo {

    private static final Logger logger = LoggerFactory.getLogger(CertificateGeneratorMojo.class);

    @Parameter(property = "ca.cert.keystore", defaultValue = "server.CA-signed.p12")
    private String caCertKeystore;

    @Parameter(property = "ca.cert.keystore.password", defaultValue = "password")
    private String caCertKeystorePassword;

    @Parameter(property = "ca.cert.alias", defaultValue = "1")
    private String caCertAlias;

    @Parameter(property = "dn", defaultValue = "127.0.0.1")
    private String dn;

    @Parameter(property = "validity.days", defaultValue = "365")
    private int validityDays;

    @Parameter(property = "cert.alias", defaultValue = "myCert")
    private String certAlias;

    @Parameter(property = "cert.keystore", defaultValue = "${project.build.directory}/generated-resources/certgen.p12")
    private String certKeystore;

    @Parameter(property = "cert.keystore.password", defaultValue = "password")
    private String certKeyStorePassword;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            new File(certKeystore).getParentFile().mkdirs();
            new X509CertificateGenerator(caCertKeystore, caCertKeystorePassword, caCertAlias, false)
                    .createCertificate(dn, validityDays, certAlias, new FileOutputStream(certKeystore), certKeyStorePassword);
            logger.info("generated certificate in keystore file: "+certKeystore);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (InvalidKeyException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (SignatureException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (CryptoException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (KeyStoreException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (NoSuchProviderException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (CertificateException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (InvalidKeySpecException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (UnrecoverableKeyException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
