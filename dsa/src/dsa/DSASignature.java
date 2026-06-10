package dsa.core.sign;

import dsa.util.DSAUtils;
import java.math.BigInteger;

/**
 * DSASignature – Lưu trữ cặp chữ ký (r, s) và metadata
 */
public class DSASignature {

    public BigInteger r;            // r = (g^k mod p) mod q
    public BigInteger s;            // s = k^{-1}(H(m) + x*r) mod q
    public BigInteger hash;         // H(m) lúc ký
    public String     sourceFile;   // Tên file gốc
    public String     signedAt;     // Thời điểm ký

    public DSASignature(BigInteger r, BigInteger s, BigInteger hash, String sourceFile) {
        this.r          = r;
        this.s          = s;
        this.hash       = hash;
        this.sourceFile = sourceFile;
        this.signedAt   = new java.util.Date().toString();
    }

    // ── Hiển thị ─────────────────────────────────────────────────────────

    public String rToBase64()    { return DSAUtils.toBase64(r); }
    public String sToBase64()    { return DSAUtils.toBase64(s); }
    public String hashToBase64() { return DSAUtils.toBase64(hash); }

    public String rToHex()       { return DSAUtils.toHexSpaced(r); }
    public String sToHex()       { return DSAUtils.toHexSpaced(s); }
    public String hashToHex()    { return DSAUtils.toHexSpaced(hash); }

    @Override
    public String toString() {
        return "r = " + DSAUtils.toHex(r) + "\n"
             + "s = " + DSAUtils.toHex(s);
    }
}

