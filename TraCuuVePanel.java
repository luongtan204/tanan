package guiClient;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.toedter.calendar.JDateChooser;
import dao.TraCuuVeDAO;
import dao.impl.TraCuuVeDAOImpl;
import entity.TicketPDFGenerator;
import model.ChiTietHoaDon;
import model.TicketDetails;
import model.VeTau;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.Dimension;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class TraCuuVePanel extends JPanel {
    private static final String RMI_SERVER_IP = "127.0.0.1";
    private static final int RMI_SERVER_PORT = 9090;
    private final JPanel panelTimVe;
    private final JPanel panelChiTietVe;
    private JRadioButton radioMaVe;
    private JRadioButton radioGiayTo;
    private JRadioButton radioHoTen;
    private ButtonGroup buttonGroup;
    private JPanel panel_ThongTinVe = new JPanel();
    private JTable table;
    private DefaultTableModel tableModel;

    // Màu chính (xanh dương) khi chưa hover
    private Color MauXanh = new Color(41, 128, 185);

    // Màu khi hover
    private Color hoverColor = new Color(66, 139, 202);

    // Màu hover cho row trong table
    private Color tableHoverColor = new Color(173, 216, 230);

    // Kích thước cơ bản cho các thành phần UI
    private final int BUTTON_HEIGHT = 30;
    private final int FIELD_HEIGHT = 25;
    private final int LABEL_HEIGHT = 20;
    private final int ICON_SIZE = 16;
    private final int BUTTON_ICON_SIZE = 18;
    private final int TITLE_ICON_SIZE = 24;
    private final Dimension TEXT_FIELD_SIZE = new Dimension(180, FIELD_HEIGHT);

    private JTextField txtHoTen;
    private JButton btnTimVe;
    private JTextField txtMaVe;
    private JTextField txtMaLich;
    private JTextField txtMaCho;
    private JTextField txtTenKH;
    private JTextField txtGiayTo;
    private JTextField txtNgayDi;
    private JTextField txtDoiTuong;
    private JTextField txtGiaVe;
    private JTextField txtTrangThai;
    private List<VeTau> veTauList;

    // Biến để theo dõi dòng đang hover
    private final int[] hoverRow = {-1};

    // Icon factory for programmatically generated icons
    private IconFactory iconFactory;
    private TraCuuVeDAO traCuuVeDAO;

    public Component get_TraCuuVe_Panel() {
        return this;
    }

    public TraCuuVePanel() {
        // Initialize the icon factory
        iconFactory = new IconFactory();

        // Thiết lập panel chính với BorderLayout để hỗ trợ responsive
        setLayout(new BorderLayout());

        // Sử dụng JSplitPane để giúp giao diện có thể responsive tốt hơn
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setResizeWeight(0.6); // Phân bổ không gian
        mainSplitPane.setOneTouchExpandable(true); // Cho phép mở rộng/thu gọn bằng nút
        add(mainSplitPane, BorderLayout.CENTER);

        // Thêm Panel tiêu đề ở trên cùng
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Panel chứa các panel tìm kiếm và chi tiết
        JPanel topPanel = new JPanel(new BorderLayout());
        mainSplitPane.setTopComponent(topPanel);

        // Panel hiển thị kết quả
        panel_ThongTinVe = new JPanel(new BorderLayout());
        mainSplitPane.setBottomComponent(panel_ThongTinVe);

        // Khởi tạo Panel TraCuuVe với bố cục có thể kéo giãn
        JSplitPane horizontalSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        horizontalSplitPane.setResizeWeight(0.5); // Cân bằng giữa hai panel
        horizontalSplitPane.setOneTouchExpandable(true);
        topPanel.add(horizontalSplitPane, BorderLayout.CENTER);

        // Khởi tạo các panel con
        panelTimVe = new JPanel(new BorderLayout(0, 5)); // Thêm khoảng cách giữa các panel con
        panelTimVe.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        horizontalSplitPane.setLeftComponent(panelTimVe);

        // Panel chi tiết vé
        panelChiTietVe = createPanelChiTietVe();
        horizontalSplitPane.setRightComponent(panelChiTietVe);

        // Thêm các panel vào panelTimVe
        panelTimVe.add(createPanelTimNhanh(), BorderLayout.NORTH);
        panelTimVe.add(createPanelTimChitiet(), BorderLayout.CENTER);
        panelTimVe.add(createPanelTimTheoThoiGian(), BorderLayout.SOUTH);

        // Tạo giao diện bảng
        panel_ThongTinVe.setLayout(new BorderLayout());
        String[] columnNames = {
                "Mã vé", "Họ tên", "Giấy tờ", "Ngày đi", "Đối tượng", "Giá vé", "Trạng thái"
        };
        tableModel = new DefaultTableModel(columnNames, 0);
        table = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                Component comp = super.prepareRenderer(renderer, row, column);
                boolean isSelected = isCellSelected(row, column);

                // Màu nền cho các dòng
                if (isSelected) {
                    comp.setBackground(MauXanh); // Màu xanh đậm khi được chọn
                    comp.setForeground(Color.WHITE);
                }
                // Nếu dòng này đang được hover
                else if (row == hoverRow[0]) {
                    comp.setBackground(tableHoverColor); // Màu xanh nhạt cho hover
                    comp.setForeground(Color.BLACK);
                }
                else {
                    // Màu xen kẽ cho các dòng
                    comp.setBackground(row % 2 == 0 ? new Color(240, 240, 240) : Color.WHITE);
                    comp.setForeground(Color.BLACK);
                }
                return comp;
            }
        };

        // Thêm hiệu ứng hover cho hàng bảng
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row != hoverRow[0]) {
                    hoverRow[0] = row;
                    table.repaint();
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoverRow[0] = -1;
                table.repaint();
            }
        });

        // Tùy chỉnh tiêu đề bảng
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Arial", Font.BOLD, 14));
        header.setBackground(MauXanh); // Đặt màu cho header
        header.setForeground(Color.WHITE);
        header.setOpaque(true);

        // Tùy chỉnh font chữ từng dòng
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
        cellRenderer.setFont(new Font("Arial", Font.PLAIN, 13));
        cellRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.setDefaultRenderer(Object.class, cellRenderer);

        // Chỉnh chiều cao dòng
        table.setRowHeight(25);

        // Đưa bảng vào JScrollPane để có thể cuộn
        JScrollPane scrollPane = new JScrollPane(table);
        panel_ThongTinVe.add(scrollPane, BorderLayout.CENTER);

        // Đặt tiêu đề cho panel kết quả
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // Thêm icon cho tiêu đề kết quả
        JLabel lblResults = new JLabel("Kết quả tìm kiếm", iconFactory.getIcon("list", TITLE_ICON_SIZE, TITLE_ICON_SIZE), JLabel.LEFT);
        lblResults.setFont(new Font("Arial", Font.BOLD, 15));
        lblResults.setIconTextGap(10);
        titlePanel.add(lblResults);
        panel_ThongTinVe.add(titlePanel, BorderLayout.NORTH);

        // Thêm sự kiện cho nút "Tìm vé"
        addSearchButtonListener();

        // Thiết lập kích thước mặc định
        setPreferredSize(new Dimension(950, 700));
        initializeDAOConnection();
    }
    public void initializeDAOConnection() {
        try {
            Registry registry = LocateRegistry.getRegistry(RMI_SERVER_IP, RMI_SERVER_PORT);

            traCuuVeDAO = (TraCuuVeDAO) registry.lookup("traCuuVeDAO");
            // Kiểm tra kết nối
            if (traCuuVeDAO.testConnection()) {
                System.out.println("Kết nối đến TraCuuVeDAO thành công!");
            } else {
                throw new Exception("Không thể kết nối đến cơ sở dữ liệu qua TraCuuVeDAO");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Lỗi kết nối đến dịch vụ tra cứu vé: " + e.getMessage(),
                    "Lỗi Kết Nối", JOptionPane.ERROR_MESSAGE);

            // Có thể thêm cơ chế thử lại kết nối
            int option = JOptionPane.showConfirmDialog(this,
                    "Bạn có muốn thử kết nối lại không?",
                    "Thử lại kết nối", JOptionPane.YES_NO_OPTION);

            if (option == JOptionPane.YES_OPTION) {
                // Thử kết nối lại sau 2 giây
                Timer timer = new Timer(2000, evt -> {
                    ((Timer) evt.getSource()).stop(); // Dừng timer
                    initializeDAOConnection(); // Thử kết nối lại
                });
                timer.setRepeats(false); // Chỉ thực hiện một lần
                timer.start();
            }
        }
    }
    // Class tạo và quản lý các icon được vẽ theo chương trình
    private class IconFactory {
        // Tạo và trả về icon theo yêu cầu với màu mặc định
        public Icon getIcon(String iconName, int width, int height) {
            return new VectorIcon(iconName, width, height);
        }

        // Tạo và trả về icon màu trắng (dùng cho các nút có nền màu)
        public Icon getWhiteIcon(String iconName, int width, int height) {
            return new VectorIcon(iconName, width, height, Color.WHITE);
        }

        // Class custom icon sử dụng vector graphics
        private class VectorIcon implements Icon {
            private final String iconName;
            private final int width;
            private final int height;
            private final Color forcedColor; // Màu bắt buộc (nếu có)

            public VectorIcon(String iconName, int width, int height) {
                this.iconName = iconName;
                this.width = width;
                this.height = height;
                this.forcedColor = null; // Không có màu bắt buộc
            }

            public VectorIcon(String iconName, int width, int height, Color forcedColor) {
                this.iconName = iconName;
                this.width = width;
                this.height = height;
                this.forcedColor = forcedColor; // Sử dụng màu được chỉ định
            }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);

                // Xác định màu biểu tượng
                Color iconColor;
                if (forcedColor != null) {
                    iconColor = forcedColor; // Sử dụng màu bắt buộc nếu có
                } else {
                    iconColor = c.isEnabled() ? new Color(41, 128, 185) : Color.GRAY;
                }
                g2.setColor(iconColor);

                // Scale icon to fit the specified width and height
                g2.scale(width / 24.0, height / 24.0);

                switch (iconName) {
                    case "train":
                        drawTrainIcon(g2, iconColor);
                        break;
                    case "detail":
                        drawDetailIcon(g2, iconColor);
                        break;
                    case "search":
                        drawSearchIcon(g2, iconColor);
                        break;
                    case "ticket":
                        drawTicketIcon(g2, iconColor);
                        break;
                    case "info":
                        drawInfoIcon(g2, iconColor);
                        break;
                    case "seat":
                        drawSeatIcon(g2, iconColor);
                        break;
                    case "user":
                        drawUserIcon(g2, iconColor);
                        break;
                    case "id-card":
                        drawIdCardIcon(g2, iconColor);
                        break;
                    case "calendar":
                        drawCalendarIcon(g2, iconColor);
                        break;
                    case "person":
                        drawPersonIcon(g2, iconColor);
                        break;
                    case "money":
                        drawMoneyIcon(g2, iconColor);
                        break;
                    case "status":
                        drawStatusIcon(g2, iconColor);
                        break;
                    case "print":
                        drawPrintIcon(g2, iconColor);
                        break;
                    case "time-search":
                        drawTimeSearchIcon(g2, iconColor);
                        break;
                    case "clock":
                        drawClockIcon(g2, iconColor);
                        break;
                    case "filter":
                        drawFilterIcon(g2, iconColor);
                        break;
                    case "search-detail":
                        drawSearchDetailIcon(g2, iconColor);
                        break;
                    case "quick-search":
                        drawQuickSearchIcon(g2, iconColor);
                        break;
                    case "qrcode":
                        drawQrCodeIcon(g2, iconColor);
                        break;
                    case "list":
                        drawListIcon(g2, iconColor);
                        break;
                    default:
                        drawDefaultIcon(g2, iconColor);
                }

                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return width;
            }

            @Override
            public int getIconHeight() {
                return height;
            }

            // Các phương thức vẽ icon cụ thể
            private void drawTrainIcon(Graphics2D g2, Color iconColor) {
                g2.setColor(iconColor);

                // Thân tàu
                g2.fillRoundRect(2, 8, 20, 12, 4, 4);

                // Đầu tàu
                g2.fillRect(18, 5, 4, 3);

                // Cửa sổ
                g2.setColor(Color.WHITE);
                g2.fillRect(5, 11, 3, 3);
                g2.fillRect(10, 11, 3, 3);
                g2.fillRect(15, 11, 3, 3);

                // Bánh xe
                g2.setColor(Color.DARK_GRAY);
                g2.fillOval(4, 18, 4, 4);
                g2.fillOval(16, 18, 4, 4);
            }

            private void drawDetailIcon(Graphics2D g2, Color iconColor) {
                g2.setColor(iconColor);

                // Background
                g2.fillRoundRect(3, 3, 18, 18, 2, 2);

                // Lines
                g2.setColor(Color.WHITE);
                g2.fillRect(6, 7, 12, 1);
                g2.fillRect(6, 11, 12, 1);
                g2.fillRect(6, 15, 12, 1);
            }

            private void drawSearchIcon(Graphics2D g2, Color iconColor) {
                g2.setColor(iconColor);

                // Kính lúp
                g2.setStroke(new BasicStroke(2));
                g2.draw(new Ellipse2D.Double(4, 4, 12, 12));
                g2.draw(new Line2D.Double(14, 14, 20, 20));
            }

            private void drawTicketIcon(Graphics2D g2, Color iconColor) {
                g2.setColor(iconColor);

                // Vé
                g2.fillRoundRect(2, 6, 20, 12, 4, 4);

                // Đường kẻ đục lỗ
                g2.setColor(Color.WHITE);
                float[] dash = {2.0f, 2.0f};
                g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, dash, 0));
                g2.drawLine(7, 6, 7, 18);

                // Nội dung vé
                g2.fillRect(10, 10, 8, 1);
                g2.fillRect(10, 13, 8, 1);
            }

            private void drawInfoIcon(Graphics2D g2, Color iconColor) {
                g2.setColor(iconColor);

                // Biểu tượng i
                g2.fillOval(8, 4, 8, 8);
                g2.fillRoundRect(11, 14, 2, 6, 1, 1);

                // Chữ i
                g2.setColor(Color.WHITE);
                g2.fillOval(11, 6, 2, 2);
                g2.fillRoundRect(11, 10, 2, 2, 1, 1);
            }

            private void drawSeatIcon(Graphics2D g2, Color iconColor) {
                g2.setColor(iconColor);

                // Ghế
                g2.fillRoundRect(3, 7, 18, 15, 2, 2);

                // Lưng ghế
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(5, 9, 14, 8, 2, 2);

                // Chân ghế
                g2.setColor(iconColor);
                g2.fillRect(5, 19, 3, 3);
                g2.fillRect(16, 19, 3, 3);
            }

            private void drawUserIcon(Graphics2D g2, Color iconColor) {
                g2.setColor(iconColor);

                // Đầu
                g2.fillOval(8, 4, 8, 8);

                // Thân
                g2.fillOval(4, 14, 16, 8);
            }

            private void drawIdCardIcon(Graphics2D g2, Color iconColor) {
                g2.setColor(iconColor);

                // Card
                g2.fillRoundRect(2, 5, 20, 14, 2, 2);

                // Avatar
                g2.setColor(Color.WHITE);
                g2.fillOval(5, 8, 6, 6);

                // Thông tin
                g2.fillRect(13, 8, 6, 1);
                g2.fillRect(13, 11, 6, 1);
                g2.fillRect(13, 14, 6, 1);
            }

            private void drawCalendarIcon(Graphics2D g2, Color iconColor) {
                g2.setColor(iconColor);

                // Calendar body
                g2.fillRoundRect(3, 6, 18, 16, 2, 2);

                // Calendar top
                g2.fillRect(7, 3, 2, 4);
                g2.fillRect(15, 3, 2, 4);

                // Calendar lines
                g2.setColor(Color.WHITE);
                g2.fillRect(6, 10, 12, 1);
                g2.fillRect(6, 14, 12, 1);
                g2.fillRect(6, 18, 12, 1);
                g2.fillRect(10, 10, 1, 9);
                g2.fillRect(14, 10, 1, 9);
            }

            private void drawPersonIcon(Graphics2D g2, Color iconColor) {
                g2.setColor(iconColor);

                // Head
                g2.fillOval(9, 3, 6, 6);

                // Body
                g2.fillRoundRect(4, 11, 16, 10, 4, 4);
            }

            private void drawMoneyIcon(Graphics2D g2, Color iconColor) {
                g2.setColor(iconColor);

                // Coin
                g2.fillOval(4, 4, 16, 16);

                // $ Symbol
                g2.setColor(Color.WHITE);
                g2.fillRect(11, 8, 2, 8);
                g2.fillRect(9, 8, 6, 2);
                g2.fillRect(9, 14, 6, 2);
            }

            private void drawStatusIcon(Graphics2D g2, Color iconColor) {
                g2.setColor(iconColor);

                // Status circles
                g2.fillOval(3, 10, 4, 4);
                g2.fillOval(10, 10, 4, 4);
                g2.fillOval(17, 10, 4, 4);

                // Lines connecting
                g2.setStroke(new BasicStroke(1));
                g2.drawLine(7, 12, 10, 12);
                g2.drawLine(14, 12, 17, 12);
            }

            private void drawPrintIcon(Graphics2D g2, Color iconColor) {
                g2.setColor(iconColor);

                // Printer body
                g2.fillRect(3, 10, 18, 8);

                // Paper
                g2.setColor(Color.WHITE);
                g2.fillRect(7, 5, 10, 5);
                g2.fillRect(7, 18, 10, 5);

                // Detail
                g2.setColor(iconColor);
                g2.fillOval(16, 13, 2, 2);
            }

            private void drawTimeSearchIcon(Graphics2D g2, Color iconColor) {
                g2.setColor(iconColor);

                // Clock face
                g2.drawOval(4, 4, 16, 16);

                // Clock hands
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(12, 12, 12, 6);
                g2.drawLine(12, 12, 16, 12);

                // Dots
                g2.fillOval(12, 12, 1, 1);

                // Magnifying glass
                g2.drawOval(16, 2, 6, 6);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(20, 8, 22, 10);
            }

            private void drawClockIcon(Graphics2D g2, Color iconColor) {
                g2.setColor(iconColor);

                // Clock face
                g2.drawOval(2, 2, 20, 20);

                // Clock hands
                g2.drawLine(12, 12, 12, 4);
                g2.drawLine(12, 12, 18, 12);

                // Center dot
                g2.fillOval(11, 11, 2, 2);

                // Clock markers
                for (int i = 0; i < 12; i++) {
                    double angle = Math.toRadians(i * 30);
                    int x1 = (int) (12 + 9 * Math.sin(angle));
                    int y1 = (int) (12 - 9 * Math.cos(angle));
                    int x2 = (int) (12 + 10 * Math.sin(angle));
                    int y2 = (int) (12 - 10 * Math.cos(angle));
                    g2.drawLine(x1, y1, x2, y2);
                }
            }

            private void drawFilterIcon(Graphics2D g2, Color iconColor) {
                g2.setColor(iconColor);

                // Filter shape - improved design
                g2.setStroke(new BasicStroke(1.5f));
                int[] xPoints = {2, 22, 15, 15, 9, 9};
                int[] yPoints = {4, 4, 12, 20, 20, 12};
                g2.fillPolygon(xPoints, yPoints, 6);

                // Filter lines
                g2.setColor(Color.WHITE);
                g2.drawLine(6, 8, 18, 8);
                g2.drawLine(10, 16, 14, 16);
            }

            private void drawSearchDetailIcon(Graphics2D g2, Color iconColor) {
                g2.setColor(iconColor);

                // Magnifying glass with document
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new Ellipse2D.Double(4, 4, 10, 10));
                g2.draw(new Line2D.Double(12, 12, 18, 18));

                // Document outline
                g2.drawRoundRect(12, 3, 9, 12, 2, 2);

                // Document lines
                g2.drawLine(14, 6, 19, 6);
                g2.drawLine(14, 9, 19, 9);
                g2.drawLine(14, 12, 17, 12);
            }

            private void drawQuickSearchIcon(Graphics2D g2, Color iconColor) {
                g2.setColor(iconColor);

                // Magnifying glass
                g2.setStroke(new BasicStroke(2));
                g2.draw(new Ellipse2D.Double(4, 4, 12, 12));
                g2.draw(new Line2D.Double(14, 14, 20, 20));

                // Flash (lightning bolt)
                g2.fillPolygon(
                        new int[]{10, 8, 12, 10, 14, 12},
                        new int[]{4, 10, 10, 14, 7, 7}, 6
                );
            }

            private void drawQrCodeIcon(Graphics2D g2, Color iconColor) {
                g2.setColor(iconColor);

                // QR code frame
                g2.fillRect(4, 4, 16, 16);

                // QR code pattern
                g2.setColor(Color.WHITE);
                // Upper left corner pattern
                g2.fillRect(6, 6, 4, 4);
                g2.setColor(iconColor);
                g2.fillRect(7, 7, 2, 2);

                // Upper right corner pattern
                g2.setColor(Color.WHITE);
                g2.fillRect(14, 6, 4, 4);
                g2.setColor(iconColor);
                g2.fillRect(15, 7, 2, 2);

                // Bottom left corner pattern
                g2.setColor(Color.WHITE);
                g2.fillRect(6, 14, 4, 4);
                g2.setColor(iconColor);
                g2.fillRect(7, 15, 2, 2);

                // Random QR code pattern
                g2.setColor(Color.WHITE);
                g2.fillRect(12, 12, 2, 2);
                g2.fillRect(15, 12, 2, 2);
                g2.fillRect(12, 15, 2, 2);
            }

            private void drawListIcon(Graphics2D g2, Color iconColor) {
                g2.setColor(iconColor);

                // List lines
                g2.setStroke(new BasicStroke(2));
                for (int i = 0; i < 4; i++) {
                    int y = 6 + i * 4;
                    // Bullet point
                    g2.fillOval(4, y, 2, 2);
                    // Line
                    g2.drawLine(8, y + 1, 20, y + 1);
                }
            }

            private void drawDefaultIcon(Graphics2D g2, Color iconColor) {
                g2.setColor(iconColor);

                // Empty box with question mark
                g2.drawRect(4, 4, 16, 16);
                g2.setFont(new Font("Dialog", Font.BOLD, 16));
                g2.drawString("?", 10, 18);
            }
        }
    }

    // Tạo panel tiêu đề với icon
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(MauXanh);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        // Tạo icon cho tiêu đề
        JLabel iconLabel = new JLabel(iconFactory.getWhiteIcon("train", 35, 35));

        // Tiêu đề
        JLabel titleLabel = new JLabel("TRA CỨU VÉ TÀU");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));

        // Panel chứa icon và tiêu đề
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.setBackground(MauXanh);
        titlePanel.add(iconLabel);
        titlePanel.add(titleLabel);

        // Thêm vào headerPanel
        headerPanel.add(titlePanel, BorderLayout.WEST);

        return headerPanel;
    }

    private JPanel createPanelChiTietVe() {
        JPanel panelChiTietVe = new JPanel();
        // Sử dụng GridBagLayout cho tính linh hoạt
        panelChiTietVe.setLayout(new GridBagLayout());

        // Tạo border với tiêu đề
        TitledBorder titledBorder = BorderFactory.createTitledBorder(null, "Chi tiết vé",
                TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION,
                new Font("Arial", Font.ITALIC, 14));
        panelChiTietVe.setBorder(titledBorder);

        // Thêm icon vào panel
        JLabel lblIcon = new JLabel(iconFactory.getIcon("detail", ICON_SIZE, ICON_SIZE));
        GridBagConstraints iconGbc = new GridBagConstraints();
        iconGbc.gridx = 0;
        iconGbc.gridy = 0;
        iconGbc.anchor = GridBagConstraints.WEST;
        iconGbc.insets = new Insets(5, 5, 5, 5);
        panelChiTietVe.add(lblIcon, iconGbc);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.weightx = 1.0; // Cho phép các thành phần kéo giãn theo chiều ngang

        // Tạo các JLabel và JTextField cho từng trường
        JLabel lblMaVe = new JLabel("Mã vé:", iconFactory.getIcon("ticket", ICON_SIZE, ICON_SIZE), JLabel.LEFT);
        lblMaVe.setFont(new Font("Arial", Font.BOLD, 13));
        lblMaVe.setIconTextGap(8);
        txtMaVe = createTextField();

        JLabel lblMaLich = new JLabel("Thông tin:", iconFactory.getIcon("info", ICON_SIZE, ICON_SIZE), JLabel.LEFT);
        lblMaLich.setFont(new Font("Arial", Font.BOLD, 13));
        lblMaLich.setIconTextGap(8);
        txtMaLich = createTextField();

        JLabel lblMaCho = new JLabel("Chỗ ngồi:", iconFactory.getIcon("seat", ICON_SIZE, ICON_SIZE), JLabel.LEFT);
        lblMaCho.setFont(new Font("Arial", Font.BOLD, 13));
        lblMaCho.setIconTextGap(8);
        txtMaCho = createTextField();

        JLabel lblTenKH = new JLabel("Tên KH:", iconFactory.getIcon("user", ICON_SIZE, ICON_SIZE), JLabel.LEFT);
        lblTenKH.setFont(new Font("Arial", Font.BOLD, 13));
        lblTenKH.setIconTextGap(8);
        txtTenKH = createTextField();

        JLabel lblGiayTo = new JLabel("Giấy tờ:", iconFactory.getIcon("id-card", ICON_SIZE, ICON_SIZE), JLabel.LEFT);
        lblGiayTo.setFont(new Font("Arial", Font.BOLD, 13));
        lblGiayTo.setIconTextGap(8);
        txtGiayTo = createTextField();

        JLabel lblNgayDi = new JLabel("Ngày đi:", iconFactory.getIcon("calendar", ICON_SIZE, ICON_SIZE), JLabel.LEFT);
        lblNgayDi.setFont(new Font("Arial", Font.BOLD, 13));
        lblNgayDi.setIconTextGap(8);
        txtNgayDi = createTextField();

        JLabel lblDoiTuong = new JLabel("Đối tượng:", iconFactory.getIcon("person", ICON_SIZE, ICON_SIZE), JLabel.LEFT);
        lblDoiTuong.setFont(new Font("Arial", Font.BOLD, 13));
        lblDoiTuong.setIconTextGap(8);
        txtDoiTuong = createTextField();

        JLabel lblGiaVe = new JLabel("Giá vé:", iconFactory.getIcon("money", ICON_SIZE, ICON_SIZE), JLabel.LEFT);
        lblGiaVe.setFont(new Font("Arial", Font.BOLD, 13));
        lblGiaVe.setIconTextGap(8);
        txtGiaVe = createTextField();

        JLabel lblTrangThai = new JLabel("Trạng thái:", iconFactory.getIcon("status", ICON_SIZE, ICON_SIZE), JLabel.LEFT);
        lblTrangThai.setFont(new Font("Arial", Font.BOLD, 13));
        lblTrangThai.setIconTextGap(8);
        txtTrangThai = createTextField();

        // Tạo button "In lại vé" với góc bo tròn
        RoundedButton btnInLaiVe = new RoundedButton("In lại vé", iconFactory.getWhiteIcon("print", BUTTON_ICON_SIZE, BUTTON_ICON_SIZE));
        btnInLaiVe.setFont(new Font("Arial", Font.BOLD, 13));
        btnInLaiVe.setBackground(MauXanh);
        btnInLaiVe.setForeground(Color.WHITE);
        btnInLaiVe.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnInLaiVe.setIconTextGap(8);
        btnInLaiVe.setPreferredSize(new Dimension(120, BUTTON_HEIGHT));

        // Thêm hiệu ứng hover cho button In lại vé
        addHoverEffect(btnInLaiVe);

        // Sắp xếp các thành phần vào GridBagLayout
        gbc.gridx = 0;
        gbc.gridy = 1; // Bắt đầu từ row 1 vì row 0 đã có icon
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0.3;
        panelChiTietVe.add(lblMaVe, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panelChiTietVe.add(txtMaVe, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.3;
        panelChiTietVe.add(lblMaLich, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panelChiTietVe.add(txtMaLich, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.3;
        panelChiTietVe.add(lblMaCho, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panelChiTietVe.add(txtMaCho, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0.3;
        panelChiTietVe.add(lblTenKH, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panelChiTietVe.add(txtTenKH, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0.3;
        panelChiTietVe.add(lblGiayTo, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panelChiTietVe.add(txtGiayTo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.weightx = 0.3;
        panelChiTietVe.add(lblNgayDi, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panelChiTietVe.add(txtNgayDi, gbc);

        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.weightx = 0.3;
        panelChiTietVe.add(lblDoiTuong, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panelChiTietVe.add(txtDoiTuong, gbc);

        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.weightx = 0.3;
        panelChiTietVe.add(lblGiaVe, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panelChiTietVe.add(txtGiaVe, gbc);

        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.weightx = 0.3;
        panelChiTietVe.add(lblTrangThai, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panelChiTietVe.add(txtTrangThai, gbc);

        // Đặt button "In lại vé" ở dưới cùng
        gbc.gridx = 0;
        gbc.gridy = 10;
        gbc.gridwidth = 2; // Chiếm hết chiều ngang của panel
        gbc.fill = GridBagConstraints.NONE; // Button không cần kéo giãn
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(10, 4, 4, 4); // Thêm khoảng cách phía trên button
        panelChiTietVe.add(btnInLaiVe, gbc);

        // Thêm thành phần để lấp đầy không gian còn lại
        gbc.gridx = 0;
        gbc.gridy = 11;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0; // Phần còn lại sẽ lấp đầy theo chiều dọc
        gbc.fill = GridBagConstraints.BOTH;
        panelChiTietVe.add(new JPanel(), gbc);


        btnInLaiVe.addActionListener(e -> {
            // Lấy mã vé từ txtMaVe
            String maVe = txtMaVe.getText().trim();

            // Kiểm tra nếu mã vé không rỗng
            if (!maVe.isEmpty()) {
                try {
                    // Sử dụng traCuuVeDAO thay vì tạo trực tiếp một đối tượng DAO_TraCuuVe
                    VeTau veTau = traCuuVeDAO.timVeTauTheoMa(maVe);

                    if (veTau == null) {
                        JOptionPane.showMessageDialog(this,
                                "Không tìm thấy vé với mã " + maVe,
                                "Lỗi", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    ChiTietHoaDon chiTietHoaDon = traCuuVeDAO.timChiTietHoaDonTheoMaVe(maVe);

                    if (chiTietHoaDon == null) {
                        JOptionPane.showMessageDialog(this,
                                "Không tìm thấy thông tin hóa đơn cho vé này!",
                                "Lỗi", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    List<ChiTietHoaDon> chiTietHoaDonList = new ArrayList<>();
                    chiTietHoaDonList.add(chiTietHoaDon);

                    // Tạo danh sách TicketDetails từ VeTau
                    List<TicketDetails> ticketDetailsList = new ArrayList<>();
                    TicketDetails ticketDetails = new TicketDetails(
                            veTau.getTenKhachHang(),
                            veTau.getDoiTuong(),
                            veTau.getGiayTo(),
                            veTau.getLichTrinhTau().getTau().getTuyenTau().getGaDi(),
                            veTau.getLichTrinhTau().getTau().getTuyenTau().getGaDen(),
                            veTau.getLichTrinhTau().getTau().getTenTau(),
                            veTau.getLichTrinhTau().getNgayDi(),
                            veTau.getLichTrinhTau().getGioDi(),
                            veTau.getChoNgoi().getToaTau().getTenToa(),
                            veTau.getChoNgoi().getTenCho(),
                            veTau.getGiaVe(),
                            chiTietHoaDon.getThanhTien()
                    );
                    ticketDetailsList.add(ticketDetails);

                    // Hiển thị dialog tiến trình
                    JDialog progressDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Đang in vé...", true);
                    progressDialog.setSize(300, 100);
                    progressDialog.setLayout(new FlowLayout(FlowLayout.CENTER));
                    progressDialog.add(new JLabel("Đang tạo file PDF, vui lòng đợi..."));
                    progressDialog.setLocationRelativeTo(this);

                    // Tạo và mở file PDF trong một luồng riêng để không làm treo giao diện
                    SwingWorker<Void, Void> worker = new SwingWorker<>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            try {
                                // Tạo tên file với timestamp để tránh ghi đè
                                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
                                String fileName = "VeTau.pdf";

                                // Gọi phương thức tạo PDF vé
                                TicketPDFGenerator.generateTicketPdf(fileName, ticketDetailsList, chiTietHoaDonList);

                                // Khi hoàn thành, mở file PDF
                                SwingUtilities.invokeLater(() -> {
                                    try {
                                        File pdfFile = new File(fileName);
                                        if (pdfFile.exists()) {
                                            progressDialog.dispose(); // Đóng dialog tiến trình

                                            // Kiểm tra xem Desktop có được hỗ trợ không
                                            if (Desktop.isDesktopSupported()) {
                                                Desktop.getDesktop().open(pdfFile);
                                                JOptionPane.showMessageDialog(TraCuuVePanel.this,
                                                        "Vé đã được in thành công!",
                                                        "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                                            } else {
                                                JOptionPane.showMessageDialog(TraCuuVePanel.this,
                                                        "File PDF đã được tạo tại: " + pdfFile.getAbsolutePath() +
                                                                "\nNhưng không thể mở tự động.",
                                                        "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                                            }
                                        } else {
                                            progressDialog.dispose(); // Đóng dialog tiến trình
                                            JOptionPane.showMessageDialog(TraCuuVePanel.this,
                                                    "Không thể tạo file PDF.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                                        }
                                    } catch (Exception ex) {
                                        progressDialog.dispose(); // Đóng dialog tiến trình
                                        ex.printStackTrace();
                                        JOptionPane.showMessageDialog(TraCuuVePanel.this,
                                                "Lỗi khi mở file PDF: " + ex.getMessage(),
                                                "Lỗi", JOptionPane.ERROR_MESSAGE);
                                    }
                                });
                            } catch (Exception ex) {
                                // Xử lý lỗi trong quá trình tạo PDF
                                SwingUtilities.invokeLater(() -> {
                                    progressDialog.dispose(); // Đóng dialog tiến trình
                                    ex.printStackTrace();
                                    JOptionPane.showMessageDialog(TraCuuVePanel.this,
                                            "Lỗi khi tạo file PDF: " + ex.getMessage(),
                                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                                });
                            }
                            return null;
                        }
                    };

                    // Bắt đầu thực hiện tác vụ và hiển thị dialog tiến trình
                    worker.execute();
                    progressDialog.setVisible(true);

                } catch (RemoteException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this,
                            "Lỗi kết nối RMI: " + ex.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this,
                            "Lỗi không xác định: " + ex.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một vé để in lại.",
                        "Thông báo", JOptionPane.WARNING_MESSAGE);
            }
        });

        return panelChiTietVe;
    }

    private JTextField createTextField() {
        JTextField textField = new JTextField();
        textField.setFont(new Font("Arial", Font.PLAIN, 13));
        textField.setPreferredSize(TEXT_FIELD_SIZE);
        return textField;
    }

    private JPanel createPanelTimTheoThoiGian() {
        // Tạo panel chính với GridBagLayout
        JPanel panelTimTheoThoiGian = new JPanel();
        panelTimTheoThoiGian.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.weightx = 1.0; // Cho phép kéo giãn theo chiều ngang

        // Title border
        TitledBorder titledBorder = BorderFactory.createTitledBorder(null, "Tìm theo thời gian",
                TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION,
                new Font("Arial", Font.ITALIC, 14));
        panelTimTheoThoiGian.setBorder(titledBorder);

        // Thêm icon vào panel
        JLabel lblIcon = new JLabel(iconFactory.getIcon("time-search", ICON_SIZE, ICON_SIZE));
        GridBagConstraints iconGbc = new GridBagConstraints();
        iconGbc.gridx = 0;
        iconGbc.gridy = 0;
        iconGbc.anchor = GridBagConstraints.WEST;
        iconGbc.insets = new Insets(5, 5, 5, 5);
        panelTimTheoThoiGian.add(lblIcon, iconGbc);

        // Hàng đầu tiên: Họ tên
        JLabel lblHoTen = new JLabel("Họ tên:", iconFactory.getIcon("user", ICON_SIZE, ICON_SIZE), JLabel.LEFT);
        lblHoTen.setFont(new Font("Arial", Font.PLAIN, 13));
        lblHoTen.setIconTextGap(8);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.2;
        panelTimTheoThoiGian.add(lblHoTen, gbc);

        JTextField txtHoTenTime = new JTextField();
        txtHoTenTime.setFont(new Font("Arial", Font.PLAIN, 13));
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 0.8;
        panelTimTheoThoiGian.add(txtHoTenTime, gbc);

        // Hàng thứ hai: Thời gian
        JLabel lblThoiGian = new JLabel("Thời gian:", iconFactory.getIcon("clock", ICON_SIZE, ICON_SIZE), JLabel.LEFT);
        lblThoiGian.setFont(new Font("Arial", Font.PLAIN, 13));
        lblThoiGian.setIconTextGap(8);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0.2;
        panelTimTheoThoiGian.add(lblThoiGian, gbc);

        // Calendar đầu tiên (JDateChooser)
        JDateChooser dateChooserFrom = new JDateChooser();
        dateChooserFrom.setFont(new Font("Arial", Font.PLAIN, 13));
        dateChooserFrom.setPreferredSize(new Dimension(0, FIELD_HEIGHT));
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0.35;
        panelTimTheoThoiGian.add(dateChooserFrom, gbc);

        // Label "đến"
        JLabel lblDen = new JLabel("đến");
        lblDen.setFont(new Font("Arial", Font.PLAIN, 13));
        lblDen.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.weightx = 0.1;
        panelTimTheoThoiGian.add(lblDen, gbc);

        // Calendar thứ hai (JDateChooser)
        JDateChooser dateChooserTo = new JDateChooser();
        dateChooserTo.setFont(new Font("Arial", Font.PLAIN, 13));
        dateChooserTo.setPreferredSize(new Dimension(0, FIELD_HEIGHT));
        gbc.gridx = 3;
        gbc.gridy = 2;
        gbc.weightx = 0.35;
        panelTimTheoThoiGian.add(dateChooserTo, gbc);

        // Nút Lọc với góc bo tròn
        RoundedButton btnLoc = new RoundedButton("Lọc", iconFactory.getWhiteIcon("filter", BUTTON_ICON_SIZE, BUTTON_ICON_SIZE));
        btnLoc.setFont(new Font("Arial", Font.BOLD, 13));
        btnLoc.setBackground(MauXanh);
        btnLoc.setForeground(Color.WHITE);
        btnLoc.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLoc.setIconTextGap(8);
        btnLoc.setPreferredSize(new Dimension(80, BUTTON_HEIGHT));

        // Thêm hiệu ứng hover
        addHoverEffect(btnLoc);

        gbc.gridx = 4;
        gbc.gridy = 1;
        gbc.gridheight = 2;
        gbc.weightx = 0.15;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(4, 8, 4, 4); // Thêm padding bên trái cho button
        panelTimTheoThoiGian.add(btnLoc, gbc);

        // Thêm sự kiện cho nút "Lọc"
        // Trong phương thức createPanelTimTheoThoiGian(), thay phần xử lý sự kiện btnLoc
        btnLoc.addActionListener(e -> {
            // Lấy giá trị từ các trường nhập liệu
            String hoTen = txtHoTenTime.getText().trim();
            Date ngayDiFrom = dateChooserFrom.getDate();
            Date ngayDiTo = dateChooserTo.getDate();

            // Kiểm tra dữ liệu nhập vào
            if (hoTen.isEmpty() || ngayDiFrom == null || ngayDiTo == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Chuyển đổi java.util.Date sang java.time.LocalDate
                LocalDate localDateFrom = ngayDiFrom.toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();

                LocalDate localDateTo = ngayDiTo.toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();

                // Tìm kiếm vé theo tên và thời gian
                List<VeTau> ketQua = traCuuVeDAO.timVeTauTheoTenKHVaThoiGian(
                        hoTen, localDateFrom, localDateTo);

                if (ketQua != null && !ketQua.isEmpty()) {
                    // Hiển thị kết quả tìm kiếm
                    hienThiKetQuaTimKiem(ketQua);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Không tìm thấy vé trong khoảng thời gian này!",
                            "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    tableModel.setRowCount(0); // Xóa dữ liệu cũ trong bảng
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Lỗi khi tìm kiếm: " + ex.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panelTimTheoThoiGian;
    }

    /**
     * Hiển thị danh sách vé tìm được lên bảng kết quả
     * @param danhSachVe Danh sách vé cần hiển thị
     */
    private void hienThiKetQuaTimKiem(List<VeTau> danhSachVe) {
        // Xóa dữ liệu cũ trong bảng
        tableModel.setRowCount(0);

        // Thêm dữ liệu mới vào bảng
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        for (VeTau veTau : danhSachVe) {
            Object[] rowData = {
                    veTau.getMaVe(),
                    veTau.getTenKhachHang(),
                    veTau.getGiayTo(),
                    veTau.getNgayDi().format(formatter),
                    veTau.getDoiTuong(),
                    String.format("%,.0f", veTau.getGiaVe()),
                    veTau.getTrangThai().toString()
            };
            tableModel.addRow(rowData);
        }

        // Cập nhật thông tin chi tiết nếu có kết quả
        if (!danhSachVe.isEmpty()) {
            capNhatThongTinChiTiet(danhSachVe.get(0));
        }

        // Thông báo kết quả tìm kiếm
        JOptionPane.showMessageDialog(this,
                "Tìm thấy " + danhSachVe.size() + " kết quả.",
                "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Cập nhật thông tin chi tiết của một vé trên panel bên phải
     * @param veTau Đối tượng vé cần hiển thị chi tiết
     */
    private void capNhatThongTinChiTiet(VeTau veTau) {
        if (veTau == null) return;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        txtMaVe.setText(veTau.getMaVe());

        // Tạo thông tin lịch trình
        String thongTinLichTrinh = "";
        if (veTau.getLichTrinhTau() != null) {
            thongTinLichTrinh = "Tàu " + veTau.getLichTrinhTau().getTau().getTenTau() + ", TG: " +
                    veTau.getLichTrinhTau().getGioDi().toString();

            if (veTau.getLichTrinhTau().getTau().getTuyenTau() != null) {
                thongTinLichTrinh += ", " + veTau.getLichTrinhTau().getTau().getTuyenTau().getTenTuyen();
            }
        }
        txtMaLich.setText(thongTinLichTrinh);

        // Thông tin chỗ ngồi
        if (veTau.getChoNgoi() != null) {
            txtMaCho.setText(veTau.getChoNgoi().getTenCho());
        } else {
            txtMaCho.setText("");
        }

        txtTenKH.setText(veTau.getTenKhachHang());
        txtGiayTo.setText(veTau.getGiayTo());
        txtNgayDi.setText(veTau.getNgayDi().format(formatter));
        txtDoiTuong.setText(veTau.getDoiTuong());
        txtGiaVe.setText(String.format("%,.0f", veTau.getGiaVe()));
        txtTrangThai.setText(veTau.getTrangThai().toString());
    }

    private JPanel createPanelTimChitiet() {
        // Tạo panel chính với GridBagLayout
        JPanel panelTimChitiet = new JPanel();
        panelTimChitiet.setLayout(new GridBagLayout());

        // Tạo border
        TitledBorder titledBorder = BorderFactory.createTitledBorder(null, "Tìm chi tiết",
                TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION,
                new Font("Arial", Font.BOLD, 14));
        panelTimChitiet.setBorder(titledBorder);

        // Thêm icon vào panel
        JLabel lblIcon = new JLabel(iconFactory.getIcon("search-detail", ICON_SIZE, ICON_SIZE));
        GridBagConstraints iconGbc = new GridBagConstraints();
        iconGbc.gridx = 0;
        iconGbc.gridy = 0;
        iconGbc.anchor = GridBagConstraints.WEST;
        iconGbc.insets = new Insets(5, 5, 5, 5);
        panelTimChitiet.add(lblIcon, iconGbc);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.weightx = 1.0;

        // Tên khách hàng
        JLabel lblTenKhachHang = new JLabel("Tên KH:", iconFactory.getIcon("user", ICON_SIZE, ICON_SIZE), JLabel.LEFT);
        lblTenKhachHang.setFont(new Font("Arial", Font.PLAIN, 13));
        lblTenKhachHang.setIconTextGap(8);
        gbc.gridx = 0;
        gbc.gridy = 1; // Bắt đầu từ row 1 vì row 0 đã có icon
        gbc.weightx = 0.3;
        panelTimChitiet.add(lblTenKhachHang, gbc);

        JTextField txtTenKhachHang = new JTextField();
        txtTenKhachHang.setFont(new Font("Arial", Font.PLAIN, 13));
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.7;
        panelTimChitiet.add(txtTenKhachHang, gbc);

        // Đối tượng
        JLabel lblDoiTuong = new JLabel("Đối tượng:", iconFactory.getIcon("person", ICON_SIZE, ICON_SIZE), JLabel.LEFT);
        lblDoiTuong.setFont(new Font("Arial", Font.PLAIN, 13));
        lblDoiTuong.setIconTextGap(8);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.3;
        panelTimChitiet.add(lblDoiTuong, gbc);

        JComboBox<String> comboDoiTuong = new JComboBox<>(new String[]{"Sinh viên", "Người lớn", "Trẻ em"});
        comboDoiTuong.setFont(new Font("Arial", Font.PLAIN, 13));
        comboDoiTuong.setPreferredSize(new Dimension(0, FIELD_HEIGHT));
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 0.7;
        panelTimChitiet.add(comboDoiTuong, gbc);

        // Giấy tờ
        JLabel lblGiayTo = new JLabel("Giấy tờ:", iconFactory.getIcon("id-card", ICON_SIZE, ICON_SIZE), JLabel.LEFT);
        lblGiayTo.setFont(new Font("Arial", Font.PLAIN, 13));
        lblGiayTo.setIconTextGap(8);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.3;
        panelTimChitiet.add(lblGiayTo, gbc);

        JTextField txtGiayToField = new JTextField();
        txtGiayToField.setFont(new Font("Arial", Font.PLAIN, 13));
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.weightx = 0.7;
        panelTimChitiet.add(txtGiayToField, gbc);

        // Ngày đi
        JLabel lblNgayDi = new JLabel("Ngày đi:", iconFactory.getIcon("calendar", ICON_SIZE, ICON_SIZE), JLabel.LEFT);
        lblNgayDi.setFont(new Font("Arial", Font.PLAIN, 13));
        lblNgayDi.setIconTextGap(8);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0.3;
        panelTimChitiet.add(lblNgayDi, gbc);

        JTextField txtNgayDiField = new JTextField();
        txtNgayDiField.setFont(new Font("Arial", Font.PLAIN, 13));
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.weightx = 0.7;
        panelTimChitiet.add(txtNgayDiField, gbc);

        // Mã chỗ ngồi
        JLabel lblMaChoNgoi = new JLabel("Mã chỗ ngồi:", iconFactory.getIcon("seat", ICON_SIZE, ICON_SIZE), JLabel.LEFT);
        lblMaChoNgoi.setFont(new Font("Arial", Font.PLAIN, 13));
        lblMaChoNgoi.setIconTextGap(8);
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0.3;
        panelTimChitiet.add(lblMaChoNgoi, gbc);

        JTextField txtMaChoNgoi = new JTextField();
        txtMaChoNgoi.setFont(new Font("Arial", Font.PLAIN, 13));
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.weightx = 0.7;
        panelTimChitiet.add(txtMaChoNgoi, gbc);

        // Button Tìm vé với góc bo tròn
        RoundedButton btnTimVeChiTiet = new RoundedButton("Tìm vé", iconFactory.getWhiteIcon("search", BUTTON_ICON_SIZE, BUTTON_ICON_SIZE));
        btnTimVeChiTiet.setFont(new Font("Arial", Font.BOLD, 13));
        btnTimVeChiTiet.setBackground(MauXanh);
        btnTimVeChiTiet.setForeground(Color.WHITE);
        btnTimVeChiTiet.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnTimVeChiTiet.setIconTextGap(8);
        btnTimVeChiTiet.setPreferredSize(new Dimension(100, BUTTON_HEIGHT));

        // Thêm hiệu ứng hover
        addHoverEffect(btnTimVeChiTiet);

        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(8, 4, 4, 4); // Thêm khoảng cách phía trên button
        panelTimChitiet.add(btnTimVeChiTiet, gbc);

        // Thêm sự kiện cho nút "Tìm vé"
        // Trong phương thức createPanelTimChitiet(), thay phần xử lý sự kiện btnTimVeChiTiet
        btnTimVeChiTiet.addActionListener(e -> {
            // Lấy giá trị từ các trường nhập liệu
            String tenKhachHang = txtTenKhachHang.getText().trim();
            String giayTo = txtGiayToField.getText().trim();
            String ngayDiStr = txtNgayDiField.getText().trim();
            String maChoNgoi = txtMaChoNgoi.getText().trim();
            String doiTuong = (String) comboDoiTuong.getSelectedItem();

            // Kiểm tra dữ liệu nhập vào
            if (tenKhachHang.isEmpty() || giayTo.isEmpty() || ngayDiStr.isEmpty() || maChoNgoi.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng điền đầy đủ thông tin!",
                        "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                // Chuyển đổi chuỗi ngày thành LocalDate
                LocalDate ngayDi = null;
                try {
                    // Thử nhiều định dạng ngày khác nhau
                    DateTimeFormatter[] formatters = {
                            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                            DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    };

                    for (DateTimeFormatter formatter : formatters) {
                        try {
                            ngayDi = LocalDate.parse(ngayDiStr, formatter);
                            break;
                        } catch (DateTimeParseException ex) {
                            // Thử định dạng tiếp theo
                        }
                    }

                    if (ngayDi == null) {
                        throw new DateTimeParseException("Không thể phân tích ngày", ngayDiStr, 0);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "Định dạng ngày không hợp lệ. Vui lòng nhập theo định dạng dd-MM-yyyy!",
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Tìm kiếm vé theo chi tiết
                List<VeTau> ketQua = traCuuVeDAO.timVeTauTheoChitiet(
                        tenKhachHang, giayTo, ngayDi, maChoNgoi, doiTuong);

                if (ketQua != null && !ketQua.isEmpty()) {
                    // Hiển thị kết quả tìm kiếm
                    hienThiKetQuaTimKiem(ketQua);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Không tìm thấy vé với thông tin đã nhập!",
                            "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    tableModel.setRowCount(0); // Xóa dữ liệu cũ trong bảng
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Lỗi khi tìm kiếm: " + ex.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panelTimChitiet;
    }

    private JPanel createPanelTimNhanh() {
        // Tạo panel chính với layout GridBagLayout
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        // Tạo border
        TitledBorder titledBorder = BorderFactory.createTitledBorder(null, "Tìm nhanh",
                TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION,
                new Font("Arial", Font.BOLD, 14));
        panel.setBorder(titledBorder);

        // Thêm icon vào panel
        JLabel lblIcon = new JLabel(iconFactory.getIcon("quick-search", ICON_SIZE, ICON_SIZE));
        GridBagConstraints iconGbc = new GridBagConstraints();
        iconGbc.gridx = 0;
        iconGbc.gridy = 0;
        iconGbc.anchor = GridBagConstraints.NORTHWEST;
        iconGbc.insets = new Insets(5, 5, 5, 5);
        panel.add(lblIcon, iconGbc);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 4, 4, 4);
        gbc.weightx = 1.0;

        // Panel radio buttons
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weighty = 0.0;
        gbc.weightx = 0.25;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(createRadioPanel(), gbc);

        // Text field tìm kiếm
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        txtHoTen = new JTextField();
        txtHoTen.setFont(new Font("Arial", Font.PLAIN, 13));
        panel.add(txtHoTen, gbc);

        // Panel chứa các buttons
        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 0, 8));

        // Button Tìm vé với góc bo tròn
        btnTimVe = new RoundedButton("Tìm vé", iconFactory.getWhiteIcon("search", BUTTON_ICON_SIZE, BUTTON_ICON_SIZE));
        btnTimVe.setFont(new Font("Arial", Font.BOLD, 13));
        btnTimVe.setBackground(MauXanh);
        btnTimVe.setForeground(Color.WHITE);
        btnTimVe.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnTimVe.setIconTextGap(8);
        btnTimVe.setPreferredSize(new Dimension(100, BUTTON_HEIGHT-2));

        // Thêm hiệu ứng hover
        addHoverEffect(btnTimVe);
        buttonPanel.add(btnTimVe);

        // Button Quét mã QR với góc bo tròn
        RoundedButton btnQuetQR = new RoundedButton("Quét mã QR", iconFactory.getWhiteIcon("qrcode", BUTTON_ICON_SIZE, BUTTON_ICON_SIZE));
        btnQuetQR.setFont(new Font("Arial", Font.BOLD, 13));
        btnQuetQR.setBackground(MauXanh);
        btnQuetQR.setForeground(Color.WHITE);
        btnQuetQR.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnQuetQR.setIconTextGap(8);
        btnQuetQR.setPreferredSize(new Dimension(100, BUTTON_HEIGHT-2));

        // Thêm hiệu ứng hover
        addHoverEffect(btnQuetQR);
        buttonPanel.add(btnQuetQR);

        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.weightx = 0.25;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(8, 8, 4, 4); // Thêm khoảng cách bên trái
        panel.add(buttonPanel, gbc);

        // Thêm sự kiện cho button Quét mã QR
        // Thêm sự kiện cho button Quét mã QR
        btnQuetQR.addActionListener(e -> {
                quetQRTuWebcam();
        });

        return panel;
    }

    /**
     * Quét mã QR từ webcam
     * Lưu ý: Phương thức này yêu cầu thư viện webcam-capture
     */
    private void quetQRTuWebcam() {
        try {
            // Hiển thị dialog chờ khi đang khởi tạo webcam
            JDialog loadingDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Đang khởi tạo webcam...", true);
            loadingDialog.setSize(250, 100);
            loadingDialog.setLayout(new FlowLayout(FlowLayout.CENTER));
            loadingDialog.add(new JLabel("Đang khởi tạo webcam, vui lòng chờ..."));
            loadingDialog.setLocationRelativeTo(this);

            // Khởi tạo webcam trong một luồng riêng
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    try {
                        // Khởi tạo webcam
                        Webcam webcam = Webcam.getDefault();
                        if (webcam == null) {
                            throw new Exception("Không tìm thấy webcam trên thiết bị");
                        }

                        // Đặt kích thước hình ảnh webcam
                        webcam.setViewSize(new Dimension(640, 480));
                        webcam.open();

                        // Tạo và hiển thị dialog quét QR
                        JDialog qrDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(TraCuuVePanel.this), "Quét mã QR", true);
                        qrDialog.setSize(700, 550);
                        qrDialog.setLayout(new BorderLayout());

                        // Panel hiển thị webcam
                        WebcamPanel webcamPanel = new WebcamPanel(webcam);
                        webcamPanel.setFPSDisplayed(true);
                        webcamPanel.setMirrored(false);
                        qrDialog.add(webcamPanel, BorderLayout.CENTER);

                        // Panel nút điều khiển
                        JPanel controlPanel = new JPanel();
                        JButton cancelButton = new JButton("Hủy");
                        cancelButton.addActionListener(e -> qrDialog.dispose());
                        controlPanel.add(cancelButton);
                        qrDialog.add(controlPanel, BorderLayout.SOUTH);

                        // Xử lý khi đóng dialog
                        qrDialog.addWindowListener(new WindowAdapter() {
                            @Override
                            public void windowClosing(WindowEvent e) {
                                if (webcam != null && webcam.isOpen()) {
                                    webcam.close();
                                }
                            }
                        });

                        // Đóng dialog loading
                        SwingUtilities.invokeLater(() -> loadingDialog.dispose());

                        // Khởi tạo luồng quét QR
                        final AtomicBoolean qrFound = new AtomicBoolean(false);
                        Thread qrScanThread = new Thread(() -> {
                            try {
                                while (!qrFound.get() && webcam.isOpen()) {
                                    BufferedImage image = webcam.getImage();
                                    if (image != null) {
                                        try {
                                            // Quét mã QR từ hình ảnh
                                            LuminanceSource source = new BufferedImageLuminanceSource(image);
                                            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                                            Result result = new MultiFormatReader().decode(bitmap);
                                            if (result != null && result.getText() != null) {
                                                // Tìm thấy mã QR
                                                final String qrText = result.getText();
                                                qrFound.set(true);

                                                // Xử lý mã QR tìm được trên EDT
                                                SwingUtilities.invokeLater(() -> {
                                                    // Đóng webcam và dialog
                                                    webcam.close();
                                                    qrDialog.dispose();

                                                    // Cập nhật UI với mã QR tìm được
                                                    txtHoTen.setText(qrText.trim());
                                                    radioMaVe.setSelected(true); // Chọn radio button mã vé

                                                    // Thông báo kết quả quét
                                                    JOptionPane.showMessageDialog(TraCuuVePanel.this,
                                                            "Đã quét được mã: " + qrText,
                                                            "Thông báo", JOptionPane.INFORMATION_MESSAGE);

                                                    // Tự động kích hoạt tìm kiếm
                                                    btnTimVe.doClick();
                                                });

                                                break;
                                            }
                                        } catch (NotFoundException ignore) {
                                            // Không tìm thấy QR trong frame hiện tại, tiếp tục tìm
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    // Tạm dừng để giảm tải CPU
                                    Thread.sleep(200);
                                }
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                        qrScanThread.setDaemon(true);
                        qrScanThread.start();

                        // Hiển thị dialog quét QR
                        qrDialog.setLocationRelativeTo(TraCuuVePanel.this);
                        qrDialog.setVisible(true);

                        // Khi dialog đóng, đảm bảo webcam cũng đóng
                        if (webcam.isOpen()) {
                            webcam.close();
                        }

                        // Đảm bảo luồng quét dừng lại
                        if (qrScanThread.isAlive()) {
                            qrScanThread.interrupt();
                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        SwingUtilities.invokeLater(() -> {
                            loadingDialog.dispose();
                            JOptionPane.showMessageDialog(TraCuuVePanel.this,
                                    "Không thể khởi tạo webcam: " + ex.getMessage(),
                                    "Lỗi", JOptionPane.ERROR_MESSAGE);
                        });
                    }
                    return null;
                }
            };

            worker.execute();
            loadingDialog.setVisible(true); // Hiển thị dialog chờ (sẽ tự đóng khi webcam khởi tạo xong)
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi khởi tạo webcam: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
    private void xuLyNoiDungQR(String qrContent) {
        // Kiểm tra định dạng nội dung
        try {
            // Kiểm tra trường hợp nội dung là mã vé
            if (qrContent.startsWith("VT") || qrContent.matches("[A-Z0-9]+")) {
                // Nội dung QR là mã vé
                // Điền mã vào ô tìm kiếm
                txtHoTen.setText(qrContent);
                // Chọn radio button mã vé
                radioMaVe.setSelected(true);
                // Tự động tìm kiếm
                btnTimVe.doClick();
                return;
            }

            // Kiểm tra trường hợp là thông tin khác (JSON, định dạng riêng,...)
            // Giả sử định dạng: MaVe:VT001,GiayTo:123456789,Ten:NguyenVanA
            if (qrContent.contains(":") && qrContent.contains(",")) {
                // Phân tích dữ liệu
                Map<String, String> data = new HashMap<>();
                String[] parts = qrContent.split(",");

                for (String part : parts) {
                    String[] keyValue = part.split(":");
                    if (keyValue.length == 2) {
                        data.put(keyValue[0].trim(), keyValue[1].trim());
                    }
                }

                // Kiểm tra loại dữ liệu và xử lý tương ứng
                if (data.containsKey("MaVe")) {
                    // Tìm theo mã vé
                    txtHoTen.setText(data.get("MaVe"));
                    radioMaVe.setSelected(true);
                    btnTimVe.doClick();
                } else if (data.containsKey("GiayTo")) {
                    // Tìm theo giấy tờ
                    txtHoTen.setText(data.get("GiayTo"));
                    radioGiayTo.setSelected(true);
                    btnTimVe.doClick();
                } else if (data.containsKey("Ten")) {
                    // Tìm theo tên
                    txtHoTen.setText(data.get("Ten"));
                    radioHoTen.setSelected(true);
                    btnTimVe.doClick();
                } else {
                    // Không nhận dạng được định dạng
                    throw new Exception("Không nhận dạng được định dạng dữ liệu QR");
                }

                return;
            }

            // Nếu không phù hợp với các định dạng đã biết
            throw new Exception("Định dạng mã QR không được hỗ trợ");

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Không thể xử lý nội dung mã QR: " + e.getMessage() + "\n" +
                            "Nội dung quét được: " + qrContent,
                    "Lỗi", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Giải mã nội dung từ ảnh chứa mã QR
     * @param bufferedImage Ảnh chứa mã QR
     * @return Nội dung mã QR hoặc null nếu không thể đọc
     */
    private String decodeQRCode(BufferedImage bufferedImage) {
        try {
            // Chuyển đổi ảnh sang dạng phù hợp để xử lý
            LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            // Cấu hình các loại mã có thể đọc
            MultiFormatReader reader = new MultiFormatReader();
            Map<DecodeHintType, Object> hints = new HashMap<>();
            hints.put(DecodeHintType.POSSIBLE_FORMATS, Arrays.asList(BarcodeFormat.QR_CODE));

            // Giải mã
            Result result = reader.decode(bitmap, hints);
            return result.getText();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private JPanel createRadioPanel() {
        // Tạo một panel riêng cho các radio button
        JPanel radioPanel = new JPanel();
        radioPanel.setLayout(new GridLayout(3, 1, 0, 8)); // 3 hàng, 1 cột, khoảng cách 8px giữa các hàng

        // Tạo các radio button với icon
        radioMaVe = new JRadioButton("Mã vé", iconFactory.getIcon("ticket", ICON_SIZE, ICON_SIZE));
        radioGiayTo = new JRadioButton("Giấy tờ", iconFactory.getIcon("id-card", ICON_SIZE, ICON_SIZE));
        radioHoTen = new JRadioButton("Họ tên", iconFactory.getIcon("user", ICON_SIZE, ICON_SIZE));

        // Cập nhật kích thước font cho các radio button và khoảng cách icon
        radioMaVe.setFont(new Font("Arial", Font.PLAIN, 13));
        radioMaVe.setIconTextGap(8);
        radioGiayTo.setFont(new Font("Arial", Font.PLAIN, 13));
        radioGiayTo.setIconTextGap(8);
        radioHoTen.setFont(new Font("Arial", Font.PLAIN, 13));
        radioHoTen.setIconTextGap(8);

        // Mặc định chọn radioMaVe
        radioMaVe.setSelected(true);

        // Nhóm các radio button lại với nhau
        buttonGroup = new ButtonGroup();
        buttonGroup.add(radioMaVe);
        buttonGroup.add(radioGiayTo);
        buttonGroup.add(radioHoTen);

        // Thêm các radio button vào panel
        radioPanel.add(radioMaVe);
        radioPanel.add(radioGiayTo);
        radioPanel.add(radioHoTen);

        return radioPanel;
    }

    private void addSearchButtonListener() {
        btnTimVe.addActionListener(e -> {
            String searchText = txtHoTen.getText().trim();

            if (searchText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập thông tin tìm kiếm!",
                        "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                List<VeTau> ketQua = null;

                if (radioMaVe.isSelected()) {
                    ketQua = traCuuVeDAO.timDanhSachVeTauTheoMa(searchText);
                    if (ketQua.isEmpty()) {
                        // Thử tìm một vé duy nhất
                        VeTau veTau = traCuuVeDAO.timVeTauTheoMa(searchText);
                        if (veTau != null) {
                            ketQua = new ArrayList<>();
                            ketQua.add(veTau);
                        }
                    }
                } else if (radioGiayTo.isSelected()) {
                    ketQua = traCuuVeDAO.timVeTauTheoGiayTo(searchText);
                } else if (radioHoTen.isSelected()) {
                    ketQua = traCuuVeDAO.timVeTauTheoTenKH(searchText);
                }

                if (ketQua != null && !ketQua.isEmpty()) {

                    hienThiKetQuaTimKiem(ketQua);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Không tìm thấy vé với thông tin đã nhập!",
                            "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    tableModel.setRowCount(0); // Xóa dữ liệu cũ trong bảng
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Lỗi khi tìm kiếm: " + ex.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    // Hiển thị dữ liệu mẫu trong bảng để demo
    private void displaySampleData() {
        tableModel.setRowCount(0);

        // Dữ liệu mẫu
        Object[][] sampleData = {
                {"VT001", "Nguyễn Văn A", "123456789", "2023-05-15", "Người lớn", "500000", "Đã thanh toán"},
                {"VT002", "Trần Thị B", "987654321", "2023-05-20", "Sinh viên", "400000", "Đã thanh toán"},
                {"VT003", "Lê Văn C", "456789123", "2023-06-10", "Trẻ em", "300000", "Chưa thanh toán"}
        };

        for (Object[] row : sampleData) {
            tableModel.addRow(row);
        }

        // Cập nhật thông tin trong panel chi tiết
        if (tableModel.getRowCount() > 0) {
            txtMaVe.setText((String) tableModel.getValueAt(0, 0));
            txtMaLich.setText("Tàu SE1, TG: 08:00 - 12:00");
            txtMaCho.setText("A12");
            txtTenKH.setText((String) tableModel.getValueAt(0, 1));
            txtGiayTo.setText((String) tableModel.getValueAt(0, 2));
            txtNgayDi.setText((String) tableModel.getValueAt(0, 3));
            txtDoiTuong.setText((String) tableModel.getValueAt(0, 4));
            txtGiaVe.setText((String) tableModel.getValueAt(0, 5));
            txtTrangThai.setText((String) tableModel.getValueAt(0, 6));
        }

        // Thêm listener cho bảng để cập nhật panel chi tiết khi click vào một dòng
        // Thêm sự kiện này trong phương thức khởi tạo sau khi tạo bảng
// Thêm listener cho bảng để cập nhật panel chi tiết khi click vào một dòng
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    try {
                        String maVe = (String) tableModel.getValueAt(row, 0);
                        VeTau veTau = traCuuVeDAO.timVeTauTheoMa(maVe);
                        if (veTau != null) {
                            capNhatThongTinChiTiet(veTau);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(TraCuuVePanel.this,
                                "Lỗi khi hiển thị chi tiết vé: " + ex.getMessage(),
                                "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
    }

    // Thêm hiệu ứng hover cho các buttons với màu mới
    private void addHoverEffect(AbstractButton button) {
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(MauXanh);
            }
        });
    }

    // Class tạo button có góc bo tròn
    private class RoundedButton extends JButton {
        private final int arcWidth = 15;
        private final int arcHeight = 15;

        public RoundedButton(String text) {
            super(text);
            setupButton();
        }

        public RoundedButton(String text, Icon icon) {
            super(text, icon);
            setupButton();
        }

        private void setupButton() {
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (getModel().isPressed()) {
                g2.setColor(getBackground().darker());
            } else if (getModel().isRollover()) {
                g2.setColor(hoverColor);
            } else {
                g2.setColor(getBackground());
            }

            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), arcWidth, arcHeight));

            super.paintComponent(g2);
            g2.dispose();
        }

        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground().darker());
            g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, arcWidth, arcHeight));
            g2.dispose();
        }
    }
}