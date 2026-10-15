import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Loads the existing protected key; exports no private material or passwords. */
public class SigningProof {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        Properties p = new Properties();
        try (var stream = Files.newInputStream(root.resolve("keystore.properties"))) { p.load(stream); }
        char[] storePassword = p.getProperty("storePassword").toCharArray();
        char[] keyPassword = p.getProperty("keyPassword").toCharArray();
        try {
            KeyStore store = KeyStore.getInstance(root.resolve(p.getProperty("storeFile")).toFile(), storePassword);
            String alias = p.getProperty("keyAlias");
            PrivateKey key = (PrivateKey)store.getKey(alias, keyPassword);
            var cert = store.getCertificate(alias);
            String algorithm = key.getAlgorithm().equals("EC") ? "SHA256withECDSA" : "SHA256withRSA";
            byte[] challenge = new byte[64];
            new SecureRandom().nextBytes(challenge);
            Signature signer = Signature.getInstance(algorithm);
            signer.initSign(key); signer.update(challenge);
            byte[] proof = signer.sign();
            Signature verifier = Signature.getInstance(algorithm);
            verifier.initVerify(cert); verifier.update(challenge);
            boolean valid = verifier.verify(proof);
            String fingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()));
            System.out.println("{\"certificateSha256\":\""+fingerprint+"\",\"signingChallengeVerified\":"+valid+",\"privateKeyExported\":false,\"independentBackupCustody\":\"requires owner confirmation\"}");
            if (!valid) throw new IllegalStateException("Signing proof failed");
        } finally {
            Arrays.fill(storePassword, '\0'); Arrays.fill(keyPassword, '\0'); p.clear();
        }
    }
}
