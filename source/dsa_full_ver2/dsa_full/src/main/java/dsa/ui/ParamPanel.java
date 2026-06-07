package dsa.ui;

import dsa.AppContext;
import dsa.core.log.LogEntry;
import dsa.util.DSAUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * ParamPanel – Tạo tham số hệ thống và quản lý khóa
 */
public class ParamPanel extends JPanel {

    private final AppContext ctx;
    private final JTextArea  outArea  = UIStyle.outputArea();
    private final JLabel     statusLbl;

    public ParamPanel(AppContext ctx) {
        this.ctx = ctx;
        setLayout(new BorderLayout(12, 12));
        setBackground(UIStyle.C_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Header
        JPanel header = makeHeader();
        add(header, BorderLayout.NORTH);

        // Center: chia 2 cột
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            makeControlPanel(), makeOutputPanel());
        split.setDividerLocation(320);
        split.setDividerSize(6);
        split.setBorder(null);
        split.setBackground(UIStyle.C_BG);
        add(split, BorderLayout.CENTER);

        // Status bar dưới cùng
        statusLbl = UIStyle.muted("Chua co tham so.");
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        statusBar.setOpaque(false);
        statusBar.add(statusLbl);
        add(statusBar, BorderLayout.SOUTH);
    }

    // ── Header ───────────────────────────────────────────────────────────

    private JPanel makeHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UIStyle.C_BG);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        p.add(UIStyle.h1("Tao Tham So & Quan Ly Khoa"), BorderLayout.WEST);
        p.add(UIStyle.muted("Sinh p, q, g theo chuan FIPS 186 | Luu / Tai / Thu hoi khoa"), BorderLayout.SOUTH);
        return p;
    }

    // ── Control panel (bên trái) ─────────────────────────────────────────

    private JPanel makeControlPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(UIStyle.C_BG);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

        // Card: Sinh tham số
        JPanel genCard = UIStyle.card(new BorderLayout(0, 10));
        genCard.setAlignmentX(LEFT_ALIGNMENT);
        genCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        JLabel genTitle = UIStyle.h2("Sinh Tham So & Cap Khoa");
        JLabel genDesc  = UIStyle.muted("Sinh p (512-bit), q (160-bit), g, x, y");

        JButton btnGen = UIStyle.primaryBtn("  Sinh Tham So  ");
        btnGen.addActionListener(e -> generateParams());

        JPanel genTop = new JPanel(new BorderLayout(0, 4)); genTop.setOpaque(false);
        genTop.add(genTitle, BorderLayout.NORTH);
        genTop.add(genDesc,  BorderLayout.CENTER);

        JPanel genBtn = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); genBtn.setOpaque(false);
        genBtn.add(btnGen);

        genCard.add(genTop, BorderLayout.NORTH);
        genCard.add(genBtn, BorderLayout.CENTER);

        // Card: Quản lý khóa
        JPanel keyCard = UIStyle.card(new GridLayout(2, 2, 8, 8));
        keyCard.setAlignmentX(LEFT_ALIGNMENT);
        keyCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        keyCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIStyle.C_BORDER),
                "Quan Ly Khoa", 0, 0, UIStyle.F_BOLD, UIStyle.C_PRI),
            BorderFactory.createEmptyBorder(6, 8, 8, 8)));

        JButton btnSave    = UIStyle.successBtn("Luu Khoa");
        JButton btnLoad    = UIStyle.secondaryBtn("Tai Khoa");
        JButton btnBackup  = UIStyle.secondaryBtn("Sao Luu");
        JButton btnRevoke  = UIStyle.dangerBtn("Thu Hoi");

        btnSave.addActionListener(e   -> saveKey());
        btnLoad.addActionListener(e   -> loadKey());
        btnBackup.addActionListener(e -> backupKey());
        btnRevoke.addActionListener(e -> revokeKey());

        keyCard.add(btnSave);
        keyCard.add(btnLoad);
        keyCard.add(btnBackup);
        keyCard.add(btnRevoke);

        // Card: Trạng thái
        JPanel statCard = UIStyle.card(new BorderLayout(0, 6));
        statCard.setAlignmentX(LEFT_ALIGNMENT);
        statCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        statCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIStyle.C_BORDER),
                "Trang Thai He Thong", 0, 0, UIStyle.F_BOLD, UIStyle.C_TEXT_MUTED),
            BorderFactory.createEmptyBorder(6, 8, 8, 8)));

        JTextArea statArea = new JTextArea(ctx.statusString());
        statArea.setEditable(false);
        statArea.setFont(UIStyle.F_SMALL);
        statArea.setForeground(UIStyle.C_TEXT_MUTED);
        statArea.setBackground(Color.WHITE);
        statCard.add(statArea, BorderLayout.CENTER);

        // Cập nhật trạng thái khi có thay đổi
        Timer timer = new Timer(1000, e -> statArea.setText(ctx.statusString()));
        timer.start();

        p.add(genCard);
        p.add(Box.createVerticalStrut(10));
        p.add(keyCard);
        p.add(Box.createVerticalStrut(10));
        p.add(statCard);
        p.add(Box.createVerticalGlue());
        return p;
    }

    // ── Output panel (bên phải) ───────────────────────────────────────────

    private JPanel makeOutputPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(UIStyle.C_BG);

        JLabel title = UIStyle.label("Ket Qua", UIStyle.F_H2, UIStyle.C_TEXT);
        JLabel hint  = UIStyle.muted("Hien thi Base64 va Hex cua tung gia tri");

        JPanel top = new JPanel(new BorderLayout(0, 2)); top.setOpaque(false);
        top.add(title, BorderLayout.NORTH);
        top.add(hint,  BorderLayout.CENTER);

        p.add(top, BorderLayout.NORTH);
        p.add(UIStyle.scrollWrap(outArea), BorderLayout.CENTER);
        return p;
    }

    // ── Actions ───────────────────────────────────────────────────────────

    private void generateParams() {
        // Canh bao neu da co khoa chua luu
        if (ctx.hasKeys() && !ctx.keySaved) {
            Object[] opts = {"Luu Khoa Truoc", "Sinh Moi Khong Luu", "Huy"};
            int choice = JOptionPane.showOptionDialog(this,
                "Hien tai da co khoa chua duoc luu!\n"
                + "Neu sinh moi, khoa cu se mat vinh vien.\n\n"
                + "Ban muon lam gi?",
                "Canh Bao Mat Khoa",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null, opts, opts[0]);
            if (choice == 0) { saveKey(); return; }
            if (choice == 2 || choice < 0) return;
        }

        outArea.setForeground(UIStyle.C_OUTPUT_TEXT);
        outArea.setText("Dang sinh tham so... vui long cho.\n(Qua trinh co the mat vai giay)");
        statusLbl.setText("Dang sinh...");

        new SwingWorker<String, Void>() {
            protected String doInBackground() throws Exception {
                ctx.param.generate();
                ctx.keyPair.generate(ctx.param);
                return null;
            }
            protected void done() {
                try {
                    get();
                    ctx.keySaved = false;  // khoa moi chua duoc luu
                    ctx.log.add(LogEntry.Action.GENERATE_PARAMS, "he thong", true, "Sinh p,q,g thanh cong");
                    ctx.log.add(LogEntry.Action.GENERATE_KEY,    "he thong", true, "Sinh x,y thanh cong");
                    outArea.setText(buildOutput());
                    statusLbl.setText("Tham so va khoa da san sang.");
                } catch (Exception ex) {
                    outArea.setForeground(UIStyle.C_DANGER);
                    outArea.setText("LOI: " + ex.getMessage());
                    ctx.log.add(LogEntry.Action.GENERATE_PARAMS, "he thong", false, ex.getMessage());
                    statusLbl.setText("Loi khi sinh tham so.");
                }
            }
        }.execute();
    }

    private String buildOutput() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== THAM SO HE THONG DSA ===\n\n");

        sb.append("-- p (512-bit) --\n");
        sb.append("Base64:\n  ").append(ctx.param.pToBase64()).append("\n");
        sb.append("Hex:\n  ").append(ctx.param.pToHex()).append("\n\n");

        sb.append("-- q (160-bit) --\n");
        sb.append("Base64:\n  ").append(ctx.param.qToBase64()).append("\n");
        sb.append("Hex:\n  ").append(ctx.param.qToHex()).append("\n\n");

        sb.append("-- g --\n");
        sb.append("Base64:\n  ").append(ctx.param.gToBase64()).append("\n");
        sb.append("Hex:\n  ").append(ctx.param.gToHex()).append("\n\n");

        sb.append("=== CAP KHOA ===\n\n");

        sb.append("-- x (Khoa bi mat) --\n");
        sb.append("Base64:\n  ").append(ctx.keyPair.xToBase64()).append("\n");
        sb.append("Hex:\n  ").append(ctx.keyPair.xToHex()).append("\n\n");

        sb.append("-- y (Khoa cong khai) --\n");
        sb.append("Base64:\n  ").append(ctx.keyPair.yToBase64()).append("\n");
        sb.append("Hex:\n  ").append(ctx.keyPair.yToHex()).append("\n\n");

        sb.append(">>> Sinh thanh cong! Co the luu khoa bang nut 'Luu Khoa'.");
        return sb.toString();
    }

    private void saveKey() {
        if (!ctx.hasKeys()) { warn("Hay sinh tham so truoc!"); return; }
        // Nguoi dung tu dat ten, chuong trinh tu them .dsakey
        String name = JOptionPane.showInputDialog(this,
            "Dat ten file khoa (khong can duoi .dsakey):\n"
            + "Vi du: khoa_hopdong_thang6",
            "Dat Ten File Khoa", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;
        name = name.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
        if (!name.endsWith(".dsakey")) name = name + ".dsakey";

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Chon noi luu file khoa");
        fc.setSelectedFile(new File(name));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File f = fc.getSelectedFile();
                if (!f.getName().endsWith(".dsakey"))
                    f = new File(f.getAbsolutePath() + ".dsakey");
                DSAUtils.saveKeyFile(f,
                    ctx.param.p, ctx.param.q, ctx.param.g,
                    ctx.keyPair.x, ctx.keyPair.y);
                ctx.keySaved = true;
                ctx.log.add(LogEntry.Action.SAVE_KEY, f.getName(), true, "Luu thanh cong");
                // Canh bao bao mat
                JOptionPane.showMessageDialog(this,
                    "Da luu khoa: " + f.getName() + "\n\n"
                    + "CANH BAO BAO MAT:\n"
                    + "File nay chua khoa bi mat x.\n"
                    + "Chi luu o noi an toan, KHONG chia se cho nguoi khac!",
                    "Luu Khoa Thanh Cong", JOptionPane.WARNING_MESSAGE);
                append("\n>>> Khoa da luu: " + f.getAbsolutePath());
                statusLbl.setText("Khoa da luu: " + f.getName());
            } catch (Exception ex) {
                ctx.log.add(LogEntry.Action.SAVE_KEY, "", false, ex.getMessage());
                warn("Loi luu khoa: " + ex.getMessage());
            }
        }
    }

    private void loadKey() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Tai file khoa (.dsakey)");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File f = fc.getSelectedFile();
                java.math.BigInteger[] vals = DSAUtils.loadKeyFile(f);
                ctx.param.p = vals[0]; ctx.param.q = vals[1]; ctx.param.g = vals[2];
                ctx.keyPair.load(vals[3], vals[4]);
                ctx.keySaved = true;  // khoa da duoc tai tu file, coi nhu da luu
                ctx.log.add(LogEntry.Action.LOAD_KEY, f.getName(), true, "Tai thanh cong");
                outArea.setText(buildOutput());
                statusLbl.setText("Da tai khoa: " + f.getName());
            } catch (Exception ex) {
                ctx.log.add(LogEntry.Action.LOAD_KEY, "", false, ex.getMessage());
                warn("Loi tai khoa: " + ex.getMessage());
            }
        }
    }

    private void backupKey() {
        if (!ctx.hasKeys()) { warn("Hay sinh tham so truoc!"); return; }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Sao luu khoa");
        fc.setSelectedFile(new File("backup_" + System.currentTimeMillis() + ".dsakey"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File f = fc.getSelectedFile();
                DSAUtils.saveKeyFile(f,
                    ctx.param.p, ctx.param.q, ctx.param.g,
                    ctx.keyPair.x, ctx.keyPair.y);
                ctx.log.add(LogEntry.Action.SAVE_KEY, f.getName(), true, "Sao luu thanh cong");
                append("\n>>> Sao luu khoa: " + f.getAbsolutePath());
                statusLbl.setText("Sao luu: " + f.getName());
            } catch (Exception ex) {
                warn("Loi sao luu: " + ex.getMessage());
            }
        }
    }

    private void revokeKey() {
        if (!ctx.hasKeys()) { warn("Khong co khoa de thu hoi."); return; }
        int r = JOptionPane.showConfirmDialog(this,
            "Ban chac chan muon thu hoi (xoa) khoa hien tai?\nHanh dong nay khong the hoan tac!",
            "Xac nhan thu hoi", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r == JOptionPane.YES_OPTION) {
            ctx.param.p = ctx.param.q = ctx.param.g = null;
            ctx.keyPair.x = ctx.keyPair.y = null;
            ctx.lastSignature = null;
            ctx.log.add(LogEntry.Action.REVOKE_KEY, "he thong", true, "Thu hoi khoa thanh cong");
            outArea.setText(">>> Khoa da bi thu hoi.\n    Vui long sinh khoa moi de tiep tuc.");
            statusLbl.setText("Khoa da bi thu hoi.");
        }
    }

    private void append(String text) {
        outArea.append(text);
        outArea.setCaretPosition(outArea.getDocument().getLength());
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Thong bao", JOptionPane.WARNING_MESSAGE);
    }
}
