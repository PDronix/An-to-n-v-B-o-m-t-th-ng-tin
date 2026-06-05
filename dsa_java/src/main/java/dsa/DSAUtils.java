package dsa;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * DSAUtils – Tiện ích dùng chung: SHA-1, hex, in kết quả
 */
public class DSAUtils {

    /**
     * Băm thông điệp bằng SHA-1, trả về BigInteger
     */
    public static BigInteger hash(String message) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(message.getBytes(StandardCharsets.UTF_8));
        return new BigInteger(1, digest);
    }

    /**
     * Chuyển BigInteger sang chuỗi hex in hoa
     */
    public static String hex(BigInteger n) {
        return n.toString(16).toUpperCase();
    }

    /**
     * In tiêu đề có đường kẻ
     */
    public static void printHeader(String title) {
        String line = "=".repeat(60);
        System.out.println(line);
        System.out.println("  " + title);
        System.out.println(line);
    }

    /**
     * In kết quả verify kèm các giá trị trung gian
     */
    public static void printVerifyResult(VerifyResult res) {
        System.out.println("  w  = " + hex(res.w));
        System.out.println("  u1 = " + hex(res.u1));
        System.out.println("  u2 = " + hex(res.u2));
        System.out.println("  v  = " + hex(res.v));
        System.out.println();
        if (res.valid) {
            System.out.println(">>> KET QUA: HOP LE  (v = r)");
        } else {
            System.out.println(">>> KET QUA: KHONG HOP LE  (v != r)");
            if (!res.note.isEmpty())
                System.out.println("    Ly do: " + res.note);
        }
    }
}
