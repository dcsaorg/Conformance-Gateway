package org.dcsa.conformance.standards.eblinterop.crypto;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import lombok.SneakyThrows;
import org.dcsa.conformance.standards.eblinterop.crypto.impl.DefaultPayloadSigner;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class PayloadSignerFactory {

    // Generated with `openssl req -x509 -newkey rsa:2048 -keyout key.pem -out cert.pem -days 4 -subj "/C=US/ST=Delaware/L=Delaware/O=SELFSIGNED/CN=foo" -nodes`
    // Contents in the `key.pem`
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

    private static final String CTK_SENDER_INCORRECT_PRIVATE_RSA_KEY_PEM = """
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

  private static final String CTK_RECEIVER_PRIVATE_KEY_PEM = """
            -----BEGIN PRIVATE KEY-----
            MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC/rvvQhg5xRz+u
            92MsFlPac5TeheeoGREmi/KTT4V2TbhgdwZ3MnYt4/mp2G1DFf+nqVHOAKTpix91
            saNAY5WE+gvxVyCWV3xXTfP35RsEmltJpqJ3DcPGYQiTm5EXnaDvplE9+yjSgXGg
            S6QmjLNLMTLsg1vCYQGyulg6tJvNJajhVDVpQwDoH6X4h2vLBtT/tsXKhoqT3ZNI
            XVjC9A7uS4pn59NiGbK/B9ulx0zUzCWS/D92laUdJ0fJYSAReERzENq5qbjUwmvL
            oiz3vRsyRRZlKMLPV2hAGXCmFQz9iKdQrpqnfmM9suGrfrmI774xVFGfh9J6XV74
            SLBexhYZAgMBAAECggEABrGFEJCF05XR1vnDiEgVUIUFt0mEv910OFzdsSAvQGTR
            Yej2HFZyQwL5dmFc22Faxo+GkEN8fr1BcXotAbQYhga3QQuyUx2l9WR+9vKUoXIE
            awt7E94yrmw4APOHOwRhmMy9fIUXNVaY0aiiiEgUgLUsmo6xtxVtGkEgkJg68oxl
            HAf3UCQg6Bka1ZRNzu0ZRhEy2+AdF1L09HRiGdSsGOr/OKsTVk3fxGu1Uq0COjFc
            6C3PrRQ0PcHFLHBNXOVnNjMIwyaICeONfkxL9ai/p1TStyfUgC7NW+7/aK4NJUBc
            PFjsdtBJGSa2w1TV0Q8Vj5an0hJQIKcPot4HGLV6UwKBgQDrU0pcYa5bj/BwloI0
            O3DCfIj563XI1ac2qDj84XMnlqTs42R+l6NR4/3ykqhu8jgLZtr7qt9xgd4mJxes
            Inm+fE3ngOaJX1qqvKfBRR9/3jOaCo9yl1jc0IrGWY8YEBo8dc1x09Txw7/xj+du
            uKNLtnlxyWtBhBtVmHKLl1Sf6wKBgQDQhiTEtuCm4p1KOZDyg+goa0yRMMjxrv5X
            xPaZt7Ef/cIBnfwWYmfT/H0aTsNC4BuAheBJyvEsgfKAMPjD8yjLsw52XHhT0r7M
            GAG4DLLTa0hZgsC6UemtanvHI88w6JCJidEEKjdoenkvFPrj6TMKbWg5aeyqlSOi
            Idh/McLlCwKBgG/9unTOk+DFVqLuLdbXtukHxVRS50IF08ciNcS7MkdT3PdTnF7W
            oYX2X8OSYhAyu9NJRsvgXOgy6trzXcOwwImTtKuI363esFJy588Fq2D6CUq03eGl
            /0dPA8wzkPLdru65DWWvbzcDdpRqbLR3sFb250LsnVuXmD6bB2BBS6ezAoGBALYf
            8408jQo1c1uY29h1DRgAX2eQTHGKfer6xMeNgM6IPCJdcge6+yRTqpCHqlOGmX6v
            by4EapCNDtiX7S53+nGvejo2mYHc13g6n4W40ZeGZDKJ2PrjAE3Oaz2LMTNubI80
            J7KTjMFb9uwATwEwdLvuwtEiiuqSSAUbupOdSrPxAoGBANtVFaBAvt6DwYm5TfPJ
            TXoIcUmuf0FrqQg2CarBFTzgBV759c7Dk1P2TnWAeB+grh/AcU5WFBpJIrvenKrw
            u1IYO4pbMvjwiL2yzMruF7/GcoFONaCy2TMKdzu7ftpiCvXOcU9pb3IO5vUPssZ2
            5fqQLI2XCduGjbk9bpe+pwat
            -----END PRIVATE KEY-----
            """;

    private static final KeyPair CTK_SENDER_RSA_KEY_PAIR = parsePEMPrivateRSAKey(CTK_SENDER_PRIVATE_KEY_PEM);
    private static final KeyPair CTK_SENDER_INCORRECT_RSA_KEY_PAIR = parsePEMPrivateRSAKey(CTK_SENDER_INCORRECT_PRIVATE_RSA_KEY_PEM);
    private static final KeyPair CTK_RECEIVER_RSA_KEY_PAIR = parsePEMPrivateRSAKey(CTK_RECEIVER_PRIVATE_KEY_PEM);
    private static final PayloadSigner CTK_SENDER_KEY_PAYLOAD_SIGNER = rsaBasedPayloadSigner(CTK_SENDER_RSA_KEY_PAIR);
    private static final PayloadSigner CTK_SENDER_INCORRECT_KEY_PAYLOAD_SIGNER = rsaBasedPayloadSigner(CTK_SENDER_INCORRECT_RSA_KEY_PAIR);
    private static final PayloadSigner CTK_RECEIVER_KEY_PAYLOAD_SIGNER = rsaBasedPayloadSigner(CTK_RECEIVER_RSA_KEY_PAIR);
    private static final SignatureVerifierWithKey CTK_SENDER_SIGNATURE_VERIFIER = (SignatureVerifierWithKey)fromPublicKey(CTK_SENDER_RSA_KEY_PAIR.getPublic());
    private static final SignatureVerifierWithKey CTK_RECEIVER_SIGNATURE_VERIFIER = (SignatureVerifierWithKey)fromPublicKey(CTK_RECEIVER_RSA_KEY_PAIR.getPublic());

    public static PayloadSigner senderPayloadSigner() {
        return CTK_SENDER_KEY_PAYLOAD_SIGNER;
    }

    public static PayloadSigner receiverPayloadSigner() {
      return CTK_RECEIVER_KEY_PAYLOAD_SIGNER;
    }

    public static PayloadSigner testIncorrectPayloadSigner() {
        return CTK_SENDER_INCORRECT_KEY_PAYLOAD_SIGNER;
    }

    public static SignatureVerifierWithKey senderKeySignatureVerifier() {
        return CTK_SENDER_SIGNATURE_VERIFIER;
    }

    public static SignatureVerifierWithKey receiverKeySignatureVerifier() {
    return CTK_RECEIVER_SIGNATURE_VERIFIER;
  }

    private static PayloadSigner rsaBasedPayloadSigner(KeyPair keyPair) {
        return new DefaultPayloadSigner(
                new JWSSignerDetails(
                        JWSAlgorithm.PS256,
                        new RSASSASigner(keyPair.getPrivate())
                )
        );
    }

    public static SignatureVerifier fromJWSVerifier(JWSVerifier jwsVerifier) {
      return new SingleKeySignatureVerifier(jwsVerifier);
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
    public static SignatureVerifier fromPublicKey(PublicKey publicKey) {
      if (publicKey instanceof RSAPublicKey rsaPublicKey) {
        return new SingleExportableKeySignatureVerifier(new RSASSAVerifier(rsaPublicKey), rsaPublicKey);
      }
      if (publicKey instanceof ECPublicKey ecPublicKey) {
        return new SingleKeySignatureVerifier(new ECDSAVerifier(ecPublicKey));
      }
      throw new IllegalArgumentException("Unsupported public key; must be a RSAPublicKey or an ECPublicKey.");
    }

    @SneakyThrows
    public static SignatureVerifier fromPemEncodedPublicKey(String publicKeyPem) {
      if (publicKeyPem.contains("-----BEGIN RSA PUBLIC KEY-----")) {
        String privKeyPEM = publicKeyPem.replace("-----BEGIN RSA PUBLIC KEY-----", "")
          .replace("-----END RSA PUBLIC KEY-----", "")
          .replaceAll("\\s++", "");

        byte [] encoded = Base64.getDecoder().decode(privKeyPEM);

        var keySpec = new X509EncodedKeySpec(encoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        var rsaPublicKey = (RSAPublicKey)kf.generatePublic(keySpec);
        return new SingleKeySignatureVerifier(new RSASSAVerifier(rsaPublicKey));
      }
      if (publicKeyPem.isEmpty()) {
        throw new AssertionError("Missing key at the time where it was needed.");
      }
      throw new UnsupportedOperationException("Unsupported key format");
    }

    private record SingleKeySignatureVerifier(JWSVerifier jwsVerifier) implements SignatureVerifier {

        @SneakyThrows
        @Override
        public boolean verifySignature(JWSObject jwsObject) {
            return jwsObject.verify(jwsVerifier);
        }
    }

    private record SingleExportableKeySignatureVerifier(JWSVerifier jwsVerifier, RSAPublicKey rsaPublicKey) implements SignatureVerifierWithKey {


      @SneakyThrows
      @Override
      public boolean verifySignature(JWSObject jwsObject) {
        return jwsObject.verify(jwsVerifier);
      }

      @Override
      public String getPublicKeyInPemFormat() {
        return pemEncodeKey(this.rsaPublicKey);
      }
    }

    private static String pemEncodeKey(RSAPublicKey key) {
      var base64 = Base64.getEncoder().encodeToString(key.getEncoded());
      var result = new StringBuilder("-----BEGIN RSA PUBLIC KEY-----\n");
      for (int i = 0; i < base64.length() ; i += 64) {
        var limit = i + Math.min(64, base64.length() - i);
        result.append(base64, i, limit);
      }
      return result.append("-----END RSA PUBLIC KEY-----").toString();
    }
}
