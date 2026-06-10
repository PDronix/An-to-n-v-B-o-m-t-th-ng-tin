package dsa.ui;

import dsa.AppContext;
import dsa.core.log.LogEntry;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * LogPanel – Nhật ký hoạt động, thống kê và xuất báo cáo
 */
public class LogPanel extends JPanel {

    private final AppContext ctx;
    private final JTextArea  statArea = UIStyle.outputArea();
    private DefaultTableModel tableModel;
    private JTable table;
    private final JLabel statusLbl;

    public LogPanel(AppContext ctx) {
        this.ctx = ctx;
        setLayout(new BorderLayout(12, 12));
        setBackground(UIStyle.C_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        add(makeHeader(),  BorderLayout.NORTH);
        add(makeContent(), BorderLayout.CENTER);

        statusLbl = UIStyle.muted("Nhat ky san sang.");
        JPanel sb = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        sb.setOpaque(false); sb.add(statusLbl);
        add(sb, BorderLayout.SOUTH);

        // Tự động refresh mỗi 2 giây
        new Timer(2000, e -> refresh()).start();
    }

    // ── Header ────────────────────────────────────────────────────────────

    private JPanel makeHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UIStyle.C_BG);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        p.add(UIStyle.h1("Nhat Ky & Thong Ke"), BorderLayout.WEST);
        p.add(UIStyle.muted("Lich su hoat dong | Thong ke | Xuat bao cao .txt / .csv"), BorderLayout.SOUTH);
        return p;
    }

    // ── Content ───────────────────────────────────────────────────────────

    private JSplitPane makeContent() {
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            makeTablePanel(), makeBottomPanel());
        split.setDividerLocation(300);
        split.setDividerSize(6);
        split.setBorder(null);
        split.setBackground(UIStyle.C_BG);
        return split;
    }

    // ── Bảng log ──────────────────────────────────────────────────────────

    private JPanel makeTablePanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(UIStyle.C_BG);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);

        JButton btnRefresh = UIStyle.secondaryBtn("Lam Moi");
        JButton btnClear   = UIStyle.dangerBtn("Xoa Log");
        JButton btnExportTxt = UIStyle.primaryBtn("Xuat .txt");
        JButton btnExportCsv = UIStyle.successBtn("Xuat .csv");

        btnRefresh.addActionListener(e   -> refresh());
        btnClear.addActionListener(e     -> clearLog());
        btnExportTxt.addActionListener(e -> exportTxt());
        btnExportCsv.addActionListener(e -> exportCsv());

        toolbar.add(btnRefresh);
        toolbar.add(btnClear);
        toolbar.add(Box.createHorizontalStrut(12));
        toolbar.add(btnExportTxt);
        toolbar.add(btnExportCsv);

        // Table
        String[] cols = {"Thoi Gian", "Hanh Dong", "Doi Tuong", "Ket Qua", "Chi Tiet"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setFont(UIStyle.F_MONO_SM);
        table.setRowHeight(22);
        table.setShowGrid(true);
        table.setGridColor(UIStyle.C_BORDER);
        table.setSelectionBackground(new Color(219, 234, 254));
        table.getTableHeader().setFont(UIStyle.F_BOLD);
        table.getTableHeader().setBackground(UIStyle.C_HEADER_BG);
        table.getTableHeader().setForeground(UIStyle.C_PRI);

        // Căn chỉnh cột
        table.getColumnModel().getColumn(0).setPreferredWidth(140);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(180);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(200);

        // Tô màu cột "Kết Quả"
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, focus, row, col);
                String val = v == null ? "" : v.toString();
                setHorizontalAlignment(SwingConstants.CENTER);
                if (val.equals("OK")) {
                    setForeground(UIStyle.C_SUCCESS);
                    setFont(new Font("Segoe UI", Font.BOLD, 11));
                } else {
                    setForeground(UIStyle.C_DANGER);
                    setFont(new Font("Segoe UI", Font.BOLD, 11));
                }
                return this;
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(UIStyle.C_BORDER));

        p.add(toolbar, BorderLayout.NORTH);
        p.add(sp,       BorderLayout.CENTER);
        return p;
    }

    // ── Panel thống kê + số liệu ──────────────────────────────────────────

    private JPanel makeBottomPanel() {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(UIStyle.C_BG);
        p.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        // Thống kê số liệu (các badge)
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 10, 0));
        statsRow.setOpaque(false);
        statsRow.setPreferredSize(new Dimension(0, 80));
        statsRow.add(statBox("Tong Hoat Dong", () -> String.valueOf(ctx.log.total()), UIStyle.C_PRI));
        statsRow.add(statBox("Da Ky",          () -> String.valueOf(ctx.totalSigned),   UIStyle.C_SUCCESS));
        statsRow.add(statBox("Da Xac Thuc",    () -> String.valueOf(ctx.totalVerified), UIStyle.C_INFO));
        statsRow.add(statBox("That Bai",       () -> String.valueOf(ctx.log.countFail()), UIStyle.C_DANGER));

        // Chi tiết thống kê
        JPanel statDetail = new JPanel(new BorderLayout(0, 4));
        statDetail.setBackground(UIStyle.C_BG);
        statDetail.add(UIStyle.h2("Chi Tiet Thong Ke"), BorderLayout.NORTH);
        statDetail.add(UIStyle.scrollWrap(statArea), BorderLayout.CENTER);

        p.add(statsRow,  BorderLayout.NORTH);
        p.add(statDetail, BorderLayout.CENTER);
        return p;
    }

    private JPanel statBox(String label, java.util.function.Supplier<String> valueSupplier, Color color) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIStyle.C_BORDER, 1, true),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        JLabel numLbl = new JLabel(valueSupplier.get(), SwingConstants.CENTER);
        numLbl.setFont(new Font("Segoe UI", Font.BOLD, 28));
        numLbl.setForeground(color);

        JLabel nameLbl = new JLabel(label, SwingConstants.CENTER);
        nameLbl.setFont(UIStyle.F_SMALL);
        nameLbl.setForeground(UIStyle.C_TEXT_MUTED);

        p.add(numLbl,  BorderLayout.CENTER);
        p.add(nameLbl, BorderLayout.SOUTH);

        // Cập nhật số liệu mỗi 2 giây
        new Timer(2000, e -> numLbl.setText(valueSupplier.get())).start();
        return p;
    }

    // ── Actions ───────────────────────────────────────────────────────────

    private void refresh() {
        tableModel.setRowCount(0);
        List<LogEntry> entries = ctx.log.getAll();
        for (int i = entries.size() - 1; i >= 0; i--) {
            LogEntry e = entries.get(i);
            tableModel.addRow(new Object[]{
                e.timestamp,
                e.action.label,
                e.target,
                e.success ? "OK" : "FAIL",
                e.detail
            });
        }
        statArea.setForeground(UIStyle.C_OUTPUT_TEXT);
        statArea.setText(ctx.log.statistics());
        statusLbl.setText("Cap nhat: " + new java.util.Date());
    }

    private void clearLog() {
        int r = JOptionPane.showConfirmDialog(this,
            "Ban chac chan muon xoa toan bo nhat ky?",
            "Xac nhan", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
            ctx.log.clear();
            tableModel.setRowCount(0);
            statArea.setText("Nhat ky da duoc xoa.");
            statusLbl.setText("Nhat ky da xoa.");
        }
    }

    private void exportTxt() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("nhat_ky_dsa.txt"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ctx.log.exportTxt(fc.getSelectedFile());
                ctx.log.add(LogEntry.Action.EXPORT_REPORT, fc.getSelectedFile().getName(), true, "Xuat .txt");
                statusLbl.setText("Xuat .txt: " + fc.getSelectedFile().getName());
            } catch (Exception ex) { warn("Loi xuat: " + ex.getMessage()); }
        }
    }

    private void exportCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("nhat_ky_dsa.csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ctx.log.exportCsv(fc.getSelectedFile());
                ctx.log.add(LogEntry.Action.EXPORT_REPORT, fc.getSelectedFile().getName(), true, "Xuat .csv");
                statusLbl.setText("Xuat .csv: " + fc.getSelectedFile().getName());
            } catch (Exception ex) { warn("Loi xuat: " + ex.getMessage()); }
        }
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Thong bao", JOptionPane.WARNING_MESSAGE);
    }
}


