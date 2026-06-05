package dsa;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * DSAParameter – Sinh và lưu tham số hệ thống (p, q, g)
 * Theo chuẩn FIPS 186: q 160-bit, p 512-bit, q | (p-1)
 */
public class DSAParameter {

    public BigInteger p, q, g;
    private static final SecureRandom RND = new SecureRandom();

    /**
     * Sinh tham số DSA chuẩn
     */
    public void generate() {
        // Sinh q: số nguyên tố 160-bit
        q = BigInteger.probablePrime(160, RND);

        // Sinh p: số nguyên tố 512-bit sao cho q | (p - 1)
        BigInteger k;
        do {
            k = new BigInteger(352, RND).setBit(351);
            p = k.multiply(q).add(BigInteger.ONE);
        } while (!p.isProbablePrime(80) || p.bitLength() < 512);

        // Tìm g: phần tử sinh bậc q trong Zp*
        BigInteger h   = BigInteger.TWO;
        BigInteger exp = p.subtract(BigInteger.ONE).divide(q);
        do {
            g = h.modPow(exp, p);
            h = h.add(BigInteger.ONE);
        } while (g.equals(BigInteger.ONE));
    }

    @Override
    public String toString() {
        return "p = " + DSAUtils.hex(p) + "\n"
             + "q = " + DSAUtils.hex(q) + "\n"
             + "g = " + DSAUtils.hex(g);
    }
}
