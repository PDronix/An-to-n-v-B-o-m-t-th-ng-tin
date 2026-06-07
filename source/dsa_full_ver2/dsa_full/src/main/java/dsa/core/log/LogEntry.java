package dsa.core.log;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * LogEntry – Một bản ghi nhật ký hoạt động
 */
public class LogEntry {

    public enum Action {
        GENERATE_PARAMS("Tao tham so"),
        GENERATE_KEY("Tao cap khoa"),
        SAVE_KEY("Luu khoa"),
        LOAD_KEY("Tai khoa"),
        REVOKE_KEY("Thu hoi khoa"),
        SIGN_TEXT("Ky van ban"),
        SIGN_FILE("Ky file"),
        VERIFY("Xac thuc"),
        EXPORT_REPORT("Xuat bao cao");

        public final String label;
        Action(String label) { this.label = label; }
    }

    public final String timestamp;
    public final Action action;
    public final String target;     // Tên file hoặc văn bản (tóm tắt)
    public final boolean success;
    public final String detail;

    public LogEntry(Action action, String target, boolean success, String detail) {
        this.timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
        this.action    = action;
        this.target    = target;
        this.success   = success;
        this.detail    = detail;
    }

    /** Dạng CSV: timestamp,action,target,success,detail */
    public String toCsv() {
        return String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
            timestamp, action.label, target,
            success ? "Thanh cong" : "That bai",
            detail.replace("\"", "'"));
    }

    /** Dạng text log */
    public String toText() {
        return String.format("[%s] %-18s | %-30s | %s | %s",
            timestamp, action.label, target,
            success ? "OK  " : "FAIL",
            detail);
    }

    @Override
    public String toString() { return toText(); }
}
