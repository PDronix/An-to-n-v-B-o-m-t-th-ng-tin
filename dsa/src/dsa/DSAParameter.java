package dsa.core.param;

import dsa.util.DSAUtils;
import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * DSAParameter – Sinh tham so he thong (p, q, g)
 * Chuan FIPS 186-4: L=2048-bit, N=224-bit
 */
public class DSAParameter {

    public BigInteger p, q, g;
    private static final SecureRandom RND = new SecureRandom();

    /**
     * Sinh tham so DSA chuan FIPS 186-4 (L=2048, N=224):
     *   q: nguyen to 224-bit
     *   p: nguyen to 2048-bit, q | (p-1)
     *   g: phan tu sinh bac q trong Zp*
     */
    public void generate() {
        // Buoc 1: sinh q nguyen to chinh xac 224-bit
        q = BigInteger.probablePrime(224, RND);

        // Buoc 2: sinh p nguyen to chinh xac 2048-bit, sao cho q | (p-1)
        // p = k*q + 1, can k co bitLength = 2048 - 224 = 1824-bit
        BigInteger k;
        do {
            // Sinh k chinh xac 1824-bit (setBit(1823) dam bao bit cao nhat = 1)
            k = new BigInteger(1823, RND).setBit(1823);
            p = k.multiply(q).add(BigInteger.ONE);
        } while (!p.isProbablePrime(80) || p.bitLength() != 2048);

        // Buoc 3: tim g – phan tu sinh bac q trong Zp*
        // exp = (p-1)/q
        BigInteger exp = p.subtract(BigInteger.ONE).divide(q);
        BigInteger h   = BigInteger.TWO;
        do {
            g = h.modPow(exp, p);
            h = h.add(BigInteger.ONE);
        } while (g.equals(BigInteger.ONE));
    }

    public boolean isReady() { return p != null && q != null && g != null; }

    public String pToBase64() { return DSAUtils.toBase64(p); }
    public String qToBase64() { return DSAUtils.toBase64(q); }
    public String gToBase64() { return DSAUtils.toBase64(g); }

    public String pToHex()    { return DSAUtils.toHexSpaced(p); }
    public String qToHex()    { return DSAUtils.toHexSpaced(q); }
    public String gToHex()    { return DSAUtils.toHexSpaced(g); }

    @Override
    public String toString() {
        return "p (" + p.bitLength() + "-bit) = " + DSAUtils.toHex(p) + "\n"
                + "q (" + q.bitLength() + "-bit) = " + DSAUtils.toHex(q) + "\n"
                + "g = " + DSAUtils.toHex(g);
    }
}

