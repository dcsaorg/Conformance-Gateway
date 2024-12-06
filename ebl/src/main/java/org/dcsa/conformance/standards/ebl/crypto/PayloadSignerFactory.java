package org.dcsa.conformance.standards.ebl.crypto;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Date;
import javax.security.auth.x500.X500Principal;
import lombok.SneakyThrows;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.dcsa.conformance.core.UserFacingException;
import org.dcsa.conformance.standards.ebl.crypto.impl.X509BackedPayloadSigner;

public class PayloadSignerFactory {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Generated with `openssl req -x509 -newkey rsa:2048 -keyout key.pem -out cert.pem -days 4 -subj "/C=US/ST=Delaware/L=Delaware/O=SELFSIGNED/CN=foo" -nodes`
    // Contents in the `key.pem`
    @SuppressWarnings("secrets:S6706")
    private static final String CTK_SENDER_PRIVATE_KEY_PEM = """
            -----BEGIN PRIVATE KEY-----
            MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCXTD3XOeBMYVZS
            Pd1LmImkCzAvCqTZ/YnMh0uhYW3HUOBOdRvE++BY5uny8EZvKI4onH10SI1Wm+oy
            HPBPDFA0jP4SN/v83uPke67a6IuMcoQumZVHLY5+plg/w8YehGv0+sdwPY5UuO1O
            IcnJoc7b7o7elJC3alJ+hXWvATE+uaw0dcNxbQf+6GaBRY1u7iw/XI0k0LuNo1sI
            EuirnoFfDWMErFOhtRJo2DNQOAACbOfM+dTIxTACKdrz3K5GdLoClIT1SoJzq5S8
            s/uc4e/CN4hJLcKFHUNQswvS7ba6SD4jf/qiGo/wUTgyaboygED9V33ZUsrfyTnY
            wSoGfDvzAgMBAAECggEALI2sDEwjy/pB9Df5icBij+cnikLFJthtksgosl5BeJdN
            Zm0//zL47tUZAYxWAXfc3QKwQuT2khGZ1qYE8hI7MC5wxyarUtzEGU1+wUIHjhVO
            7XYWqn403wDXLffVyLjQHbUXs+q8liBa6U4z4OeARe2rLsprD0gFAPMGI8HjIYgR
            bz+953x695vbOA9DCommw3fdJLiKckLj9i/o0TyNv1aLRyQjrjCSB5JYcfo/rNjJ
            eTItoox2/oJYzZeJQ3SdMLf5iqEF8AONCg1+4td25B2KiJhnnbEuHkIp4na0wqeQ
            mpqjuYreTiL86vRjB+8ujaa7X2xUorFuXA9Z4qEb4QKBgQDVWFUtDXEBfYyGUU1C
            Hoe/SJsvwaQ++amI0Y04rZ6pgA271PfikV2l+W1KMhkxajD8LQ5Nx7WtvhaSuBeT
            TwS+1MNL7JF7HhpuoMCy4VgfPivnErnZLUkv8o0HytDK+vtq5c+BwD33NuqPKas4
            cAosNEyE4d09aYMs88Xmxbr8AwKBgQC1jCMsDkmy6JNg5e6R1UdvTAOWqFrHb0fU
            S2cmyn/++kEUsUw4rvk58m4v1Ci72ZxdkhpDEJRIneoJjrYJzUUS5GuR9EYlk+tB
            wjFTNRfzrGenO7F4pJUA3uGG6JMKy7LQ2d4Sm3GM8eR6PHkvO/O0+ZNSjlNDDwS7
            rhTuW4zVUQKBgDBCqBnl5X9J0ET+FTT0xQ5fNUOrUSUxwskBZinBFJgRMIoh1eU5
            ru6BqthS1uIXvHb/FjJAD/f6fQ65eBPJlzA33unI3Ov11lLaKF0OnqmKndHKqaHY
            Hasr+f0eQvb3qXH4BGW8gAfxM0QpT+MXbSWsuvaARVTEDnlXt5fJeM/TAoGAB/sW
            HLywDrZcrDjPaQfIMSNVUQ0rmHLS5IlACpuCTvIvZDp7EE7Y0+xNXbrk44Uoc5CV
            qPcUnbCbdjoY1It6it8Rv4POhZ5gDC7+PhsqZ2Lf16EvJw+NIVGq9mRI+oOD49x/
            /69nqXuEwL7h0OrAxublzA5HqL4DRkDb2LKbmVECgYB+XvRvR0GDE6Wo1zYI2sd/
            omQJ0jJ3rT65rWPLPPHg8Pe9Z8VdK56EoJUCStlqDvF4dct+wNt4O5rUJRTpv2jh
            ySiy2tAy9P+r91+PQW9Z8p9ecDi/BR2s9TLdceCGjvY518KD5HLLzP8LzG88VySr
            s/bpdr+2hBUcSaTF5KXNGw==
            -----END PRIVATE KEY-----
            """;

    @SuppressWarnings("secrets:S6706")
    private static final String CTK_CARRIER_PRIVATE_RSA_KEY_PEM = """
            -----BEGIN PRIVATE KEY-----
            MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQC588633cONawxd
            r7ynVvwmeRd2KVTpskYHxn51qYwUqK1jVPxGgDy1I8j+ImWbI1Q8ZaDbvWoIHcrv
            sQoO7FIDsBBb31nXbxqwI4bGop0qlbFe+lYlHaBfFe7o0Tmokb1HUUZuFk+9sgjn
            AkGypVynRNQW0OGgCxzbvMS4n6DjU9TY2r9obnIz3+5dOf/MI4Y2Qv79hJxMyerj
            51u4XX1PYl0nYp6baJxwOrSe2qIGfUHUVEnTApI1pELmjph9bzYKvgjMNZndTfRP
            gVC91ClZoN24/icp50dhvHKxb/7YYeTryYCIbguF4Xww+r9MQqaKCJ4ho7DAtLHk
            cWuKzqZlAgMBAAECggEAPs27/zicn+pMSmYc0u3bisjyJhvujGGEKoMdWfMSFzYj
            HX3qGIueNVWpQD/wzjVf8WgnrJ+sLKKXVF4YdhLV3l38IHNungbt3hiZoAPzDhtx
            xRDKwI1hiUvYnXRww2C5q1klbvAFLZ3wSMln1ATqpqnl8fDJi2rFa+e1D2AGkFBA
            GSSK5TmKwf7S32GOkja+JG8NG+WjQPMxU1qGcSkDVJNmOqHk1OMYgKjPlha8Dkpv
            cX2pNaUyMuETogN0oYogtI3RNlBj9i0yvDkKcy2xtDFXcoBWPl0PIsgXSFW4CRJM
            tFjdLfdUZnfd+mGDJUabourKwla4y1mw8v5lydI+XwKBgQDdZEpr7GnMlok0op1N
            AFNuCbErpInv8e2dLZ5bW3I8FG3be+jOpPGOBiZboNUndO9UH4bHIkw20GsAbgam
            5F9JNgbLCoSQaBx5OpdsY0EGZ8fbTSPiwx2SpFfY4dT8tGFG3a+61FbYeuuuhpU1
            zlm7lMaMmZg0JK/julShg++xRwKBgQDXBUt1PdMWmtbzHYiyOHODVWlYZMjteW3g
            lXI6xHMPEDA9Vu3xilat9arjno+Aa6XoP4UhXJukZCObzYsZeHYfSLOirU/KK3Rg
            p7Olp2LqNHWltQCI/UPhPBeUHxuYyST/8HZ/5qNh6MBsN2BVDMEaox0koW6EIc94
            /i0iSxug8wKBgHpwnewkGrsoQgeXK7HLTVjdCVweqp7GSOiVsy/JWls53Sv20mF+
            vY0Tf6FLSLeCp1359ZsqL8Zc6+CX+RvRz5T4yTb/wSLwQVcWfWpXVj4JpXF2rzMZ
            P8C7HU54T0fXJrl/n1GPX9xn1vJ1wg246s2gUVKvG4szAwfKJEYTZru/AoGAZTI8
            vUUHn8/n8iuoNhiTZPBB0DQ+zGUl7Vjolff3HtPDoFrVSaSN/vlsIAx0BUCkqJWc
            loL7TXdDuwQVvzsOfNK+mIVw0/l3oDXNOt14lDl0VTTGt7JazBp4DmJFnrasDzig
            zLlDk8TzKvs0/1ItX9f800yWsuEmwA8ANu+aZTkCgYByfPqSfpLdmPdLNhg6UY1D
            d1UNDFXNPyFYoqHrX1NXtDfBVeW50kDBHKBROOAUgJhwKG85aFXvwkqwUKPvMszv
            GVZhkK0swLScgg5I2td8jw6hmpG0DjyFx814VWPBfou+SEnyKxlYCETAWrsyvrei
            qGGESdtrmEydMGuuOIZ7zA==
            -----END PRIVATE KEY-----
            """;

  @SuppressWarnings("secrets:S6706")
  private static final String CTK_RECEIVER_PRIVATE_KEY_PEM = """
            -----BEGIN EC PRIVATE KEY-----
            MHcCAQEEIJTPoxr2hvrglK9q4L8UUBZk1QYm9Yv4wstC5BKPaxPYoAoGCCqGSM49
            AwEHoUQDQgAEBHrpbOJO5f60HZIq0p8Ia/Xp5SA+xQf6xk0JfVNi6Ny7bjCHy7bK
            0eQ2k/puDGgQiT0nzfW5SC0LwTGc712uZw==
            -----END EC PRIVATE KEY-----
            """;

    private static final KeyPair CTK_SENDER_RSA_KEY_PAIR = parsePEMPrivateRSAKey(CTK_SENDER_PRIVATE_KEY_PEM);
    private static final KeyPair CTK_CARRIER_RSA_KEY_PAIR = parsePEMPrivateRSAKey(CTK_CARRIER_PRIVATE_RSA_KEY_PEM);
    private static final KeyPair CTK_RECEIVER_EC_KEY_PAIR = parsePEMPrivateECKey(CTK_RECEIVER_PRIVATE_KEY_PEM);
    private static final PayloadSignerWithKey CTK_SENDER_KEY_PAYLOAD_SIGNER = rsaBasedPayloadSigner(CTK_SENDER_RSA_KEY_PAIR);
    private static final PayloadSignerWithKey CTK_SENDER_INCORRECT_KEY_PAYLOAD_SIGNER = rsaBasedPayloadSigner(CTK_CARRIER_RSA_KEY_PAIR);
    private static final PayloadSignerWithKey CTK_RECEIVER_KEY_PAYLOAD_SIGNER = ecBasedPayloadSigner(CTK_RECEIVER_EC_KEY_PAIR);

    public static PayloadSignerWithKey senderPayloadSigner() {
        return CTK_SENDER_KEY_PAYLOAD_SIGNER;
    }

    public static PayloadSignerWithKey receiverPayloadSigner() {
      return CTK_RECEIVER_KEY_PAYLOAD_SIGNER;
    }

    public static PayloadSignerWithKey carrierPayloadSigner() {
        return CTK_SENDER_INCORRECT_KEY_PAYLOAD_SIGNER;
    }

    private static PayloadSignerWithKey rsaBasedPayloadSigner(KeyPair keyPair) {
        return new X509BackedPayloadSigner(
                new JWSSignerDetails(
                        JWSAlgorithm.PS256,
                        new RSASSASigner(keyPair.getPrivate())
                ),
                generateSelfSignedCertificateSecret(keyPair)
        );
    }

  @SneakyThrows
  private static PayloadSignerWithKey ecBasedPayloadSigner(KeyPair keyPair) {
    return new X509BackedPayloadSigner(
      new JWSSignerDetails(
        JWSAlgorithm.ES256,
        new ECDSASigner((ECPrivateKey) keyPair.getPrivate())
      ),
      generateSelfSignedCertificateSecret(keyPair)
    );
  }

    @SneakyThrows
    private static KeyPair parsePEMPrivateRSAKey(String pem) {
        String privKeyPEM = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s++", "");

        byte [] encoded = Base64.getDecoder().decode(privKeyPEM);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        var privateKey = (RSAPrivateCrtKey)kf.generatePrivate(keySpec);
        var publicKey = kf.generatePublic(new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
        return new KeyPair(publicKey, privateKey);
    }


  @SneakyThrows
  private static KeyPair parsePEMPrivateECKey(String pem) {
    ECPrivateKey privateKey;
    try (var pemParser = new PEMParser(new StringReader(pem))) {
      var privateKeyInfo = (PEMKeyPair)pemParser.readObject();
      privateKey = (ECPrivateKey) new JcaPEMKeyConverter().getPrivateKey(privateKeyInfo.getPrivateKeyInfo());
      var publicKey = (ECPublicKey) new JcaPEMKeyConverter().getPublicKey(privateKeyInfo.getPublicKeyInfo());
      return new KeyPair(publicKey, privateKey);
    }
  }

    @SneakyThrows
    public static SignatureVerifier verifierFromPublicKey(PublicKey publicKey) {
      if (publicKey instanceof RSAPublicKey rsaPublicKey) {
        return new SingleKeySignatureVerifier(new RSASSAVerifier(rsaPublicKey));
      }
      if (publicKey instanceof ECPublicKey ecPublicKey) {
        return new SingleKeySignatureVerifier(new ECDSAVerifier(ecPublicKey));
      }
      throw new UserFacingException("Unsupported public key; must be a RSAPublicKey or an ECPublicKey.");
    }

    @SneakyThrows
    public static SignatureVerifier verifierFromPemEncodedPublicKey(String publicKeyPem) {
      try (var reader = new PEMParser(new StringReader(publicKeyPem))) {
        var parsedObject = reader.readObject();
        if (parsedObject instanceof X509CertificateHolder x509CertificateHolder) {
          var cert = CertificateFactory.getInstance("X.509")
            .generateCertificate(new ByteArrayInputStream(x509CertificateHolder.getEncoded()));
          return verifierFromPublicKey(cert.getPublicKey());
        }
        throw new UserFacingException("The provided PEM object was a X509 encoded certificate. Please provide a CERTIFICATE instead");
      } catch (Exception e) {
        throw new UserFacingException("Could not parse the PEM content string as an X509 encoded PEM certificate");
      }
    }

    private record SingleKeySignatureVerifier(JWSVerifier jwsVerifier) implements SignatureVerifier {

        @SneakyThrows
        @Override
        public boolean verifySignature(JWSObject jwsObject) {
            return jwsObject.verify(jwsVerifier);
        }
    }

    @SneakyThrows
    public static String pemEncodeCertificate(X509CertificateHolder x509CertificateHolder) {
      var w = new StringWriter();
      try (var pemWriter = new PemWriter(w)) {
        pemWriter.writeObject(new PemObject("CERTIFICATE", x509CertificateHolder.getEncoded()));
      }
      return w.getBuffer().toString();
    }

  private static X509CertificateHolder generateSelfSignedCertificateSecret(KeyPair keyPair) {
    X500Principal subject = new X500Principal("CN=DCSA-Conformance-Toolkit");

    long notBefore = System.currentTimeMillis();
    // 2500 days (several) years should be sufficient.
    long notAfter = notBefore + (1000L * 3600L * 24 * 2500);
    byte[] serialBytes = new byte[16];
    SECURE_RANDOM.nextBytes(serialBytes);
    X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
      subject,
      new BigInteger(serialBytes),
      new Date(notBefore),
      new Date(notAfter),
      subject,
      keyPair.getPublic()
    );

    try {
      certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
      certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));

      var algo = switch (keyPair.getPrivate()) {
        case ECPrivateKey ignored -> "SHA256withECDSA";
        case RSAPrivateKey ignored -> "SHA256withRSA";
        default -> throw new UnsupportedOperationException("Unsupported key");
      };
      final ContentSigner signer = new JcaContentSignerBuilder(algo).build(keyPair.getPrivate());
      return certBuilder.build(signer);
    } catch (Exception e) {
      throw new UserFacingException("Error while generating a self-certificate: " + e.getMessage(), e);
    }
  }
}
