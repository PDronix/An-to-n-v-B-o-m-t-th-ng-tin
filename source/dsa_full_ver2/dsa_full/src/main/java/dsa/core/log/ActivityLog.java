package dsa.core.log;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ActivityLog – Quản lý toàn bộ nhật ký hoạt động
 */
public class ActivityLog {

    private final List<LogEntry> entries = new ArrayList<>();

    // ── Thêm bản ghi ─────────────────────────────────────────────────────

    public void add(LogEntry.Action action, String target, boolean success, String detail) {
        entries.add(new LogEntry(action, target, success, detail));
    }

    public void add(LogEntry.Action action, String target, boolean success) {
        add(action, target, success, "");
    }

    // ── Truy vấn ─────────────────────────────────────────────────────────

    public List<LogEntry> getAll() {
        return Collections.unmodifiableList(entries);
    }

    public List<LogEntry> getByAction(LogEntry.Action action) {
        return entries.stream()
            .filter(e -> e.action == action)
            .collect(Collectors.toList());
    }

    public int countByAction(LogEntry.Action action) {
        return (int) entries.stream().filter(e -> e.action == action).count();
    }

    public int countSuccess() {
        return (int) entries.stream().filter(e -> e.success).count();
    }

    public int countFail() {
        return (int) entries.stream().filter(e -> !e.success).count();
    }

    public int total() { return entries.size(); }

    public void clear() { entries.clear(); }

    // ── Thống kê ─────────────────────────────────────────────────────────

    public String statistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== THONG KE HOAT DONG ===\n\n");
        sb.append(String.format("  Tong so hoat dong : %d\n", total()));
        sb.append(String.format("  Thanh cong        : %d\n", countSuccess()));
        sb.append(String.format("  That bai          : %d\n", countFail()));
        sb.append("\n  Chi tiet theo loai:\n");
        for (LogEntry.Action a : LogEntry.Action.values()) {
            int cnt = countByAction(a);
            if (cnt > 0)
                sb.append(String.format("    %-20s: %d\n", a.label, cnt));
        }
        return sb.toString();
    }

    // ── Xuất file ────────────────────────────────────────────────────────

    /** Xuất ra file .txt */
    public void exportTxt(File file) throws Exception {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("NHAT KY HOAT DONG – DSA Digital Signature – Nhom 10 – HaUI");
            pw.println("=".repeat(80));
            for (LogEntry e : entries) pw.println(e.toText());
            pw.println("=".repeat(80));
            pw.println(statistics());
        }
    }

    /** Xuất ra file .csv */
    public void exportCsv(File file) throws Exception {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("\"Thoi gian\",\"Hanh dong\",\"Doi tuong\",\"Ket qua\",\"Chi tiet\"");
            for (LogEntry e : entries) pw.println(e.toCsv());
        }
    }
}
