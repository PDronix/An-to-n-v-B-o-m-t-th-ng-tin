package dsa.ui;

import dsa.AppContext;

import javax.swing.*;
import java.awt.*;

/**
 * MainFrame – Cửa sổ chính với sidebar navigation bên trái
 */
public class MainFrame extends JFrame {

    private final AppContext ctx;
    private final JPanel     contentArea;
    private final JPanel[]   menuItems;
    private final JPanel[]   panels;
    private int              selected = 0;

    private static final String[] MENU_LABELS = {
        "Tong Quan",
        "Tao Tham So",
        "Tao Chu Ky",
        "Kiem Tra Chu Ky",
        "Nhat Ky"
    };

    private static final String[] MENU_ICONS = {
        "  [*]  ",
        "  [P]  ",
        "  [S]  ",
        "  [V]  ",
        "  [L]  "
    };

    public MainFrame(AppContext ctx) {
        this.ctx = ctx;
        setTitle("DSA – Digital Signature Algorithm  |  Nhom 10 – HaUI-SICT-IT6001.2");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        // Khởi tạo các panel
        SignPanel signPanel = new SignPanel(ctx);
        panels = new JPanel[]{
            new HomePanel(ctx),
            new ParamPanel(ctx),
            signPanel,
            new VerifyPanel(ctx),
            new LogPanel(ctx)
        };
        // Wire callback: bam nut trong SignPanel se chuyen sang tab Tao Tham So (index 1)
        signPanel.setGoToParamPanel(() -> selectMenu(1));
        // Wire tuong tu cho VerifyPanel
        ((VerifyPanel) panels[3]).setGoToParamPanel(() -> selectMenu(1));

        // Content area
        contentArea = new JPanel(new CardLayout());
        contentArea.setBackground(UIStyle.C_BG);
        for (int i = 0; i < panels.length; i++) {
            contentArea.add(panels[i], String.valueOf(i));
        }

        // Sidebar
        menuItems = new JPanel[MENU_LABELS.length];
        JPanel sidebar = buildSidebar();

        // Layout chính
        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, contentArea);
        main.setDividerLocation(200);
        main.setDividerSize(0);
        main.setBorder(null);

        setContentPane(main);
        selectMenu(0);

        // Canh bao khi dong cua so neu chua luu khoa
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (ctx.hasKeys() && !ctx.keySaved) {
                    Object[] opts = {"Luu Khoa roi Thoat", "Thoat Khong Luu", "Huy"};
                    int choice = JOptionPane.showOptionDialog(
                        MainFrame.this,
                        "Ban chua luu khoa!\n\n"
                        + "Neu thoat, khoa se mat vinh vien.\n"
                        + "File .sig da tao se KHONG xac thuc duoc sau nay.",
                        "Canh Bao Chua Luu Khoa",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null, opts, opts[0]);
                    if (choice == 0) {
                        // Chuyen sang tab Tao Tham So de luu
                        selectMenu(1);
                        return;
                    }
                    if (choice == 2 || choice < 0) return;  // Huy
                }
                dispose();
                System.exit(0);
            }
        });
    }

    // ── Sidebar ───────────────────────────────────────────────────────────

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UIStyle.C_SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(200, 0));

        // Logo / tiêu đề
        JPanel logo = new JPanel(new BorderLayout());
        logo.setBackground(new Color(11, 16, 30));
        logo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        logo.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JLabel title = new JLabel("DSA DEMO");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("Nhom 10 – HaUI");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        sub.setForeground(new Color(100, 116, 139));

        logo.add(title, BorderLayout.NORTH);
        logo.add(sub,   BorderLayout.CENTER);
        sidebar.add(logo);

        // Separator
        JSeparator sep1 = new JSeparator();
        sep1.setForeground(new Color(30, 41, 59));
        sep1.setBackground(new Color(30, 41, 59));
        sep1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        sidebar.add(sep1);
        sidebar.add(Box.createVerticalStrut(8));

        // Menu label
        JLabel menuLbl = new JLabel("MENU");
        menuLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        menuLbl.setForeground(new Color(71, 85, 105));
        menuLbl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        sidebar.add(menuLbl);
        sidebar.add(Box.createVerticalStrut(4));

        // Menu items
        for (int i = 0; i < MENU_LABELS.length; i++) {
            final int idx = i;
            JPanel item = buildMenuItem(i);
            item.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) { selectMenu(idx); }
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    if (idx != selected) item.setBackground(UIStyle.C_SIDEBAR_ITEM);
                }
                public void mouseExited(java.awt.event.MouseEvent e) {
                    if (idx != selected) item.setBackground(UIStyle.C_SIDEBAR_BG);
                }
            });
            menuItems[i] = item;
            sidebar.add(item);
        }

        sidebar.add(Box.createVerticalGlue());

        // Trạng thái hệ thống ở dưới
        JSeparator sep2 = new JSeparator();
        sep2.setForeground(new Color(30, 41, 59));
        sep2.setBackground(new Color(30, 41, 59));
        sep2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        sidebar.add(sep2);
        sidebar.add(buildStatusPanel());

        return sidebar;
    }

    private JPanel buildMenuItem(int idx) {
        JPanel item = new JPanel(new BorderLayout());
        item.setBackground(UIStyle.C_SIDEBAR_BG);
        item.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel icon = new JLabel(MENU_ICONS[idx]);
        icon.setFont(new Font("Monospaced", Font.PLAIN, 11));
        icon.setForeground(UIStyle.C_SIDEBAR_TEXT);

        JLabel lbl = new JLabel(MENU_LABELS[idx]);
        lbl.setFont(UIStyle.F_SIDEBAR);
        lbl.setForeground(UIStyle.C_SIDEBAR_TEXT);

        item.add(icon, BorderLayout.WEST);
        item.add(lbl,  BorderLayout.CENTER);
        return item;
    }

    private JPanel buildStatusPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(UIStyle.C_SIDEBAR_BG);
        p.setBorder(BorderFactory.createEmptyBorder(10, 18, 14, 18));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JLabel title = new JLabel("TRANG THAI");
        title.setFont(new Font("Segoe UI", Font.BOLD, 10));
        title.setForeground(new Color(71, 85, 105));
//        title.setAlignmentX(LEFT_ALIGNMENT);
        p.add(title);
        p.add(Box.createVerticalStrut(6));

        JTextArea sta = new JTextArea();
        sta.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        sta.setForeground(new Color(148, 163, 184));
        sta.setBackground(UIStyle.C_SIDEBAR_BG);
        sta.setEditable(false);
//        sta.setAlignmentX(LEFT_ALIGNMENT);

        new Timer(1000, e -> {
            sta.setText(
                (ctx.hasParams()    ? "* Tham so: OK\n" : "* Tham so: --\n") +
                (ctx.hasKeys()      ? "* Khoa: OK\n"    : "* Khoa: --\n") +
                (ctx.hasSignature() ? "* Chu ky: OK"    : "* Chu ky: --")
            );
        }).start();

        p.add(sta);
        return p;
    }

    private void selectMenu(int idx) {
        // Bỏ highlight cũ
        if (menuItems[selected] != null) {
            menuItems[selected].setBackground(UIStyle.C_SIDEBAR_BG);
            Component[] comps = menuItems[selected].getComponents();
            for (Component c : comps) {
                if (c instanceof JLabel) ((JLabel) c).setForeground(UIStyle.C_SIDEBAR_TEXT);
            }
        }
        selected = idx;
        // Highlight mới
        menuItems[selected].setBackground(UIStyle.C_SIDEBAR_SEL);
        Component[] comps = menuItems[selected].getComponents();
        for (Component c : comps) {
            if (c instanceof JLabel) ((JLabel) c).setForeground(Color.WHITE);
        }
        // Chuyển panel
        CardLayout cl = (CardLayout) contentArea.getLayout();
        if(idx == 3){
            ((VerifyPanel) panels[3]).autoFillFromContext();
        }
        cl.show(contentArea, String.valueOf(idx));
    }
}

