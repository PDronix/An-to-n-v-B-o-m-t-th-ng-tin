package dsa.ui;

import dsa.AppContext;
import dsa.core.log.LogEntry;
import dsa.core.sign.DSASignature;
import dsa.util.DSAUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.File;
import java.util.List;

/**
 * SignPanel – Tạo chữ ký số cho văn bản hoặc file
 */
public class SignPanel extends JPanel {

    private final AppContext ctx;
    private final JTextArea outArea = UIStyle.outputArea();
    private final JTextField txtField = UIStyle.textField();
    private final JLabel fileLabel = UIStyle.muted("Chua chon file");
    private final JLabel statusLbl;
    private File selectedFile = null;
    private File lastSavedTextFile = null;
    private Runnable goToParamPanel = null;  // callback chuyen sang tab Tao Tham So

    /**
     * Dat callback chuyen tab — goi tu MainFrame
     */
    public void setGoToParamPanel(Runnable r) {
        this.goToParamPanel = r;
    }

    public SignPanel(AppContext ctx) {
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

        statusLbl = UIStyle.muted("San sang ky.");
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
        p.add(UIStyle.h1("Tao Chu Ky So"), BorderLayout.WEST);
        p.add(UIStyle.muted("Ky van ban hoac file | Ho tro PDF, DOCX, XLSX, PNG, JPG, TXT, CSV, XML, JSON..."), BorderLayout.SOUTH);
        return p;
    }

    // ── Control panel ─────────────────────────────────────────────────────

    private JPanel makeControlPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(UIStyle.C_BG);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

        // Card: Ký văn bản
        JPanel textCard = UIStyle.card(new BorderLayout(0, 8));
        textCard.setAlignmentX(LEFT_ALIGNMENT);
        textCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        textCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(UIStyle.C_BORDER),
                        "Ky Van Ban", 0, 0, UIStyle.F_BOLD, UIStyle.C_PRI),
                BorderFactory.createEmptyBorder(8, 10, 10, 10)));

        txtField.setAlignmentX(LEFT_ALIGNMENT);

        // Hang 1: o nhap van ban
        JPanel tRow = new JPanel(new BorderLayout(6, 0));
        tRow.setOpaque(false);
        tRow.add(txtField, BorderLayout.CENTER);

        // Hang 2: 2 nut — Luu .txt (tuy chon) + Ky
        JButton btnSaveTxt = UIStyle.secondaryBtn("Luu File .txt");
        JButton btnSignText = UIStyle.successBtn("Ky Van Ban");
        btnSaveTxt.setToolTipText("Tuy chon: luu noi dung thanh file .txt (tu dat ten)");
        btnSignText.setToolTipText("Ky truc tiep noi dung van ban, khong can luu file truoc");
        btnSaveTxt.addActionListener(e -> saveTextFile());
        btnSignText.addActionListener(e -> signText());

        JPanel tBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tBtns.setOpaque(false);
        tBtns.add(btnSaveTxt);
        tBtns.add(btnSignText);

        textCard.add(UIStyle.muted("Nhap noi dung can ky (co the luu thanh .txt hoac ky truc tiep):"), BorderLayout.NORTH);
        textCard.add(tRow, BorderLayout.CENTER);
        textCard.add(tBtns, BorderLayout.SOUTH);

        // Card: Ký file (drop zone)
        JPanel fileCard = UIStyle.card(new BorderLayout(0, 8));
        fileCard.setAlignmentX(LEFT_ALIGNMENT);
        fileCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        fileCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(UIStyle.C_BORDER),
                        "Ky File", 0, 0, UIStyle.F_BOLD, UIStyle.C_PRI),
                BorderFactory.createEmptyBorder(8, 10, 10, 10)));

        // Drop zone
        JPanel dropZone = makeDropZone();

        // Info file + nút chọn
        JPanel fileInfo = new JPanel(new BorderLayout(6, 0));
        fileInfo.setOpaque(false);
        JButton btnChoose = UIStyle.secondaryBtn("Chon File");
        btnChoose.addActionListener(e -> chooseFile());
        fileInfo.add(fileLabel, BorderLayout.CENTER);
        fileInfo.add(btnChoose, BorderLayout.EAST);

        JButton btnSignFile = UIStyle.primaryBtn("  Ky File  ");
        btnSignFile.addActionListener(e -> signFile());
        JPanel fBtn = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        fBtn.setOpaque(false);
        fBtn.add(btnSignFile);

        fileCard.add(dropZone, BorderLayout.NORTH);
        fileCard.add(fileInfo, BorderLayout.CENTER);
        fileCard.add(fBtn, BorderLayout.SOUTH);

        // Card: Lưu chữ ký
        JPanel saveCard = UIStyle.card(new BorderLayout(0, 6));
        saveCard.setAlignmentX(LEFT_ALIGNMENT);
        saveCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        saveCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(UIStyle.C_BORDER),
                        "Luu Chu Ky", 0, 0, UIStyle.F_BOLD, UIStyle.C_TEXT_MUTED),
                BorderFactory.createEmptyBorder(6, 10, 8, 10)));

        JButton btnSaveSig = UIStyle.secondaryBtn("Luu File .sig");
        btnSaveSig.addActionListener(e -> saveSignature());
        JPanel sRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        sRow.setOpaque(false);
        sRow.add(btnSaveSig);
        saveCard.add(UIStyle.muted("Luu chu ky ra file .sig de xac thuc sau:"), BorderLayout.NORTH);
        saveCard.add(sRow, BorderLayout.CENTER);

        p.add(textCard);
        p.add(Box.createVerticalStrut(10));
        p.add(fileCard);
        p.add(Box.createVerticalStrut(10));
        p.add(saveCard);
        p.add(Box.createVerticalGlue());
        return p;
    }

    // ── Drop zone ─────────────────────────────────────────────────────────

    private JPanel makeDropZone() {
        JPanel zone = new JPanel(new BorderLayout());
        zone.setBackground(new Color(239, 246, 255));
        zone.setPreferredSize(new Dimension(0, 80));
        zone.setBorder(BorderFactory.createDashedBorder(UIStyle.C_PRI, 2f, 6f, 3f, true));

        JLabel hint = new JLabel("Keo tha file vao day", SwingConstants.CENTER);
        hint.setFont(UIStyle.F_BODY);
        hint.setForeground(UIStyle.C_PRI);
        zone.add(hint, BorderLayout.CENTER);

        // Drag & Drop
        new DropTarget(zone, new DropTargetListener() {
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> files = (List<File>)
                            dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) {
                        selectedFile = files.get(0);
                        updateFileLabel();
                        zone.setBackground(new Color(220, 252, 231));
                        hint.setText("File: " + selectedFile.getName());
                        hint.setForeground(UIStyle.C_SUCCESS);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            public void dragEnter(DropTargetDragEvent e) {
                zone.setBackground(new Color(219, 234, 254));
            }

            public void dragExit(DropTargetEvent e) {
                zone.setBackground(new Color(239, 246, 255));
            }

            public void dragOver(DropTargetDragEvent e) {
            }

            public void dropActionChanged(DropTargetDragEvent e) {
            }
        });

        return zone;
    }

    // ── Output panel ──────────────────────────────────────────────────────

    private JPanel makeOutputPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(UIStyle.C_BG);

        JPanel top = new JPanel(new BorderLayout(0, 2));
        top.setOpaque(false);
        top.add(UIStyle.h2("Ket Qua Ky"), BorderLayout.NORTH);
        top.add(UIStyle.muted("Hien thi Base64 va Hex cua (r, s) va H(m)"), BorderLayout.CENTER);

        p.add(top, BorderLayout.NORTH);
        p.add(UIStyle.scrollWrap(outArea), BorderLayout.CENTER);
        return p;
    }

    // ── Actions ───────────────────────────────────────────────────────────

    // Luu van ban thanh file .txt (tuy chon, nguoi dung tu dat ten)
    private void saveTextFile() {
        String text = txtField.getText().trim();
        if (text.isEmpty()) {
            warn("Vui long nhap noi dung truoc khi luu.");
            return;
        }
        String name = JOptionPane.showInputDialog(this,
                "Dat ten file van ban (khong can duoi .txt):\nVi du: hopdong_thang6",
                "Dat Ten File", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;
        name = name.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
        if (!name.endsWith(".txt")) name += ".txt";
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Chon noi luu file .txt");
        fc.setSelectedFile(new File(name));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File f = fc.getSelectedFile();
                if (!f.getName().endsWith(".txt")) f = new File(f.getAbsolutePath() + ".txt");
                java.nio.file.Files.write(f.toPath(),
                        text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                lastSavedTextFile = f;
                ctx.sysGocFile = f;
                ctx.sysGocText = text;
                append(">>> Van ban da luu: " + f.getAbsolutePath() + "\n");
                statusLbl.setText("Da luu van ban: " + f.getName());
            } catch (Exception ex) {
                warn("Loi luu file: " + ex.getMessage());
            }
        }
    }

    // Ky truc tiep noi dung van ban (khong can luu file truoc)
    private void signText() {
        if (!ctx.hasKeys()) {
            warnNoKey();
            return;
        }
        String text = txtField.getText().trim();
        if (text.isEmpty()) {
            warn("Vui long nhap noi dung can ky.");
            return;
        }
        try {
            ctx.lastSignature = ctx.newSigner().signText(text);
            ctx.totalSigned++;
            String src = lastSavedTextFile != null ? lastSavedTextFile.getName() : "van_ban_nhap_tay";
            ctx.lastSignature.sourceFile = src;
            ctx.log.add(LogEntry.Action.SIGN_TEXT, shorten(text), true, "Ky van ban thanh cong");
            outArea.setForeground(UIStyle.C_OUTPUT_TEXT);
            outArea.setText(buildSignOutput("Van ban", src));
            statusLbl.setText("Da ky van ban thanh cong.");
            showSaveReminder();
        } catch (Exception ex) {
            ctx.log.add(LogEntry.Action.SIGN_TEXT, shorten(text), false, ex.getMessage());
            outArea.setForeground(new Color(255, 100, 100));
            outArea.setText("LOI ky: " + ex.getMessage());
        }
    }

    private void chooseFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Chon file can ky");
        fc.setFileFilter(new FileNameExtensionFilter(
                "Cac file ho tro (pdf, docx, xlsx, png, jpg, txt, csv, xml, json, zip)",
                "pdf", "docx", "xlsx", "png", "jpg", "jpeg", "txt", "csv", "xml", "json", "zip"));
        fc.setAcceptAllFileFilterUsed(true);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedFile = fc.getSelectedFile();
            updateFileLabel();
        }
    }

    private void signFile() {
        if (!ctx.hasKeys()) {
            warnNoKey();
            return;
        }
        if (selectedFile == null) {
            warn("Vui long chon file truoc!");
            return;
        }

        statusLbl.setText("Dang ky file: " + selectedFile.getName() + "...");
        new SwingWorker<DSASignature, Void>() {
            protected DSASignature doInBackground() throws Exception {
                return ctx.newSigner().signFile(selectedFile);
            }

            protected void done() {
                try {
                    ctx.lastSignature = get();
                    ctx.totalSigned++;
                    ctx.sysGocFile = selectedFile;
                    ctx.log.add(LogEntry.Action.SIGN_FILE, selectedFile.getName(), true, "Ky file thanh cong");
                    outArea.setForeground(UIStyle.C_OUTPUT_TEXT);
                    outArea.setText(buildSignOutput("File", selectedFile.getName()) + "\nhash: " + ctx.lastSignature.hash);
                    statusLbl.setText("Da ky file: " + selectedFile.getName());
                    showSaveReminder();
                } catch (Exception ex) {
                    ctx.log.add(LogEntry.Action.SIGN_FILE, selectedFile.getName(), false, ex.getMessage());
                    outArea.setForeground(new Color(255, 100, 100));
                    outArea.setText("LOI ky file: " + ex.getMessage());
                    statusLbl.setText("Loi ky file.");
                }
            }
        }.execute();
    }

    private void saveSignature() {
        if (ctx.lastSignature == null) {
            warn("Chua co chu ky. Hay ky truoc!");
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Luu file chu ky .sig");
        // Tu dong dat ten theo file goc
        String sigName = ctx.lastSignature.sourceFile + ".sig";
        fc.setSelectedFile(new File(sigName));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File f = fc.getSelectedFile();
                if (!f.getName().endsWith(".sig")) {
                    f = new File(f.getAbsolutePath() + ".sig");
                }
                append("\nsave hash:" + ctx.lastSignature.hash);
                DSAUtils.saveSignatureFile(f,
                        ctx.lastSignature.r, ctx.lastSignature.s,
                        ctx.lastSignature.sourceFile, ctx.lastSignature.hash);
                ctx.log.add(LogEntry.Action.SIGN_FILE, f.getName(), true, "Luu chu ky thanh cong");
                ctx.sysSigFile = f;
                append("\n>>> Chu ky da luu: " + f.getAbsolutePath());
                statusLbl.setText("Luu chu ky: " + f.getName());
            } catch (Exception ex) {
                warn("Loi luu chu ky: " + ex.getMessage());
            }
        }
    }

    // ── Build output ──────────────────────────────────────────────────────

    private String buildSignOutput(String type, String source) {
        DSASignature sig = ctx.lastSignature;
        StringBuilder sb = new StringBuilder();
        sb.append("=== KET QUA KY SO DSA ===\n\n");
        sb.append("Loai    : ").append(type).append("\n");
        sb.append("Nguon   : ").append(source).append("\n");
        sb.append("Thoi gian: ").append(sig.signedAt).append("\n\n");

        sb.append("-- H(m) SHA-224 --\n");
        sb.append("Base64:\n  ").append(sig.hashToBase64()).append("\n");
        sb.append("Hex:\n  ").append(sig.hashToHex()).append("\n\n");

        sb.append("-- r --\n");
        sb.append("Base64:\n  ").append(sig.rToBase64()).append("\n");
        sb.append("Hex:\n  ").append(sig.rToHex()).append("\n\n");

        sb.append("-- s --\n");
        sb.append("Base64:\n  ").append(sig.sToBase64()).append("\n");
        sb.append("Hex:\n  ").append(sig.sToHex()).append("\n\n");

        sb.append(">>> Ky thanh cong!\n");
        sb.append(">>> Bam 'Luu File .sig' de luu chu ky.\n");
        sb.append(">>> Chuyen sang 'Kiem Tra Chu Ky' de xac thuc.");
        return sb.toString();
    }

    private void updateFileLabel() {
        if (selectedFile != null) {
            fileLabel.setText(selectedFile.getName()
                    + "  (" + DSAUtils.formatSize(selectedFile.length()) + ")");
            fileLabel.setForeground(UIStyle.C_SUCCESS);
        }
    }

    private void append(String text) {
        outArea.append(text);
        outArea.setCaretPosition(outArea.getDocument().getLength());
    }

    private String shorten(String s) {
        return s.length() > 30 ? s.substring(0, 30) + "..." : s;
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Thong bao", JOptionPane.WARNING_MESSAGE);
    }

    private void showSaveReminder() {
        JOptionPane.showMessageDialog(this,
                "Ky thanh cong!\n\n"
                        + "De xac thuc sau khi tat chuong trinh, ban can luu:\n"
                        + "  1. File chu ky (.sig)  → bam 'Luu File .sig'\n"
                        + "  2. File khoa (.dsakey) → vao 'Tao Tham So' → 'Luu Khoa'\n\n"
                        + "Thieu 1 trong 2 file tren se KHONG xac thuc duoc!",
                "Nhac Nho Luu File", JOptionPane.INFORMATION_MESSAGE);
    }

    private void warnNoKey() {
        Object[] options = {"Chuyen sang Tao Tham So", "Dong"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Chua co tham so va khoa!\n\n"
                        + "Ban can vao muc 'Tao Tham So' truoc:\n"
                        + "  1. Bam nut 'Sinh Tham So & Cap Khoa'\n"
                        + "  2. Doi khoang 5-10 giay\n"
                        + "  3. Quay lai day de ky\n\n"
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


