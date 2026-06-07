package dsa.core.param;

import dsa.util.DSAUtils;
import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * DSAParameter – Sinh và lưu tham số hệ thống (p, q, g)
 * Chuẩn FIPS 186: q 160-bit, p 512-bit, q | (p-1)
 */
public class DSAParameter {

    public BigInteger p, q, g;
    private static final SecureRandom RND = new SecureRandom();

    /** Sinh tham số DSA */
    public void generate() {
        q = BigInteger.probablePrime(160, RND);
        BigInteger k;
        do {
            k = new BigInteger(352, RND).setBit(351);
            p = k.multiply(q).add(BigInteger.ONE);
        } while (!p.isProbablePrime(80) || p.bitLength() < 512);

        BigInteger h   = BigInteger.TWO;
        BigInteger exp = p.subtract(BigInteger.ONE).divide(q);
        do {
            g = h.modPow(exp, p);
            h = h.add(BigInteger.ONE);
        } while (g.equals(BigInteger.ONE));
    }

    public boolean isReady() { return p != null && q != null && g != null; }

    // ── Hiển thị ─────────────────────────────────────────────────────────

    public String pToBase64()  { return DSAUtils.toBase64(p); }
    public String qToBase64()  { return DSAUtils.toBase64(q); }
    public String gToBase64()  { return DSAUtils.toBase64(g); }

    public String pToHex()     { return DSAUtils.toHexSpaced(p); }
    public String qToHex()     { return DSAUtils.toHexSpaced(q); }
    public String gToHex()     { return DSAUtils.toHexSpaced(g); }

    @Override
    public String toString() {
        return "p = " + DSAUtils.toHex(p) + "\n"
             + "q = " + DSAUtils.toHex(q) + "\n"
             + "g = " + DSAUtils.toHex(g);
    }
}
