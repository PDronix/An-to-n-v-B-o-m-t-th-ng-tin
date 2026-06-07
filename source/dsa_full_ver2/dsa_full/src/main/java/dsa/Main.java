package dsa;

import dsa.ui.MainFrame;

import javax.swing.*;

/**
 * Main – Điểm vào ứng dụng DSA Digital Signature
 *
 * Biên dịch: mvn package
 * Chạy     : java -jar target/dsa_full.jar
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            AppContext ctx = new AppContext();
            MainFrame  frame = new MainFrame(ctx);
            frame.setVisible(true);
        });
    }
}
