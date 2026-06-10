package dsa.ui;

import dsa.AppContext;

import javax.swing.*;
import java.awt.*;

/**
 * HomePanel – Màn hình tổng quan, hướng dẫn sử dụng và công thức DSA
 */
public class HomePanel extends JPanel {

    public HomePanel(AppContext ctx) {
        setLayout(new BorderLayout());
        setBackground(UIStyle.C_BG);

        // Banner
        JPanel banner = new JPanel(new GridLayout(3, 1, 0, 4));
        banner.setBackground(UIStyle.C_SIDEBAR_BG);
        banner.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        banner.add(UIStyle.label("DSA – Digital Signature Algorithm",
            new Font("Segoe UI", Font.BOLD, 24), Color.WHITE));
        banner.add(UIStyle.label("Minh hoa Chu Ky So  |  Nhom 10  |  HaUI-SICT-IT6001.2",
            new Font("Segoe UI", Font.PLAIN, 14), new Color(148,163,184)));
        banner.add(UIStyle.label("Chuan FIPS 186  |  Khoa 512-bit  |  Ham bam SHA-1  |  Ho tro nhieu loai file",
            new Font("Segoe UI", Font.ITALIC, 12), new Color(100,116,139)));

        // Nội dung
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(UIStyle.C_BG);
        body.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));

        body.add(sectionTitle("HUONG DAN SU DUNG"));
        body.add(Box.createVerticalStrut(12));
        body.add(stepCard("1", "Tao Tham So",
            "Sinh tham so he thong (p, q, g) va cap khoa (x bi mat, y cong khai). Luu ra file .dsakey de tai lai sau."));
        body.add(Box.createVerticalStrut(8));
        body.add(stepCard("2", "Tao Chu Ky",
            "Chon file hoac nhap van ban, bam Ky. He thong sinh chu ky (r, s) va luu ra file .sig."));
        body.add(Box.createVerticalStrut(8));
        body.add(stepCard("3", "Kiem Tra Chu Ky",
            "Tai file goc + file .sig, bam Kiem Tra. Hien thi day du: hop le, toan ven, gia mao."));
        body.add(Box.createVerticalStrut(8));
        body.add(stepCard("4", "Nhat Ky",
            "Xem lich su hoat dong, thong ke va xuat bao cao dang .txt hoac .csv."));

        body.add(Box.createVerticalStrut(24));
        body.add(sectionTitle("CONG THUC DSA"));
        body.add(Box.createVerticalStrut(12));
        body.add(formulaPanel());

        body.add(Box.createVerticalStrut(24));
        body.add(sectionTitle("DINH DANG FILE HO TRO"));
        body.add(Box.createVerticalStrut(12));
        body.add(fileTypesPanel());

        JScrollPane sc = new JScrollPane(body);
        sc.setBorder(null);
        sc.getViewport().setBackground(UIStyle.C_BG);

        add(banner, BorderLayout.NORTH);
        add(sc, BorderLayout.CENTER);
    }

    private JLabel sectionTitle(String t) {
        JLabel l = UIStyle.label(t, new Font("Segoe UI", Font.BOLD, 12), UIStyle.C_PRI);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JPanel stepCard(String num, String title, String desc) {
        JPanel c = new JPanel(new BorderLayout(14, 0));
        c.setBackground(Color.WHITE);
        c.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIStyle.C_BORDER, 1, true),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        c.setAlignmentX(LEFT_ALIGNMENT);

        JLabel numL = new JLabel(num, SwingConstants.CENTER);
        numL.setFont(new Font("Segoe UI", Font.BOLD, 18));
        numL.setForeground(Color.WHITE);
        numL.setBackground(UIStyle.C_PRI);
        numL.setOpaque(true);
        numL.setPreferredSize(new Dimension(38, 38));

        JPanel txt = new JPanel(new BorderLayout(0, 3));
        txt.setOpaque(false);
        JLabel tl = UIStyle.label(title, UIStyle.F_BOLD, UIStyle.C_PRI);
        JLabel dl = new JLabel("<html>" + desc + "</html>");
        dl.setFont(UIStyle.F_BODY);
        dl.setForeground(UIStyle.C_TEXT_MUTED);
        txt.add(tl, BorderLayout.NORTH);
        txt.add(dl, BorderLayout.CENTER);

        c.add(numL, BorderLayout.WEST);
        c.add(txt, BorderLayout.CENTER);
        return c;
    }

    private JPanel formulaPanel() {
        JPanel p = new JPanel(new GridLayout(1, 2, 12, 0));
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        p.add(formulaBox("KY SO",
            "r = (g^k mod p) mod q\n" +
            "s = k^-1 * (H(m) + x*r) mod q\n\n" +
            "k: so ngau nhien, 0 < k < q\n" +
            "H(m): SHA-1 cua tai lieu"));
        p.add(formulaBox("XAC THUC",
            "w  = s^-1 mod q\n" +
            "u1 = H(m)*w mod q\n" +
            "u2 = r*w mod q\n" +
            "v  = (g^u1 * y^u2 mod p) mod q\n" +
            "Hop le khi: v = r"));
        return p;
    }

    private JPanel formulaBox(String title, String text) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(new Color(239, 246, 255));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(191, 219, 254), 1, true),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        p.add(UIStyle.label(title, UIStyle.F_BOLD, UIStyle.C_PRI), BorderLayout.NORTH);
        JTextArea ta = new JTextArea(text);
        ta.setEditable(false); ta.setOpaque(false);
        ta.setFont(new Font("Consolas", Font.PLAIN, 11));
        ta.setForeground(new Color(30, 58, 138));
        p.add(ta, BorderLayout.CENTER);
        return p;
    }

    private JPanel fileTypesPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);
        String[] types = {"PDF", "DOCX", "XLSX", "PNG", "JPG", "TXT", "CSV", "XML", "JSON", "ZIP"};
        Color[] colors = {
            new Color(220,38,38), new Color(37,99,235), new Color(22,163,74),
            new Color(168,85,247), new Color(234,179,8), new Color(100,116,139),
            new Color(8,145,178), new Color(249,115,22), new Color(16,185,129),
            new Color(156,163,175)
        };
        for (int i = 0; i < types.length; i++) {
            JLabel badge = new JLabel("  " + types[i] + "  ");
            badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
            badge.setForeground(Color.WHITE);
            badge.setBackground(colors[i % colors.length]);
            badge.setOpaque(true);
            badge.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
            p.add(badge);
        }
        return p;
    }
}
