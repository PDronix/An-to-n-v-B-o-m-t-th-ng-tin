package dsa;

import javax.swing.*;
import java.awt.*;
import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Main – Điểm vào ứng dụng, xây dựng giao diện Swing
 *
 * Biên dịch: mvn package
 * Chạy     : java -jar target/dsa_java.jar
 *      hoặc: mvn exec:java -Dexec.mainClass=dsa.Main
 */
public class Main {

    // =================================================================
    //  STYLE
    // =================================================================
    static final Color C_BG   = new Color(245, 247, 252);
    static final Color C_PRI  = new Color(25,  90,  170);
    static final Color C_OK   = new Color(0,   130,  55);
    static final Color C_ERR  = new Color(185,  20,  20);
    static final Color C_CBOR = new Color(190, 210, 245);
    static final Color C_CBG  = new Color(233, 241, 255);
    static final Font  F_MONO = new Font("Monospaced", Font.PLAIN, 12);
    static final Font  F_UI   = new Font("Segoe UI",   Font.PLAIN, 13);
    static final Font  F_BOLD = new Font("Segoe UI",   Font.BOLD,  13);
    static final Font  F_H1   = new Font("Segoe UI",   Font.BOLD,  15);

    // =================================================================
    //  UI HELPERS
    // =================================================================
    static JTextArea mkOut() {
        JTextArea ta = new JTextArea();
        ta.setEditable(false); ta.setFont(F_MONO);
        ta.setBackground(Color.WHITE);
        ta.setLineWrap(true); ta.setWrapStyleWord(true);
        ta.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        return ta;
    }
    static JScrollPane mkScroll(JTextArea ta) {
        JScrollPane sp = new JScrollPane(ta);
        sp.setBorder(BorderFactory.createLineBorder(new Color(200, 210, 230)));
        return sp;
    }
    static JButton mkBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFont(F_BOLD); b.setOpaque(true);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        return b;
    }
    static JPanel mkHeader(String title, String sub) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setBackground(C_CBG);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_CBOR),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        JLabel t = new JLabel(title); t.setFont(F_H1); t.setForeground(C_PRI);
        JLabel s = new JLabel(sub);   s.setFont(F_UI); s.setForeground(new Color(70, 90, 130));
        p.add(t, BorderLayout.NORTH); p.add(s, BorderLayout.CENTER);
        return p;
    }
    static JTextField mkField(boolean editable) {
        JTextField tf = new JTextField();
        tf.setFont(F_MONO); tf.setEditable(editable);
        tf.setBackground(editable ? Color.WHITE : new Color(248, 248, 252));
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 200, 230)),
            BorderFactory.createEmptyBorder(4, 7, 4, 7)));
        return tf;
    }
    static JLabel mkLbl(String t) {
        JLabel l = new JLabel(t); l.setFont(F_BOLD); return l;
    }

    // =================================================================
    //  TAB 1 – GIAO DIEN
    // =================================================================
    static class HomePanel extends JPanel {
        HomePanel() {
            setLayout(new BorderLayout()); setBackground(C_BG);
            JPanel banner = new JPanel(new GridLayout(3, 1, 0, 3));
            banner.setBackground(C_PRI);
            banner.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
            banner.add(lbl("DSA - Digital Signature Algorithm",
                new Font("Segoe UI", Font.BOLD, 21), Color.WHITE));
            banner.add(lbl("Minh hoa Chu Ky So - Nhom 10 - HaUI-SICT-IT6001.2",
                new Font("Segoe UI", Font.PLAIN, 13), new Color(200, 220, 255)));
            banner.add(lbl("Chuan FIPS 186  |  Khoa 512-bit  |  Ham bam SHA-1",
                new Font("Segoe UI", Font.ITALIC, 12), new Color(180, 205, 255)));

            JPanel body = new JPanel();
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setBackground(C_BG);
            body.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

            body.add(secLbl("QUY TRINH SU DUNG"));
            body.add(Box.createVerticalStrut(10));
            body.add(card(1, "Tab 2 - Tao Tham So",
                "Bam nut de sinh tham so he thong DSA (p, q, g) va cap khoa (x bi mat, y cong khai)."));
            body.add(Box.createVerticalStrut(8));
            body.add(card(2, "Tab 3 - Tao Chu Ky",
                "Nhap thong diep, bam Ky de sinh chu ky (r, s)."));
            body.add(Box.createVerticalStrut(8));
            body.add(card(3, "Tab 4 - Kiem Tra Chu Ky",
                "Bam 'Dien tu dong', sau do bam 'KIEM TRA CHU KY' de xem toan bo ket qua."));
            body.add(Box.createVerticalStrut(18));

            body.add(secLbl("CONG THUC DSA"));
            body.add(Box.createVerticalStrut(10));
            body.add(formulas());

            JScrollPane sc = new JScrollPane(body);
            sc.setBorder(null); sc.getViewport().setBackground(C_BG);
            add(banner, BorderLayout.NORTH); add(sc, BorderLayout.CENTER);
        }
        JLabel lbl(String t, Font f, Color c) { JLabel l = new JLabel(t); l.setFont(f); l.setForeground(c); return l; }
        JLabel secLbl(String t) {
            JLabel l = new JLabel(t); l.setFont(new Font("Segoe UI", Font.BOLD, 13));
            l.setForeground(C_PRI); l.setAlignmentX(LEFT_ALIGNMENT); return l;
        }
        JPanel card(int n, String title, String desc) {
            JPanel c = new JPanel(new BorderLayout(12, 0));
            c.setBackground(Color.WHITE);
            c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 220, 240)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));
            c.setAlignmentX(LEFT_ALIGNMENT);
            JLabel num = new JLabel(String.valueOf(n), SwingConstants.CENTER);
            num.setFont(new Font("Segoe UI", Font.BOLD, 20));
            num.setForeground(Color.WHITE); num.setBackground(C_PRI);
            num.setOpaque(true); num.setPreferredSize(new Dimension(36, 36));
            JPanel txt = new JPanel(new BorderLayout(0, 3)); txt.setOpaque(false);
            JLabel tl = new JLabel(title); tl.setFont(F_BOLD); tl.setForeground(C_PRI);
            JLabel dl = new JLabel("<html>" + desc + "</html>"); dl.setFont(F_UI); dl.setForeground(new Color(60, 60, 70));
            txt.add(tl, BorderLayout.NORTH); txt.add(dl, BorderLayout.CENTER);
            c.add(num, BorderLayout.WEST); c.add(txt, BorderLayout.CENTER);
            return c;
        }
        JPanel formulas() {
            JPanel p = new JPanel(new GridLayout(1, 2, 12, 0));
            p.setBackground(C_BG); p.setAlignmentX(LEFT_ALIGNMENT);
            p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
            p.add(fbox("Ky so", "r = (g^k mod p) mod q\ns = k^-1 * (H(m) + x*r) mod q\n\nDieu kien: 0 < k < q ngau nhien"));
            p.add(fbox("Xac thuc", "w = s^-1 mod q\nu1 = H(m)*w mod q\nu2 = r*w mod q\nv = (g^u1 * y^u2 mod p) mod q\nHop le khi v = r"));
            return p;
        }
        JPanel fbox(String title, String text) {
            JPanel p = new JPanel(new BorderLayout(0, 4));
            p.setBackground(new Color(232, 240, 255));
            p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_CBOR),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            JLabel tl = new JLabel(title); tl.setFont(F_BOLD); tl.setForeground(C_PRI);
            JTextArea ta = new JTextArea(text); ta.setEditable(false); ta.setOpaque(false);
            ta.setFont(new Font("Monospaced", Font.PLAIN, 11)); ta.setForeground(new Color(30, 30, 60));
            p.add(tl, BorderLayout.NORTH); p.add(ta, BorderLayout.CENTER);
            return p;
        }
    }

    // =================================================================
    //  TAB 2 – TAO THAM SO
    // =================================================================
    static class ParamPanel extends JPanel {
        final DSA dsa; final JTextArea out = mkOut();
        ParamPanel(DSA dsa) {
            this.dsa = dsa;
            setLayout(new BorderLayout(8, 8)); setBackground(C_BG);
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            JPanel top = new JPanel(new BorderLayout(0, 8)); top.setOpaque(false);
            top.add(mkHeader("Tao Tham So & Cap Khoa",
                "Sinh tham so he thong DSA (p, q, g) va cap khoa (x bi mat - y cong khai)."), BorderLayout.NORTH);
            JButton b = mkBtn("Sinh Tham So & Cap Khoa", C_PRI);
            b.addActionListener(e -> generate());
            JPanel br = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); br.setOpaque(false); br.add(b);
            top.add(br, BorderLayout.CENTER);
            add(top, BorderLayout.NORTH); add(mkScroll(out), BorderLayout.CENTER);
        }
        void generate() {
            out.setForeground(new Color(100, 100, 100));
            out.setText("Dang sinh tham so... vui long cho.");
            new SwingWorker<String, Void>() {
                protected String doInBackground() throws Exception {
                    dsa.setup();
                    return "=== THAM SO HE THONG DSA ===\n" + dsa.param
                         + "\n\n=== CAP KHOA ===\n" + dsa.keyPair
                         + "\n\n>>> Hoan thanh! Chuyen sang Tab 3 de ky thong diep.";
                }
                protected void done() {
                    try { out.setForeground(C_OK); out.setText(get()); out.setCaretPosition(0); }
                    catch (Exception ex) { out.setForeground(C_ERR); out.setText("Loi: " + ex.getMessage()); }
                }
            }.execute();
        }
    }

    // =================================================================
    //  TAB 3 – TAO CHU KY
    // =================================================================
    static class SignPanel extends JPanel {
        final DSA dsa; final JTextArea out = mkOut();
        final JTextField msgF = mkField(true);
        SignPanel(DSA dsa) {
            this.dsa = dsa;
            setLayout(new BorderLayout(8, 8)); setBackground(C_BG);
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            JPanel top = new JPanel(new BorderLayout(0, 8)); top.setOpaque(false);
            top.add(mkHeader("Tao Chu Ky So",
                "Nhap thong diep va bam Ky de sinh chu ky (r, s) bang khoa bi mat x."), BorderLayout.NORTH);
            JButton b = mkBtn("Ky Thong Diep", new Color(15, 110, 15));
            b.addActionListener(e -> sign());
            JPanel row = new JPanel(new BorderLayout(8, 0)); row.setOpaque(false);
            JPanel lp = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); lp.setOpaque(false);
            lp.add(mkLbl("Thong diep:")); lp.add(Box.createHorizontalStrut(6)); lp.add(b);
            row.add(lp, BorderLayout.WEST); row.add(msgF, BorderLayout.CENTER);
            top.add(row, BorderLayout.CENTER);
            add(top, BorderLayout.NORTH); add(mkScroll(out), BorderLayout.CENTER);
        }
        void sign() {
            if (!dsa.ready()) { warn("Hay tao tham so o Tab 2 truoc!"); return; }
            String msg = msgF.getText().trim();
            if (msg.isEmpty()) { warn("Vui long nhap thong diep."); return; }
            try {
                DSASignature sig = dsa.sign(msg);
                out.setForeground(C_OK);
                out.setText("=== KET QUA KY SO DSA ===\n"
                    + "Thong diep : " + msg + "\n"
                    + "H(m) SHA-1 : " + DSAUtils.hex(DSAUtils.hash(msg)) + "\n\n"
                    + "Chu ky (r, s):\n  " + sig.toString().replace("\n", "\n  ")
                    + "\n\n>>> Ky thanh cong!\n"
                    + ">>> Chuyen sang Tab 4, bam 'Dien tu dong' roi bam 'KIEM TRA CHU KY'.");
                out.setCaretPosition(0);
            } catch (Exception ex) { out.setForeground(C_ERR); out.setText("Loi: " + ex.getMessage()); }
        }
        void warn(String m) { JOptionPane.showMessageDialog(this, m, "Thong bao", JOptionPane.WARNING_MESSAGE); }
    }

    // =================================================================
    //  TAB 4 – KIEM TRA CHU KY (1 nut, hien toan bo ket qua)
    // =================================================================
    static class VerifyPanel extends JPanel {
        final DSA        dsa;
        final JTextArea  out  = mkOut();
        final JTextField msgF = mkField(true);
        final JTextField rF   = mkField(true);
        final JTextField sF   = mkField(true);

        VerifyPanel(DSA dsa) {
            this.dsa = dsa;
            setLayout(new BorderLayout(8, 8)); setBackground(C_BG);
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

            JPanel top = new JPanel(new BorderLayout(0, 8)); top.setOpaque(false);
            top.add(mkHeader("Kiem Tra Chu Ky",
                "Nhap thong diep + chu ky (r, s) roi bam 'KIEM TRA' — hien day du ket qua bên duoi"),
                BorderLayout.NORTH);

            // Grid nhap lieu
            JPanel grid = new JPanel(new GridBagLayout());
            grid.setBackground(new Color(250, 251, 255));
            grid.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_CBOR),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
            GridBagConstraints gc = new GridBagConstraints();
            gc.insets = new Insets(4, 4, 4, 4); gc.anchor = GridBagConstraints.WEST;

            gc.gridx=0; gc.gridy=0; gc.weightx=0; gc.fill=GridBagConstraints.NONE;
            grid.add(mkLbl("Thong diep:"), gc);
            gc.gridx=1; gc.weightx=1; gc.fill=GridBagConstraints.HORIZONTAL;
            grid.add(msgF, gc);

            gc.gridx=0; gc.gridy=1; gc.weightx=0; gc.fill=GridBagConstraints.NONE;
            grid.add(mkLbl("r (hex):"), gc);
            gc.gridx=1; gc.weightx=1; gc.fill=GridBagConstraints.HORIZONTAL;
            grid.add(rF, gc);

            gc.gridx=0; gc.gridy=2; gc.weightx=0; gc.fill=GridBagConstraints.NONE;
            grid.add(mkLbl("s (hex):"), gc);
            gc.gridx=1; gc.weightx=1; gc.fill=GridBagConstraints.HORIZONTAL;
            grid.add(sF, gc);

            // Nut dien tu dong
            JButton bFill = new JButton("Dien tu dong tu Tab 3");
            bFill.setFont(F_UI); bFill.setForeground(C_PRI);
            bFill.setBackground(new Color(230, 238, 255));
            bFill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_CBOR),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)));
            bFill.setFocusPainted(false);
            bFill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            bFill.addActionListener(e -> autoFill());

            // Nut reset
            JButton bReset = new JButton("Reset ve goc");
            bReset.setFont(F_UI); bReset.setForeground(new Color(120, 60, 0));
            bReset.setBackground(new Color(255, 243, 220));
            bReset.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 180, 100)),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)));
            bReset.setFocusPainted(false);
            bReset.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            bReset.addActionListener(e -> resetToOriginal());

            // NUT KIEM TRA DUY NHAT
            JButton bCheck = mkBtn("  KIEM TRA CHU KY  ", C_PRI);
            bCheck.setFont(new Font("Segoe UI", Font.BOLD, 14));
            bCheck.addActionListener(e -> doFullCheck());

            JPanel btnRow1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            btnRow1.setOpaque(false);
            btnRow1.add(bFill); btnRow1.add(bReset);

            JPanel btnRow2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            btnRow2.setOpaque(false);
            btnRow2.add(bCheck);

            JPanel btnArea = new JPanel();
            btnArea.setLayout(new BoxLayout(btnArea, BoxLayout.Y_AXIS));
            btnArea.setOpaque(false);
            btnArea.add(btnRow1);
            btnArea.add(Box.createVerticalStrut(6));
            btnArea.add(btnRow2);

            JPanel mid = new JPanel(new BorderLayout(0, 8)); mid.setOpaque(false);
            mid.add(grid,    BorderLayout.NORTH);
            mid.add(btnArea, BorderLayout.CENTER);
            top.add(mid, BorderLayout.CENTER);

            add(top,           BorderLayout.NORTH);
            add(mkScroll(out), BorderLayout.CENTER);
        }

        void autoFill() {
            if (!dsa.ready())  { warn("Hay tao tham so o Tab 2 truoc!"); return; }
            if (!dsa.hasSig()) { warn("Hay ky thong diep o Tab 3 truoc!"); return; }
            msgF.setText(dsa.lastMsg);
            rF.setText(DSAUtils.hex(dsa.lastSig.r));
            sF.setText(DSAUtils.hex(dsa.lastSig.s));
            out.setForeground(new Color(80, 80, 80));
            out.setText(">>> Da dien du lieu tu Tab 3.\n"
                + "    Thong diep : " + dsa.lastMsg + "\n"
                + "    r          = " + DSAUtils.hex(dsa.lastSig.r) + "\n"
                + "    s          = " + DSAUtils.hex(dsa.lastSig.s) + "\n\n"
                + "Bam 'KIEM TRA CHU KY' de xem ket qua day du.");
            out.setCaretPosition(0);
        }

        void resetToOriginal() {
            if (!dsa.hasSig()) { warn("Chua co chu ky (hay ky o Tab 3 truoc)."); return; }
            msgF.setText(dsa.lastMsg);
            rF.setText(DSAUtils.hex(dsa.lastSig.r));
            sF.setText(DSAUtils.hex(dsa.lastSig.s));
            out.setForeground(new Color(80, 80, 80));
            out.setText(">>> Da reset ve gia tri goc tu Tab 3.");
            out.setCaretPosition(0);
        }

        String     getMsg() { return msgF.getText().trim(); }
        BigInteger getR() {
            try { return new BigInteger(rF.getText().trim().replaceAll("[^0-9a-fA-F]",""), 16); }
            catch (Exception e) { warn("r khong hop le (phai la hex)."); return null; }
        }
        BigInteger getS() {
            try { return new BigInteger(sF.getText().trim().replaceAll("[^0-9a-fA-F]",""), 16); }
            catch (Exception e) { warn("s khong hop le (phai la hex)."); return null; }
        }

        // ================================================================
        //  KIEM TRA TONG HOP – 1 nut, hien ca 4 ket qua
        //
        //  [1] Chu ky co hop le? (verify msg, r, s)
        //  [2] Thong diep co bi vi pham toan ven? (so sanh msg voi lastMsg)
        //  [3] Chu ky co bi sua doi? (so sanh r/s voi lastSig)
        //  [4] Chu ky co bi gia mao? (suy luan tu [1][3])
        // ================================================================
        void doFullCheck() {
            if (!dsa.ready())       { warn("Hay tao tham so o Tab 2 truoc!"); return; }
            if (!dsa.hasSig())      { warn("Hay ky thong diep o Tab 3 truoc!"); return; }
            if (getMsg().isEmpty()) { warn("Thong diep khong duoc de trong."); return; }

            String     msg = getMsg();
            BigInteger r   = getR();
            BigInteger s   = getS();
            if (r == null || s == null) return;

            try {
                DSASignature sig = new DSASignature(r, s);
                VerifyResult res = dsa.verify(msg, sig);

                String line = "=".repeat(54);
                String bar  = "-".repeat(54);
                StringBuilder sb = new StringBuilder();

                sb.append(line).append("\n");
                sb.append("  KET QUA KIEM TRA CHU KY DSA\n");
                sb.append(line).append("\n\n");
                sb.append("Thong diep kiem tra : ").append(msg).append("\n");
                sb.append("H(m) SHA-1          = ").append(DSAUtils.hex(DSAUtils.hash(msg))).append("\n");
                sb.append("r                   = ").append(DSAUtils.hex(r)).append("\n");
                sb.append("s                   = ").append(DSAUtils.hex(s)).append("\n\n");

                // ---- [1] CHU KY HOP LE? ----
                sb.append(bar).append("\n");
                sb.append(" [1] CHU KY CO HOP LE KHONG?\n");
                sb.append(bar).append("\n");
                if (res.valid) {
                    sb.append(" => HOP LE  (v = r)\n");
                    sb.append("    Chu ky xac thuc thanh cong voi khoa cong khai y.\n");
                } else {
                    sb.append(" => KHONG HOP LE  (v != r)\n");
                    sb.append("    Thong diep hoac chu ky bi sai / bi thay doi.\n");
                    if (!res.note.isEmpty())
                        sb.append("    Chi tiet: ").append(res.note).append("\n");
                }
                sb.append(" Gia tri trung gian:\n");
                if (res.w  != null) sb.append("   w  = ").append(DSAUtils.hex(res.w)).append("\n");
                if (res.u1 != null) sb.append("   u1 = ").append(DSAUtils.hex(res.u1)).append("\n");
                if (res.u2 != null) sb.append("   u2 = ").append(DSAUtils.hex(res.u2)).append("\n");
                if (res.v  != null) sb.append("   v  = ").append(DSAUtils.hex(res.v)).append("\n");
                sb.append("\n");

                // ---- [2] TOAN VEN THONG DIEP? ----
                sb.append(bar).append("\n");
                sb.append(" [2] THONG DIEP CO BI VI PHAM TOAN VEN KHONG?\n");
                sb.append(bar).append("\n");
                boolean msgMatch = msg.equals(dsa.lastMsg);
                if (msgMatch) {
                    sb.append(" => KHONG VI PHAM\n");
                    sb.append("    Thong diep khop voi ban goc luc ky: \"").append(dsa.lastMsg).append("\"\n");
                } else {
                    sb.append(" => BI VI PHAM TOAN VEN\n");
                    sb.append("    Thong diep GOC luc ky : \"").append(dsa.lastMsg).append("\"\n");
                    sb.append("    Thong diep dang kiem  : \"").append(msg).append("\"\n");
                    sb.append("    H(goc) = ").append(DSAUtils.hex(DSAUtils.hash(dsa.lastMsg))).append("\n");
                    sb.append("    H(moi) = ").append(DSAUtils.hex(DSAUtils.hash(msg))).append("\n");
                    sb.append("    => Ham bam khac nhau hoan toan!\n");
                }
                sb.append("\n");

                // ---- [3] CHU KY BI SUA? ----
                sb.append(bar).append("\n");
                sb.append(" [3] CHU KY CO BI SUA DOI KHONG?\n");
                sb.append(bar).append("\n");
                boolean rMatch = r.equals(dsa.lastSig.r);
                boolean sMatch = s.equals(dsa.lastSig.s);
                if (rMatch && sMatch) {
                    sb.append(" => CHU KY NGUYEN VEN\n");
                    sb.append("    r va s khop voi chu ky goc.\n");
                } else {
                    sb.append(" => CHU KY BI SUA DOI\n");
                    if (!rMatch) {
                        sb.append("    r GOC = ").append(DSAUtils.hex(dsa.lastSig.r)).append("\n");
                        sb.append("    r MOI = ").append(DSAUtils.hex(r)).append("  <-- DA THAY DOI\n");
                    }
                    if (!sMatch) {
                        sb.append("    s GOC = ").append(DSAUtils.hex(dsa.lastSig.s)).append("\n");
                        sb.append("    s MOI = ").append(DSAUtils.hex(s)).append("  <-- DA THAY DOI\n");
                    }
                }
                sb.append("\n");

                // ---- [4] GIA MAO? ----
                sb.append(bar).append("\n");
                sb.append(" [4] CHU KY CO BI GIA MAO KHONG?\n");
                sb.append(bar).append("\n");
                if (res.valid) {
                    sb.append(" => KHONG GIA MAO\n");
                    sb.append("    Chu ky xac thuc thanh cong, khong phai gia mao.\n");
                } else if (!rMatch || !sMatch) {
                    sb.append(" => CO THE GIA MAO HOAC BI SUA\n");
                    sb.append("    Chu ky KHONG hop le va KHAC chu ky goc.\n");
                    sb.append("    Ke tan cong khong co khoa bi mat x khong the tao\n");
                    sb.append("    chu ky hop le (bai toan Logarit Roi Rac trong Zp*).\n");
                } else {
                    sb.append(" => KHONG GIA MAO (chu ky goc nhung thong diep bi sua)\n");
                }
                sb.append("\n");

                // ---- TONG KET ----
                boolean allOk = res.valid && msgMatch && rMatch && sMatch;
                sb.append(line).append("\n");
                sb.append("  TONG KET: ");
                if (allOk) {
                    sb.append("CHU KY HOP LE HOAN TOAN — An toan!\n");
                } else {
                    sb.append("PHAT HIEN VAN DE!\n");
                    if (!res.valid)          sb.append("  - Chu ky KHONG xac thuc duoc\n");
                    if (!msgMatch)           sb.append("  - Thong diep BI VI PHAM TOAN VEN\n");
                    if (!rMatch || !sMatch)  sb.append("  - Chu ky BI SUA DOI hoac GIA MAO\n");
                }
                sb.append(line);

                out.setForeground(allOk ? C_OK : C_ERR);
                out.setText(sb.toString());
                out.setCaretPosition(0);

            } catch (Exception ex) { errDlg(ex); }
        }

        void warn(String m)      { JOptionPane.showMessageDialog(this, m, "Thong bao", JOptionPane.WARNING_MESSAGE); }
        void errDlg(Exception e) { JOptionPane.showMessageDialog(this, "Loi: " + e.getMessage(), "Loi", JOptionPane.ERROR_MESSAGE); }
    }

    // =================================================================
    //  MAIN
    // =================================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}

            DSA dsa = new DSA();

            JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
            tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
            tabs.setBackground(C_BG);

            tabs.addTab("  Giao Dien  ",    new HomePanel());
            tabs.addTab("  Tao Tham So  ",  new ParamPanel(dsa));
            tabs.addTab("  Tao Chu Ky  ",   new SignPanel(dsa));
            tabs.addTab("  Kiem Tra  ",     new VerifyPanel(dsa));

            tabs.setForegroundAt(0, C_PRI);
            tabs.setForegroundAt(1, new Color(10,  110,  10));
            tabs.setForegroundAt(2, new Color(150,  70,   0));
            tabs.setForegroundAt(3, new Color(160,   0,   0));

            JFrame frame = new JFrame("DSA - Digital Signature Algorithm  |  Nhom 10 - HaUI-SICT-IT6001.2");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(tabs);
            frame.setSize(1000, 700);
            frame.setMinimumSize(new Dimension(820, 560));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
