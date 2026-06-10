package dsa.core.verify;

import dsa.core.param.DSAKeyPair;
import dsa.core.param.DSAParameter;
import dsa.core.sign.DSASignature;
import dsa.util.DSAUtils;

import java.io.File;
import java.math.BigInteger;

/**
 * DSAVerifier – Xac thuc chu ky DSA chuan FIPS 186-4
 * Su dung SHA-224 tuong ung voi q 224-bit
 */
public class DSAVerifier {

    private final DSAParameter param;
    private final DSAKeyPair   keyPair;

    public DSAVerifier(DSAParameter param, DSAKeyPair keyPair) {
        this.param   = param;
        this.keyPair = keyPair;
    }

    /** Xac thuc chuoi van ban */
    public VerifyResult verifyText(String text, DSASignature sig) throws Exception {
        BigInteger currentHash = DSAUtils.hashString(text);  // SHA-224
        return doVerify(currentHash, sig);
    }

    /** Xac thuc file */
    public VerifyResult verifyFile(File file, DSASignature sig) throws Exception {
        BigInteger currentHash = DSAUtils.hashFile(file);    // SHA-224
        return doVerify(currentHash, sig);
    }

    /** Xac thuc voi hash co san */
    public VerifyResult verifyWithHash(BigInteger currentHash,
                                       BigInteger r, BigInteger s,
                                       BigInteger originalHash) throws Exception {
        DSASignature sig = new DSASignature(r, s, originalHash, "");
        return doVerify(currentHash, sig);
    }

    private VerifyResult doVerify(BigInteger currentHash, DSASignature sig) throws Exception {
        VerifyResult res = new VerifyResult();
        res.computedHash = currentHash;
        res.originalHash = sig.hash;

        BigInteger q = param.q;
        BigInteger p = param.p;
        BigInteger g = param.g;
        BigInteger y = keyPair.y;
        BigInteger r = sig.r;
        BigInteger s = sig.s;

        // Kiem tra pham vi r, s
        if (r.compareTo(BigInteger.ZERO) <= 0 || r.compareTo(q) >= 0
                || s.compareTo(BigInteger.ZERO) <= 0 || s.compareTo(q) >= 0) {
            res.signatureValid   = false;
            res.integrityOk      = false;
            res.signatureMatches = false;
            res.notForged        = false;
            res.note = "r hoac s nam ngoai khoang hop le (0, q).";
            return res;
        }

        // Tinh cac gia tri trung gian
        res.w  = s.modInverse(q);
        res.u1 = currentHash.multiply(res.w).mod(q);
        res.u2 = r.multiply(res.w).mod(q);
        res.v  = g.modPow(res.u1, p)
                .multiply(y.modPow(res.u2, p))
                .mod(p).mod(q);

        res.signatureValid   = res.v.equals(r);
        res.integrityOk      = (sig.hash != null) && currentHash.equals(sig.hash);
        res.signatureMatches = res.signatureValid;
        res.notForged        = res.signatureValid;

        if (!res.signatureValid)
            res.note = "v != r: chu ky KHONG khop voi thong diep / khoa cong khai.";

        return res;
    }
}


