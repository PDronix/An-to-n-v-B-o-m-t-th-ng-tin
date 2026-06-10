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

        JPanel header = makeHeader();
        add(header, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                makeControlPanel(), makeOutputPanel());
        split.setDividerLocation(320);
        split.setDividerSize(6);
        split.setBorder(null);
        split.setBackground(UIStyle.C_BG);
        add(split, BorderLayout.CENTER);

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
        p.add(UIStyle.muted("Sinh p, q, g theo chuan FIPS 186-4 (2048/224) | Luu / Tai / Thu hoi khoa"), BorderLayout.SOUTH);
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
        JLabel genDesc  = UIStyle.muted("Sinh p (2048-bit), q (224-bit), g, x, y (SHA-224)");

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
        JPanel keyCard = UIStyle.card(new GridLayout(3, 2, 8, 6));
        keyCard.setAlignmentX(LEFT_ALIGNMENT);
        keyCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        keyCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(UIStyle.C_BORDER),
                        "Quan Ly Khoa", 0, 0, UIStyle.F_BOLD, UIStyle.C_PRI),
                BorderFactory.createEmptyBorder(6, 8, 8, 8)));

        JButton btnSavePriv = UIStyle.dangerBtn("Luu Khoa Bi Mat");
        JButton btnSavePub  = UIStyle.successBtn("Luu Khoa Cong Khai");
        JButton btnLoadPriv = UIStyle.secondaryBtn("Tai Khoa Bi Mat");
        JButton btnLoadPub  = UIStyle.secondaryBtn("Tai Khoa Cong Khai");
        JButton btnBackup   = UIStyle.secondaryBtn("Sao Luu");
        JButton btnRevoke   = UIStyle.dangerBtn("Thu Hoi");

        btnSavePriv.addActionListener(e -> savePrivateKey());
        btnSavePub.addActionListener(e  -> savePublicKey());
        btnLoadPriv.addActionListener(e -> loadPrivateKey());
        btnLoadPub.addActionListener(e  -> loadPublicKey());
        btnBackup.addActionListener(e   -> backupKey());
        btnRevoke.addActionListener(e   -> revokeKey());

        btnSavePriv.setToolTipText("Luu p,q,g,x — TUYET DOI khong chia se!");
        btnSavePub.setToolTipText("Luu p,q,g,y — Co the gui cho nguoi xac thuc");
        btnLoadPriv.setToolTipText("Tai khoa bi mat de ky tai lieu");
        btnLoadPub.setToolTipText("Tai khoa cong khai de xac thuc");

        keyCard.add(btnSavePriv);
        keyCard.add(btnSavePub);
        keyCard.add(btnLoadPriv);
        keyCard.add(btnLoadPub);
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
        // Cảnh báo nếu đã có khóa chưa lưu
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
            if (choice == 0) { saveKey(); return; }   // "Luu Khoa Truoc"
            if (choice == 2 || choice < 0) return;    // "Huy" hoac dong dialog
            // choice == 1 → "Sinh Moi Khong Luu" → tiep tuc sinh
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
                    ctx.keySaved = false;
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

    /**
     * Hỏi người dùng muốn lưu khóa nào (bí mật / công khai / cả hai),
     * sau đó gọi method lưu tương ứng.
     * Được gọi khi người dùng chọn "Luu Khoa Truoc" trong dialog cảnh báo.
     */
    private void saveKey() {
        if (!ctx.hasKeys()) {
            warn("Chua co khoa de luu. Hay sinh tham so truoc!");
            return;
        }
        Object[] opts = {"Luu Khoa Bi Mat", "Luu Khoa Cong Khai", "Luu Ca Hai", "Huy"};
        int choice = JOptionPane.showOptionDialog(this,
                "Ban muon luu khoa nao?",
                "Lua Chon Loai Khoa",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, opts, opts[2]);
        switch (choice) {
            case 0:
                savePrivateKey();
                break;
            case 1:
                savePublicKey();
                break;
            case 2:
                savePrivateKey();
                savePublicKey();
                break;
            // case 3 (Huy) hoac dong dialog: khong lam gi
        }
    }

    private String buildOutput() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== THAM SO HE THONG DSA ===\n\n");

        sb.append("-- p (2048-bit) --\n");
        sb.append("Base64:\n  ").append(ctx.param.pToBase64()).append("\n");
        sb.append("Hex:\n  ").append(ctx.param.pToHex()).append("\n\n");

        sb.append("-- q (224-bit) --\n");
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

    private void savePrivateKey() {
        if (!ctx.hasKeys()) { warn("Hay sinh tham so truoc!"); return; }
        String name = askFileName("Dat ten file KHOA BI MAT\n(Vi du: khoa_baomat_thang6)", ".private.dsakey");
        if (name == null) return;
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Luu Khoa Bi Mat (.private.dsakey)");
        fc.setSelectedFile(new File(name));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File f = ensureExt(fc.getSelectedFile(), ".private.dsakey");
                DSAUtils.savePrivateKey(f,
                        ctx.param.p, ctx.param.q, ctx.param.g, ctx.keyPair.x);
                ctx.keySaved = true; ctx.privateKeySaved = true;
                ctx.log.add(LogEntry.Action.SAVE_KEY, f.getName(), true, "Luu khoa bi mat");
                JOptionPane.showMessageDialog(this,
                        "Da luu: " + f.getName() + "\n\n"
                                + "CANH BAO BAO MAT:\n"
                                + "File nay chua khoa bi mat x.\n"
                                + "TUYET DOI khong chia se cho bat ky ai!",
                        "Luu Khoa Bi Mat", JOptionPane.WARNING_MESSAGE);
                append("\n>>> Khoa bi mat da luu: " + f.getAbsolutePath());
                statusLbl.setText("Da luu khoa bi mat: " + f.getName());
            } catch (Exception ex) {
                ctx.log.add(LogEntry.Action.SAVE_KEY, "", false, ex.getMessage());
                warn("Loi luu khoa bi mat: " + ex.getMessage());
            }
        }
    }

    private void savePublicKey() {
        if (!ctx.hasKeys()) { warn("Hay sinh tham so truoc!"); return; }
        String name = askFileName("Dat ten file KHOA CONG KHAI\n(Vi du: khoa_congkhai_thang6)", ".public.dsakey");
        if (name == null) return;
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Luu Khoa Cong Khai (.public.dsakey)");
        fc.setSelectedFile(new File(name));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File f = ensureExt(fc.getSelectedFile(), ".public.dsakey");
                DSAUtils.savePublicKey(f,
                        ctx.param.p, ctx.param.q, ctx.param.g, ctx.keyPair.y);
                ctx.publicKeySaved = true;
                ctx.sysPublicDsakeyFile = f;
                ctx.log.add(LogEntry.Action.SAVE_KEY, f.getName(), true, "Luu khoa cong khai");
                JOptionPane.showMessageDialog(this,
                        "Da luu: " + f.getName() + "\n\n"
                                + "File nay chua khoa cong khai y.\n"
                                + "Co the gui cho nguoi can xac thuc chu ky cua ban.",
                        "Luu Khoa Cong Khai", JOptionPane.INFORMATION_MESSAGE);
                append("\n>>> Khoa cong khai da luu: " + f.getAbsolutePath());
                statusLbl.setText("Da luu khoa cong khai: " + f.getName());
            } catch (Exception ex) {
                ctx.log.add(LogEntry.Action.SAVE_KEY, "", false, ex.getMessage());
                warn("Loi luu khoa cong khai: " + ex.getMessage());
            }
        }
    }

    private void loadPrivateKey() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Tai Khoa Bi Mat (.private.dsakey)");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Khoa bi mat (*.private.dsakey)", "dsakey"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File f = fc.getSelectedFile();
                java.math.BigInteger[] vals = DSAUtils.loadPrivateKey(f);
                ctx.param.p = vals[0]; ctx.param.q = vals[1];
                ctx.param.g = vals[2]; ctx.keyPair.x = vals[3];
                ctx.keyPair.y = ctx.param.g.modPow(ctx.keyPair.x, ctx.param.p);
                ctx.keySaved = true; ctx.privateKeySaved = true;
                ctx.log.add(LogEntry.Action.LOAD_KEY, f.getName(), true, "Tai khoa bi mat");
                outArea.setText(
                        "=== TAI KHOA BI MAT THANH CONG ===\n\n" +
                                "File: " + f.getAbsolutePath() + "\n\n" +
                                buildOutput()
                );
                statusLbl.setText("Da tai khoa bi mat: " + f.getName());
            } catch (Exception ex) {
                ctx.log.add(LogEntry.Action.LOAD_KEY, "", false, ex.getMessage());
                warn("Loi tai khoa bi mat: " + ex.getMessage());
            }
        }
    }
    /*private void loadPublicKey() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Tai Khoa Cong Khai (.public.dsakey)");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Khoa cong khai (*.public.dsakey)", "dsakey"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File f = fc.getSelectedFile();
                java.math.BigInteger[] vals = DSAUtils.loadPublicKey(f);
                ctx.param.p = vals[0]; ctx.param.q = vals[1];
                ctx.param.g = vals[2]; ctx.keyPair.y = vals[3];
                ctx.publicKeySaved = true;
                ctx.sysPublicDsakeyFile = f;
                ctx.log.add(LogEntry.Action.LOAD_KEY, f.getName(), true, "Tai khoa cong khai");
                outArea.setText(
                        "=== TAI KHOA CONG KHAI THANH CONG ===\n\n" +
                                "File: " + f.getAbsolutePath() + "\n\n" +
                                buildOutput()
                );
                statusLbl.setText("Da tai khoa cong khai: " + f.getName());
            } catch (Exception ex) {
                ctx.log.add(LogEntry.Action.LOAD_KEY, "", false, ex.getMessage());
                warn("Loi tai khoa cong khai: " + ex.getMessage());
            }
        }
    }*/

    private void loadPublicKey() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Tai Khoa Cong Khai (.public.dsakey)");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Khoa cong khai (*.public.dsakey)", "dsakey"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File f = fc.getSelectedFile();
                java.math.BigInteger[] vals = DSAUtils.loadPublicKey(f);

                // Nếu đã có khóa bí mật, kiểm tra p/q/g có khớp không
                if (ctx.param.p != null && ctx.privateKeySaved) {
                    if (!vals[0].equals(ctx.param.p) ||
                            !vals[1].equals(ctx.param.q) ||
                            !vals[2].equals(ctx.param.g)) {
                        int choice = JOptionPane.showConfirmDialog(this,
                                "CANH BAO: Tham so (p, q, g) cua khoa cong khai nay\n"
                                        + "KHONG KHOP voi khoa bi mat hien tai!\n\n"
                                        + "Hai khoa thuoc 2 phien khac nhau, kiem tra se SAI.\n\n"
                                        + "Ban co muon tiep tuc tai?",
                                "Khoa Khong Tuong Thich",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE);
                        if (choice != JOptionPane.YES_OPTION) return;
                    }
                }

                ctx.param.p = vals[0]; ctx.param.q = vals[1];
                ctx.param.g = vals[2]; ctx.keyPair.y = vals[3];
                ctx.publicKeySaved = true;
                ctx.sysPublicDsakeyFile = f;
                ctx.log.add(LogEntry.Action.LOAD_KEY, f.getName(), true, "Tai khoa cong khai");
                outArea.setText(
                        "=== TAI KHOA CONG KHAI THANH CONG ===\n\n" +
                                "File: " + f.getAbsolutePath() + "\n\n" +
                                buildOutput()
                );
                statusLbl.setText("Da tai khoa cong khai: " + f.getName());

                // ── HỎI CHỌN FILE .sig ĐI KÈM ────────────────────────────
//                int choice = JOptionPane.showConfirmDialog(this,
//                        "Ban co muon chon file chu ky (.sig) di kem khong?\n"
//                                + "(Can thiet neu ban muon kiem tra chu ky tu phien truoc)",
//                        "Chon File Chu Ky Di Kem",
//                        JOptionPane.YES_NO_OPTION,
//                        JOptionPane.QUESTION_MESSAGE);
                int choice = JOptionPane.NO_OPTION;
                if (choice == JOptionPane.YES_OPTION) {
                    JFileChooser sigFc = new JFileChooser();
                    sigFc.setDialogTitle("Chon file chu ky (.sig)");
                    sigFc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                            "DSA Signature (*.sig)", "sig"));
                    // Mở cùng thư mục với file khóa
                    sigFc.setCurrentDirectory(f.getParentFile());
                    if (sigFc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                        ctx.sysSigFile = sigFc.getSelectedFile();
                        append("\n>>> File .sig da chon: " + ctx.sysSigFile.getAbsolutePath());
                        statusLbl.setText("Da tai khoa + chu ky: san sang kiem tra.");
                    }
                }
                // ──────────────────────────────────────────────────────────

            } catch (Exception ex) {
                ctx.log.add(LogEntry.Action.LOAD_KEY, "", false, ex.getMessage());
                warn("Loi tai khoa cong khai: " + ex.getMessage());
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
            ctx.keySaved = false; ctx.privateKeySaved = false; ctx.publicKeySaved = false;
            ctx.log.add(LogEntry.Action.REVOKE_KEY, "he thong", true, "Thu hoi khoa thanh cong");
            outArea.setText(">>> Khoa da bi thu hoi.\n    Vui long sinh khoa moi de tiep tuc.");
            statusLbl.setText("Khoa da bi thu hoi.");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String askFileName(String prompt, String ext) {
        String name = JOptionPane.showInputDialog(this, prompt, "Dat Ten File", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return null;
        name = name.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return name.endsWith(ext) ? name : name + ext;
    }

    private File ensureExt(File f, String ext) {
        return f.getName().endsWith(ext) ? f : new File(f.getAbsolutePath() + ext);
    }

    private void append(String text) {
        outArea.append(text);
        outArea.setCaretPosition(outArea.getDocument().getLength());
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Thong bao", JOptionPane.WARNING_MESSAGE);
    }
}


