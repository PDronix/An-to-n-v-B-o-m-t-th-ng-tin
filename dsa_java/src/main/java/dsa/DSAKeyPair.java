package dsa;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * DSAKeyPair – Sinh cặp khóa (x bí mật, y công khai)
 */
public class DSAKeyPair {

    public BigInteger x;  // Khóa bí mật: 0 < x < q
    public BigInteger y;  // Khóa công khai: y = g^x mod p

    private static final SecureRandom RND = new SecureRandom();

    /**
     * Sinh cặp khóa từ bộ tham số đã có
     */
    public void generate(DSAParameter param) {
        do {
            x = new BigInteger(param.q.bitLength(), RND);
        } while (x.compareTo(BigInteger.ONE) < 0 || x.compareTo(param.q) >= 0);

        y = param.g.modPow(x, param.p);
    }

    @Override
    public String toString() {
        return "x (bi mat)    = " + DSAUtils.hex(x) + "\n"
             + "y (cong khai) = " + DSAUtils.hex(y);
    }
}
