package dsa.core.sign;

import dsa.core.param.DSAKeyPair;
import dsa.core.param.DSAParameter;
import dsa.util.DSAUtils;

import java.io.File;
import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * DSASigner – Thực hiện ký số DSA cho chuỗi hoặc file
 */
public class DSASigner {

    private final DSAParameter param;
    private final DSAKeyPair   keyPair;
    private static final SecureRandom RND = new SecureRandom();

    public DSASigner(DSAParameter param, DSAKeyPair keyPair) {
        this.param   = param;
        this.keyPair = keyPair;
    }

    // ── Ký chuỗi văn bản ─────────────────────────────────────────────────

    public DSASignature signText(String text) throws Exception {
        BigInteger hash = DSAUtils.hashString(text);
        return doSign(hash, text);
    }

    // ── Ký file ──────────────────────────────────────────────────────────

    public DSASignature signFile(File file) throws Exception {
        BigInteger hash = DSAUtils.hashFile(file);
        return doSign(hash, file.getName());
    }

    // ── Logic ký DSA ─────────────────────────────────────────────────────

    private DSASignature doSign(BigInteger hash, String sourceName) throws Exception {
        BigInteger q = param.q;
        BigInteger p = param.p;
        BigInteger g = param.g;
        BigInteger x = keyPair.x;

        BigInteger r, s, k;
        do {
            do {
                k = new BigInteger(q.bitLength(), RND);
            } while (k.compareTo(BigInteger.ONE) < 0 || k.compareTo(q) >= 0);

            r = g.modPow(k, p).mod(q);                      // r = (g^k mod p) mod q
            s = k.modInverse(q)
                 .multiply(hash.add(x.multiply(r)))
                 .mod(q);                                    // s = k^{-1}(H(m) + x*r) mod q
        } while (r.equals(BigInteger.ZERO) || s.equals(BigInteger.ZERO));

        return new DSASignature(r, s, hash, sourceName);
    }
}
