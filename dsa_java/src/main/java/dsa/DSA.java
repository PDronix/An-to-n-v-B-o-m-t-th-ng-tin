package dsa;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * DSA – Lớp điều phối chính: ký số và xác thực
 */
public class DSA {

    public  DSAParameter param   = new DSAParameter();
    public  DSAKeyPair   keyPair = new DSAKeyPair();
    public  DSASignature lastSig = null;
    public  String       lastMsg = "";

    private static final SecureRandom RND = new SecureRandom();

    /**
     * Sinh tham số hệ thống và cặp khóa
     */
    public void setup() {
        param.generate();
        keyPair.generate(param);
        lastSig = null;
        lastMsg = "";
    }

    /**
     * Tạo chữ ký DSA cho thông điệp
     */
    public DSASignature sign(String message) throws Exception {
        BigInteger q  = param.q;
        BigInteger p  = param.p;
        BigInteger g  = param.g;
        BigInteger x  = keyPair.x;
        BigInteger Hm = DSAUtils.hash(message);

        BigInteger r, s, k;
        do {
            do {
                k = new BigInteger(q.bitLength(), RND);
            } while (k.compareTo(BigInteger.ONE) < 0 || k.compareTo(q) >= 0);

            r = g.modPow(k, p).mod(q);                         // r = (g^k mod p) mod q
            s = k.modInverse(q)
                 .multiply(Hm.add(x.multiply(r)))
                 .mod(q);                                       // s = k^{-1}(H(m) + x*r) mod q
        } while (r.equals(BigInteger.ZERO) || s.equals(BigInteger.ZERO));

        lastSig = new DSASignature(r, s);
        lastMsg = message;
        return lastSig;
    }

    /**
     * Xác thực chữ ký DSA
     */
    public VerifyResult verify(String message, DSASignature sig) throws Exception {
        VerifyResult res = new VerifyResult();
        BigInteger q = param.q;
        BigInteger p = param.p;
        BigInteger g = param.g;
        BigInteger y = keyPair.y;

        // Kiểm tra điều kiện biên
        if (sig.r.compareTo(BigInteger.ZERO) <= 0 || sig.r.compareTo(q) >= 0
         || sig.s.compareTo(BigInteger.ZERO) <= 0 || sig.s.compareTo(q) >= 0) {
            res.valid = false;
            res.note  = "r hoac s nam ngoai khoang hop le (0, q).";
            return res;
        }

        BigInteger Hm = DSAUtils.hash(message);
        res.w  = sig.s.modInverse(q);                           // w  = s^{-1} mod q
        res.u1 = Hm.multiply(res.w).mod(q);                    // u1 = H(m)*w mod q
        res.u2 = sig.r.multiply(res.w).mod(q);                 // u2 = r*w mod q
        res.v  = g.modPow(res.u1, p)
                  .multiply(y.modPow(res.u2, p))
                  .mod(p).mod(q);                              // v  = (g^u1 * y^u2 mod p) mod q

        res.valid = res.v.equals(sig.r);
        if (!res.valid && res.note.isEmpty())
            res.note = "v != r => chu ky KHONG khop voi thong diep / khoa cong khai.";
        return res;
    }

    public boolean ready()  { return param.p != null; }
    public boolean hasSig() { return lastSig != null; }
}
