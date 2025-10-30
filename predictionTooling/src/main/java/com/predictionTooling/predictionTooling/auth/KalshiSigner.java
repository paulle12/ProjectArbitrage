package com.predictionTooling.predictionTooling.auth;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;

public class KalshiSigner {
    private KalshiSigner() {
    }

    /**
     * Returns base64 signature for headers:
     * KALSHI-ACCESS-SIGNATURE over (timestamp + method + pathWithoutQuery)
     *
     * @param keyId         not used in the cryptographic operation (for
     *                      completeness)
     * @param privateKeyPem PEM string: "-----BEGIN PRIVATE KEY-----" (PKCS#8) or
     *                      "-----BEGIN RSA PRIVATE KEY-----" (PKCS#1)
     * @param timestampMs   current time in milliseconds
     * @param method        uppercase HTTP method, e.g. "GET", "POST"
     * @param path          the request path WITHOUT query string, e.g.
     *                      "/trade-api/v2/portfolio/balance"
     */
    public static String sign(String keyId, String privateKeyPem, long timestampMs, String method, String path) {
        try {
            // https://docs.kalshi.com/getting_started/api_keys
            String msg = String.valueOf(timestampMs) + method + path;
            PrivateKey privateKey = loadPrivateKeyFromPem(privateKeyPem);

            Signature sig = Signature.getInstance("RSASSA-PSS");
            PSSParameterSpec pssSpec = new PSSParameterSpec(
                    "SHA-256",
                    "MGF1",
                    new MGF1ParameterSpec("SHA-256"),
                    32, // salt length = digest length for SHA-256
                    1);
            sig.setParameter(pssSpec);
            sig.initSign(privateKey);
            sig.update(msg.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = sig.sign();
            return Base64.getEncoder().encodeToString(signatureBytes);

        } catch (Exception e) {
            throw new RuntimeException("Failed to sign Kalshi request", e);
        }
    }

    /** Accepts PKCS#8 (BEGIN PRIVATE KEY) or PKCS#1 (BEGIN RSA PRIVATE KEY). */
    private static PrivateKey loadPrivateKeyFromPem(String pem) throws Exception {
        String normalized = pem.replace("\r", "").replace("\n", "\n").trim();

        if (normalized.contains("BEGIN PRIVATE KEY")) {
            // PKCS#8
            byte[] der = base64Body(normalized, "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----");
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);

        } else if (normalized.contains("BEGIN RSA PRIVATE KEY")) {
            // PKCS#1 -> convert to PKCS#8
            byte[] pkcs1 = base64Body(normalized, "-----BEGIN RSA PRIVATE KEY-----", "-----END RSA PRIVATE KEY-----");
            // Wrap PKCS#1 in PKCS#8 header
            // PKCS#8 = SEQ { algo SEQ {1.2.840.113549.1.1.1, NULL}, OCTET STRING (PKCS#1) }
            byte[] pkcs8 = wrapPkcs1ToPkcs8(pkcs1);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(pkcs8);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        } else {
            throw new IllegalArgumentException("Unsupported PEM format. Expect PKCS#8 or PKCS#1 RSA private key.");
        }
    }

    private static byte[] base64Body(String pem, String begin, String end) {
        String body = pem.replace(begin, "").replace(end, "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }

    // Minimal DER builder to wrap PKCS#1 into PKCS#8
    private static byte[] wrapPkcs1ToPkcs8(byte[] pkcs1) throws Exception {
        // ASN.1 for: SEQUENCE {
        // SEQUENCE { OID 1.2.840.113549.1.1.1, NULL }
        // OCTET STRING (pkcs1)
        // }
        // Hard-code the AlgorithmIdentifier for rsaEncryption with NULL params.
        byte[] rsaAlgId = new byte[] {
                0x30, 0x0D, // SEQUENCE (13)
                0x06, 0x09, // OID (9)
                0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x0D, 0x01, 0x01, 0x01,
                0x05, 0x00 // NULL
        };

        byte[] pkcs1Octet = encodeOctetString(pkcs1);
        byte[] seqInner = concat(rsaAlgId, pkcs1Octet);
        byte[] seqOuter = encodeSequence(seqInner);
        return seqOuter;
    }

    private static byte[] encodeOctetString(byte[] data) {
        byte[] len = derLength(data.length);
        byte[] out = new byte[1 + len.length + data.length];
        out[0] = 0x04; // OCTET STRING
        System.arraycopy(len, 0, out, 1, len.length);
        System.arraycopy(data, 0, out, 1 + len.length, data.length);
        return out;
    }

    private static byte[] encodeSequence(byte[] data) {
        byte[] len = derLength(data.length);
        byte[] out = new byte[1 + len.length + data.length];
        out[0] = 0x30; // SEQUENCE
        System.arraycopy(len, 0, out, 1, len.length);
        System.arraycopy(data, 0, out, 1 + len.length, data.length);
        return out;
    }

    private static byte[] derLength(int length) {
        if (length < 0x80)
            return new byte[] { (byte) length };
        if (length <= 0xFF)
            return new byte[] { (byte) 0x81, (byte) length };
        return new byte[] { (byte) 0x82, (byte) ((length >> 8) & 0xFF), (byte) (length & 0xFF) };
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
