package guiClient;

import dao.LichTrinhTauDAO;
import model.LichTrinhTau;
import model.TrangThai;

import javax.swing.*;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panel hiển thị lịch trình tàu theo dạng lịch
 */
public class TrainScheduleCalendarPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(TrainScheduleCalendarPanel.class.getName());

    // Các biến thành viên
    private LocalDate currentMonth; // Tháng hiện tại đang hiển thị
    private JPanel calendarGridPanel; // Panel chứa các ô ngày trong lịch
    private JLabel monthLabel; // Nhãn hiển thị tháng/năm
    private JButton prevMonthButton; // Nút chuyển tháng trước
    private JButton nextMonthButton; // Nút chuyển tháng sau
    private Map<LocalDate, List<LichTrinhTau>> schedulesByDate; // Lưu các lịch trình theo ngày
    private LichTrinhTauDAO lichTrinhTauDAO; // DAO để truy vấn dữ liệu
    private JPanel selectedDayPanel; // Panel ngày được chọn
    private LocalDate selectedDate; // Ngày được chọn
    private DayPanelClickListener dayPanelClickListener; // Listener cho sự kiện click vào panel ngày

    // Hằng số cho màu sắc theo trạng thái - Cập nhật màu sắc theo yêu cầu
    private static final Map<String, Color> STATUS_COLORS = new HashMap<>();

    // Cập nhật bảng màu trong static block
    static {
        // Cập nhật bảng màu theo yêu cầu
        STATUS_COLORS.put("Đã khởi hành", new Color(46, 139, 87));    // Xanh lá đậm (Sea Green)
        STATUS_COLORS.put("Chưa khởi hành", new Color(30, 144, 255)); // Xanh dương (Dodger Blue)
        STATUS_COLORS.put("Đã hủy", new Color(220, 20, 60));          // Đỏ (Crimson)
        STATUS_COLORS.put("Hoạt động", new Color(255, 140, 0));       // Vàng cam đậm (Dark Orange) - Màu nổi bật hơn
        STATUS_COLORS.put("default", new Color(110, 110, 110));       // Xám (Màu mặc định)
    }

    /**
     * Constructor cho TrainScheduleCalendarPanel
     *
     * @param lichTrinhTauDAO DAO để truy vấn dữ liệu lịch trình tàu
     */
    public TrainScheduleCalendarPanel(LichTrinhTauDAO lichTrinhTauDAO) {
        this.lichTrinhTauDAO = lichTrinhTauDAO;
        this.currentMonth = LocalDate.now();
        this.schedulesByDate = new HashMap<>();
        this.selectedDate = LocalDate.now();

        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Khởi tạo giao diện
        initializeUI();

        if (lichTrinhTauDAO != null) {
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
        // Panel cho điều khiển lịch (tháng trước, tháng sau)
        JPanel controlPanel = new JPanel(new BorderLayout());

        // Tạo panel chứa nút điều hướng
        JPanel navigationPanel = new JPanel(new FlowLayout());

        // Nút tháng trước
        prevMonthButton = new JButton("◀");
        prevMonthButton.setFont(new Font("Arial", Font.BOLD, 14));
        prevMonthButton.addActionListener(e -> {
            currentMonth = currentMonth.minusMonths(1);
            updateCalendarView();
        });

        // Nút tháng sau
        nextMonthButton = new JButton("▶");
        nextMonthButton.setFont(new Font("Arial", Font.BOLD, 14));
        nextMonthButton.addActionListener(e -> {
            currentMonth = currentMonth.plusMonths(1);
            updateCalendarView();
        });

        // Label hiển thị tháng/năm hiện tại
        monthLabel = new JLabel("", JLabel.CENTER);
        monthLabel.setFont(new Font("Arial", Font.BOLD, 16));
        updateMonthLabel();

        // Thêm các thành phần vào panel điều hướng
        navigationPanel.add(prevMonthButton);
        navigationPanel.add(monthLabel);
        navigationPanel.add(nextMonthButton);
        controlPanel.add(navigationPanel, BorderLayout.CENTER);

        // Tạo panel chứa lịch (grid)
        calendarGridPanel = new JPanel(new GridLayout(0, 7, 5, 5));

        // Panel chứa lịch bọc bởi scroll pane
        JScrollPane calendarScrollPane = new JScrollPane(calendarGridPanel);
        calendarScrollPane.setBorder(BorderFactory.createEmptyBorder());

        // Thêm vào panel chính
        add(controlPanel, BorderLayout.NORTH);
        add(calendarScrollPane, BorderLayout.CENTER);

        // Legend panel để hiển thị các màu tương ứng với trạng thái
        JPanel legendPanel = createLegendPanel();
        add(legendPanel, BorderLayout.SOUTH);
    }

    /**
     * Tạo panel chú thích màu sắc cho các trạng thái
     */
    private JPanel createLegendPanel() {
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        legendPanel.setBorder(BorderFactory.createTitledBorder("Chú thích trạng thái"));

        // Chỉ hiển thị những trạng thái trong enum TrangThai
        for (TrangThai status : TrangThai.values()) {
            String statusValue = status.getValue();
            Color color = STATUS_COLORS.getOrDefault(statusValue, STATUS_COLORS.get("default"));

            JPanel colorBox = new JPanel();
            colorBox.setPreferredSize(new Dimension(15, 15));
            colorBox.setBackground(color);
            colorBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));

            JLabel statusLabel = new JLabel(statusValue);
            statusLabel.setFont(new Font("Arial", Font.PLAIN, 11));

            JPanel itemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            itemPanel.add(colorBox);
            itemPanel.add(statusLabel);

            legendPanel.add(itemPanel);
        }

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

        // Tải dữ liệu lịch trình cho tháng hiện tại
        loadMonthData(currentMonth);

        calendarGridPanel.revalidate();
        calendarGridPanel.repaint();
    }

    /**
     * Tạo panel hiển thị cho một ngày trong lịch
     */
    private JPanel createDayPanel(LocalDate date) {
        JPanel dayPanel = new JPanel(new BorderLayout());
        dayPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // Hiển thị số ngày ở góc trên bên trái
        JLabel dayLabel = new JLabel(String.valueOf(date.getDayOfMonth()));
        dayLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 0, 0));

        // Nếu là ngày hôm nay, đánh dấu đặc biệt
        if (date.equals(LocalDate.now())) {
            dayLabel.setForeground(Color.RED);
            dayLabel.setFont(new Font("Arial", Font.BOLD, 12));
        } else {
            dayLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        }

        // Panel chứa các lịch trình trong ngày
        JPanel schedulesPanel = new JPanel();
        schedulesPanel.setLayout(new BoxLayout(schedulesPanel, BoxLayout.Y_AXIS));
        schedulesPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        // Thêm dayLabel và schedulesPanel vào dayPanel
        dayPanel.add(dayLabel, BorderLayout.NORTH);
        dayPanel.add(schedulesPanel, BorderLayout.CENTER);

        // Thêm mouse listener cho panel ngày
        dayPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                // Bỏ highlight cho ngày đã chọn trước đó
                if (selectedDayPanel != null) {
                    selectedDayPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                }

                // Thiết lập ngày mới được chọn
                selectedDate = date;
                selectedDayPanel = dayPanel;

                // Highlight ngày được chọn
                highlightSelectedDay();

                // Thông báo cho listener (nếu có)
                if (dayPanelClickListener != null) {
                    dayPanelClickListener.onDayPanelClicked(date, schedulesByDate.getOrDefault(date, new ArrayList<>()));
                }
            }
        });

        return dayPanel;
    }

    /**
     * Tải dữ liệu lịch trình cho một tháng
     */
    public void loadMonthData(LocalDate month) {
        if (lichTrinhTauDAO == null) {
            return; // Không làm gì nếu DAO là null
        }
        try {
            // Xóa dữ liệu cũ
            schedulesByDate.clear();

            // Lấy ngày đầu và cuối của tháng
            LocalDate startDate = month.withDayOfMonth(1);
            LocalDate endDate = month.withDayOfMonth(month.getMonth().length(month.isLeapYear()));

            // Truy vấn lịch trình trong khoảng thời gian này
            List<LichTrinhTau> schedules = lichTrinhTauDAO.getListLichTrinhTauByDateRange(startDate, endDate);

            // Nhóm lịch trình theo ngày
            for (LichTrinhTau schedule : schedules) {
                LocalDate date = schedule.getNgayDi();
                schedulesByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(schedule);
            }

            // Cập nhật hiển thị các lịch trình lên lịch
            updateScheduleDisplay();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tải dữ liệu lịch trình", e);
        }
    }

    /**
     * Cập nhật hiển thị các lịch trình trên lịch
     */
    private void updateScheduleDisplay() {
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

                    // Lấy panel chứa lịch trình
                    JPanel schedulesPanel = null;
                    if (dayPanel.getComponentCount() > 1 && dayPanel.getComponent(1) instanceof JPanel) {
                        schedulesPanel = (JPanel) dayPanel.getComponent(1);
                        schedulesPanel.removeAll(); // Xóa các lịch trình cũ
                    } else {
                        schedulesPanel = new JPanel();
                        schedulesPanel.setLayout(new BoxLayout(schedulesPanel, BoxLayout.Y_AXIS));
                        schedulesPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
                        dayPanel.add(schedulesPanel, BorderLayout.CENTER);
                    }

                    // Thêm các lịch trình cho ngày này
                    List<LichTrinhTau> daySchedules = schedulesByDate.getOrDefault(date, new ArrayList<>());

                    // Giới hạn số lượng hiển thị để tránh quá tải
                    int maxDisplay = Math.min(daySchedules.size(), 3);
                    for (int i = 0; i < maxDisplay; i++) {
                        LichTrinhTau schedule = daySchedules.get(i);
                        JPanel scheduleItem = createScheduleItem(schedule);
                        schedulesPanel.add(scheduleItem);

                        // Thêm khoảng cách giữa các lịch trình
                        if (i < maxDisplay - 1) {
                            schedulesPanel.add(Box.createVerticalStrut(2));
                        }
                    }

                    // Nếu có nhiều lịch trình hơn số lượng hiển thị
                    if (daySchedules.size() > maxDisplay) {
                        JLabel moreLabel = new JLabel("+" + (daySchedules.size() - maxDisplay) + " lịch trình khác");
                        moreLabel.setForeground(Color.BLUE);
                        moreLabel.setFont(new Font("Arial", Font.ITALIC, 10));
                        schedulesPanel.add(Box.createVerticalStrut(2));
                        schedulesPanel.add(moreLabel);
                    }
                }
            }
        }

        // Cập nhật giao diện
        calendarGridPanel.revalidate();
        calendarGridPanel.repaint();
    }

    /**
     * Tạo panel hiển thị thông tin cho một lịch trình
     */
    /**
     * Tạo panel hiển thị thông tin cho một lịch trình
     */
    /**
     * Tạo panel hiển thị thông tin cho một lịch trình
     */
    private JPanel createScheduleItem(LichTrinhTau schedule) {
        JPanel panel = new JPanel(new BorderLayout());

        // Lấy thời gian hiện tại
        LocalDate currentDate = LocalDate.now();
        java.time.LocalTime currentTime = java.time.LocalTime.now();

        // Xác định trạng thái hiển thị ban đầu
        TrangThai originalStatus = schedule.getTrangThai();
        TrangThai displayStatus = originalStatus;

        // Kiểm tra nếu lịch trình đã qua (ngày đi trước ngày hiện tại, hoặc cùng ngày nhưng giờ đã qua)
        boolean isSchedulePast = schedule.getNgayDi().isBefore(currentDate) ||
                (schedule.getNgayDi().isEqual(currentDate) &&
                        schedule.getGioDi().isBefore(currentTime));

        // Kiểm tra nếu lịch trình sắp khởi hành trong vòng 30 phút
        boolean isAboutToStart = false;
        if (schedule.getNgayDi().isEqual(currentDate)) {
            // Tính khoảng cách thời gian giữa giờ hiện tại và giờ đi
            long minutesUntilDeparture = java.time.Duration.between(currentTime, schedule.getGioDi()).toMinutes();
            isAboutToStart = minutesUntilDeparture >= 0 && minutesUntilDeparture <= 30;
        }

        // Quy tắc xác định trạng thái hiển thị
        if (isAboutToStart && displayStatus == TrangThai.CHUA_KHOI_HANH) {
            // Nếu sắp khởi hành trong vòng 30 phút -> "Hoạt động"
            displayStatus = TrangThai.HOAT_DONG;
        } else if (isSchedulePast && displayStatus != TrangThai.DA_HUY) {
            // Nếu đã qua thời gian khởi hành và không phải đã hủy -> "Đã khởi hành"
            displayStatus = TrangThai.DA_KHOI_HANH;
        }

        // Lấy màu sắc tương ứng với trạng thái hiển thị
        String statusValue = displayStatus.getValue();
        Color statusColor = STATUS_COLORS.getOrDefault(statusValue, STATUS_COLORS.get("default"));
        panel.setBackground(statusColor);

        // Thêm viền đặc biệt cho trạng thái "Hoạt động"
        if (displayStatus == TrangThai.HOAT_DONG) {
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(255, 215, 0), 1), // Viền màu vàng gold
                    BorderFactory.createEmptyBorder(1, 1, 1, 1)
            ));
        } else {
            panel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        }

        String trainInfo = schedule.getTau().getMaTau() + " - " + schedule.getGioDi().toString();
        JLabel scheduleLabel = new JLabel(trainInfo);
        scheduleLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        scheduleLabel.setForeground(Color.WHITE);
        scheduleLabel.setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 3));
        panel.add(scheduleLabel, BorderLayout.CENTER);

        // Tooltip hiển thị cả trạng thái thực tế và trạng thái hiển thị nếu khác nhau
        String tooltipStatus;
        if (isAboutToStart && originalStatus == TrangThai.CHUA_KHOI_HANH) {
            tooltipStatus = originalStatus.getValue() + " (Hiển thị: Hoạt động)";
        } else if (isSchedulePast && originalStatus != TrangThai.DA_KHOI_HANH && originalStatus != TrangThai.DA_HUY) {
            tooltipStatus = originalStatus.getValue() + " (Hiển thị: Đã khởi hành)";
        } else {
            tooltipStatus = originalStatus.getValue();
        }

        // Hiển thị thông tin chi tiết hơn trong tooltip
        String departureTimeInfo = "";
        if (isAboutToStart) {
            long minutesUntilDeparture = java.time.Duration.between(currentTime, schedule.getGioDi()).toMinutes();
            departureTimeInfo = " (Khởi hành trong " + minutesUntilDeparture + " phút)";
        }

        String tooltip = "<html>" +
                "Mã lịch: " + schedule.getMaLich() + "<br>" +
                "Tàu: " + schedule.getTau().getMaTau() + " - " + schedule.getTau().getTenTau() + "<br>" +
                "Tuyến: " + schedule.getTau().getTuyenTau().getGaDi() + " -> " + schedule.getTau().getTuyenTau().getGaDen() + "<br>" +
                "Thời gian: " + schedule.getGioDi() + departureTimeInfo + "<br>" +
                "Trạng thái: " + tooltipStatus +
                "</html>";
        panel.setToolTipText(tooltip);

        return panel;
    }

    /**
     * Làm nổi bật ngày được chọn
     */
    private void highlightSelectedDay() {
        if (selectedDayPanel != null) {
            selectedDayPanel.setBorder(BorderFactory.createLineBorder(new Color(66, 139, 202), 2));
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
        void onDayPanelClicked(LocalDate date, List<LichTrinhTau> schedules);
    }

    /**
     * Cập nhật lịch sau khi có thay đổi (thêm, sửa, xóa lịch trình)
     */
    public void refreshCalendar() {
        if (lichTrinhTauDAO == null) {
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