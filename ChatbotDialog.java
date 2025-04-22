package guiClient;

import guiClient.AIAssistantPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Dialog nhỏ hiển thị trợ lý ảo ở góc màn hình với giao diện được cải thiện
 */
public class ChatbotDialog extends JDialog {

    private AIAssistantPanel chatbotPanel;
    private boolean isExpanded = true;

    public ChatbotDialog(Frame owner) {
        super(owner, "Trợ lý ảo", false);

        // Thiết lập dialog
        setUndecorated(true); // Loại bỏ khung mặc định
        setSize(350, 500);
        setLocation(owner.getWidth() - 370, owner.getHeight() - 530);
        setLayout(new BorderLayout());
        setAlwaysOnTop(true);

        // Panel tiêu đề với gradient và nút thu nhỏ và nút đóng
        JPanel titlePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                // Gradient từ màu xanh đậm đến xanh nhạt
                GradientPaint gradient = new GradientPaint(0, 0, new Color(41, 128, 185), getWidth(), 0, new Color(109, 213, 250));
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        titlePanel.setLayout(new BorderLayout());
        titlePanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));

        // Thêm chữ "Trợ lý ảo" trên tiêu đề và căn lề trái
        JLabel titleLabel = new JLabel("Trợ lý ảo");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titlePanel.add(titleLabel, BorderLayout.WEST);

        // Tạo panel chứa nút thu nhỏ và nút đóng
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setOpaque(false);

        // Nút thu nhỏ
        JButton minimizeButton = new JButton("−");
        minimizeButton.setForeground(Color.WHITE);
        minimizeButton.setBackground(new Color(41, 128, 185));
        minimizeButton.setBorderPainted(false);
        minimizeButton.setFocusPainted(false);
        minimizeButton.setContentAreaFilled(false);
        minimizeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleDialogSize();
            }
        });
        buttonPanel.add(minimizeButton);

        // Nút đóng (Dấu "X")
        JButton closeButton = new JButton("×");
        closeButton.setForeground(Color.WHITE);
        closeButton.setBackground(new Color(41, 128, 185));
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose(); // Đóng hộp thoại
            }
        });
        buttonPanel.add(closeButton);

        // Thêm panel chứa nút vào tiêu đề
        titlePanel.add(buttonPanel, BorderLayout.EAST);

        // Thêm sự kiện click để mở rộng khi nhấn vào toàn bộ tiêu đề
        titlePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isExpanded) {
                    toggleDialogSize();
                }
            }
        });

        // Tạo panel chatbot và đảm bảo không hiện chữ "Trợ lý ảo" bên trong
        chatbotPanel = new AIAssistantPanel();

        // Thêm các thành phần vào dialog
        add(new RoundedPanel(titlePanel), BorderLayout.NORTH);
        add(new RoundedPanel(chatbotPanel), BorderLayout.CENTER);

        // Không cho phép resize dialog
        setResizable(false);
    }

    private void toggleDialogSize() {
        isExpanded = !isExpanded;
        if (isExpanded) {
            setSize(350, 500);
        } else {
            setSize(350, 40);
        }
        adjustPosition();
    }

    @Override
    public void paint(Graphics g) {
        // Vẽ shadow và bo góc
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Shadow
        int shadowSize = 10;
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.fillRoundRect(shadowSize, shadowSize, getWidth() - shadowSize, getHeight() - shadowSize, 20, 20);

        // Bo góc
        g2d.setColor(getBackground());
        g2d.fillRoundRect(0, 0, getWidth() - shadowSize, getHeight() - shadowSize, 20, 20);

        super.paint(g);
    }

    /**
     * Hiển thị dialog ở góc phải dưới của frame cha
     */
    public void showAtCorner() {
        Frame owner = (Frame) getOwner();

        // Tính toán vị trí để hiển thị ở góc phải dưới
        int x = owner.getX() + owner.getWidth() - this.getWidth() - 20;
        int y = owner.getY() + owner.getHeight() - this.getHeight() - 40;

        // Đảm bảo dialog không vượt ra ngoài màn hình
        x = Math.max(x, 0);
        y = Math.max(y, 0);

        setLocation(x, y);
        setVisible(true);
    }

    /**
     * Xử lý khi cửa sổ chính thay đổi kích thước
     */
    public void adjustPosition() {
        if (isVisible()) {
            Frame owner = (Frame) getOwner();
            int x = owner.getX() + owner.getWidth() - this.getWidth() - 20;
            int y = owner.getY() + owner.getHeight() - this.getHeight() - 40;

            // Đảm bảo dialog không vượt ra ngoài màn hình
            x = Math.max(x, 0);
            y = Math.max(y, 0);

            setLocation(x, y);
        }
    }

    /**
     * Để tự động cập nhật vị trí khi frame cha thay đổi kích thước hoặc vị trí
     */
    public void attachToOwner() {
        Frame owner = (Frame) getOwner();

        // Thêm ComponentListener để theo dõi sự thay đổi kích thước và vị trí của frame cha
        owner.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                adjustPosition();
            }

            public void componentMoved(java.awt.event.ComponentEvent evt) {
                adjustPosition();
            }
        });
    }

    private static class RoundedPanel extends JPanel {
        public RoundedPanel(JPanel panel) {
            setLayout(new BorderLayout());
            add(panel);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Vẽ nền bo góc
            g2d.setColor(getBackground());
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
        }
    }
}