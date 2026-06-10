package dsa.core.verify;

import dsa.util.DSAUtils;
import java.math.BigInteger;

/**
 * VerifyResult – Kết quả xác thực kèm các giá trị trung gian
 */
public class VerifyResult {

    // Kết quả tổng
    public boolean signatureValid   = false;   // Chữ ký hợp lệ?
    public boolean integrityOk      = false;   // Toàn vẹn dữ liệu?
    public boolean signatureMatches = false;   // Chữ ký khớp bản gốc?
    public boolean notForged        = false;   // Không bị giả mạo?

    // Giá trị trung gian
    public BigInteger w, u1, u2, v;
    public BigInteger computedHash;            // Hash tính lại từ file/text hiện tại
    public BigInteger originalHash;           // Hash lưu trong file .sig

    // Ghi chú
    public String note = "";

    /** Tóm tắt kết quả */
    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append(signatureValid   ? "  [OK] Chu ky hop le\n"               : "  [!!] Chu ky KHONG hop le\n");
        sb.append(integrityOk      ? "  [OK] Toan ven du lieu\n"            : "  [!!] Du lieu bi THAY DOI\n");
        sb.append(signatureMatches ? "  [OK] Chu ky nguyen ven\n"           : "  [!!] Chu ky bi SUA DOI\n");
        sb.append(notForged        ? "  [OK] Khong phat hien gia mao\n"     : "  [!!] CO THE gia mao\n");
        if (!note.isEmpty()) sb.append("\n  Ghi chu: ").append(note);
        return sb.toString();
    }

    public boolean allOk() {
        return signatureValid && integrityOk && signatureMatches && notForged;
    }

    /** Giá trị trung gian dạng Base64 + Hex */
    public String intermediateValues() {
        if (w == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("w:\n")
          .append("  Base64: ").append(DSAUtils.toBase64(w)).append("\n")
          .append("  Hex   : ").append(DSAUtils.toHexSpaced(w)).append("\n\n");
        sb.append("u1:\n")
          .append("  Base64: ").append(DSAUtils.toBase64(u1)).append("\n")
          .append("  Hex   : ").append(DSAUtils.toHexSpaced(u1)).append("\n\n");
        sb.append("u2:\n")
          .append("  Base64: ").append(DSAUtils.toBase64(u2)).append("\n")
          .append("  Hex   : ").append(DSAUtils.toHexSpaced(u2)).append("\n\n");
        sb.append("v:\n")
          .append("  Base64: ").append(DSAUtils.toBase64(v)).append("\n")
          .append("  Hex   : ").append(DSAUtils.toHexSpaced(v)).append("\n");
        return sb.toString();
    }
}


