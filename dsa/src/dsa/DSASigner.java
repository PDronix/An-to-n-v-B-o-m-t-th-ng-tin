package dsa.core.sign;

import dsa.core.param.DSAKeyPair;
import dsa.core.param.DSAParameter;
import dsa.util.DSAUtils;

import java.io.File;
import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * DSASigner – Ky so DSA chuan FIPS 186-4
 * Su dung SHA-224 tuong ung voi q 224-bit
 */
public class DSASigner {

    private final DSAParameter param;
    private final DSAKeyPair   keyPair;
    private static final SecureRandom RND = new SecureRandom();

    public DSASigner(DSAParameter param, DSAKeyPair keyPair) {
        this.param   = param;
        this.keyPair = keyPair;
    }

    /** Ky chuoi van ban bang SHA-224 */
    public DSASignature signText(String text) throws Exception {
        BigInteger hash = DSAUtils.hashString(text);  // SHA-224
        return doSign(hash, text);
    }

    /** Ky file bang SHA-224 */
    public DSASignature signFile(File file) throws Exception {
        BigInteger hash = DSAUtils.hashFile(file);    // SHA-224
        return doSign(hash, file.getName());
    }

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

            r = g.modPow(k, p).mod(q);
            s = k.modInverse(q)
                    .multiply(hash.add(x.multiply(r)))
                    .mod(q);
        } while (r.equals(BigInteger.ZERO) || s.equals(BigInteger.ZERO));

        return new DSASignature(r, s, hash, sourceName);
    }
}

