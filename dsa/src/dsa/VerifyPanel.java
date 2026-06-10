package dsa.ui;

import dsa.AppContext;
import dsa.core.log.LogEntry;
import dsa.core.sign.DSASignature;
import dsa.core.verify.VerifyResult;
import dsa.util.DSAUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.math.BigInteger;

/**
 * VerifyPanel – Kiểm tra chữ ký:
 * Toggle: Kiểm tra File | Kiểm tra Văn bản nhập tay
 * Đã sửa logic đối chiếu chéo chuẩn xác 8 trường hợp lịch sử hệ thống.
 */
public class VerifyPanel extends JPanel {

    private final AppContext ctx;
    private final JTextArea outArea = UIStyle.outputArea();
    private final JLabel fileLbl = UIStyle.muted("Chua chon file goc");
    private final JLabel filesigLbl = UIStyle.muted("Chua chon file .sig");
    private final JLabel filepubLbl = UIStyle.muted("Chua chon file .public.dsakey");
    private final JLabel textSigLbl = UIStyle.muted("Chua chon file .sig");
    private final JLabel textPubLbl = UIStyle.muted("Chua chon file .public.dsakey");
    private final JLabel statusLbl;
    private final JTextArea txtInput;

    // Toggle mode: 0=File, 1=VanBan
    private int verifyMode = 0;
    private File selectedFile = null;
    private File selectedSigForFile = null;
    private File selectedSigForText = null;
    private File selectedPubForFile = null;
    private File selectedPubForText = null;

    private JPanel filePanel;
    private JPanel textPanel;
    private JButton btnModeFile;
    private JButton btnModeText;

    private Runnable goToParamPanel = null;

    public void setGoToParamPanel(Runnable r) {
        this.goToParamPanel = r;
    }

    public VerifyPanel(AppContext ctx) {
        this.ctx = ctx;
        txtInput = new JTextArea(3, 30);
        txtInput.setFont(UIStyle.F_MONO);
        txtInput.setLineWrap(true);
        txtInput.setWrapStyleWord(true);
        txtInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyle.C_BORDER),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));

        setLayout(new BorderLayout(12, 12));
        setBackground(UIStyle.C_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        add(makeHeader(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                makeControlPanel(), makeOutputPanel());
        split.setDividerLocation(360);
        split.setDividerSize(6);
        split.setBorder(null);
        split.setBackground(UIStyle.C_BG);
        add(split, BorderLayout.CENTER);

        statusLbl = UIStyle.muted("San sang kiem tra.");
        JPanel sb = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        sb.setOpaque(false);
        sb.add(statusLbl);
        add(sb, BorderLayout.SOUTH);
    }

    // ── Header ────────────────────────────────────────────────────────────

    private JPanel makeHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UIStyle.C_BG);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        p.add(UIStyle.h1("Kiem Tra Chu Ky So"), BorderLayout.WEST);
        p.add(UIStyle.muted("Chon phuong thuc kiem tra ben duoi"), BorderLayout.SOUTH);
        return p;
    }

    // ── Control panel ─────────────────────────────────────────────────────

    private JPanel makeControlPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(UIStyle.C_BG);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

        // Toggle bar
        JPanel toggleBar = new JPanel(new GridLayout(1, 2, 4, 0));
        toggleBar.setOpaque(false);
        toggleBar.setAlignmentX(LEFT_ALIGNMENT);
        toggleBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        btnModeFile = UIStyle.primaryBtn("Kiem Tra File");
        btnModeText = UIStyle.secondaryBtn("Kiem Tra Van Ban");
        btnModeFile.addActionListener(e -> switchMode(0));
        btnModeText.addActionListener(e -> switchMode(1));
        toggleBar.add(btnModeFile);
        toggleBar.add(btnModeText);

        // Panel cho mode File
        filePanel = makeFilePanel();
        filePanel.setAlignmentX(LEFT_ALIGNMENT);

        // Panel cho mode Van Ban
        textPanel = makeTextPanel();
        textPanel.setAlignmentX(LEFT_ALIGNMENT);
        textPanel.setVisible(false);

        // Card nut kiem tra + reset
        JPanel actionCard = UIStyle.card(new BorderLayout(0, 8));
        actionCard.setAlignmentX(LEFT_ALIGNMENT);
        actionCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        actionCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(UIStyle.C_BORDER),
                        "Tien Hanh Kiem Tra", 0, 0, UIStyle.F_BOLD, UIStyle.C_PRI),
                BorderFactory.createEmptyBorder(8, 10, 10, 10)));

        JButton btnVerify = UIStyle.successBtn("  KIEM TRA CHU KY   ");
        btnVerify.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnVerify.addActionListener(e -> {
            if (verifyMode == 0) verifyFile();
            else verifyText();
        });
        JButton btnReset = UIStyle.secondaryBtn("Reset");
        btnReset.addActionListener(e -> reset());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setOpaque(false);
        btnRow.add(btnVerify);
        btnRow.add(btnReset);
        actionCard.add(UIStyle.muted("Can chon du file ben tren:"), BorderLayout.NORTH);
        actionCard.add(btnRow, BorderLayout.CENTER);

        // Hint card
        JPanel hintCard = makeHintCard();
        hintCard.setAlignmentX(LEFT_ALIGNMENT);

        p.add(toggleBar);
        p.add(Box.createVerticalStrut(10));
        p.add(filePanel);
        p.add(textPanel);
        p.add(Box.createVerticalStrut(10));
        p.add(actionCard);
        p.add(Box.createVerticalStrut(10));
        p.add(hintCard);
        p.add(Box.createVerticalGlue());
        return p;
    }

    // ── File Panel ────────────────────────────────────────────────────────

    private JPanel makeFilePanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JPanel c1 = inputCard("Buoc 1 – Chon File Goc",
                "Chon file can kiem tra (txt, pdf, docx...):", "Chon File Goc",
                fileLbl, e -> chooseFile());

        JPanel c2 = inputCard("Buoc 2 – Chon File Chu Ky",
                "Chon file chu ky (.sig):", "Chon .sig",
                filesigLbl, e -> chooseSig());

        JPanel c3 = inputCard("Buoc 3 – Chon Khoa Cong Khai",
                "Chon file khoa cong khai (.public.dsakey):", "Chon .public.dsakey",
                filepubLbl, e -> choosePubKey());

        p.add(c1);
        p.add(Box.createVerticalStrut(8));
        p.add(c2);
        p.add(Box.createVerticalStrut(8));
        p.add(c3);
        return p;
    }

    // ── Text Panel ────────────────────────────────────────────────────────

    private JPanel makeTextPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JPanel txtCard = UIStyle.card(new BorderLayout(0, 8));
        txtCard.setAlignmentX(LEFT_ALIGNMENT);
        txtCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        txtCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(UIStyle.C_BORDER),
                        "Buoc 1 – Nhap Van Ban Can Kiem Tra", 0, 0, UIStyle.F_BOLD, UIStyle.C_PRI),
                BorderFactory.createEmptyBorder(8, 10, 10, 10)));
        txtCard.add(UIStyle.muted("Nhap lai dung noi dung da ky:"), BorderLayout.NORTH);
        txtCard.add(new JScrollPane(txtInput), BorderLayout.CENTER);

        JPanel c2 = inputCard("Buoc 2 – Chon File Chu Ky",
                "Chon file chu ky (.sig):", "Chon .sig",
                textSigLbl, e -> chooseSig());

        JPanel c3 = inputCard("Buoc 3 – Chon Khoa Cong Khai",
                "Chon file khoa cong khai (.public.dsakey):", "Chon .public.dsakey",
                textPubLbl, e -> choosePubKey());

        p.add(txtCard);
        p.add(Box.createVerticalStrut(8));
        p.add(c2);
        p.add(Box.createVerticalStrut(8));
        p.add(c3);
        return p;
    }

    // ── Helper: input card ────────────────────────────────────────────────

    private JPanel inputCard(String title, String hint, String btnTxt,
                             JLabel lbl, java.awt.event.ActionListener action) {
        JPanel c = UIStyle.card(new BorderLayout(0, 8));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(UIStyle.C_BORDER),
                        title, 0, 0, UIStyle.F_BOLD, UIStyle.C_PRI),
                BorderFactory.createEmptyBorder(6, 10, 8, 10)));
        JButton btn = UIStyle.primaryBtn(btnTxt);
        btn.addActionListener(action);
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.add(lbl, BorderLayout.CENTER);
        row.add(btn, BorderLayout.EAST);
        c.add(UIStyle.muted(hint), BorderLayout.NORTH);
        c.add(row, BorderLayout.CENTER);
        return c;
    }

    // ── Hint card ─────────────────────────────────────────────────────────

    private JPanel makeHintCard() {
        JPanel p = UIStyle.card(new BorderLayout());
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        p.setBackground(new Color(239, 246, 255));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(191, 219, 254), 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        JTextArea hint = new JTextArea(
                "Luu y:\n"
                        + "  - File .sig: xuat tu tab 'Tao Chu Ky'\n"
                        + "  - File .public.dsakey: luu tu tab 'Tao Tham So'\n"
                        + "  - Can dung DUNG BỘ khoa voi chu ky");
        hint.setEditable(false);
        hint.setOpaque(false);
        hint.setFont(UIStyle.F_SMALL);
        hint.setForeground(new Color(30, 64, 175));
        p.add(hint, BorderLayout.CENTER);
        return p;
    }

    // ── Output panel ──────────────────────────────────────────────────────

    private JPanel makeOutputPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(UIStyle.C_BG);
        JPanel top = new JPanel(new BorderLayout(0, 2));
        top.setOpaque(false);
        top.add(UIStyle.h2("Ket Qua Kiem Tra"), BorderLayout.NORTH);
        top.add(UIStyle.muted("Doi chieu chi tiet thong tin lich su he thong"), BorderLayout.CENTER);
        p.add(top, BorderLayout.NORTH);
        p.add(UIStyle.scrollWrap(outArea), BorderLayout.CENTER);
        return p;
    }

    // ── Switch mode ───────────────────────────────────────────────────────

    private void switchMode(int mode) {
        verifyMode = mode;
        filePanel.setVisible(mode == 0);
        textPanel.setVisible(mode == 1);
        btnModeFile.setBackground(mode == 0 ? UIStyle.C_PRI : new Color(241, 245, 249));
        btnModeFile.setForeground(mode == 0 ? Color.WHITE : UIStyle.C_TEXT);
        btnModeText.setBackground(mode == 1 ? UIStyle.C_PRI : new Color(241, 245, 249));
        btnModeText.setForeground(mode == 1 ? Color.WHITE : UIStyle.C_TEXT);
        outArea.setText(">>> Che do: " + (mode == 0 ? "Kiem tra File" : "Kiem tra Van ban"));
        revalidate();
        repaint();
    }

    // ── Actions ───────────────────────────────────────────────────────────

    private void chooseFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Chon file goc can kiem tra");
        fc.setAcceptAllFileFilterUsed(true);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedFile = fc.getSelectedFile();
            fileLbl.setText(selectedFile.getName()
                    + " (" + DSAUtils.formatSize(selectedFile.length()) + ")");
            fileLbl.setForeground(UIStyle.C_SUCCESS);
            statusLbl.setText("Da chon file: " + selectedFile.getName());
        }
    }

    private void chooseSig() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Chon file chu ky (.sig)");
        fc.setFileFilter(new FileNameExtensionFilter("DSA Signature File (*.sig)", "sig"));

        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            ctx.sysSigFile=file;
            if (verifyMode == 0) {
                selectedSigForFile = file;
                filesigLbl.setText(file.getName());
                filesigLbl.setForeground(UIStyle.C_SUCCESS);
            } else {
                selectedSigForText = file;
                textSigLbl.setText(file.getName());
                textSigLbl.setForeground(UIStyle.C_SUCCESS);
            }
            statusLbl.setText("Da chon .sig: " + file.getName());
        }
    }

    private void choosePubKey() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Chon file khoa cong khai (.public.dsakey)");
        fc.setFileFilter(new FileNameExtensionFilter("DSA Public Key (*.public.dsakey)", "dsakey"));

        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            ctx.sysPublicDsakeyFile=file;
            if (verifyMode == 0) {
                selectedPubForFile = file;
                filepubLbl.setText(file.getName());
                filepubLbl.setForeground(UIStyle.C_SUCCESS);
            } else {
                selectedPubForText = file;
                textPubLbl.setText(file.getName());
                textPubLbl.setForeground(UIStyle.C_SUCCESS);
            }
            statusLbl.setText("Da chon khoa: " + file.getName());
        }
    }

    // ── Verify File ───────────────────────────────────────────────────────

    private void verifyFile() {
        if (!checkCommonInput()) return;
        if (selectedFile == null) {
            warn("Chua chon file goc! (Buoc 1)");
            return;
        }
        if (!selectedFile.exists()) {
            warn("File goc khong ton tai: " + selectedFile.getAbsolutePath());
            return;
        }

        final File currentSig = selectedSigForFile;
        final File currentPub = selectedPubForFile;

        statusLbl.setText("Dang kiem tra file...");
        outArea.setForeground(UIStyle.C_OUTPUT_TEXT);
        outArea.setText("Dang kiem tra, vui long cho...");

        new SwingWorker<VerifyResult, Void>() {
            private DSASignature loadedSig;
            private BigInteger currentHash;

            protected VerifyResult doInBackground() throws Exception {
                BigInteger[] pubVals = DSAUtils.loadPublicKey(currentPub);

                Object[] sigData = DSAUtils.loadSignatureFile(currentSig);
                BigInteger r = (BigInteger) sigData[0];
                BigInteger s = (BigInteger) sigData[1];
                BigInteger hash = (BigInteger) sigData[2];
                String source = (String) sigData[3];
                loadedSig = new DSASignature(r, s, hash, source);

                currentHash = DSAUtils.hashFile(selectedFile);

                dsa.core.param.DSAParameter tempParam = new dsa.core.param.DSAParameter();
                tempParam.p = pubVals[0];
                tempParam.q = pubVals[1];
                tempParam.g = pubVals[2];
                dsa.core.param.DSAKeyPair tempKey = new dsa.core.param.DSAKeyPair();
                tempKey.y = pubVals[3];

                dsa.core.verify.DSAVerifier verifier =
                        new dsa.core.verify.DSAVerifier(tempParam, tempKey);
                return verifier.verifyFile(selectedFile, loadedSig);
            }

            protected void done() {
                try {
                    VerifyResult res = get();
                    ctx.totalVerified++;

                    // Đối chiếu lịch sử cho chế độ FILE
                    boolean isGocMatch = ctx.sysGocFile != null && selectedFile.getAbsolutePath().equals(ctx.sysGocFile.getAbsolutePath());
                    boolean isSigMatch = ctx.sysSigFile != null && currentSig.getAbsolutePath().equals(ctx.sysSigFile.getAbsolutePath());
                    boolean isKeyMatch = ctx.sysPublicDsakeyFile != null && currentPub.getAbsolutePath().equals(ctx.sysPublicDsakeyFile.getAbsolutePath());

                    // Truyền selectedFile thay vì null vào hàm xuất kết quả
                    String output = build8CasesOutput(res, currentHash, loadedSig, "File: " + selectedFile.getName(),
                            isGocMatch, isSigMatch, isKeyMatch, selectedFile, null, currentSig, currentPub);

                    outArea.setText(output);

                    if (res.allOk()) {
                        outArea.setForeground(new Color(46, 139, 87)); // Xanh lá đậm khi an toàn
                        statusLbl.setText("CHU KY HOP LE TOAN DIEN.");
                        ctx.log.add(LogEntry.Action.VERIFY, selectedFile.getName(), true, "Xac thuc file thanh cong - Hop le");
                    } else {
                        outArea.setForeground(Color.RED); // Chuyển màu ĐỎ cảnh báo
                        statusLbl.setText("PHAT HIEN VAN DE!");
                        ctx.log.add(LogEntry.Action.VERIFY, selectedFile.getName(), false, "Phat hien sai lech hoac tap tin da bi sua doi");
                    }
                } catch (Exception ex) {
                    String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                    outArea.setForeground(Color.RED);
                    outArea.setText(buildErrorOutput(msg));
                    statusLbl.setText("Loi kiem tra.");
                    ctx.log.add(LogEntry.Action.VERIFY, selectedFile.getName(), false, msg);
                }
            }
        }.execute();
    }

    // ── Verify Text ───────────────────────────────────────────────────────

    private void verifyText() {
        if (!checkCommonInput()) return;
        String text = txtInput.getText().trim();
        if (text.isEmpty()) {
            warn("Chua nhap van ban can kiem tra! (Buoc 1)");
            return;
        }

        final File currentSig = selectedSigForText;
        final File currentPub = selectedPubForText;

        statusLbl.setText("Dang kiem tra van ban...");
        outArea.setForeground(UIStyle.C_OUTPUT_TEXT);
        outArea.setText("Dang kiem tra, vui long cho...");

        new SwingWorker<VerifyResult, Void>() {
            private DSASignature loadedSig;
            private BigInteger currentHash;

            protected VerifyResult doInBackground() throws Exception {
                BigInteger[] pubVals = DSAUtils.loadPublicKey(currentPub);
                Object[] sigData = DSAUtils.loadSignatureFile(currentSig);
                BigInteger r = (BigInteger) sigData[0];
                BigInteger s = (BigInteger) sigData[1];
                BigInteger hash = (BigInteger) sigData[2];
                String source = (String) sigData[3];
                loadedSig = new DSASignature(r, s, hash, source);

                currentHash = DSAUtils.hashString(text);

                dsa.core.param.DSAParameter tempParam = new dsa.core.param.DSAParameter();
                tempParam.p = pubVals[0];
                tempParam.q = pubVals[1];
                tempParam.g = pubVals[2];
                dsa.core.param.DSAKeyPair tempKey = new dsa.core.param.DSAKeyPair();
                tempKey.y = pubVals[3];

                dsa.core.verify.DSAVerifier verifier =
                        new dsa.core.verify.DSAVerifier(tempParam, tempKey);
                return verifier.verifyText(text, loadedSig);
            }

            protected void done() {
                try {
                    VerifyResult res = get();
                    ctx.totalVerified++;

                    // Đối chiếu lịch sử cho chế độ VĂN BẢN
                    boolean isGocMatch = (ctx.sysGocText != null) && text.equals(ctx.sysGocText);
                    boolean isSigMatch = (ctx.sysSigFile != null) && currentSig.getAbsolutePath().equals(ctx.sysSigFile.getAbsolutePath());
                    boolean isKeyMatch = (ctx.sysPublicDsakeyFile != null) && currentPub.getAbsolutePath().equals(ctx.sysPublicDsakeyFile.getAbsolutePath());

                    // Truyền text nội dung vào tham số chuỗi văn bản
                    String output = build8CasesOutput(res, currentHash, loadedSig, "Van ban: \"" + shorten(text) + "\"",
                            isGocMatch, isSigMatch, isKeyMatch, null, text, currentSig, currentPub);

                    outArea.setText(output);

                    if (res.allOk() ) {
                        outArea.setForeground(new Color(46, 139, 87));
                        statusLbl.setText("CHU KY HOP LE TOAN DIEN.");
                        ctx.log.add(LogEntry.Action.VERIFY, shorten(text), true, "Xac thuc van ban thanh cong");
                    } else {
                        outArea.setForeground(Color.RED);
                        statusLbl.setText("PHAT HIEN VAN DE!");
                        ctx.log.add(LogEntry.Action.VERIFY, shorten(text), false, "Van ban bi thay doi hoac sai lech phien");
                    }
                } catch (Exception ex) {
                    String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                    outArea.setForeground(Color.RED);
                    outArea.setText(buildErrorOutput(msg));
                    statusLbl.setText("Loi kiem tra.");
                    ctx.log.add(LogEntry.Action.VERIFY, shorten(text), false, msg);
                }
            }
        }.execute();
    }

    // ── Check common input ────────────────────────────────────────────────

    private boolean checkCommonInput() {
        File currentSig = (verifyMode == 0) ? selectedSigForFile : selectedSigForText;
        File currentPub = (verifyMode == 0) ? selectedPubForFile : selectedPubForText;

        if (currentSig == null) {
            warn("Chua chon file .sig! (Buoc 2)");
            return false;
        }
        if (currentPub == null) {
            warn("Chua chon file khoa cong khai! (Buoc 3)");
            return false;
        }
        if (!currentSig.exists()) {
            warn("File .sig khong ton tai: " + currentSig.getAbsolutePath());
            return false;
        }
        if (!currentPub.exists()) {
            warn("File khoa khong ton tai: " + currentPub.getAbsolutePath());
            return false;
        }
        return true;
    }

    // ── Reset ─────────────────────────────────────────────────────────────

    private void reset() {
        selectedFile = null;
        selectedSigForFile = null;
        selectedSigForText = null;
        selectedPubForFile = null;
        selectedPubForText = null;
        txtInput.setText("");
        fileLbl.setText("Chua chon file goc");
        fileLbl.setForeground(UIStyle.C_TEXT_MUTED);
        filesigLbl.setText("Chua chon file .sig");
        filesigLbl.setForeground(UIStyle.C_TEXT_MUTED);
        filepubLbl.setText("Chua chon file .public.dsakey");
        filepubLbl.setForeground(UIStyle.C_TEXT_MUTED);
        textSigLbl.setText("Chua chon file .sig");
        textSigLbl.setForeground(UIStyle.C_TEXT_MUTED);
        textPubLbl.setText("Chua chon file .public.dsakey");
        textPubLbl.setForeground(UIStyle.C_TEXT_MUTED);
        outArea.setForeground(UIStyle.C_OUTPUT_TEXT);
        outArea.setText(">>> Da dat lai. San sang kiem tra moi.");
        statusLbl.setText("San sang kiem tra.");
    }

    // ── Build 8 Cases Output ──────────────────────────────────────────────

    private String build8CasesOutput(VerifyResult res, BigInteger currentHash, DSASignature sig, String source,
                                     boolean isGocMatch, boolean isSigMatch, boolean isKeyMatch,
                                     File currentGocFile, String currentGocText, File currentSig, File currentPub) {
        String line = "=".repeat(56);
        String bar = "-".repeat(56);
        StringBuilder sb = new StringBuilder();

        sb.append(line).append("\n");
        sb.append("        KET QUA KIEM TRA TOAN VEN & LICH SU DSA\n");
        sb.append(line).append("\n\n");

        sb.append("Nguon xac thuc :\n  -> ").append(source).append("\n");
        sb.append("File .sig nap   :\n  -> ").append(currentSig.getName()).append("\n");
        sb.append("File .key nap   :\n  -> ").append(currentPub.getName()).append("\n\n");

        // [1] Chữ ký hợp lệ toán học
        sb.append(bar).append("\n");
        sb.append(" [1] CHU KY CO HOP LE VE MAT TOAN HOC KHONG?\n").append(bar).append("\n");
        if (res.signatureValid) {
            sb.append(" => HOP LE (v = r)\n  Chu ky so phu hop ve thuat toan voi khoa cong khai cung cap.\n");
        } else {
            sb.append(" => KHONG HOP LE (v != r)\n  Cap gia tri (r,s) hoac khoa cong khai khong hop nhat.\n");
        }
        sb.append("\n");

        // [2] Toàn vẹn dữ liệu nạp
        sb.append(bar).append("\n");
        sb.append(" [2] NOI DUNG CO KHOP VOI MA HASH LUC KY KHONG?\n").append(bar).append("\n");
        if (res.integrityOk) {
            sb.append(" => NGUYEN VEN - Ma bam du lieu hien tai khop voi file chu ky.\n");
        } else {
            sb.append(" => BI THAY DOI - Ma bam du lieu hien tai khong khop voi file chu ky(txt.sig).\n");
        }
        sb.append("\n");

        // [3] Đối chiếu file chữ ký lịch sử
//        sb.append(bar).append("\n");
//        sb.append(" [3] FILE CHU KY (.sig) CO BI SUA DOI / TRAO DOI KHONG?\n").append(bar).append("\n");
//        if (isSigMatch) {
//            sb.append(" => NGUYEN VEN - File .sig chinh xac va hop nhat phien lam viec.\n");
//        } else {
//            sb.append(" => CANH BAO: File .sig hien tai KHONG TRUNG KHOP voi file tao ra o tab Tao Chu Ky!\n");
//            sb.append("    + File ban nap: ").append(currentSig.getName()).append("\n");
//            sb.append("    + File cua phien: ").append(ctx.sysSigFile != null ? ctx.sysSigFile.getName() : "[Trong]").append("\n");
//        }
//        sb.append("\n");
//
//        // [4] Đối chiếu file khóa công khai lịch sử
//        sb.append(bar).append("\n");
//        sb.append(" [4] FILE KHOA CONG KHAI (.public.dsakey) CO CHINH XAC KHONG?\n").append(bar).append("\n");
//        if (isKeyMatch) {
//            sb.append(" => CHINH XAC - Khop hoan toan voi file tu tab Tao Tham So.\n");
//        } else {
//            sb.append(" => CANH BAO: File khoa cong khai KHONG TRUNG KHOP voi file tu tab Tao Tham So!\n");
//            sb.append("    + File ban nap: ").append(currentPub.getName()).append("\n");
//            sb.append("    + File cua phien: ").append(ctx.sysPublicDsakeyFile != null ? ctx.sysPublicDsakeyFile.getName() : "[Trong]").append("\n");
//        }
        sb.append("\n");

        /*// ── [5] TỔNG HỢP ĐỐI CHIẾU CHÉO 8 TRƯỜNG HỢP LỊCH SỬ ──────────────────
        sb.append(bar).append("\n");
        sb.append(" [5] DANH GIA TOAN VEN: CA DU LIEU LAN KHOA CO BI SUA KHONG?\n").append(bar).append("\n");

        // Phân rã rành mạch các trạng thái nhị phân của (isGocMatch, isSigMatch, isKeyMatch)
        if (isGocMatch && isSigMatch && isKeyMatch) {
            sb.append(" => [TRUONG HOP 1]: AN TOAN TUYET DOI (1-1-1)\n");
            sb.append("    - Danh gia: Ca 3 thanh phan (Du lieu goc, File chu ky, Khoa cong khai) hoan toan trùng khop phien va nguyen ven.\n");
        } else if (!isGocMatch && isSigMatch && isKeyMatch) {
            sb.append(" => [TRUONG HOP 2]: DU LIEU GOC BI THAY DOI / GIA MAO (0-1-1)\n");
            sb.append("    - Danh gia: File .sig va .public.dsakey dung phien, nhung noi dung du lieu da bi can thiep hoac tai file khac.\n");
            if (verifyMode == 0 && currentGocFile != null) {
                sb.append("    + File dang check: ").append(currentGocFile.getName()).append("\n");
                sb.append("    + File thuc te phien ky: ").append(ctx.sysGocFile != null ? ctx.sysGocFile.getName() : "[Khong ro]").append("\n");
            } else if (verifyMode == 1) {
                sb.append("    + Text dang check: \"").append(shorten(currentGocText)).append("\"\n");
                sb.append("    + Text thuc te phien ky: \"").append(ctx.sysGocText != null ? shorten(ctx.sysGocText) : "[Khong ro]").append("\"\n");
            }
        } else if (isGocMatch && !isSigMatch && isKeyMatch) {
            sb.append(" => [TRUONG HOP 3]: THAY THE/GIA MAO FILE CHU KY .SIG (1-0-1)\n");
            sb.append("    - Danh gia: Du lieu va khoa hop le voi phien, nhung file chu ky .sig duoc nap tu phien hoac nguon khac.\n");
        } else if (isGocMatch && isSigMatch && !isKeyMatch) {
            sb.append(" => [TRUONG HOP 4]: THAY THE KHOA CONG KHAI KHAC PHIEN (1-1-0)\n");
            sb.append("    - Danh gia: Du lieu va chu ky khop phien, nhung file public key bi dung sai (phai dung dung file goc o tab Tham so).\n");
        } else if (!isGocMatch && isSigMatch && !isKeyMatch) {
            sb.append(" => [TRUONG HOP 5]: DU LIEU GOC VA KHOA CONG KHAI DEU SAI PHIEN (0-1-0)\n");
            sb.append("    - Danh gia: Chi co tep chu ky hop le với phien, con du lieu kiem tra va khoa cong khai da bi xáo tron.\n");
        } else if (!isGocMatch && !isSigMatch && isKeyMatch) {
            sb.append(" => [TRUONG HOP 6]: SAI PHIEN CAP DU LIEU XAC THUC (0-0-1)\n");
            sb.append("    - Danh gia: Khoa khop phien, nhung ca file goc va file chu ky deu khong an khop voi phien vua sinh.\n");
        } else if (isGocMatch && !isSigMatch && !isKeyMatch) {
            sb.append(" => [TRUONG HOP 7]: CAP KHOA VA CHU KY DI KEM DEU KHONG KHOP PHIEN (1-0-0)\n");
            sb.append("    - Danh gia: Noi dung goc dung phien hien tai, nhung cap chu ky/khoa di kem lai thuoc mot chuoi an toan khac.\n");
        } else { // !isGocMatch && !isSigMatch && !isKeyMatch
            sb.append(" => [TRUONG HOP 8]: PHIEN LAM VIEC GIA MAO TOAN BO (0-0-0)\n");
            sb.append("    - Danh gia: Ca 3 file nap vao deu sai lech hoan toan so voi lich su hoat dong cua ung dung trong phien nay.\n");
        }
        sb.append("\n");*/

        // Đúc kết cuối cùng ở dòng đáy khung hiển thị
        sb.append(line).append("\n");
        if (res.allOk()) {
            sb.append("  TONG KET: CHU KY HOP LE HOAN TOAN [AN TOAN]\n");
        } else {
            sb.append("  TONG KET: CHU KY KHONG HOP LE BAN PHIEN [CANH BAO NGUY HIEM]\n");
        }
        sb.append(line);

        return sb.toString();
    }

    // ── Error output ──────────────────────────────────────────────────────

    private String buildErrorOutput(String msg) {
        String line = "=".repeat(56);
        StringBuilder sb = new StringBuilder();
        sb.append(line).append("\n");
        sb.append("  LOI KHI KIEM TRA CHU KY\n");
        sb.append(line).append("\n\n");
        sb.append("Chi tiet loi:\n  ").append(msg).append("\n\n");
        sb.append("Goi y khac phuc:\n");
        sb.append("  1. Kiem tra dinh dang va cau truc file .sig nap vao.\n");
        sb.append("  2. Dam bao dung file .public.dsakey duoc sinh tu he thong.\n");
        sb.append(line);
        return sb.toString();
    }


    private String shorten(String s) {
        return s.length() > 30 ? s.substring(0, 30) + "..." : s;
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Thong bao", JOptionPane.WARNING_MESSAGE);
    }
    public void autoFillFromContext() {
        if (ctx.sysSigFile != null && ctx.sysSigFile.exists()) {
            selectedSigForFile = ctx.sysSigFile;
            filesigLbl.setText(ctx.sysSigFile.getName());
            filesigLbl.setForeground(UIStyle.C_SUCCESS);

            selectedSigForText = ctx.sysSigFile;
            textSigLbl.setText(ctx.sysSigFile.getName());
            textSigLbl.setForeground(UIStyle.C_SUCCESS);
        }
        if (ctx.sysPublicDsakeyFile != null && ctx.sysPublicDsakeyFile.exists()) {
            selectedPubForFile = ctx.sysPublicDsakeyFile;
            filepubLbl.setText(ctx.sysPublicDsakeyFile.getName());
            filepubLbl.setForeground(UIStyle.C_SUCCESS);

            selectedPubForText = ctx.sysPublicDsakeyFile;
            textPubLbl.setText(ctx.sysPublicDsakeyFile.getName());
            textPubLbl.setForeground(UIStyle.C_SUCCESS);
        }
    }
}