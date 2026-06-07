package dsa.core.param;

import dsa.util.DSAUtils;
import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * DSAKeyPair – Sinh và quản lý cặp khóa (x bí mật, y công khai)
 */
public class DSAKeyPair {

    public BigInteger x;   // Khóa bí mật: 0 < x < q
    public BigInteger y;   // Khóa công khai: y = g^x mod p

    private static final SecureRandom RND = new SecureRandom();

    /** Sinh cặp khóa từ tham số */
    public void generate(DSAParameter param) {
        do {
            x = new BigInteger(param.q.bitLength(), RND);
        } while (x.compareTo(BigInteger.ONE) < 0 || x.compareTo(param.q) >= 0);
        y = param.g.modPow(x, param.p);
    }

    /** Nạp khóa từ giá trị có sẵn (khi load từ file) */
    public void load(BigInteger x, BigInteger y) {
        this.x = x;
        this.y = y;
    }

    public boolean isReady() { return x != null && y != null; }

    // ── Hiển thị ─────────────────────────────────────────────────────────

    public String xToBase64() { return DSAUtils.toBase64(x); }
    public String yToBase64() { return DSAUtils.toBase64(y); }

    public String xToHex()    { return DSAUtils.toHexSpaced(x); }
    public String yToHex()    { return DSAUtils.toHexSpaced(y); }

    @Override
    public String toString() {
        return "x (bi mat)    = " + DSAUtils.toHex(x) + "\n"
             + "y (cong khai) = " + DSAUtils.toHex(y);
    }
}
