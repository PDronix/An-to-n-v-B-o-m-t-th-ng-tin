package dsa.ui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * UIStyle – Hằng số màu sắc, font, và helper tạo component dùng chung
 */
public class UIStyle {

    // ── Màu sắc ──────────────────────────────────────────────────────────
    public static final Color C_SIDEBAR_BG   = new Color(18,  24,  43);   // Navy đậm
    public static final Color C_SIDEBAR_ITEM = new Color(30,  40,  65);   // Navy nhạt hơn
    public static final Color C_SIDEBAR_SEL  = new Color(59, 130, 246);   // Xanh dương sáng
    public static final Color C_SIDEBAR_TEXT = new Color(148,163,184);    // Xám nhạt
    public static final Color C_SIDEBAR_TSEL = Color.WHITE;

    public static final Color C_BG           = new Color(248,250,252);    // Trắng xám nhẹ
    public static final Color C_CARD         = Color.WHITE;
    public static final Color C_BORDER       = new Color(226,232,240);
    public static final Color C_HEADER_BG    = new Color(239,246,255);    // Xanh rất nhạt

    public static final Color C_PRI          = new Color(37, 99, 235);    // Xanh dương chính
    public static final Color C_PRI_DARK     = new Color(29, 78, 216);
    public static final Color C_SUCCESS      = new Color(22, 163, 74);    // Xanh lá
    public static final Color C_DANGER       = new Color(220, 38, 38);    // Đỏ
    public static final Color C_WARNING      = new Color(217,119, 6);     // Cam
    public static final Color C_INFO         = new Color(8, 145, 178);    // Xanh cyan

    public static final Color C_TEXT         = new Color(15,  23,  42);   // Đen đậm
    public static final Color C_TEXT_MUTED   = new Color(100,116,139);    // Xám

    public static final Color C_OUTPUT_BG    = new Color(15,  23,  42);   // Nền output tối
    public static final Color C_OUTPUT_TEXT  = new Color(134,239,172);    // Xanh lá nhạt (như terminal)

    // ── Font ─────────────────────────────────────────────────────────────
    public static final Font F_TITLE   = new Font("Segoe UI", Font.BOLD,  20);
    public static final Font F_H1      = new Font("Segoe UI", Font.BOLD,  16);
    public static final Font F_H2      = new Font("Segoe UI", Font.BOLD,  14);
    public static final Font F_BODY    = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font F_BOLD    = new Font("Segoe UI", Font.BOLD,  13);
    public static final Font F_SMALL   = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font F_MONO    = new Font("Consolas", Font.PLAIN, 12);
    public static final Font F_MONO_SM = new Font("Consolas", Font.PLAIN, 11);
    public static final Font F_SIDEBAR = new Font("Segoe UI", Font.BOLD,  13);

    // ── Border ───────────────────────────────────────────────────────────
    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER, 1, true),
            BorderFactory.createEmptyBorder(12, 16, 12, 16));
    }
    public static Border sectionBorder(String title) {
        return BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(C_BORDER),
            title, 0, 0, F_BOLD, C_PRI);
    }
    public static Border padding(int v, int h) {
        return BorderFactory.createEmptyBorder(v, h, v, h);
    }

    // ── Button ───────────────────────────────────────────────────────────
    public static JButton primaryBtn(String text) {
        JButton b = new JButton(text);
        b.setBackground(C_PRI); b.setForeground(Color.WHITE);
        b.setFont(F_BOLD); b.setOpaque(true);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(9, 20, 9, 20));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(C_PRI_DARK); }
            public void mouseExited(java.awt.event.MouseEvent e)  { b.setBackground(C_PRI); }
        });
        return b;
    }

    public static JButton successBtn(String text) {
        JButton b = new JButton(text);
        b.setBackground(C_SUCCESS); b.setForeground(Color.WHITE);
        b.setFont(F_BOLD); b.setOpaque(true);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(9, 20, 9, 20));
        return b;
    }

    public static JButton dangerBtn(String text) {
        JButton b = new JButton(text);
        b.setBackground(C_DANGER); b.setForeground(Color.WHITE);
        b.setFont(F_BOLD); b.setOpaque(true);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(9, 20, 9, 20));
        return b;
    }

    public static JButton secondaryBtn(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(241,245,249)); b.setForeground(C_TEXT);
        b.setFont(F_BOLD); b.setOpaque(true);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(8, 18, 8, 18)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ── TextField ────────────────────────────────────────────────────────
    public static JTextField textField() {
        JTextField tf = new JTextField();
        tf.setFont(F_BODY);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        return tf;
    }

    // ── Output area (terminal-style) ─────────────────────────────────────
    public static JTextArea outputArea() {
        JTextArea ta = new JTextArea();
        ta.setEditable(false);
        ta.setFont(F_MONO);
        ta.setBackground(C_OUTPUT_BG);
        ta.setForeground(C_OUTPUT_TEXT);
        ta.setCaretColor(C_OUTPUT_TEXT);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        return ta;
    }

    public static JScrollPane scrollWrap(JTextArea ta) {
        JScrollPane sp = new JScrollPane(ta);
        sp.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85)));
        sp.getVerticalScrollBar().setBackground(C_OUTPUT_BG);
        return sp;
    }

    // ── Label ────────────────────────────────────────────────────────────
    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text); l.setFont(font); l.setForeground(color); return l;
    }
    public static JLabel h1(String text) { return label(text, F_H1, C_TEXT); }
    public static JLabel h2(String text) { return label(text, F_H2, C_TEXT); }
    public static JLabel muted(String text) { return label(text, F_SMALL, C_TEXT_MUTED); }

    // ── Badge (trạng thái) ───────────────────────────────────────────────
    public static JLabel badge(String text, Color bg) {
        JLabel l = new JLabel(" " + text + " ");
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(Color.WHITE);
        l.setBackground(bg); l.setOpaque(true);
        l.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        return l;
    }

    // ── Card panel ───────────────────────────────────────────────────────
    public static JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(C_CARD);
        p.setBorder(cardBorder());
        return p;
    }

    public static JPanel card(LayoutManager layout) {
        JPanel p = card();
        p.setLayout(layout);
        return p;
    }

    // ── Separator ────────────────────────────────────────────────────────
    public static JSeparator separator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(C_BORDER);
        return sep;
    }

    // ── Drop zone panel ──────────────────────────────────────────────────
    public static JPanel dropZone(String hint) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(239, 246, 255));
        p.setBorder(BorderFactory.createDashedBorder(C_PRI, 2f, 6f, 3f, true));
        p.setPreferredSize(new Dimension(0, 100));
        JLabel l = new JLabel(hint, SwingConstants.CENTER);
        l.setFont(F_BODY); l.setForeground(C_PRI);
        p.add(l, BorderLayout.CENTER);
        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return p;
    }
}


