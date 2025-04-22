package guiClient;

import dao.KhuyenMaiDAO;
import model.KhuyenMai;
import model.TrangThaiKM;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panel hiển thị khuyến mãi theo dạng lịch
 */
public class PromotionCalendarPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(PromotionCalendarPanel.class.getName());

    // Các biến thành viên
    private LocalDate currentMonth; // Tháng hiện tại đang hiển thị
    private JPanel calendarGridPanel; // Panel chứa các ô ngày trong lịch
    private JLabel monthLabel; // Nhãn hiển thị tháng/năm
    private JButton prevMonthButton; // Nút chuyển tháng trước
    private JButton nextMonthButton; // Nút chuyển tháng sau
    private Map<LocalDate, List<KhuyenMai>> promotionsByDate; // Lưu các khuyến mãi theo ngày
    private KhuyenMaiDAO khuyenMaiDAO; // DAO để truy vấn dữ liệu
    private JPanel selectedDayPanel; // Panel ngày được chọn
    private LocalDate selectedDate; // Ngày được chọn
    private DayPanelClickListener dayPanelClickListener; // Listener cho sự kiện click vào panel ngày

    // Hằng số cho màu sắc theo trạng thái
    private static final Map<String, Color> STATUS_COLORS = new HashMap<>();

    // Cập nhật bảng màu trong static block
    static {
        STATUS_COLORS.put("Đang diễn ra", new Color(46, 204, 113)); // Xanh lá
        STATUS_COLORS.put("Hết hạn", new Color(231, 76, 60)); // Đỏ
        STATUS_COLORS.put("default", new Color(110, 110, 110)); // Xám (Màu mặc định)
    }

    /**
     * Constructor cho PromotionCalendarPanel
     *
     * @param khuyenMaiDAO DAO để truy vấn dữ liệu khuyến mãi
     */
    public PromotionCalendarPanel(KhuyenMaiDAO khuyenMaiDAO) {
        this.khuyenMaiDAO = khuyenMaiDAO;
        this.currentMonth = LocalDate.now();
        this.promotionsByDate = new HashMap<>();
        this.selectedDate = LocalDate.now();

        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Khởi tạo giao diện
        initializeUI();

        if (khuyenMaiDAO != null) {
            // Tạo lịch và tải dữ liệu ban đầu
            updateCalendarView(); // Thêm dòng này để tạo lịch trước khi load dữ liệu
        } else {
            // Hiển thị thông báo lỗi nếu DAO là null
            JLabel errorLabel = new JLabel("Không thể kết nối đến server để hiển thị lịch");
            errorLabel.setHorizontalAlignment(JLabel.CENTER);
            errorLabel.setForeground(Color.RED);
            add(errorLabel, BorderLayout.CENTER);
        }
    }

    /**
     * Khởi tạo các thành phần giao diện
     */
    private void initializeUI() {
        // Panel cho điều khiển lịch (tháng trước, tháng sau) với gradient background
        JPanel controlPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Create gradient from top to bottom
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(41, 128, 185, 220),
                    0, getHeight(), new Color(52, 152, 219, 220)
                );
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
            }
        };
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // Tạo panel chứa nút điều hướng
        JPanel navigationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        navigationPanel.setOpaque(false);

        // Nút tháng trước với styling
        prevMonthButton = new JButton("◀");
        prevMonthButton.setFont(new Font("Arial", Font.BOLD, 16));
        prevMonthButton.setForeground(Color.WHITE);
        prevMonthButton.setBackground(new Color(41, 128, 185));
        prevMonthButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        prevMonthButton.setFocusPainted(false);
        prevMonthButton.setBorderPainted(false);
        prevMonthButton.setContentAreaFilled(false);
        prevMonthButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        prevMonthButton.addActionListener(e -> {
            currentMonth = currentMonth.minusMonths(1);
            updateCalendarView();
        });

        // Add hover effect
        prevMonthButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                prevMonthButton.setForeground(new Color(255, 255, 255, 200));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                prevMonthButton.setForeground(Color.WHITE);
            }
        });

        // Nút tháng sau với styling
        nextMonthButton = new JButton("▶");
        nextMonthButton.setFont(new Font("Arial", Font.BOLD, 16));
        nextMonthButton.setForeground(Color.WHITE);
        nextMonthButton.setBackground(new Color(41, 128, 185));
        nextMonthButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        nextMonthButton.setFocusPainted(false);
        nextMonthButton.setBorderPainted(false);
        nextMonthButton.setContentAreaFilled(false);
        nextMonthButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        nextMonthButton.addActionListener(e -> {
            currentMonth = currentMonth.plusMonths(1);
            updateCalendarView();
        });

        // Add hover effect
        nextMonthButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                nextMonthButton.setForeground(new Color(255, 255, 255, 200));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                nextMonthButton.setForeground(Color.WHITE);
            }
        });

        // Label hiển thị tháng/năm hiện tại với styling
        monthLabel = new JLabel("", JLabel.CENTER);
        monthLabel.setFont(new Font("Arial", Font.BOLD, 18));
        monthLabel.setForeground(Color.WHITE);
        updateMonthLabel();

        // Thêm các thành phần vào panel điều hướng
        navigationPanel.add(prevMonthButton);
        navigationPanel.add(monthLabel);
        navigationPanel.add(nextMonthButton);
        controlPanel.add(navigationPanel, BorderLayout.CENTER);

        // Tạo panel chứa lịch (grid) với background và padding
        JPanel calendarContainer = new JPanel(new BorderLayout());
        calendarContainer.setBackground(new Color(250, 250, 255));
        calendarContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        calendarGridPanel = new JPanel(new GridLayout(0, 7, 8, 8));
        calendarGridPanel.setBackground(new Color(250, 250, 255));

        // Panel chứa lịch bọc bởi scroll pane với styling
        JScrollPane calendarScrollPane = new JScrollPane(calendarGridPanel);
        calendarScrollPane.setBorder(BorderFactory.createEmptyBorder());
        calendarScrollPane.setBackground(new Color(250, 250, 255));
        calendarScrollPane.getViewport().setBackground(new Color(250, 250, 255));

        // Style the scrollbars
        calendarScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        calendarScrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 10));
        calendarScrollPane.getVerticalScrollBar().setBackground(Color.WHITE);
        calendarScrollPane.getHorizontalScrollBar().setBackground(Color.WHITE);

        calendarContainer.add(calendarScrollPane, BorderLayout.CENTER);

        // Thêm vào panel chính
        add(controlPanel, BorderLayout.NORTH);
        add(calendarContainer, BorderLayout.CENTER);

        // Legend panel để hiển thị các màu tương ứng với trạng thái
        JPanel legendPanel = createLegendPanel();
        add(legendPanel, BorderLayout.SOUTH);
    }

    /**
     * Tạo panel chú thích màu sắc cho các trạng thái với thiết kế hiện đại
     */
    private JPanel createLegendPanel() {
        // Create a panel with a modern, rounded border and subtle shadow effect
        JPanel legendPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw a subtle shadow
                g2d.setColor(new Color(0, 0, 0, 20));
                g2d.fillRoundRect(3, 3, getWidth() - 6, getHeight() - 6, 15, 15);

                // Draw the main background
                g2d.setColor(new Color(250, 250, 255));
                g2d.fillRoundRect(0, 0, getWidth() - 3, getHeight() - 3, 15, 15);
            }
        };

        // Create a titled border with custom font and color
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createEmptyBorder(),
            "Chú thích trạng thái",
            TitledBorder.DEFAULT_JUSTIFICATION,
            TitledBorder.DEFAULT_POSITION,
            new Font("Arial", Font.BOLD, 12),
            new Color(41, 128, 185)
        );

        legendPanel.setBorder(BorderFactory.createCompoundBorder(
            titledBorder,
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        // Create a panel for the legend items
        JPanel itemsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        itemsPanel.setOpaque(false);

        // Hiển thị các trạng thái khuyến mãi với thiết kế hiện đại
        for (TrangThaiKM status : TrangThaiKM.values()) {
            String statusValue = status.getValue();
            Color color = STATUS_COLORS.getOrDefault(statusValue, STATUS_COLORS.get("default"));

            // Create a rounded color box
            JPanel colorBox = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setColor(color);
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 5, 5);
                }
            };
            colorBox.setPreferredSize(new Dimension(16, 16));
            colorBox.setBorder(BorderFactory.createEmptyBorder());
            colorBox.setOpaque(false);

            // Create a styled label
            JLabel statusLabel = new JLabel(statusValue);
            statusLabel.setFont(new Font("Arial", Font.BOLD, 12));
            statusLabel.setForeground(new Color(70, 70, 70));

            // Create a panel for the item with hover effect
            JPanel itemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            itemPanel.setOpaque(false);
            itemPanel.add(colorBox);
            itemPanel.add(statusLabel);

            // Add a border to make it look like a button
            itemPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 10, 5, 10),
                BorderFactory.createEmptyBorder()
            ));

            // Add hover effect
            itemPanel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    itemPanel.setBackground(new Color(240, 240, 250));
                    itemPanel.setOpaque(true);
                    itemPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    itemPanel.setOpaque(false);
                    itemPanel.setBackground(null);
                }
            });

            itemsPanel.add(itemPanel);
        }

        legendPanel.add(itemsPanel, BorderLayout.CENTER);
        return legendPanel;
    }

    /**
     * Cập nhật nhãn hiển thị tháng/năm
     */
    private void updateMonthLabel() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault());
        monthLabel.setText(currentMonth.format(formatter));
    }

    /**
     * Cập nhật giao diện lịch với tháng hiện tại
     */
    private void updateCalendarView() {
        updateMonthLabel();
        calendarGridPanel.removeAll();

        // Thêm tiêu đề các ngày trong tuần
        String[] weekDays = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};
        for (String day : weekDays) {
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            headerPanel.setBackground(new Color(41, 128, 185)); // Màu xanh dương cho header

            JLabel dayLabel = new JLabel(day, JLabel.CENTER);
            dayLabel.setForeground(Color.WHITE);
            dayLabel.setFont(new Font("Arial", Font.BOLD, 12));
            headerPanel.add(dayLabel, BorderLayout.CENTER);

            calendarGridPanel.add(headerPanel);
        }

        // Lấy ngày đầu tiên của tháng hiện tại
        LocalDate firstDayOfMonth = currentMonth.withDayOfMonth(1);

        // Lấy ngày trong tuần của ngày đầu tiên (0 = Sunday, 1 = Monday, ...)
        int dayOfWeek = firstDayOfMonth.getDayOfWeek().getValue() % 7;

        // Thêm các ô trống cho những ngày trước ngày đầu tiên của tháng
        for (int i = 0; i < dayOfWeek; i++) {
            JPanel emptyPanel = new JPanel();
            emptyPanel.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
            emptyPanel.setBackground(new Color(245, 245, 245));
            calendarGridPanel.add(emptyPanel);
        }

        // Thêm các ngày trong tháng
        int daysInMonth = currentMonth.getMonth().length(currentMonth.isLeapYear());
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.withDayOfMonth(day);
            JPanel dayPanel = createDayPanel(date);
            calendarGridPanel.add(dayPanel);

            // Nếu là ngày được chọn, lưu lại panel này để làm nổi bật
            if (date.equals(selectedDate)) {
                selectedDayPanel = dayPanel;
                highlightSelectedDay();
            }
        }

        // Tải dữ liệu khuyến mãi cho tháng hiện tại
        loadMonthData(currentMonth);

        calendarGridPanel.revalidate();
        calendarGridPanel.repaint();
    }

    /**
     * Tạo panel hiển thị cho một ngày trong lịch với thiết kế hiện đại
     */
    private JPanel createDayPanel(LocalDate date) {
        // Create a panel with rounded corners and shadow effect
        JPanel dayPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw background with rounded corners
                if (date.equals(LocalDate.now())) {
                    // Today's date gets a special background
                    g2d.setColor(new Color(255, 245, 245));
                    g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

                    // Add a subtle red border
                    g2d.setColor(new Color(255, 200, 200));
                    g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                } else if (date.getDayOfWeek().getValue() >= 6) {
                    // Weekend days get a light blue background
                    g2d.setColor(new Color(245, 245, 255));
                    g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                } else {
                    // Regular days get a white background
                    g2d.setColor(Color.WHITE);
                    g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

                    // Add a subtle border
                    g2d.setColor(new Color(230, 230, 230));
                    g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                }
            }
        };

        // Remove the default border and set padding
        dayPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        dayPanel.setOpaque(false);

        // Create a header panel for the day number
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        // Hiển thị số ngày với styling
        JLabel dayLabel = new JLabel(String.valueOf(date.getDayOfMonth()), JLabel.CENTER);
        dayLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        // Style based on the day type
        if (date.equals(LocalDate.now())) {
            // Today's date
            dayLabel.setForeground(new Color(220, 50, 50));
            dayLabel.setFont(new Font("Arial", Font.BOLD, 14));
        } else if (date.getDayOfWeek().getValue() >= 6) {
            // Weekend
            dayLabel.setForeground(new Color(70, 130, 180));
            dayLabel.setFont(new Font("Arial", Font.BOLD, 12));
        } else {
            // Regular weekday
            dayLabel.setForeground(new Color(70, 70, 70));
            dayLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        }

        headerPanel.add(dayLabel, BorderLayout.CENTER);

        // Panel chứa các khuyến mãi trong ngày
        JPanel promotionsPanel = new JPanel();
        promotionsPanel.setLayout(new BoxLayout(promotionsPanel, BoxLayout.Y_AXIS));
        promotionsPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        promotionsPanel.setOpaque(false);

        // Thêm headerPanel và promotionsPanel vào dayPanel
        dayPanel.add(headerPanel, BorderLayout.NORTH);
        dayPanel.add(promotionsPanel, BorderLayout.CENTER);

        // Thêm mouse listener cho panel ngày với hiệu ứng hover
        dayPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (dayPanel != selectedDayPanel) {
                    dayPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createEmptyBorder(1, 1, 1, 1),
                        BorderFactory.createLineBorder(new Color(66, 139, 202, 100), 2, true)
                    ));
                }
                dayPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (dayPanel != selectedDayPanel) {
                    dayPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
                }
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                // Bỏ highlight cho ngày đã chọn trước đó
                if (selectedDayPanel != null) {
                    selectedDayPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
                }

                // Thiết lập ngày mới được chọn
                selectedDate = date;
                selectedDayPanel = dayPanel;

                // Highlight ngày được chọn
                highlightSelectedDay();

                // Thông báo cho listener (nếu có)
                if (dayPanelClickListener != null) {
                    dayPanelClickListener.onDayPanelClicked(date, promotionsByDate.getOrDefault(date, new ArrayList<>()));
                }
            }
        });

        return dayPanel;
    }

    /**
     * Tải dữ liệu khuyến mãi cho một tháng
     */
    public void loadMonthData(LocalDate month) {
        if (khuyenMaiDAO == null) {
            return; // Không làm gì nếu DAO là null
        }
        try {
            // Xóa dữ liệu cũ
            promotionsByDate.clear();

            // Lấy ngày đầu và cuối của tháng
            LocalDate startDate = month.withDayOfMonth(1);
            LocalDate endDate = month.withDayOfMonth(month.getMonth().length(month.isLeapYear()));

            // Truy vấn khuyến mãi trong khoảng thời gian này
            List<KhuyenMai> promotions = khuyenMaiDAO.findAll();

            // Lọc và nhóm khuyến mãi theo ngày
            for (KhuyenMai promotion : promotions) {
                // Lấy khoảng thời gian của khuyến mãi
                LocalDate promoStart = promotion.getThoiGianBatDau();
                LocalDate promoEnd = promotion.getThoiGianKetThuc();

                // Kiểm tra xem khuyến mãi có hiệu lực trong tháng hiện tại không
                if (!(promoEnd.isBefore(startDate) || promoStart.isAfter(endDate))) {
                    // Tính toán các ngày hiệu lực trong tháng hiện tại
                    LocalDate effectiveStart = promoStart.isBefore(startDate) ? startDate : promoStart;
                    LocalDate effectiveEnd = promoEnd.isAfter(endDate) ? endDate : promoEnd;

                    // Thêm khuyến mãi vào tất cả các ngày hiệu lực
                    LocalDate currentDate = effectiveStart;
                    while (!currentDate.isAfter(effectiveEnd)) {
                        promotionsByDate.computeIfAbsent(currentDate, k -> new ArrayList<>()).add(promotion);
                        currentDate = currentDate.plusDays(1);
                    }
                }
            }

            // Cập nhật hiển thị các khuyến mãi lên lịch
            updatePromotionDisplay();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tải dữ liệu khuyến mãi", e);
        }
    }

    /**
     * Cập nhật hiển thị các khuyến mãi trên lịch
     */
    private void updatePromotionDisplay() {
        // Duyệt qua tất cả các panel ngày trong lịch
        for (Component component : calendarGridPanel.getComponents()) {
            if (component instanceof JPanel) {
                JPanel dayPanel = (JPanel) component;

                // Kiểm tra xem panel này có phải là một ngày trong tháng hay không
                if (dayPanel.getComponentCount() > 0 &&
                        dayPanel.getComponent(0) instanceof JLabel &&
                        Character.isDigit(((JLabel)dayPanel.getComponent(0)).getText().charAt(0))) {

                    // Lấy ngày từ label
                    int day = Integer.parseInt(((JLabel)dayPanel.getComponent(0)).getText());

                    // Tạo đối tượng LocalDate cho ngày này
                    LocalDate date = currentMonth.withDayOfMonth(day);

                    // Lấy panel chứa khuyến mãi
                    JPanel promotionsPanel = null;
                    if (dayPanel.getComponentCount() > 1 && dayPanel.getComponent(1) instanceof JPanel) {
                        promotionsPanel = (JPanel) dayPanel.getComponent(1);
                        promotionsPanel.removeAll(); // Xóa các khuyến mãi cũ
                    } else {
                        promotionsPanel = new JPanel();
                        promotionsPanel.setLayout(new BoxLayout(promotionsPanel, BoxLayout.Y_AXIS));
                        promotionsPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
                        dayPanel.add(promotionsPanel, BorderLayout.CENTER);
                    }

                    // Thêm các khuyến mãi cho ngày này
                    List<KhuyenMai> dayPromotions = promotionsByDate.getOrDefault(date, new ArrayList<>());

                    // Giới hạn số lượng hiển thị để tránh quá tải
                    int maxDisplay = Math.min(dayPromotions.size(), 3);
                    for (int i = 0; i < maxDisplay; i++) {
                        KhuyenMai promotion = dayPromotions.get(i);
                        JPanel promotionItem = createPromotionItem(promotion);
                        promotionsPanel.add(promotionItem);

                        // Thêm khoảng cách giữa các khuyến mãi
                        if (i < maxDisplay - 1) {
                            promotionsPanel.add(Box.createVerticalStrut(2));
                        }
                    }

                    // Nếu có nhiều khuyến mãi hơn số lượng hiển thị
                    if (dayPromotions.size() > maxDisplay) {
                        JLabel moreLabel = new JLabel("+" + (dayPromotions.size() - maxDisplay) + " khuyến mãi khác");
                        moreLabel.setForeground(Color.BLUE);
                        moreLabel.setFont(new Font("Arial", Font.ITALIC, 10));
                        promotionsPanel.add(Box.createVerticalStrut(2));
                        promotionsPanel.add(moreLabel);
                    }
                }
            }
        }

        // Cập nhật giao diện
        calendarGridPanel.revalidate();
        calendarGridPanel.repaint();
    }

    /**
     * Tạo panel hiển thị thông tin cho một khuyến mãi với thiết kế hiện đại
     */
    private JPanel createPromotionItem(KhuyenMai promotion) {
        // Create a panel with rounded corners and gradient background
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Xác định trạng thái hiển thị
                TrangThaiKM status = promotion.getTrangThai();

                // Lấy màu sắc tương ứng với trạng thái
                String statusValue = status.getValue();
                Color baseColor = STATUS_COLORS.getOrDefault(statusValue, STATUS_COLORS.get("default"));

                // Create a slightly lighter version of the color for gradient
                Color lighterColor = new Color(
                    Math.min(baseColor.getRed() + 20, 255),
                    Math.min(baseColor.getGreen() + 20, 255),
                    Math.min(baseColor.getBlue() + 20, 255),
                    baseColor.getAlpha()
                );

                // Create gradient from top to bottom
                GradientPaint gradient = new GradientPaint(
                    0, 0, lighterColor,
                    0, getHeight(), baseColor
                );

                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                // Add a subtle highlight at the top
                g2d.setColor(new Color(255, 255, 255, 60));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight()/3, 8, 8);
            }
        };

        // Set the panel to be non-opaque and add padding
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));

        // Create a container for the content with proper layout
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);

        // Hiển thị tên khuyến mãi với icon
        String promotionInfo = promotion.getTenKM();
        JLabel promotionLabel = new JLabel(promotionInfo);
        promotionLabel.setFont(new Font("Arial", Font.BOLD, 10));
        promotionLabel.setForeground(Color.WHITE);

        // Add a small discount icon
        ImageIcon discountIcon = createDiscountIcon(10, 10, Color.WHITE);
        promotionLabel.setIcon(discountIcon);
        promotionLabel.setIconTextGap(4);

        // Add a subtle text shadow for better readability
        promotionLabel.setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 3));
        contentPanel.add(promotionLabel, BorderLayout.CENTER);

        // Add discount percentage as a badge on the right if available
        if (promotion.getChietKhau() > 0) {
            JLabel discountLabel = new JLabel(String.format("%.0f%%", promotion.getChietKhau() * 100));
            discountLabel.setFont(new Font("Arial", Font.BOLD, 9));
            discountLabel.setForeground(Color.WHITE);
            discountLabel.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
            contentPanel.add(discountLabel, BorderLayout.EAST);
        }

        panel.add(contentPanel, BorderLayout.CENTER);

        // Add hover effect
        panel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                panel.setBorder(BorderFactory.createEmptyBorder(2, 4, 4, 4));
                panel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                panel.repaint();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                panel.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
                panel.repaint();
            }
        });

        // Hiển thị thông tin chi tiết hơn trong tooltip với HTML styling
        String tooltip = "<html>" +
                "<div style='font-family: Arial; padding: 5px;'>" +
                "<b style='color: #2980b9; font-size: 12px;'>" + promotion.getTenKM() + "</b><br>" +
                "<table style='margin-top: 5px;'>" +
                "<tr><td><b>Mã KM:</b></td><td>" + promotion.getMaKM() + "</td></tr>" +
                "<tr><td><b>Thời gian:</b></td><td>" + promotion.getThoiGianBatDau() + " - " + promotion.getThoiGianKetThuc() + "</td></tr>" +
                "<tr><td><b>Chiết khấu:</b></td><td>" + String.format("%.1f%%", promotion.getChietKhau() * 100) + "</td></tr>" +
                "<tr><td><b>Đối tượng:</b></td><td>" + promotion.getDoiTuongApDung().getValue() + "</td></tr>" +
                "<tr><td><b>Trạng thái:</b></td><td>" + promotion.getTrangThai().getValue() + "</td></tr>" +
                "</table>" +
                "</div>" +
                "</html>";
        panel.setToolTipText(tooltip);

        return panel;
    }

    /**
     * Tạo icon giảm giá nhỏ
     */
    private ImageIcon createDiscountIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw a % symbol
        g2.setColor(color);
        g2.setFont(new Font("Arial", Font.BOLD, 9));
        g2.drawString("%", 1, 8);

        g2.dispose();
        return new ImageIcon(image);
    }

    /**
     * Làm nổi bật ngày được chọn với thiết kế hiện đại
     */
    private void highlightSelectedDay() {
        if (selectedDayPanel != null) {
            // Create a compound border with padding and a rounded blue border
            selectedDayPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(1, 1, 1, 1),
                BorderFactory.createLineBorder(new Color(41, 128, 185), 2, true) // true for rounded corners
            ));

            // Add a subtle glow effect by setting a custom background painter in the next repaint
            selectedDayPanel.repaint();
        }
    }

    /**
     * Thiết lập listener cho sự kiện click vào panel ngày
     */
    public void setDayPanelClickListener(DayPanelClickListener listener) {
        this.dayPanelClickListener = listener;
    }

    /**
     * Interface để lắng nghe sự kiện click vào panel ngày
     */
    public interface DayPanelClickListener {
        void onDayPanelClicked(LocalDate date, List<KhuyenMai> promotions);
    }

    /**
     * Cập nhật lịch sau khi có thay đổi (thêm, sửa, xóa khuyến mãi)
     */
    public void refreshCalendar() {
        if (khuyenMaiDAO == null) {
            return; // Không làm gì nếu DAO là null
        }
        loadMonthData(currentMonth);
    }

    /**
     * Thiết lập ngày được chọn
     */
    public void setSelectedDate(LocalDate date) {
        this.selectedDate = date;

        // Nếu ngày thuộc tháng khác, chuyển sang tháng đó
        if (date.getMonth() != currentMonth.getMonth() || date.getYear() != currentMonth.getYear()) {
            currentMonth = date.withDayOfMonth(1);
            updateCalendarView();
        } else {
            // Tìm và highlight ngày được chọn
            for (Component component : calendarGridPanel.getComponents()) {
                if (component instanceof JPanel) {
                    JPanel dayPanel = (JPanel) component;
                    if (dayPanel.getComponentCount() > 0 &&
                            dayPanel.getComponent(0) instanceof JLabel) {
                        JLabel dayLabel = (JLabel) dayPanel.getComponent(0);
                        if (Character.isDigit(dayLabel.getText().charAt(0))) {
                            int day = Integer.parseInt(dayLabel.getText());
                            if (day == date.getDayOfMonth()) {
                                // Bỏ highlight cho ngày đã chọn trước đó
                                if (selectedDayPanel != null) {
                                    selectedDayPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                                }

                                selectedDayPanel = dayPanel;
                                highlightSelectedDay();
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Lấy ngày đang được chọn
     */
    public LocalDate getSelectedDate() {
        return selectedDate;
    }
}
