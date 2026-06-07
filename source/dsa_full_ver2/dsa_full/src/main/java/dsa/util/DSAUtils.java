package dsa.util;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * DSAUtils – Tiện ích dùng chung:
 *   - Băm file/chuỗi bằng SHA-1
 *   - Chuyển đổi BigInteger ↔ Base64 ↔ Hex
 *   - Đọc/ghi file khóa và chữ ký
 */
public class DSAUtils {

    // ── Băm ─────────────────────────────────────────────────────────────

    /** Băm chuỗi bằng SHA-1 */
    public static BigInteger hashString(String msg) throws Exception {
        return new BigInteger(1,
            MessageDigest.getInstance("SHA-1")
                .digest(msg.getBytes("UTF-8")));
    }

    /** Băm nội dung file bằng SHA-1 */
    public static BigInteger hashFile(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        try (InputStream is = new FileInputStream(file)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) md.update(buf, 0, n);
        }
        return new BigInteger(1, md.digest());
    }

    /** Băm bytes thô */
    public static BigInteger hashBytes(byte[] data) throws Exception {
        return new BigInteger(1,
            MessageDigest.getInstance("SHA-1").digest(data));
    }

    // ── Chuyển đổi BigInteger ────────────────────────────────────────────

    /** BigInteger → chuỗi HEX in hoa, cách nhau bằng dấu cách (30 2C 02...) */
    public static String toHexSpaced(BigInteger n) {
        String hex = n.toString(16).toUpperCase();
        if (hex.length() % 2 != 0) hex = "0" + hex;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 2) {
            if (i > 0) sb.append(" ");
            sb.append(hex, i, i + 2);
        }
        return sb.toString();
    }

    /** BigInteger → chuỗi HEX liền (không cách) */
    public static String toHex(BigInteger n) {
        return n.toString(16).toUpperCase();
    }

    /** BigInteger → Base64 */
    public static String toBase64(BigInteger n) {
        return Base64.getEncoder().encodeToString(n.toByteArray());
    }

    /** Base64 → BigInteger */
    public static BigInteger fromBase64(String b64) {
        return new BigInteger(1, Base64.getDecoder().decode(b64));
    }

    /** bytes → Base64 */
    public static String bytesToBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /** Base64 → bytes */
    public static byte[] base64ToBytes(String b64) {
        return Base64.getDecoder().decode(b64);
    }

    // ── Đọc/Ghi file ─────────────────────────────────────────────────────

    /**
     * Lưu khóa ra file .dsakey (định dạng properties đơn giản)
     * Mỗi dòng: KEY=Base64Value
     */
    public static void saveKeyFile(File file,
                                   BigInteger p, BigInteger q, BigInteger g,
                                   BigInteger x, BigInteger y) throws Exception {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("# DSA Key File – Nhom 10 – HaUI");
            pw.println("p=" + toBase64(p));
            pw.println("q=" + toBase64(q));
            pw.println("g=" + toBase64(g));
            pw.println("x=" + toBase64(x));
            pw.println("y=" + toBase64(y));
        }
    }

    /** Đọc file .dsakey, trả về mảng [p, q, g, x, y] */
    public static BigInteger[] loadKeyFile(File file) throws Exception {
        java.util.Properties props = new java.util.Properties();
        try (FileReader fr = new FileReader(file)) {
            props.load(fr);
        }
        return new BigInteger[]{
            fromBase64(props.getProperty("p")),
            fromBase64(props.getProperty("q")),
            fromBase64(props.getProperty("g")),
            fromBase64(props.getProperty("x")),
            fromBase64(props.getProperty("y"))
        };
    }

    /**
     * Lưu chữ ký ra file .sig
     * Định dạng: r=Base64, s=Base64, file=tên file gốc, hash=Base64
     */
    public static void saveSignatureFile(File sigFile,
                                         BigInteger r, BigInteger s,
                                         String sourceFileName,
                                         BigInteger hash) throws Exception {
        try (PrintWriter pw = new PrintWriter(new FileWriter(sigFile))) {
            pw.println("# DSA Signature File – Nhom 10 – HaUI");
            pw.println("source=" + sourceFileName);
            pw.println("hash=" + toBase64(hash));
            pw.println("r=" + toBase64(r));
            pw.println("s=" + toBase64(s));
        }
    }

    /** Đọc file .sig, trả về [r, s, hash] và tên file gốc qua mảng Object */
    public static Object[] loadSignatureFile(File sigFile) throws Exception {
        java.util.Properties props = new java.util.Properties();
        try (FileReader fr = new FileReader(sigFile)) {
            props.load(fr);
        }
        BigInteger r    = fromBase64(props.getProperty("r"));
        BigInteger s    = fromBase64(props.getProperty("s"));
        BigInteger hash = fromBase64(props.getProperty("hash"));
        String source   = props.getProperty("source", "");
        return new Object[]{r, s, hash, source};
    }

    /** Đọc toàn bộ nội dung file dưới dạng bytes */
    public static byte[] readFileBytes(File file) throws Exception {
        return Files.readAllBytes(file.toPath());
    }

    /** Lấy extension của file (chữ thường) */
    public static String getExtension(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";
    }

    /** Định dạng kích thước file (bytes → KB/MB) */
    public static String formatSize(long bytes) {
        if (bytes < 1024)       return bytes + " B";
        if (bytes < 1024*1024)  return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024));
    }
}
