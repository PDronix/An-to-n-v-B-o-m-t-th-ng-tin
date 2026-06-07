package dsa.ui;

import dsa.AppContext;
import dsa.core.log.LogEntry;
import dsa.core.sign.DSASignature;
import dsa.core.verify.VerifyResult;
import dsa.util.DSAUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.math.BigInteger;

/**
 * VerifyPanel – Xác thực chữ ký số (1 nút, hiện đầy đủ kết quả)
 */
public class VerifyPanel extends JPanel {

    private final AppContext ctx;
    private final JTextArea  outArea  = UIStyle.outputArea();
    private final JTextField txtField = UIStyle.textField();
    private final JLabel     fileLbl  = UIStyle.muted("Chua chon file");
    private final JLabel     sigLbl   = UIStyle.muted("Chua chon file .sig");
    private final JLabel     statusLbl;

    private File selectedFile = null;
    private File selectedSig  = null;
    private Runnable goToParamPanel = null;

    public void setGoToParamPanel(Runnable r) { this.goToParamPanel = r; }

    public VerifyPanel(AppContext ctx) {
        this.ctx = ctx;
        setLayout(new BorderLayout(12, 12));
        setBackground(UIStyle.C_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        add(makeHeader(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            makeControlPanel(), makeOutputPanel());
        split.setDividerLocation(340);
        split.setDividerSize(6);
        split.setBorder(null);
        split.setBackground(UIStyle.C_BG);
        add(split, BorderLayout.CENTER);

        statusLbl = UIStyle.muted("San sang kiem tra.");
        JPanel sb = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        sb.setOpaque(false); sb.add(statusLbl);
        add(sb, BorderLayout.SOUTH);
    }

    // ── Header ────────────────────────────────────────────────────────────

    private JPanel makeHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UIStyle.C_BG);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        p.add(UIStyle.h1("Kiem Tra Chu Ky So"), BorderLayout.WEST);
        p.add(UIStyle.muted("Xac thuc hop le | Toan ven du lieu | Phat hien gia mao"), BorderLayout.SOUTH);
        return p;
    }

    // ── Control panel ─────────────────────────────────────────────────────

    private JPanel makeControlPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(UIStyle.C_BG);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

        // Card: Kiểm tra văn bản
        JPanel textCard = UIStyle.card(new BorderLayout(0, 8));
        textCard.setAlignmentX(LEFT_ALIGNMENT);
        textCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        textCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIStyle.C_BORDER),
                "Kiem Tra Van Ban", 0, 0, UIStyle.F_BOLD, UIStyle.C_PRI),
            BorderFactory.createEmptyBorder(8, 10, 10, 10)));

        JButton btnFill = new JButton("Dien tu dong");
        btnFill.setFont(UIStyle.F_BODY);
        btnFill.setForeground(UIStyle.C_PRI);
        btnFill.setBackground(new Color(239, 246, 255));
        btnFill.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIStyle.C_BORDER),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        btnFill.setFocusPainted(false);
        btnFill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnFill.addActionListener(e -> autoFill());

        JButton btnVerifyText = UIStyle.primaryBtn("Kiem Tra");
        btnVerifyText.addActionListener(e -> verifyText());

        JPanel tRow = new JPanel(new BorderLayout(6, 0)); tRow.setOpaque(false);
        tRow.add(txtField, BorderLayout.CENTER);

        JPanel tBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0)); tBtns.setOpaque(false);
        tBtns.add(btnFill); tBtns.add(btnVerifyText);

        textCard.add(UIStyle.muted("Nhap van ban can kiem tra:"), BorderLayout.NORTH);
        textCard.add(tRow,  BorderLayout.CENTER);
        textCard.add(tBtns, BorderLayout.SOUTH);

        // Card: Kiểm tra file
        JPanel fileCard = UIStyle.card(new BorderLayout(0, 8));
        fileCard.setAlignmentX(LEFT_ALIGNMENT);
        fileCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));
        fileCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIStyle.C_BORDER),
                "Kiem Tra File", 0, 0, UIStyle.F_BOLD, UIStyle.C_PRI),
            BorderFactory.createEmptyBorder(8, 10, 10, 10)));

        JPanel fileRow = new JPanel(new BorderLayout(6, 0)); fileRow.setOpaque(false);
        JButton btnChooseFile = UIStyle.secondaryBtn("Chon File");
        btnChooseFile.addActionListener(e -> chooseFile());
        fileRow.add(fileLbl,      BorderLayout.CENTER);
        fileRow.add(btnChooseFile, BorderLayout.EAST);

        JPanel sigRow = new JPanel(new BorderLayout(6, 0)); sigRow.setOpaque(false);
        JButton btnChooseSig = UIStyle.secondaryBtn("Chon .sig");
        btnChooseSig.addActionListener(e -> chooseSig());
        sigRow.add(sigLbl,      BorderLayout.CENTER);
        sigRow.add(btnChooseSig, BorderLayout.EAST);

        JButton btnVerifyFile = UIStyle.primaryBtn("  Kiem Tra File  ");
        btnVerifyFile.addActionListener(e -> verifyFile());
        JPanel fBtn = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); fBtn.setOpaque(false);
        fBtn.add(btnVerifyFile);

        JPanel rows = new JPanel(new GridLayout(2, 1, 0, 6)); rows.setOpaque(false);
        rows.add(fileRow); rows.add(sigRow);

        fileCard.add(rows, BorderLayout.CENTER);
        fileCard.add(fBtn, BorderLayout.SOUTH);

        // Card: Reset
        JPanel resetCard = UIStyle.card(new FlowLayout(FlowLayout.LEFT, 8, 0));
        resetCard.setAlignmentX(LEFT_ALIGNMENT);
        resetCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        JButton btnReset = UIStyle.secondaryBtn("Reset");
        btnReset.addActionListener(e -> reset());
        resetCard.add(btnReset);
        resetCard.add(UIStyle.muted("Xoa ket qua va dat lai tu dau"));

        p.add(textCard);
        p.add(Box.createVerticalStrut(10));
        p.add(fileCard);
        p.add(Box.createVerticalStrut(10));
        p.add(resetCard);
        p.add(Box.createVerticalGlue());
        return p;
    }

    // ── Output panel ──────────────────────────────────────────────────────

    private JPanel makeOutputPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(UIStyle.C_BG);

        JPanel top = new JPanel(new BorderLayout(0, 2)); top.setOpaque(false);
        top.add(UIStyle.h2("Ket Qua Kiem Tra"), BorderLayout.NORTH);
        top.add(UIStyle.muted("Hien thi: hop le, toan ven, gia mao + cac gia tri trung gian"), BorderLayout.CENTER);

        p.add(top, BorderLayout.NORTH);
        p.add(UIStyle.scrollWrap(outArea), BorderLayout.CENTER);
        return p;
    }

    // ── Actions ───────────────────────────────────────────────────────────

    private void autoFill() {
        if (!ctx.hasSignature()) { warn("Hay ky tai lieu o muc 'Tao Chu Ky' truoc!"); return; }
        txtField.setText(ctx.lastSignature.sourceFile.startsWith("/") ||
                         ctx.lastSignature.sourceFile.contains("\\")
            ? "" : ctx.lastSignature.sourceFile);
        statusLbl.setText("Da dien tu dong. Bam 'Kiem Tra' de xem ket qua.");
    }

    private void chooseFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Chon file goc can kiem tra");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedFile = fc.getSelectedFile();
            fileLbl.setText(selectedFile.getName()
                + " (" + DSAUtils.formatSize(selectedFile.length()) + ")");
            fileLbl.setForeground(UIStyle.C_SUCCESS);
        }
    }

    private void chooseSig() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Chon file chu ky .sig");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("File chu ky (*.sig)", "sig"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedSig = fc.getSelectedFile();
            sigLbl.setText(selectedSig.getName());
            sigLbl.setForeground(UIStyle.C_SUCCESS);
        }
    }

    private void verifyText() {
        if (!ctx.hasKeys())      { warn("Chua co khoa. Hay tao tham so truoc!"); return; }
        if (!ctx.hasSignature()) { warn("Chua co chu ky. Hay ky van ban truoc!"); return; }
        String text = txtField.getText().trim();
        if (text.isEmpty()) { warn("Vui long nhap van ban can kiem tra."); return; }

        try {
            VerifyResult res = ctx.newVerifier().verifyText(text, ctx.lastSignature);
            ctx.totalVerified++;
            ctx.log.add(LogEntry.Action.VERIFY, shorten(text), res.allOk(),
                res.allOk() ? "Xac thuc thanh cong" : "Phat hien van de");
            outArea.setForeground(UIStyle.C_OUTPUT_TEXT);
            outArea.setText(buildVerifyOutput("Van ban", text, res));
            statusLbl.setText(res.allOk() ? "Chu ky HOP LE." : "Phat hien van de!");
        } catch (Exception ex) {
            warn("Loi kiem tra: " + ex.getMessage());
        }
    }

    private void verifyFile() {
        if (!ctx.hasKeys())       { warnNoKey(); return; }
        if (selectedFile == null) { warn("Vui long chon file goc!"); return; }
        if (selectedSig  == null) { warn("Vui long chon file .sig!"); return; }

        statusLbl.setText("Dang kiem tra...");
        new SwingWorker<VerifyResult, Void>() {
            protected VerifyResult doInBackground() throws Exception {
                Object[] sigData = DSAUtils.loadSignatureFile(selectedSig);
                BigInteger r    = (BigInteger) sigData[0];
                BigInteger s    = (BigInteger) sigData[1];
                BigInteger hash = (BigInteger) sigData[2];
                DSASignature sig = new DSASignature(r, s, hash, (String) sigData[3]);
                return ctx.newVerifier().verifyFile(selectedFile, sig);
            }
            protected void done() {
                try {
                    VerifyResult res = get();
                    ctx.totalVerified++;
                    ctx.log.add(LogEntry.Action.VERIFY, selectedFile.getName(), res.allOk(),
                        res.allOk() ? "Xac thuc thanh cong" : "Phat hien van de");
                    outArea.setForeground(UIStyle.C_OUTPUT_TEXT);
                    outArea.setText(buildVerifyOutput("File", selectedFile.getName(), res));
                    statusLbl.setText(res.allOk() ? "Chu ky HOP LE." : "Phat hien van de!");
                } catch (Exception ex) {
                    outArea.setForeground(new Color(255,100,100));
                    outArea.setText("LOI: " + ex.getMessage());
                    statusLbl.setText("Loi kiem tra.");
                }
            }
        }.execute();
    }

    private void reset() {
        selectedFile = null; selectedSig = null;
        txtField.setText("");
        fileLbl.setText("Chua chon file"); fileLbl.setForeground(UIStyle.C_TEXT_MUTED);
        sigLbl.setText("Chua chon file .sig"); sigLbl.setForeground(UIStyle.C_TEXT_MUTED);
        outArea.setForeground(UIStyle.C_OUTPUT_TEXT);
        outArea.setText(">>> Da dat lai. San sang kiem tra moi.");
        statusLbl.setText("San sang kiem tra.");
    }

    // ── Build output ──────────────────────────────────────────────────────

    private String buildVerifyOutput(String type, String source, VerifyResult res) {
        String line = "=".repeat(56);
        String bar  = "-".repeat(56);
        StringBuilder sb = new StringBuilder();

        sb.append(line).append("\n");
        sb.append("  KET QUA KIEM TRA CHU KY DSA\n");
        sb.append(line).append("\n\n");
        sb.append("Loai  : ").append(type).append("\n");
        sb.append("Nguon : ").append(source).append("\n\n");

        // [1] Chữ ký hợp lệ?
        sb.append(bar).append("\n");
        sb.append(" [1] CHU KY CO HOP LE KHONG?\n").append(bar).append("\n");
        if (res.signatureValid) {
            sb.append(" => HOP LE  (v = r)\n");
            sb.append("    Chu ky xac thuc thanh cong voi khoa cong khai y.\n");
        } else {
            sb.append(" => KHONG HOP LE  (v != r)\n");
            sb.append("    Thong diep hoac chu ky bi sai / bi thay doi.\n");
        }
        sb.append(" Gia tri trung gian:\n").append(res.intermediateValues()).append("\n");

        // [2] Toàn vẹn?
        sb.append(bar).append("\n");
        sb.append(" [2] DU LIEU CO BI VI PHAM TOAN VEN KHONG?\n").append(bar).append("\n");
        if (res.integrityOk) {
            sb.append(" => KHONG VI PHAM\n");
            sb.append("    Hash hien tai khop voi hash luc ky.\n");
        } else {
            sb.append(" => BI VI PHAM TOAN VEN\n");
            if (res.computedHash != null && res.originalHash != null) {
                sb.append("    Hash luc ky:\n");
                sb.append("      Base64: ").append(DSAUtils.toBase64(res.originalHash)).append("\n");
                sb.append("      Hex   : ").append(DSAUtils.toHexSpaced(res.originalHash)).append("\n");
                sb.append("    Hash hien tai:\n");
                sb.append("      Base64: ").append(DSAUtils.toBase64(res.computedHash)).append("\n");
                sb.append("      Hex   : ").append(DSAUtils.toHexSpaced(res.computedHash)).append("\n");
            }
        }
        sb.append("\n");

        // [3] Chữ ký bị sửa?
        sb.append(bar).append("\n");
        sb.append(" [3] CHU KY CO BI SUA DOI KHONG?\n").append(bar).append("\n");
        if (res.signatureMatches) {
            sb.append(" => NGUYEN VEN\n");
            sb.append("    Chu ky hop le, khong bi sua doi.\n");
        } else {
            sb.append(" => CO THE BI SUA DOI\n");
            sb.append("    Chu ky khong xac thuc duoc voi du lieu nay.\n");
        }
        sb.append("\n");

        // [4] Giả mạo?
        sb.append(bar).append("\n");
        sb.append(" [4] CHU KY CO BI GIA MAO KHONG?\n").append(bar).append("\n");
        if (res.notForged) {
            sb.append(" => KHONG GIA MAO\n");
            sb.append("    Chu ky xac thuc thanh cong, khong the gia mao\n");
            sb.append("    ma khong co khoa bi mat x (Logarit Roi Rac Zp*).\n");
        } else {
            sb.append(" => CO THE GIA MAO HOAC BI SUA\n");
            sb.append("    Chu ky KHONG hop le. Ke tan cong khong co x\n");
            sb.append("    khong the tao chu ky hop le (bai toan KHO giai).\n");
        }
        sb.append("\n");

        // Tổng kết
        sb.append(line).append("\n");
        if (res.allOk()) {
            sb.append("  TONG KET: CHU KY HOP LE HOAN TOAN  [AN TOAN]\n");
        } else {
            sb.append("  TONG KET: PHAT HIEN VAN DE  [CANH BAO]\n");
            sb.append(res.summary());
        }
        sb.append(line);

        if (!res.note.isEmpty()) {
            sb.append("\n\nGhi chu: ").append(res.note);
        }
        return sb.toString();
    }

    private String shorten(String s) {
        return s.length() > 30 ? s.substring(0, 30) + "..." : s;
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Thong bao", JOptionPane.WARNING_MESSAGE);
    }

    private void warnNoKey() {
        Object[] options = {"Chuyen sang Tao Tham So", "Dong"};
        int choice = JOptionPane.showOptionDialog(
            this,
            "Chua co tham so va khoa!\n\n"
            + "Ban can vao muc 'Tao Tham So' truoc:\n"
            + "  1. Bam nut 'Sinh Tham So & Cap Khoa'\n"
            + "  2. Doi khoang 5-10 giay\n"
            + "  3. Quay lai day de kiem tra\n\n"
            + "Hoac tai khoa cu bang nut 'Tai Khoa'.",
            "Can Tao Tham So Truoc",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null, options, options[0]);
        if (choice == 0 && goToParamPanel != null) {
            goToParamPanel.run();
        }
    }
}
