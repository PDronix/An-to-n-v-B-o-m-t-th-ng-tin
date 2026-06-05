import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class DSADemo {

    // =================================================================
    //  CORE DSA ENGINE
    // =================================================================
    static class DSASystem {
        BigInteger p, q, g, x, y;
        BigInteger lastR, lastS;
        String     lastMsg = "";
        private static final SecureRandom RND = new SecureRandom();

        void generateParams() {
            q = BigInteger.probablePrime(160, RND);
            BigInteger k;
            do {
                k = new BigInteger(352, RND).setBit(351);
                p = k.multiply(q).add(BigInteger.ONE);
            } while (!p.isProbablePrime(80) || p.bitLength() < 512);
            BigInteger h = BigInteger.TWO;
            BigInteger exp = p.subtract(BigInteger.ONE).divide(q);
            do { g = h.modPow(exp, p); h = h.add(BigInteger.ONE); }
            while (g.equals(BigInteger.ONE));
            do { x = new BigInteger(q.bitLength(), RND); }
            while (x.compareTo(BigInteger.ONE) < 0 || x.compareTo(q) >= 0);
            y = g.modPow(x, p);
            lastR = null; lastS = null; lastMsg = "";
        }

        BigInteger hash(String msg) throws Exception {
            return new BigInteger(1,
                    MessageDigest.getInstance("SHA-1")
                            .digest(msg.getBytes(StandardCharsets.UTF_8)));
        }

        void sign(String msg) throws Exception {
            BigInteger Hm = hash(msg);
            BigInteger r, s, k;
            do {
                do { k = new BigInteger(q.bitLength(), RND); }
                while (k.compareTo(BigInteger.ONE) < 0 || k.compareTo(q) >= 0);
                r = g.modPow(k, p).mod(q);
                s = k.modInverse(q).multiply(Hm.add(x.multiply(r))).mod(q);
            } while (r.equals(BigInteger.ZERO) || s.equals(BigInteger.ZERO));
            lastR = r; lastS = s; lastMsg = msg;
        }

        VerifyResult verify(String msg, BigInteger r, BigInteger s) throws Exception {
            VerifyResult res = new VerifyResult();
            if (r.compareTo(BigInteger.ZERO) <= 0 || r.compareTo(q) >= 0
                    || s.compareTo(BigInteger.ZERO) <= 0 || s.compareTo(q) >= 0) {
                res.valid = false;
                res.note  = "r hoac s nam ngoai khoang hop le (0, q).";
                return res;
            }
            BigInteger Hm = hash(msg);
            res.w  = s.modInverse(q);
            res.u1 = Hm.multiply(res.w).mod(q);
            res.u2 = r.multiply(res.w).mod(q);
            res.v  = g.modPow(res.u1, p).multiply(y.modPow(res.u2, p)).mod(p).mod(q);
            res.valid = res.v.equals(r);
            if (!res.valid && res.note.isEmpty())
                res.note = "v != r => chu ky KHONG khop voi thong diep / khoa cong khai.";
            return res;
        }

        boolean ready()  { return p != null; }
        boolean hasSig() { return lastR != null; }
    }

    static class VerifyResult {
        boolean valid; String note = "";
        BigInteger w, u1, u2, v;
        String detail() {
            StringBuilder sb = new StringBuilder();
            if (w  != null) sb.append("  w  = ").append(hex(w)).append("\n");
            if (u1 != null) sb.append("  u1 = ").append(hex(u1)).append("\n");
            if (u2 != null) sb.append("  u2 = ").append(hex(u2)).append("\n");
            if (v  != null) sb.append("  v  = ").append(hex(v)).append("\n");
            return sb.toString();
        }
    }

    static String hex(BigInteger n) { return n.toString(16).toUpperCase(); }

    // =================================================================
    //  STYLE CONSTANTS
    // =================================================================
    static final Color C_BG   = new Color(245, 247, 252);
    static final Color C_PRI  = new Color(25,  90,  170);
    static final Color C_OK   = new Color(0,   130,  55);
    static final Color C_ERR  = new Color(185,  20,  20);
    static final Color C_WARN = new Color(160,  75,   0);
    static final Color C_PURP = new Color(95,    0,  155);
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
                    "Nhap thong diep, bam Ky de sinh chu ky (r, s). Ket qua tu dong hien vao Tab 4."));
            body.add(Box.createVerticalStrut(8));
            body.add(card(3, "Tab 4 - Kiem Tra Chu Ky",
                    "Bam 'Dien tu dong' roi chon 1 trong 4 kich ban kiem tra."));
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
        final DSASystem dsa; final JTextArea out = mkOut();
        ParamPanel(DSASystem dsa) {
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
                    dsa.generateParams();
                    return "=== THAM SO HE THONG DSA ===\n"
                            + "p = " + hex(dsa.p) + "\n"
                            + "q = " + hex(dsa.q) + "\n"
                            + "g = " + hex(dsa.g) + "\n\n"
                            + "=== CAP KHOA ===\n"
                            + "x (bi mat)    = " + hex(dsa.x) + "\n"
                            + "y (cong khai) = " + hex(dsa.y) + "\n\n"
                            + ">>> Hoan thanh! Chuyen sang Tab 3 de ky thong diep.";
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
        final DSASystem dsa; final JTextArea out = mkOut();
        final JTextField msgF = mkField(true);
        SignPanel(DSASystem dsa) {
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
                dsa.sign(msg);
                out.setForeground(C_OK);
                out.setText(
                        "=== KET QUA KY SO DSA ===\n"
                                + "Thong diep : " + msg + "\n"
                                + "H(m) SHA-1 : " + hex(dsa.hash(msg)) + "\n\n"
                                + "Chu ky (r, s):\n"
                                + "  r = " + hex(dsa.lastR) + "\n"
                                + "  s = " + hex(dsa.lastS) + "\n\n"
                                + ">>> Ky thanh cong!\n"
                                + ">>> Chuyen sang Tab 4, bam 'Dien tu dong' roi kiem tra.");
                out.setCaretPosition(0);
            } catch (Exception ex) { out.setForeground(C_ERR); out.setText("Loi: " + ex.getMessage()); }
        }
        void warn(String m) { JOptionPane.showMessageDialog(this, m, "Thong bao", JOptionPane.WARNING_MESSAGE); }
    }

    // =================================================================
    //  TAB 4 – KIEM TRA CHU KY (4 kich ban)
    //
    //  Nguyen tac:
    //  - 3 o nhap (msg, r, s) nguoi dung co the sua tay
    //  - "Dien tu dong" copy ket qua tu Tab 3 vao 3 o nay
    //  - 4 nut kiem tra DEU doc tu 3 o nay, chi bien doi 1 yeu to:
    //    [1] verify(msg, r, s)             -- khong thay doi gi
    //    [2] verify(msg+"[SUA DOI]", r, s) -- thay thong diep
    //    [3] verify(msg, r+1 mod q, s)     -- thay r
    //    [4] verify(msg, randR, randS)     -- r/s ngau nhien (KHONG ghi de o nhap)
    // =================================================================
    static class VerifyPanel extends JPanel {
        final DSASystem dsa;
        final JTextArea  out  = mkOut();
        final JTextField msgF = mkField(true);
        final JTextField rF   = mkField(true);   // cho nhap tay
        final JTextField sF   = mkField(true);   // cho nhap tay

        VerifyPanel(DSASystem dsa) {
            this.dsa = dsa;
            setLayout(new BorderLayout(8, 8)); setBackground(C_BG);
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

            JPanel top = new JPanel(new BorderLayout(0, 8)); top.setOpaque(false);
            top.add(mkHeader("Kiem Tra Chu Ky - 4 Kich Ban",
                            "[1] Hop le  [2] Vi pham toan ven  [3] Chu ky sai  [4] Gia mao  |  Mau XANH = dung kich ban, DO = sai kich ban"),
                    BorderLayout.NORTH);

            // Grid nhap lieu
            JPanel grid = new JPanel(new GridBagLayout());
            grid.setBackground(new Color(250, 251, 255));
            grid.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_CBOR),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)));
            GridBagConstraints gc = new GridBagConstraints();
            gc.insets = new Insets(4, 4, 4, 4); gc.anchor = GridBagConstraints.WEST;

            gc.gridx = 0; gc.gridy = 0; gc.weightx = 0; gc.fill = GridBagConstraints.NONE;
            grid.add(mkLbl("Thong diep:"), gc);
            gc.gridx = 1; gc.weightx = 1; gc.fill = GridBagConstraints.HORIZONTAL;
            grid.add(msgF, gc);

            gc.gridx = 0; gc.gridy = 1; gc.weightx = 0; gc.fill = GridBagConstraints.NONE;
            grid.add(mkLbl("r (hex):"), gc);
            gc.gridx = 1; gc.weightx = 1; gc.fill = GridBagConstraints.HORIZONTAL;
            grid.add(rF, gc);

            gc.gridx = 0; gc.gridy = 2; gc.weightx = 0; gc.fill = GridBagConstraints.NONE;
            grid.add(mkLbl("s (hex):"), gc);
            gc.gridx = 1; gc.weightx = 1; gc.fill = GridBagConstraints.HORIZONTAL;
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

            // Nut reset ve gia tri goc
            JButton bReset = new JButton("Reset ve goc");
            bReset.setFont(F_UI); bReset.setForeground(new Color(120, 60, 0));
            bReset.setBackground(new Color(255, 243, 220));
            bReset.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 180, 100)),
                    BorderFactory.createEmptyBorder(5, 12, 5, 12)));
            bReset.setFocusPainted(false);
            bReset.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            bReset.addActionListener(e -> resetToOriginal());

            // 4 nut kich ban
            JButton b1 = mkBtn("[1]  Chu Ky Hop Le",       C_OK);
            JButton b2 = mkBtn("[2]  Vi Pham Toan Ven",     C_WARN);
            JButton b3 = mkBtn("[3]  Chu Ky Khong Hop Le",  C_ERR);
            JButton b4 = mkBtn("[4]  Chu Ky Gia Mao",       C_PURP);

            b1.setToolTipText("Ky vong: HOP LE  — mau xanh neu dung, do neu sai");
            b2.setToolTipText("Ky vong: KHONG HOP LE — mau xanh neu dung phat hien, do neu bi qua");
            b3.setToolTipText("Ky vong: KHONG HOP LE — mau xanh neu dung phat hien, do neu bi qua");
            b4.setToolTipText("Ky vong: KHONG HOP LE — mau xanh neu dung phat hien, do neu bi qua");

            b1.addActionListener(e -> sc1_valid());
            b2.addActionListener(e -> sc2_tampered());
            b3.addActionListener(e -> sc3_badSig());
            b4.addActionListener(e -> sc4_forged());

            JPanel btnRow1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            btnRow1.setOpaque(false);
            btnRow1.add(bFill); btnRow1.add(bReset);

            JPanel btnRow2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            btnRow2.setOpaque(false);
            btnRow2.add(b1); btnRow2.add(b2); btnRow2.add(b3); btnRow2.add(b4);

            JPanel btnArea = new JPanel();
            btnArea.setLayout(new BoxLayout(btnArea, BoxLayout.Y_AXIS));
            btnArea.setOpaque(false);
            btnArea.add(btnRow1);
            btnArea.add(Box.createVerticalStrut(4));
            btnArea.add(btnRow2);

            JPanel mid = new JPanel(new BorderLayout(0, 8)); mid.setOpaque(false);
            mid.add(grid,    BorderLayout.NORTH);
            mid.add(btnArea, BorderLayout.CENTER);
            top.add(mid, BorderLayout.CENTER);

            add(top,           BorderLayout.NORTH);
            add(mkScroll(out), BorderLayout.CENTER);
        }

        // ---- Dien tu dong tu Tab 3 ----
        void autoFill() {
            if (!dsa.ready())  { warn("Hay tao tham so o Tab 2 truoc!"); return; }
            if (!dsa.hasSig()) { warn("Hay ky thong diep o Tab 3 truoc!"); return; }
            msgF.setText(dsa.lastMsg);
            rF.setText(hex(dsa.lastR));
            sF.setText(hex(dsa.lastS));
            out.setForeground(new Color(80, 80, 80));
            out.setText(">>> Da dien du lieu tu Tab 3.\n"
                    + "    Thong diep : " + dsa.lastMsg + "\n"
                    + "    r          = " + hex(dsa.lastR) + "\n"
                    + "    s          = " + hex(dsa.lastS) + "\n\n"
                    + "Chon mot trong 4 nut kich ban de kiem tra.\n\n"
                    + "Luu y:\n"
                    + "  - Mau XANH = ket qua dung voi ky vong cua kich ban\n"
                    + "  - Mau DO   = ket qua nguoc ky vong (co the du lieu o nhap bi sai)");
            out.setCaretPosition(0);
        }

        // ---- Reset r/s/msg ve gia tri goc tu Tab 3 ----
        void resetToOriginal() {
            if (!dsa.hasSig()) { warn("Chua co chu ky nao (hay ky o Tab 3 truoc)."); return; }
            msgF.setText(dsa.lastMsg);
            rF.setText(hex(dsa.lastR));
            sF.setText(hex(dsa.lastS));
            out.setForeground(new Color(80, 80, 80));
            out.setText(">>> Da reset ve gia tri goc tu Tab 3.\n"
                    + "    Thong diep : " + dsa.lastMsg + "\n"
                    + "    r          = " + hex(dsa.lastR) + "\n"
                    + "    s          = " + hex(dsa.lastS));
            out.setCaretPosition(0);
        }

        // ---- Doc du lieu tu 3 o nhap ----
        String getMsg() { return msgF.getText().trim(); }

        BigInteger getR() {
            try {
                return new BigInteger(rF.getText().trim().replaceAll("[^0-9a-fA-F]", ""), 16);
            } catch (Exception e) {
                warn("Gia tri r khong hop le (phai la so hex)."); return null;
            }
        }

        BigInteger getS() {
            try {
                return new BigInteger(sF.getText().trim().replaceAll("[^0-9a-fA-F]", ""), 16);
            } catch (Exception e) {
                warn("Gia tri s khong hop le (phai la so hex)."); return null;
            }
        }

        boolean checkInput() {
            if (!dsa.ready())       { warn("Hay tao tham so o Tab 2 truoc!"); return false; }
            if (getMsg().isEmpty()) { warn("Thong diep khong duoc de trong."); return false; }
            return true;
        }

        /**
         * Hien thi ket qua voi mau sac dua theo ky vong:
         *   expectedValid = ky vong la HOP LE (kich ban [1])
         *   expectedValid = false => ky vong KHONG HOP LE (kich ban [2][3][4])
         *
         * Mau XANH: ket qua == ky vong (dung)
         * Mau DO  : ket qua != ky vong (sai - co the du lieu nhap bi thay doi)
         */
        void show(String title, String body, boolean actualValid, boolean expectedValid) {
            String line = "=".repeat(50);
            String bar  = "-".repeat(50);
            boolean correct = (actualValid == expectedValid);

            String icon;
            if (actualValid) {
                icon = ">>> KET QUA: HOP LE  (v = r)";
            } else {
                icon = ">>> KET QUA: KHONG HOP LE  (v != r)";
            }
            if (correct) {
                icon += "  [DUNG KY VONG ✓]";
            } else {
                icon += "  [NGUOC KY VONG - kiem tra lai du lieu nhap!]";
            }

            // Xanh neu ket qua dung ky vong, do neu nguoc ky vong
            out.setForeground(correct ? C_OK : C_ERR);
            out.setText(line + "\n  " + title + "\n" + line + "\n"
                    + body + "\n" + bar + "\n" + icon + "\n" + bar);
            out.setCaretPosition(0);
        }

        // ================================================================
        //  [1] CHU KY HOP LE — ky vong: HOP LE
        // ================================================================
        void sc1_valid() {
            if (!checkInput()) return;
            String msg = getMsg();
            BigInteger r = getR(), s = getS();
            if (r == null || s == null) return;
            try {
                VerifyResult res = dsa.verify(msg, r, s);
                String body =
                        "Thong diep    : " + msg + "\n"
                                + "H(m) SHA-1   = " + hex(dsa.hash(msg)) + "\n\n"
                                + "Chu ky dung vao:\n"
                                + "  r = " + hex(r) + "\n"
                                + "  s = " + hex(s) + "\n\n"
                                + "Gia tri trung gian:\n" + res.detail()
                                + (res.note.isEmpty() ? "" : "\nGhi chu: " + res.note + "\n");
                // [1] ky vong HOP LE => expectedValid = true
                show("[1] KIEM TRA CHU KY HOP LE", body, res.valid, true);
            } catch (Exception ex) { errDlg(ex); }
        }

        // ================================================================
        //  [2] VI PHAM TOAN VEN — ky vong: KHONG HOP LE
        // ================================================================
        void sc2_tampered() {
            if (!checkInput()) return;
            String orig = getMsg();
            BigInteger r = getR(), s = getS();
            if (r == null || s == null) return;
            String sua = orig + " [SUA DOI]";
            try {
                VerifyResult res = dsa.verify(sua, r, s);
                String hOrig = hex(dsa.hash(orig)), hSua = hex(dsa.hash(sua));
                String body =
                        "Thong diep GOC   : " + orig + "\n"
                                + "Thong diep BI SUA: " + sua  + "\n\n"
                                + "H(goc) = " + hOrig + "\n"
                                + "H(sua) = " + hSua  + "\n"
                                + "=> Ham bam thay doi hoan toan khi them vai ky tu!\n\n"
                                + "Chu ky KHONG thay doi:\n"
                                + "  r = " + hex(r) + "\n"
                                + "  s = " + hex(s) + "\n\n"
                                + "Gia tri trung gian (tinh voi thong diep bi sua):\n"
                                + res.detail()
                                + (res.note.isEmpty() ? "" : "\nGhi chu: " + res.note + "\n");
                // [2] ky vong KHONG HOP LE => expectedValid = false
                show("[2] KIEM TRA VI PHAM TOAN VEN", body, res.valid, false);
            } catch (Exception ex) { errDlg(ex); }
        }

        // ================================================================
        //  [3] CHU KY KHONG HOP LE — ky vong: KHONG HOP LE
        // ================================================================
        void sc3_badSig() {
            if (!checkInput()) return;
            String msg = getMsg();
            BigInteger rOrig = getR(), s = getS();
            if (rOrig == null || s == null) return;
            BigInteger rBad = rOrig.add(BigInteger.ONE).mod(dsa.q);
            try {
                VerifyResult res = dsa.verify(msg, rBad, s);
                String body =
                        "Thong diep giu nguyen : " + msg + "\n\n"
                                + "r GOC         = " + hex(rOrig) + "\n"
                                + "r BI SUA (+1) = " + hex(rBad)  + "\n"
                                + "s giu nguyen  = " + hex(s)      + "\n\n"
                                + "=> Chi thay doi r di 1 don vi, toan bo xac thuc that bai.\n\n"
                                + "Gia tri trung gian:\n" + res.detail()
                                + (res.note.isEmpty() ? "" : "\nGhi chu: " + res.note + "\n");
                // [3] ky vong KHONG HOP LE => expectedValid = false
                show("[3] KIEM TRA CHU KY KHONG HOP LE", body, res.valid, false);
            } catch (Exception ex) { errDlg(ex); }
        }

        // ================================================================
        //  [4] CHU KY GIA MAO — ky vong: KHONG HOP LE
        // ================================================================
        void sc4_forged() {
            if (!checkInput()) return;
            String msg = getMsg();
            SecureRandom rnd = new SecureRandom();
            BigInteger fR, fS;
            do { fR = new BigInteger(dsa.q.bitLength(), rnd); }
            while (fR.compareTo(BigInteger.ONE) < 0 || fR.compareTo(dsa.q) >= 0);
            do { fS = new BigInteger(dsa.q.bitLength(), rnd); }
            while (fS.compareTo(BigInteger.ONE) < 0 || fS.compareTo(dsa.q) >= 0);
            try {
                VerifyResult res = dsa.verify(msg, fR, fS);
                String body =
                        "Thong diep giu nguyen : " + msg + "\n\n"
                                + "Ke tan cong KHONG biet khoa bi mat x.\n"
                                + "Ho tu chon (r, s) ngau nhien:\n"
                                + "  r gia mao = " + hex(fR) + "\n"
                                + "  s gia mao = " + hex(fS) + "\n\n"
                                + "Phan tich bao mat:\n"
                                + "  De gia mao hop le can giai: y = g^x mod p\n"
                                + "  Day la bai toan Logarit Roi Rac trong Zp*\n"
                                + "  Khong co thuat toan da thuc nao giai duoc\n"
                                + "  khi p du lon (512-bit trong demo nay).\n\n"
                                + "Gia tri trung gian:\n" + res.detail()
                                + (res.note.isEmpty() ? "" : "\nGhi chu: " + res.note + "\n");
                // [4] ky vong KHONG HOP LE => expectedValid = false
                show("[4] KIEM TRA CHU KY GIA MAO", body, res.valid, false);
            } catch (Exception ex) { errDlg(ex); }
        }

        void warn(String m)     { JOptionPane.showMessageDialog(this, m, "Thong bao", JOptionPane.WARNING_MESSAGE); }
        void errDlg(Exception e){ JOptionPane.showMessageDialog(this, "Loi: " + e.getMessage(), "Loi", JOptionPane.ERROR_MESSAGE); }
    }

    // =================================================================
    //  MAIN
    // =================================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}

            DSASystem dsa = new DSASystem();

            JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
            tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
            tabs.setBackground(C_BG);

            tabs.addTab("  Giao Dien  ",     new HomePanel());
            tabs.addTab("  Tao Tham So  ",   new ParamPanel(dsa));
            tabs.addTab("  Tao Chu Ky  ",    new SignPanel(dsa));
            tabs.addTab("  Kiem Tra  ",      new VerifyPanel(dsa));

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
