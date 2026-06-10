package dsa.util;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * DSAUtils – Tien ich dung chung
 * Ham bam: SHA-224 (FIPS 186-4, N=224)
 * Chuyen doi: BigInteger <-> Base64 / Hex
 * Doc/Ghi: file khoa, file chu ky
 */
public class DSAUtils {

    // =========================================================
    // HAM BAM – SHA-224 (chuan FIPS 186-4 voi N=224)
    // =========================================================

    /** Bam chuoi van ban bang SHA-224 */
    public static BigInteger hashString(String msg) throws Exception {
        return new BigInteger(1,
                MessageDigest.getInstance("SHA-224")
                        .digest(msg.getBytes("UTF-8")));
    }

    /** Bam noi dung file bang SHA-224 */
    public static BigInteger hashFile(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-224");
        try (InputStream is = new FileInputStream(file)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) md.update(buf, 0, n);
        }
        return new BigInteger(1, md.digest());
    }

    /** Bam bytes thô bang SHA-224 */
    public static BigInteger hashBytes(byte[] data) throws Exception {
        return new BigInteger(1,
                MessageDigest.getInstance("SHA-224").digest(data));
    }

    // =========================================================
    // CHUYEN DOI BIGINTEGER
    // =========================================================

    /** BigInteger -> Hex co dau cach (XX XX XX...) */
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

    /** BigInteger -> Hex lien (khong cach) */
    public static String toHex(BigInteger n) {
        return n.toString(16).toUpperCase();
    }

    /** BigInteger -> Base64 */
    public static String toBase64(BigInteger n) {
        return Base64.getEncoder().encodeToString(n.toByteArray());
    }

    /** Base64 -> BigInteger */
    public static BigInteger fromBase64(String b64) {
        return new BigInteger(1, Base64.getDecoder().decode(b64));
    }

    /** bytes -> Base64 */
    public static String bytesToBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /** Base64 -> bytes */
    public static byte[] base64ToBytes(String b64) {
        return Base64.getDecoder().decode(b64);
    }

    // =========================================================
    // LUU / TAI KHOA BI MAT (.private.dsakey)
    // =========================================================

    public static void savePrivateKey(File file,
                                      BigInteger p, BigInteger q,
                                      BigInteger g, BigInteger x) throws Exception {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("# DSA PRIVATE Key File – Nhom 10 – HaUI");
            pw.println("# CANH BAO: TUYET DOI khong chia se file nay!");
            pw.println("# Chuan: FIPS 186-4 (L=2048, N=224, SHA-224)");
            pw.println("type=PRIVATE");
            pw.println("p=" + toBase64(p));
            pw.println("q=" + toBase64(q));
            pw.println("g=" + toBase64(g));
            pw.println("x=" + toBase64(x));
        }
    }

    public static BigInteger[] loadPrivateKey(File file) throws Exception {
        java.util.Properties props = new java.util.Properties();
        try (FileReader fr = new FileReader(file)) { props.load(fr); }
        return new BigInteger[]{
                fromBase64(props.getProperty("p")),
                fromBase64(props.getProperty("q")),
                fromBase64(props.getProperty("g")),
                fromBase64(props.getProperty("x"))
        };
    }

    // =========================================================
    // LUU / TAI KHOA CONG KHAI (.public.dsakey)
    // =========================================================

    public static void savePublicKey(File file,
                                     BigInteger p, BigInteger q,
                                     BigInteger g, BigInteger y) throws Exception {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("# DSA PUBLIC Key File – Nhom 10 – HaUI");
            pw.println("# Co the phan phoi cong khai de xac thuc chu ky.");
            pw.println("# Chuan: FIPS 186-4 (L=2048, N=224, SHA-224)");
            pw.println("type=PUBLIC");
            pw.println("p=" + toBase64(p));
            pw.println("q=" + toBase64(q));
            pw.println("g=" + toBase64(g));
            pw.println("y=" + toBase64(y));
        }
    }

    public static BigInteger[] loadPublicKey(File file) throws Exception {
        java.util.Properties props = new java.util.Properties();
        try (FileReader fr = new FileReader(file)) { props.load(fr); }
        return new BigInteger[]{
                fromBase64(props.getProperty("p")),
                fromBase64(props.getProperty("q")),
                fromBase64(props.getProperty("g")),
                fromBase64(props.getProperty("y"))
        };
    }

    // =========================================================
    // SAO LUU (luu ca p,q,g,x,y)
    // =========================================================

    public static void saveKeyFile(File file,
                                   BigInteger p, BigInteger q, BigInteger g,
                                   BigInteger x, BigInteger y) throws Exception {
        savePrivateKey(file, p, q, g, x);
    }

    public static BigInteger[] loadKeyFile(File file) throws Exception {
        java.util.Properties props = new java.util.Properties();
        try (FileReader fr = new FileReader(file)) { props.load(fr); }
        String type = props.getProperty("type", "PRIVATE");
        if ("PUBLIC".equals(type)) {
            return new BigInteger[]{
                    fromBase64(props.getProperty("p")),
                    fromBase64(props.getProperty("q")),
                    fromBase64(props.getProperty("g")),
                    BigInteger.ZERO,
                    fromBase64(props.getProperty("y"))
            };
        }
        return new BigInteger[]{
                fromBase64(props.getProperty("p")),
                fromBase64(props.getProperty("q")),
                fromBase64(props.getProperty("g")),
                fromBase64(props.getProperty("x", "AA==")),
                fromBase64(props.getProperty("y", "AA=="))
        };
    }

    // =========================================================
    // LUU / TAI CHU KY (.sig)
    // =========================================================

    /**
     * Luu chu ky ra file .sig
     * Tham so theo thu tu: (file, r, s, sourceName, hash)
     */
    public static void saveSignatureFile(File sigFile,
                                         BigInteger r, BigInteger s,
                                         String sourceFileName,
                                         BigInteger hash) throws Exception {
        try (PrintWriter pw = new PrintWriter(new FileWriter(sigFile))) {
            pw.println("# DSA Signature File – Nhom 10 – HaUI");
            pw.println("# Chuan: FIPS 186-4 (SHA-224)");
            pw.println("source=" + sourceFileName);
            pw.println("hash=" + toBase64(hash));
            pw.println("r=" + toBase64(r));
            pw.println("s=" + toBase64(s));
        }
    }

    /**
     * Overload: (file, r, s, hash, sourceName) – thu tu nguoc
     */
    public static void saveSignatureFile(File sigFile,
                                         BigInteger r, BigInteger s,
                                         BigInteger hash,
                                         String sourceFileName) throws Exception {
        saveSignatureFile(sigFile, r, s, sourceFileName, hash);
    }

    /** Doc file .sig, tra ve [r, s, hash, sourceName] */
    public static Object[] loadSignatureFile(File sigFile) throws Exception {
        java.util.Properties props = new java.util.Properties();
        try (FileReader fr = new FileReader(sigFile)) { props.load(fr); }
        BigInteger r    = fromBase64(props.getProperty("r"));
        BigInteger s    = fromBase64(props.getProperty("s"));
        BigInteger hash = fromBase64(props.getProperty("hash"));
        String source   = props.getProperty("source", "");
        return new Object[]{r, s, hash, source};
    }

    // =========================================================
    // TIEN ICH FILE
    // =========================================================

    public static byte[] readFileBytes(File file) throws Exception {
        return Files.readAllBytes(file.toPath());
    }

    public static String getExtension(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024)      return bytes + " B";
        if (bytes < 1024*1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024));
    }
}

