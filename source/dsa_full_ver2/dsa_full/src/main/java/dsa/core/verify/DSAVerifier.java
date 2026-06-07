package dsa.core.verify;

import dsa.core.param.DSAKeyPair;
import dsa.core.param.DSAParameter;
import dsa.core.sign.DSASignature;
import dsa.util.DSAUtils;

import java.io.File;
import java.math.BigInteger;

/**
 * DSAVerifier – Xác thực chữ ký DSA cho chuỗi hoặc file
 */
public class DSAVerifier {

    private final DSAParameter param;
    private final DSAKeyPair   keyPair;

    public DSAVerifier(DSAParameter param, DSAKeyPair keyPair) {
        this.param   = param;
        this.keyPair = keyPair;
    }

    // ── Xác thực với chuỗi văn bản ───────────────────────────────────────

    public VerifyResult verifyText(String text, DSASignature sig) throws Exception {
        BigInteger currentHash = DSAUtils.hashString(text);
        return doVerify(currentHash, sig);
    }

    // ── Xác thực với file ────────────────────────────────────────────────

    public VerifyResult verifyFile(File file, DSASignature sig) throws Exception {
        BigInteger currentHash = DSAUtils.hashFile(file);
        return doVerify(currentHash, sig);
    }

    // ── Xác thực với hash có sẵn ─────────────────────────────────────────

    public VerifyResult verifyWithHash(BigInteger currentHash,
                                       BigInteger r, BigInteger s,
                                       BigInteger originalHash) throws Exception {
        DSASignature sig = new DSASignature(r, s, originalHash, "");
        return doVerify(currentHash, sig);
    }

    // ── Logic xác thực DSA ───────────────────────────────────────────────

    private VerifyResult doVerify(BigInteger currentHash, DSASignature sig) throws Exception {
        VerifyResult res = new VerifyResult();
        res.computedHash  = currentHash;
        res.originalHash  = sig.hash;

        BigInteger q = param.q;
        BigInteger p = param.p;
        BigInteger g = param.g;
        BigInteger y = keyPair.y;
        BigInteger r = sig.r;
        BigInteger s = sig.s;

        // Kiểm tra điều kiện biên
        if (r.compareTo(BigInteger.ZERO) <= 0 || r.compareTo(q) >= 0
         || s.compareTo(BigInteger.ZERO) <= 0 || s.compareTo(q) >= 0) {
            res.signatureValid   = false;
            res.integrityOk      = false;
            res.signatureMatches = false;
            res.notForged        = false;
            res.note = "r hoac s nam ngoai khoang hop le (0, q).";
            return res;
        }

        // Tính các giá trị trung gian
        res.w  = s.modInverse(q);                                   // w  = s^{-1} mod q
        res.u1 = currentHash.multiply(res.w).mod(q);               // u1 = H(m)*w mod q
        res.u2 = r.multiply(res.w).mod(q);                         // u2 = r*w mod q
        res.v  = g.modPow(res.u1, p)
                  .multiply(y.modPow(res.u2, p))
                  .mod(p).mod(q);                                   // v  = (g^u1 * y^u2 mod p) mod q

        // [1] Chữ ký hợp lệ?
        res.signatureValid = res.v.equals(r);

        // [2] Toàn vẹn dữ liệu? (hash hiện tại == hash lúc ký)
        res.integrityOk = (sig.hash != null) && currentHash.equals(sig.hash);

        // [3] Chữ ký khớp bản gốc? (r, s đúng và chữ ký hợp lệ)
        res.signatureMatches = res.signatureValid;

        // [4] Không bị giả mạo? (chữ ký hợp lệ = không giả mạo được)
        res.notForged = res.signatureValid;

        if (!res.signatureValid)
            res.note = "v != r: chu ky KHONG khop voi thong diep / khoa cong khai.";

        return res;
    }
}
