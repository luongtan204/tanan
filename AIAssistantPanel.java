package guiClient;

import dao.LichTrinhTauDAO;
import dao.impl.LichTrinhTauDAOImpl;
import service.ChatbotEngine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panel hiển thị chatbot trợ lý ảo
 */
public class AIAssistantPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(AIAssistantPanel.class.getName());

    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JScrollPane scrollPane;

    // Khởi tạo chatbotEngine với null để tránh lỗi "might not have been initialized"
    private ChatbotEngine chatbotEngine = null;

    public AIAssistantPanel() {
        // Khởi tạo giao diện trước
        initGUI();

        try {
            // Khởi tạo DAO và chatbot engine
            LichTrinhTauDAO lichTrinhTauDAO = new LichTrinhTauDAOImpl();
            this.chatbotEngine = new ChatbotEngine(lichTrinhTauDAO);

            // Thêm tin nhắn chào mừng
            addAssistantMessage("Xin chào! Tôi là trợ lý ảo hỗ trợ thông tin lịch trình tàu. " +
                    "Tôi có thể giúp bạn tìm kiếm lịch trình, kiểm tra trạng thái tàu, hoặc cung cấp thông tin về các ga tàu.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khởi tạo ChatbotEngine", e);
            addAssistantMessage("Xin lỗi, hệ thống đang gặp sự cố kết nối. " +
                    "Một số chức năng có thể không khả dụng. Vui lòng thử lại sau.");
            JOptionPane.showMessageDialog(this,
                    "Không thể khởi tạo trợ lý ảo: " + e.getMessage(),
                    "Lỗi khởi tạo",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initGUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Tiêu đề
        JLabel titleLabel = new JLabel("Trợ lý ảo", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(new Color(41, 128, 185));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // Khu vực chat
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 14));

        scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Panel nhập tin nhắn
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputField = new JTextField();
        inputField.setFont(new Font("Arial", Font.PLAIN, 14));

        // Xử lý phím Enter
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });

        // Tạo nút gửi với biểu tượng
        sendButton = new JButton();
        sendButton.setIcon(createSendIcon(20, 20));
        sendButton.setBackground(new Color(52, 152, 219));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setBorderPainted(false);
        sendButton.setPreferredSize(new Dimension(36, 36));

        // Xử lý nút Gửi
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        // Thêm tooltip
        inputField.setToolTipText("Nhập câu hỏi của bạn tại đây");
        sendButton.setToolTipText("Gửi tin nhắn");

        // Thêm vào panel nhập liệu
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // Thêm các thành phần vào panel chính
        add(titleLabel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
    }

    /**
     * Tạo biểu tượng gửi tin nhắn
     */
    private ImageIcon createSendIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        // Thiết lập chất lượng vẽ
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Vẽ mũi tên gửi
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.5f));

        // Tạo đường dẫn cho hình mũi tên
        int[] xPoints = {3, 17, 3, 8};
        int[] yPoints = {3, 10, 17, 10};
        g2.fillPolygon(xPoints, yPoints, 4);

        g2.dispose();
        return new ImageIcon(image);
    }

    /**
     * Gửi tin nhắn và xử lý phản hồi
     */
    private void sendMessage() {
        String userMessage = inputField.getText().trim();

        // Không xử lý tin nhắn rỗng
        if (userMessage.isEmpty()) {
            return;
        }

        // Hiển thị tin nhắn của người dùng
        addUserMessage(userMessage);

        // Xóa input
        inputField.setText("");

        // Kiểm tra nếu chatbot engine chưa được khởi tạo
        if (chatbotEngine == null) {
            addAssistantMessage("Xin lỗi, trợ lý ảo hiện không khả dụng do lỗi kết nối. Vui lòng thử lại sau.");
            return;
        }

        // Xử lý tin nhắn trong một luồng riêng
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                // Thêm tin nhắn "đang nhập..." trong khi chờ đợi
                SwingUtilities.invokeLater(() -> {
                    chatArea.append("Trợ lý: Đang nhập...\n\n");
                });

                // Xử lý tin nhắn qua chatbot engine
                return chatbotEngine.processMessage(userMessage);
            }

            @Override
            protected void done() {
                try {
                    // Xóa tin nhắn "đang nhập..."
                    String text = chatArea.getText();
                    if (text.endsWith("Trợ lý: Đang nhập...\n\n")) {
                        chatArea.setText(text.substring(0, text.length() - "Trợ lý: Đang nhập...\n\n".length()));
                    }

                    // Hiển thị phản hồi
                    String response = get();
                    addAssistantMessage(response);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Lỗi khi xử lý tin nhắn: " + e.getMessage(), e);
                    addAssistantMessage("Xin lỗi, đã xảy ra lỗi khi xử lý tin nhắn của bạn. Vui lòng thử lại sau.");
                }
            }
        };

        worker.execute();
    }

    /**
     * Thêm tin nhắn của người dùng vào khu vực chat
     */
    private void addUserMessage(String message) {
        // Lấy thời gian hiện tại
        LocalDateTime now = LocalDateTime.now();
        String time = now.format(DateTimeFormatter.ofPattern("HH:mm"));

        // Thêm tin nhắn vào chatArea
        chatArea.append("Bạn (" + time + "): " + message + "\n\n");

        // Cuộn xuống dưới
        scrollToBottom();
    }

    /**
     * Thêm tin nhắn của trợ lý vào khu vực chat
     */
    private void addAssistantMessage(String message) {
        // Lấy thời gian hiện tại
        LocalDateTime now = LocalDateTime.now();
        String time = now.format(DateTimeFormatter.ofPattern("HH:mm"));

        // Thêm tin nhắn vào chatArea
        chatArea.append("Trợ lý (" + time + "): " + message + "\n\n");

        // Cuộn xuống dưới
        scrollToBottom();
    }

    /**
     * Cuộn khu vực chat xuống dưới cùng
     */
    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }
}