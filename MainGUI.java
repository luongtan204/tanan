package guiClient;

import dao.DoiVeDAO;
import dao.impl.DoiVeDAOImpl;
import dao.impl.NhanVienDAOImpl;
import model.NhanVien;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class MainGUI extends JFrame {

    private JPanel contentPanel; // Content panel managed by CardLayout
    private CardLayout cardLayout; // CardLayout for switching panels
    private Map<String, JPanel> panelMap; // Cache for panels
    private LichTrinhTauPanel lichTrinhTauPanel;
    private NhanVien nhanVien;

    public MainGUI(NhanVien nv) {
        nhanVien = nv;
        setTitle("Quản lý tàu hỏa");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize panel map
        panelMap = new HashMap<>();

        // Create the main layout
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Create header
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Create vertical menu
        JPanel verticalMenu = createVerticalMenu();
        mainPanel.add(verticalMenu, BorderLayout.WEST);

        // Create content panel with CardLayout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        // Add default content panel
        JPanel defaultPanel = createDefaultContentPanel();
        contentPanel.add(defaultPanel, "Trang chủ");
        panelMap.put("Trang chủ", defaultPanel);

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        add(mainPanel);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(41, 128, 185)); // Blue header background
        headerPanel.setPreferredSize(new Dimension(0, 60));

        JLabel titleLabel = new JLabel("Hệ thống quản lý tàu hỏa", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);

        headerPanel.add(titleLabel, BorderLayout.CENTER);
        return headerPanel;
    }

    @Override
    public void dispose() {
        // Giải phóng các tài nguyên
        if (lichTrinhTauPanel != null) {
            lichTrinhTauPanel.shutdown();
        }

        // Gọi phương thức dispose của lớp cha
        super.dispose();
    }

    private void setupWindowListeners() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Giải phóng tài nguyên trước khi đóng
                if (lichTrinhTauPanel != null) {
                    lichTrinhTauPanel.shutdown();
                }
                dispose();
            }
        });
    }

    private JPanel createVerticalMenu() {
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBackground(new Color(52, 73, 94)); // Dark gray menu background
        menuPanel.setPreferredSize(new Dimension(250, 0));

        String[] menuItems = {
                "Trang chủ", "Thông tin hoạt động", "Quản lý khách hàng",
                "Quản lý vé", "Quản lý lịch trình", "Báo cáo", "Tra cứu vé", "Đổi vé", "Trả vé", "Quản lý nhân viên",
                "Thống kê số lượng vé theo thời gian", "Quản lý khuyến mãi"
        };

        for (String item : menuItems) {
            JPanel menuItemPanel = new JPanel(new BorderLayout());
            menuItemPanel.setBackground(new Color(52, 73, 94));
            menuItemPanel.setMaximumSize(new Dimension(250, 50));

            JLabel menuLabel = new JLabel(item);
            menuLabel.setForeground(Color.WHITE);
            menuLabel.setFont(new Font("Arial", Font.PLAIN, 16));
            menuLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 10));

            menuItemPanel.add(menuLabel, BorderLayout.CENTER);

            // Hover effect
            menuItemPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    menuItemPanel.setBackground(new Color(41, 128, 185)); // Blue hover background
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    menuItemPanel.setBackground(new Color(52, 73, 94));
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    // Switch content based on the menu item clicked
                    switchToPanel(item);
                }
            });

            menuPanel.add(menuItemPanel);
        }

        return menuPanel;
    }

    private JPanel createDefaultContentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JLabel contentLabel = new JLabel("Chào mừng đến hệ thống quản lý tàu hỏa!", JLabel.CENTER);
        contentLabel.setFont(new Font("Arial", Font.ITALIC, 18));
        contentLabel.setForeground(Color.GRAY);

        panel.add(contentLabel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createPlaceholderPanel(String menuName) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JLabel placeholderLabel = new JLabel("Nội dung cho " + menuName + " đang được phát triển.", JLabel.CENTER);
        placeholderLabel.setFont(new Font("Arial", Font.ITALIC, 18));
        placeholderLabel.setForeground(Color.GRAY);

        panel.add(placeholderLabel, BorderLayout.CENTER);
        return panel;
    }

    private void switchToPanel(String panelName) {
        // Check if the panel already exists in the cache
        if (!panelMap.containsKey(panelName)) {
            JPanel newPanel;

            if (panelName.equals("Quản lý khách hàng")) {
                // Display loading interface
                JPanel loadingPanel = createLoadingPanel("Đang tải dữ liệu khách hàng...");
                contentPanel.add(loadingPanel, "Loading_" + panelName);
                cardLayout.show(contentPanel, "Loading_" + panelName);

                // Create customer management panel in a separate thread
                SwingWorker<QuanLyKhachHangPanel, Void> worker = new SwingWorker<>() {
                    @Override
                    protected QuanLyKhachHangPanel doInBackground() throws Exception {
                        return new QuanLyKhachHangPanel();
                    }

                    @Override
                    protected void done() {
                        try {
                            // Get the panel after it's created
                            QuanLyKhachHangPanel panel = get();

                            // Add to cache and display
                            contentPanel.add(panel, panelName);
                            panelMap.put(panelName, panel);
                            cardLayout.show(contentPanel, panelName);

                            // Remove loading panel
                            contentPanel.remove(loadingPanel);
                        } catch (Exception e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(MainGUI.this,
                                    "Không thể tải dữ liệu khách hàng: " + e.getMessage(),
                                    "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
                            cardLayout.show(contentPanel, "Trang chủ");
                        }
                    }
                };

                worker.execute();
                return; // Exit early, don't execute the rest of the method
            } else if (panelName.equals("Quản lý lịch trình")) {
                // Hiển thị giao diện tải dữ liệu
                JPanel loadingPanel = createLoadingPanel("Đang tải dữ liệu lịch trình...");
                contentPanel.add(loadingPanel, "Loading_" + panelName);
                cardLayout.show(contentPanel, "Loading_" + panelName);

                // Tạo panel quản lý lịch trình trong luồng riêng
                SwingWorker<LichTrinhTauPanel, Void> worker = new SwingWorker<>() {
                    @Override
                    protected LichTrinhTauPanel doInBackground() {
                        return new LichTrinhTauPanel();
                    }

                    @Override
                    protected void done() {
                        try {
                            // Lấy panel sau khi đã tạo xong
                            LichTrinhTauPanel panel = get();
                            lichTrinhTauPanel = panel;

                            // Thêm vào cache và hiển thị
                            contentPanel.add(panel, panelName);
                            panelMap.put(panelName, panel);
                            cardLayout.show(contentPanel, panelName);

                            // Xóa panel loading
                            contentPanel.remove(loadingPanel);
                        } catch (Exception e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(MainGUI.this,
                                    "Không thể tải dữ liệu: " + e.getMessage(),
                                    "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
                            cardLayout.show(contentPanel, "Trang chủ");
                        }
                    }
                };

                worker.execute();
                return; // Thoát sớm, không thực hiện phần còn lại của method
            }       // THÊM ĐOẠN NÀY
            else if (panelName.equals("Đổi vé")) {
                // Hiển thị giao diện tải dữ liệu
                JPanel loadingPanel = createLoadingPanel("Đang tải dữ liệu quản lý vé...");
                contentPanel.add(loadingPanel, "Loading_" + panelName);
                cardLayout.show(contentPanel, "Loading_" + panelName);

                // Tạo panel quản lý vé trong luồng riêng
                SwingWorker<DoiVePanel, Void> worker = new SwingWorker<>() {
                    @Override
                    protected DoiVePanel doInBackground() {
                        return new DoiVePanel(nhanVien);
                    }

                    @Override
                    protected void done() {
                        try {
                            // Lấy panel sau khi đã tạo xong
                            DoiVePanel panel = get();

                            // Thêm vào cache và hiển thị
                            contentPanel.add(panel, panelName);
                            panelMap.put(panelName, panel);
                            cardLayout.show(contentPanel, panelName);

                            // Xóa panel loading
                            contentPanel.remove(loadingPanel);
                        } catch (Exception e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(MainGUI.this,
                                    "Không thể tải dữ liệu: " + e.getMessage(),
                                    "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
                            cardLayout.show(contentPanel, "Trang chủ");
                        }
                    }
                };

                worker.execute();
                return; // Thoát sớm
            } else if (panelName.equals("Trả vé")) {
                // Hiển thị giao diện tải dữ liệu
                NhanVien nhanVien_1 = new NhanVien(
                        "NV202504180002",                           // maNV
                        "Nguyen Van A",                    // tenNV
                        "0900765432",                      // soDT
                        "Hoạt động",                       // trangThai
                        "CCCD67890",                    // cccd
                        "Ha Noi", // diaChi
                        LocalDate.of(2025, 04, 05),         // ngayVaoLam
                        "Nhân viên bán vé",               // chucVu
                        "avatar2.jpg",              // avata
                        null,                              // taiKhoan - sẽ thiết lập sau
                        null,                              // danhSachLichLamViec - sẽ thiết lập sau
                        null                               // danhSachHoaDon - sẽ thiết lập sau
                );
                JPanel loadingPanel = createLoadingPanel("Đang tải dữ liệu trả vé...");
                contentPanel.add(loadingPanel, "Loading_" + panelName);
                cardLayout.show(contentPanel, "Loading_" + panelName);
                // Tạo panel trả vé trong luồng riêng
                SwingWorker<TraVePanel, Void> worker = new SwingWorker<>() {
                    @Override
                    protected TraVePanel doInBackground() {
//                        return new TraVePanel(nhanVien); // TraVePanel sẽ tự kết nối RMI
                        return new TraVePanel(nhanVien_1);
                    }

                    @Override
                    protected void done() {
                        try {
                            // Lấy panel sau khi đã tạo xong
                            TraVePanel panel = get();
                            // Thêm vào cache và hiển thị
                            contentPanel.add(panel, panelName);
                            panelMap.put(panelName, panel);
                            cardLayout.show(contentPanel, panelName);
                            // Xóa panel loading
                            contentPanel.remove(loadingPanel);
                        } catch (Exception e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(MainGUI.this,
                                    "Không thể tải dữ liệu trả vé: " + e.getMessage(),
                                    "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
                            cardLayout.show(contentPanel, "Trang chủ");
                        }
                    }
                };
                worker.execute();
                return; // Thoát sớm
            } else if (panelName.equals("Quản lý nhân viên")) {
                // Hiển thị giao diện tải dữ liệu
                JPanel loadingPanel = createLoadingPanel("Đang tải dữ liệu nhân viên...");
                contentPanel.add(loadingPanel, "Loading_" + panelName);
                cardLayout.show(contentPanel, "Loading_" + panelName);

                // Tạo panel trả vé trong luồng riêng
                SwingWorker<QuanLyNhanVienPanel, Void> worker = new SwingWorker<>() {
                    @Override
                    protected QuanLyNhanVienPanel doInBackground() throws RemoteException {
                        return new QuanLyNhanVienPanel(); // TraVePanel sẽ tự kết nối RMI
                    }

                    @Override
                    protected void done() {
                        try {
                            // Lấy panel sau khi đã tạo xong
                            QuanLyNhanVienPanel panel = get();

                            // Thêm vào cache và hiển thị
                            contentPanel.add(panel, panelName);
                            panelMap.put(panelName, panel);
                            cardLayout.show(contentPanel, panelName);

                            // Xóa panel loading
                            contentPanel.remove(loadingPanel);
                        } catch (Exception e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(MainGUI.this,
                                    "Không thể tải dữ liệu nhân viên: " + e.getMessage(),
                                    "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
                            cardLayout.show(contentPanel, "Trang chủ");
                        }
                    }
                };

                worker.execute();
                return; // Thoát sớm
            } else if (panelName.equals("Quản lý khuyến mãi")) {
                // Hiển thị giao diện tải dữ liệu
                JPanel loadingPanel = createLoadingPanel("Đang tải dữ liệu khuyến mãi...");
                contentPanel.add(loadingPanel, "Loading_" + panelName);
                cardLayout.show(contentPanel, "Loading_" + panelName);

                // Tạo panel quản lý khuyến mãi trong luồng riêng
                SwingWorker<KhuyenMaiPanel, Void> worker = new SwingWorker<>() {
                    @Override
                    protected KhuyenMaiPanel doInBackground() throws RemoteException {
                        return new KhuyenMaiPanel(); // KhuyenMaiPanel sẽ tự kết nối RMI
                    }

                    @Override
                    protected void done() {
                        try {
                            // Lấy panel sau khi đã tạo xong
                            KhuyenMaiPanel panel = get();

                            // Thêm vào cache và hiển thị
                            contentPanel.add(panel, panelName);
                            panelMap.put(panelName, panel);
                            cardLayout.show(contentPanel, panelName);

                            // Xóa panel loading
                            contentPanel.remove(loadingPanel);
                        } catch (Exception e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(MainGUI.this,
                                    "Không thể tải dữ liệu khuyến mãi: " + e.getMessage(),
                                    "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
                            cardLayout.show(contentPanel, "Trang chủ");
                        }
                    }
                };

                worker.execute();
                return; // Thoát sớm
            } else if (panelName.equals("Tra cứu vé")) {
                // Hiển thị giao diện tải dữ liệu
                JPanel loadingPanel = createLoadingPanel("Đang tải dữ liệu quản lý vé...");
                contentPanel.add(loadingPanel, "Loading_" + panelName);
                cardLayout.show(contentPanel, "Loading_" + panelName);

                // Tạo panel quản lý vé trong luồng riêng
                SwingWorker<TraCuuVePanel, Void> worker = new SwingWorker<>() {
                    @Override
                    protected TraCuuVePanel doInBackground() {
                        return new TraCuuVePanel();
                    }

                    @Override
                    protected void done() {
                        try {
                            // Lấy panel sau khi đã tạo xong
                            TraCuuVePanel panel = get();

                            // Thêm vào cache và hiển thị
                            contentPanel.add(panel, panelName);
                            panelMap.put(panelName, panel);
                            cardLayout.show(contentPanel, panelName);

                            // Xóa panel loading
                            contentPanel.remove(loadingPanel);
                        } catch (Exception e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(MainGUI.this,
                                    "Không thể tải dữ liệu: " + e.getMessage(),
                                    "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
                            cardLayout.show(contentPanel, "Trang chủ");
                        }
                    }
                };

                worker.execute();
                return; // Thoát sớm
            }else if (panelName.equals("Thống kê số lượng vé theo thời gian")) {
                // Hiển thị giao diện tải dữ liệu
                JPanel loadingPanel = createLoadingPanel("Đang tải dữ liệu quản lý vé...");
                contentPanel.add(loadingPanel, "Loading_" + panelName);
                cardLayout.show(contentPanel, "Loading_" + panelName);

                // Tạo panel quản lý vé trong luồng riêng
                SwingWorker<ThongKeVePanel, Void> worker = new SwingWorker<>() {
                    @Override
                    protected ThongKeVePanel doInBackground() {
                        return new ThongKeVePanel();
                    }

                    @Override
                    protected void done() {
                        try {
                            // Lấy panel sau khi đã tạo xong
                            ThongKeVePanel panel = get();

                            // Thêm vào cache và hiển thị
                            contentPanel.add(panel, panelName);
                            panelMap.put(panelName, panel);
                            cardLayout.show(contentPanel, panelName);

                            // Xóa panel loading
                            contentPanel.remove(loadingPanel);
                        } catch (Exception e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(MainGUI.this,
                                    "Không thể tải dữ liệu: " + e.getMessage(),
                                    "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
                            cardLayout.show(contentPanel, "Trang chủ");
                        }
                    }
                };

                worker.execute();
                return; // Thoát sớm
            }
        }


        // Switch to the panel
        cardLayout.show(contentPanel, panelName);
    }

    private JPanel createLoadingPanel(String message) {
        JPanel loadingPanel = new JPanel(new BorderLayout());
        loadingPanel.setBackground(Color.WHITE);

        JLabel loadingLabel = new JLabel(message, JLabel.CENTER);
        loadingLabel.setFont(new Font("Arial", Font.ITALIC, 18));
        loadingLabel.setForeground(Color.GRAY);

        loadingPanel.add(loadingLabel, BorderLayout.CENTER);
        return loadingPanel;
    }

    // Phương thức tạo spinner (hoặc bạn có thể sử dụng một ảnh GIF spinner có sẵn)
    private Image createLoadingSpinnerGif() {
        // Bạn có thể thay thế cái này bằng một ảnh GIF spinner thực tế
        // Đây chỉ là một placeholder đơn giản
        BufferedImage image = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(new Color(41, 128, 185));
        g2.fillOval(0, 0, 50, 50);
        g2.dispose();
        return image;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            new MainGUI(new NhanVien()).setVisible(true);
        });
    }
}
