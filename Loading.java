package guiClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.rmi.RemoteException;
import javax.imageio.ImageIO;

public class Loading extends JDialog {
    private JPanel contentPaneLoading;
    private JProgressBar progressBar;
    private JLabel backgroundLabel;
    private JLabel percentageLabel;
    private JLabel titleLabel;

    public Loading() {
        setUndecorated(true);
        setModal(true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setSize(800, 492);
        setLocationRelativeTo(null);

        contentPaneLoading = new JPanel(new BorderLayout());
        setContentPane(contentPaneLoading);

        // Load và scale ảnh vừa khung
        try {
            BufferedImage originalImage = ImageIO.read(getClass().getResource("/Anh_HeThong/banner_1.jpg"));
            Image scaledImage = originalImage.getScaledInstance(getWidth(), getHeight(), Image.SCALE_SMOOTH);
            backgroundLabel = new JLabel(new ImageIcon(scaledImage));
            backgroundLabel.setLayout(new GridBagLayout()); // Sử dụng GridBagLayout để dễ dàng căn giữa
            contentPaneLoading.add(backgroundLabel, BorderLayout.CENTER);
        } catch (IOException | NullPointerException e) {
            System.err.println("Không tìm thấy ảnh nền: " + e.getMessage());
            contentPaneLoading.setBackground(new Color(240, 248, 255));
        }

        // Dòng chữ "Ga Lạc Hồng" nổi bật hơn
        titleLabel = new JLabel("Ga Lạc Hồng", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setStroke(new BasicStroke(3));
                g2d.setColor(new Color(0, 0, 0, 150));
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

                // Sử dụng phiên bản createGlyphVector nhận String
                g2d.drawGlyphVector(getFont().createGlyphVector(fm.getFontRenderContext(), getText()), x, y);

                g2d.setColor(Color.WHITE);
                g2d.drawString(getText(), x, y);
            }
        };
        titleLabel.setFont(new Font("Serif", Font.BOLD, 60)); // Font lớn hơn
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0.7; // Đẩy chữ lên một chút
        gbc.anchor = GridBagConstraints.CENTER;
        if (backgroundLabel != null) {
            backgroundLabel.add(titleLabel, gbc);
        }

        // Panel chứa phần trăm và progress bar
        JPanel progressContainer = new JPanel();
        progressContainer.setLayout(new BoxLayout(progressContainer, BoxLayout.Y_AXIS));
        progressContainer.setOpaque(false);
        progressContainer.setBorder(BorderFactory.createEmptyBorder(10, 40, 20, 40));

        percentageLabel = new JLabel("0%", SwingConstants.CENTER);
        percentageLabel.setFont(new Font("Arial", Font.BOLD, 16));
        percentageLabel.setForeground(Color.WHITE);
        percentageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        progressBar = new JProgressBar(0, 100);
        progressBar.setForeground(new Color(65, 105, 225));
        progressBar.setBackground(new Color(230, 230, 230));
        progressBar.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        progressBar.setPreferredSize(new Dimension(700, 6));
        progressBar.setMaximumSize(new Dimension(700, 6));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressBar.setStringPainted(false);

        progressContainer.add(percentageLabel);
        progressContainer.add(Box.createRigidArea(new Dimension(0, 8)));
        progressContainer.add(progressBar);

        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 0.3; // Đặt progress bar ở phía dưới
        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 20, 0); // Thêm khoảng cách phía dưới
        if (backgroundLabel != null) {
            backgroundLabel.add(progressContainer, gbc);
        } else {
            contentPaneLoading.add(progressContainer, BorderLayout.SOUTH);
        }

        startLoading();
    }

    private void startLoading() {
        SwingWorker<Void, Integer> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                for (int i = 0; i <= 100; i++) {
                    Thread.sleep(30);
                    publish(i);
                }
                return null;
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
                int value = chunks.get(chunks.size() - 1);
                progressBar.setValue(value);
                percentageLabel.setText(value + "%");
            }

            @Override
            protected void done() {
                dispose();
                FrmDangNhap dn = null;

                try {
                    dn = new FrmDangNhap();
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }

                dn.setVisible(true);
            }
        };
        worker.execute();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Loading dialog = new Loading();
            dialog.setVisible(true);
        });
    }


}