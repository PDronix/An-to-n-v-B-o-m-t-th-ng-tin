package dsa;

import java.math.BigInteger;

/**
 * DSASignature – Lưu trữ cặp chữ ký (r, s)
 */
public class DSASignature {

    public BigInteger r;  // r = (g^k mod p) mod q
    public BigInteger s;  // s = k^{-1}(H(m) + x*r) mod q

    public DSASignature(BigInteger r, BigInteger s) {
        this.r = r;
        this.s = s;
    }

    @Override
    public String toString() {
        return "r = " + DSAUtils.hex(r) + "\n"
             + "s = " + DSAUtils.hex(s);
    }
}
