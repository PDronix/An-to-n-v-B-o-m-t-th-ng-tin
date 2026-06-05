package dsa;

import java.math.BigInteger;

/**
 * VerifyResult – Kết quả xác thực kèm các giá trị trung gian
 */
public class VerifyResult {

    public boolean    valid  = false;
    public String     note   = "";
    public BigInteger w, u1, u2, v;
}
