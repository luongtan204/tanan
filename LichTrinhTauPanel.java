package guiClient;

import com.toedter.calendar.JDateChooser;
import dao.LichTrinhTauDAO;
import dao.TauDAO;
import model.LichTrinhTau;
import model.Tau;
import model.TrangThai;
import service.AITravelTimePredictor;
import service.ScheduleStatusManager;

import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.time.format.DateTimeParseException;
import java.util.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LichTrinhTauPanel extends JPanel {

    private static final Logger LOGGER = Logger.getLogger(LichTrinhTauPanel.class.getName());
    // Địa chỉ IP và port của RMI server
    private static final String RMI_SERVER_IP = "127.0.0.1";
    private static final int RMI_SERVER_PORT = 9090;
    private JTable scheduleTable;
    private DefaultTableModel tableModel;
    private JDateChooser dateChooser;
    private JButton searchButton;
    private JButton refreshButton;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JComboBox<Object> filterComboBox;
    private JTabbedPane viewTabbedPane; // Tab để chuyển đổi giữa dạng bảng và lịch
    private TrainScheduleCalendarPanel calendarPanel; // Panel dạng lịch
    private JPanel tableViewPanel; // Panel chứa bảng

    private LichTrinhTauDAO lichTrinhTauDAO;
    private boolean isConnected = false;
    private static LocalDate lastGeneratedDate = LocalDate.now();
    private static int count = 0;
    private ScheduleStatusManager statusManager;
    private AITravelTimePredictor aiPredictor;
    private ChatbotDialog chatbotDialog;

    public LichTrinhTauPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Connect to RMI server
        connectToRMIServer();

        // Add components to the panel
        add(createTitlePanel(), BorderLayout.NORTH);
        add(createCenterPanel(), BorderLayout.CENTER);

        // Load initial data
        if (isConnected) {
            try {
                initStatusManager();
                loadAllScheduleData();
                this.aiPredictor = AITravelTimePredictor.getInstance();
            } catch (RemoteException ex) {
                LOGGER.log(Level.SEVERE, "Error loading schedule data", ex);
                showErrorMessage("Không thể tải dữ liệu lịch trình", ex);
            }
        } else {
            showErrorMessage("Không thể kết nối đến máy chủ", null);
        }
        loadDataInBackground();
    }

    private void loadDataInBackground() {
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                // Kết nối đến RMI server
                connectToRMIServer();
                return isConnected;
            }

            @Override
            protected void done() {
                try {
                    boolean connected = get();
                    if (connected) {
                        // Xóa thông báo "đang tải"
                        tableModel.setRowCount(0);
                        // Tải dữ liệu lịch trình
                        loadAllScheduleData();

                        // Khởi tạo trình quản lý trạng thái
                        initStatusManager();
                    } else {
                        tableModel.setRowCount(0);
                        tableModel.addRow(new Object[]{"Không thể kết nối đến máy chủ", "", "", "", "", "", ""});
                        showErrorMessage("Không thể kết nối đến máy chủ", null);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error loading schedule data", e);
                    tableModel.setRowCount(0);
                    tableModel.addRow(new Object[]{"Lỗi: " + e.getMessage(), "", "", "", "", "", ""});
                    showErrorMessage("Không thể tải dữ liệu lịch trình", e);
                }
            }
        };

        worker.execute();
    }

    private void initStatusManager() {
        if (isConnected && lichTrinhTauDAO != null) {
            // Tạo callback làm mới dữ liệu
            Runnable refreshCallback = this::refreshDataAfterUpdate;

            // Khởi tạo trình quản lý trạng thái
            statusManager = new ScheduleStatusManager(lichTrinhTauDAO, refreshCallback);

            LOGGER.info("Đã khởi tạo trình quản lý cập nhật trạng thái tự động");
        }
    }
    private void refreshDataAfterUpdate() {
        try {
            // Làm mới dữ liệu trên giao diện mà không gọi cập nhật trạng thái lại
            loadDataWithoutStatusCheck();

            // Hiển thị thông báo nhỏ (tùy chọn)
            showNotification("Đã cập nhật trạng thái các lịch trình tàu");

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Lỗi khi làm mới dữ liệu sau khi cập nhật trạng thái", ex);
        }
    }

    private void loadDataWithoutStatusCheck() throws RemoteException {
        if (!isConnected || lichTrinhTauDAO == null) {
            connectToRMIServer();
            if (!isConnected) {
                throw new RemoteException("Not connected to RMI server");
            }
        }

        tableModel.setRowCount(0);

        try {
            List<LichTrinhTau> schedules = lichTrinhTauDAO.getAllList();

            if (schedules == null || schedules.isEmpty()) {
                LOGGER.info("Không có lịch trình nào để hiển thị.");
                return;
            }

            // Lọc và hiển thị dữ liệu theo bộ lọc hiện tại
            String filterOption = filterComboBox.getSelectedItem().toString();
            for (LichTrinhTau schedule : schedules) {
                if (matchesFilter(schedule, filterOption)) {
                    tableModel.addRow(createTableRow(schedule));
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi chi tiết khi tải dữ liệu: " + e.getMessage(), e);
            throw new RemoteException("Lỗi khi tải dữ liệu: " + e.getMessage(), e);
        }
    }

    /**
     * Hiển thị thông báo nhỏ ở góc màn hình
     */
    private void showNotification(String message) {
        // Bạn có thể triển khai một thông báo nhỏ ở góc màn hình
        // hoặc cập nhật một label trạng thái trên giao diện
    }

    private JPanel createTitlePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel titleLabel = new JLabel("QUẢN LÝ LỊCH TRÌNH TÀU", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(new Color(41, 128, 185));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        panel.add(titleLabel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        panel.add(createSearchPanel(), BorderLayout.NORTH);

        // Tạo TabbedPane để chứa cả chế độ xem bảng và lịch
        viewTabbedPane = new JTabbedPane();

        // Tạo panel chế độ xem bảng
        tableViewPanel = new JPanel(new BorderLayout());
        tableViewPanel.add(createTablePanel(), BorderLayout.CENTER);
        calendarPanel = new TrainScheduleCalendarPanel(isConnected ? lichTrinhTauDAO : null);
        // Nếu đã kết nối, tạo panel lịch
        if (isConnected && lichTrinhTauDAO != null) {
            // Thiết lập listener cho sự kiện click ngày
            calendarPanel.setDayPanelClickListener((date, schedules) -> {
                // Hiển thị danh sách lịch trình của ngày được chọn
                if (!schedules.isEmpty()) {
                    showScheduleDetailsDialog(date, schedules);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Không có lịch trình nào cho ngày " + date,
                            "Thông tin",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            });
        } else {
            // Sử dụng TrainScheduleCalendarPanel thay vì JPanel
            calendarPanel = new TrainScheduleCalendarPanel(null);

            // Thêm nhãn lỗi vào panel
            JLabel errorLabel = new JLabel("Không thể kết nối đến server để hiển thị lịch");
            errorLabel.setHorizontalAlignment(JLabel.CENTER);
            errorLabel.setForeground(Color.RED);
            calendarPanel.removeAll(); // Xóa tất cả thành phần khác
            calendarPanel.setLayout(new BorderLayout());
            calendarPanel.add(errorLabel, BorderLayout.CENTER);
        }

        // Thêm các tab vào TabbedPane
        viewTabbedPane.addTab("Dạng Bảng", new ImageIcon(), tableViewPanel, "Hiển thị dạng bảng");
        viewTabbedPane.addTab("Dạng Lịch", new ImageIcon(), calendarPanel, "Hiển thị dạng lịch");

        panel.add(viewTabbedPane, BorderLayout.CENTER);
        panel.add(createActionPanel(), BorderLayout.SOUTH);

        return panel;
    }
    // Hiện hộp thoại chi tiết lịch trình khi click vào một ngày trong lịch
    private void showScheduleDetailsDialog(LocalDate date, List<LichTrinhTau> schedules) {
        JDialog dialog = new JDialog();
        dialog.setTitle("Lịch trình ngày " + date);
        dialog.setSize(800, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setModal(true);
        dialog.setLayout(new BorderLayout());

        // Tạo panel chứa bảng lịch trình
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Tạo model cho bảng lịch trình
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Mã lịch trình");
        model.addColumn("Tàu");
        model.addColumn("Tuyến đường");
        model.addColumn("Giờ đi");
        model.addColumn("Giờ đến (dự kiến)");
        model.addColumn("Trạng thái");

        // Thêm dữ liệu vào bảng
        for (LichTrinhTau lichTrinh : schedules) {
            model.addRow(new Object[]{
                    lichTrinh.getMaLich(),
                    lichTrinh.getTau().getMaTau() + " - " + lichTrinh.getTau().getTenTau(),
                    lichTrinh.getTau().getTuyenTau().getGaDi() + " - " + lichTrinh.getTau().getTuyenTau().getGaDen(),
                    lichTrinh.getGioDi().toString(),
                    lichTrinh.getGioDi().plusHours(estimateTravelTime(lichTrinh)).toString(),
                    lichTrinh.getTrangThai()
            });
        }

        // Tạo bảng hiển thị lịch trình
        JTable scheduleTable = new JTable(model);
        scheduleTable.setRowHeight(25);
        scheduleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scheduleTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));

        // Set renderer cho cột trạng thái để hiển thị màu tương ứng
        scheduleTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                String status = value.toString();

                // Thiết lập màu nền tương ứng với trạng thái
                if ("Đã khởi hành".equals(status)) {
                    comp.setBackground(new Color(46, 204, 113)); // Xanh lá
                    comp.setForeground(Color.WHITE);
                } else if ("Đã hủy".equals(status)) {
                    comp.setBackground(new Color(231, 76, 60)); // Đỏ
                    comp.setForeground(Color.WHITE);
                } else if ("Bị trễ".equals(status)) {
                    comp.setBackground(new Color(243, 156, 18)); // Cam
                    comp.setForeground(Color.WHITE);
                } else if ("Chưa khởi hành".equals(status)) {
                    comp.setBackground(new Color(52, 152, 219)); // Xanh dương
                    comp.setForeground(Color.WHITE);
                } else {
                    if (isSelected) {
                        comp.setBackground(table.getSelectionBackground());
                        comp.setForeground(table.getSelectionForeground());
                    } else {
                        comp.setBackground(table.getBackground());
                        comp.setForeground(table.getForeground());
                    }
                }

                return comp;
            }
        });

        // Thêm bảng vào scroll pane
        JScrollPane scrollPane = new JScrollPane(scheduleTable);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        // Thêm các nút thao tác
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton editButton = new JButton("Chỉnh sửa", createEditIcon(16, 16));
        JButton deleteButton = new JButton("Xóa", createDeleteIcon(16, 16));
        JButton closeButton = new JButton("Đóng");

        // Sự kiện cho nút chỉnh sửa
        editButton.addActionListener(e -> {
            int row = scheduleTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(dialog,
                        "Vui lòng chọn một lịch trình để chỉnh sửa",
                        "Thông báo",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Lấy mã lịch trình đã chọn
            String maLich = (String) scheduleTable.getValueAt(row, 0);

            // Đóng dialog hiện tại
            dialog.dispose();

            // Gọi hàm chỉnh sửa với mã lịch trình
            editScheduleById(maLich);
        });

        // Sự kiện cho nút xóa
        deleteButton.addActionListener(e -> {
            int row = scheduleTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(dialog,
                        "Vui lòng chọn một lịch trình để xóa",
                        "Thông báo",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Lấy mã lịch trình đã chọn
            String maLich = (String) scheduleTable.getValueAt(row, 0);

            // Xác nhận xóa
            int option = JOptionPane.showConfirmDialog(dialog,
                    "Bạn có chắc chắn muốn xóa lịch trình " + maLich + "?",
                    "Xác nhận xóa",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (option == JOptionPane.YES_OPTION) {
                try {
                    // Thực hiện xóa
                    boolean deleted = lichTrinhTauDAO.delete(maLich);

                    if (deleted) {
                        JOptionPane.showMessageDialog(dialog,
                                "Đã xóa thành công lịch trình " + maLich,
                                "Thành công",
                                JOptionPane.INFORMATION_MESSAGE);

                        // Cập nhật lại dữ liệu
                        refreshData();

                        // Đóng dialog
                        dialog.dispose();
                    } else {
                        JOptionPane.showMessageDialog(dialog,
                                "Không thể xóa lịch trình " + maLich,
                                "Lỗi",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog,
                            "Lỗi khi xóa lịch trình: " + ex.getMessage(),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Sự kiện cho nút đóng
        closeButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(closeButton);

        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }
    private void editScheduleById(String maLich) {
        // TODO: Triển khai chức năng chỉnh sửa lịch trình theo mã
        JOptionPane.showMessageDialog(this,
                "Chức năng chỉnh sửa lịch trình sẽ được triển khai trong phiên bản tiếp theo.",
                "Thông báo",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void connectToRMIServer() {
        try {
            System.out.println("Đang kết nối đến RMI server...");

            // Sử dụng trực tiếp RMI registry thay vì JNDI
            Registry registry = LocateRegistry.getRegistry(RMI_SERVER_IP, RMI_SERVER_PORT);
            lichTrinhTauDAO = (LichTrinhTauDAO) registry.lookup("lichTrinhTauDAO");

            // Kiểm tra kết nối đến cơ sở dữ liệu
            try {
                boolean dbConnected = lichTrinhTauDAO.testConnection();
                if (dbConnected) {
                    isConnected = true;
                    LOGGER.info("Kết nối thành công đến RMI server và cơ sở dữ liệu");

                    // Kiểm tra và ghi log danh sách trạng thái
                    try {
                        List<TrangThai> statuses = lichTrinhTauDAO.getTrangThai();
                        LOGGER.info("Đã tải " + (statuses != null ? statuses.size() : 0) + " trạng thái từ cơ sở dữ liệu");
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Lỗi khi tải danh sách trạng thái trong quá trình kết nối", e);
                    }
                } else {
                    isConnected = false;
                    LOGGER.warning("Kết nối thành công đến RMI server nhưng không thể kết nối đến cơ sở dữ liệu");
                    showErrorMessage("Kết nối đến RMI server thành công nhưng không thể kết nối đến cơ sở dữ liệu", null);
                }
            } catch (Exception e) {
                isConnected = false;
                LOGGER.log(Level.SEVERE, "Kiểm tra kết nối cơ sở dữ liệu thất bại", e);
                showErrorMessage("Kiểm tra kết nối cơ sở dữ liệu thất bại", e);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Không thể kết nối đến RMI server", ex);
            isConnected = false;
            showErrorMessage("Không thể kết nối đến RMI server: " + ex.getMessage(), ex);
        }
    }

    private JPanel createSearchPanel() {
        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.setBorder(BorderFactory.createTitledBorder("Tìm Kiếm Lịch Trình"));

        // Panel chứa tất cả các điều khiển tìm kiếm
        JPanel mainSearchPanel = new JPanel(new BorderLayout(0, 10));

        // Panel cho hàng đầu tiên (ngày đi và trạng thái)
        JPanel firstRowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        // Thành phần tìm kiếm theo ngày
        JLabel dateLabel = new JLabel("Ngày đi:");
        dateChooser = new JDateChooser();
        dateChooser.setDateFormatString("yyyy-MM-dd");
        dateChooser.setDate(new Date());
        dateChooser.setPreferredSize(new Dimension(150, 28));

        // Thành phần lọc theo trạng thái
        JLabel filterLabel = new JLabel("Lọc theo trạng thái:");
        DefaultComboBoxModel<Object> filterModel = new DefaultComboBoxModel<>();
        filterModel.addElement("Tất cả");

        try {
            if (isConnected && lichTrinhTauDAO != null) {
                List<TrangThai> dbStatuses = lichTrinhTauDAO.getTrangThai();
                if (dbStatuses != null && !dbStatuses.isEmpty()) {
                    for (TrangThai status : dbStatuses) {
                        filterModel.addElement(status);
                    }
                    LOGGER.info("Đã tải thành công " + dbStatuses.size() + " trạng thái từ cơ sở dữ liệu");
                } else {
                    LOGGER.warning("Không tìm thấy trạng thái nào trong cơ sở dữ liệu");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tải danh sách trạng thái: " + e.getMessage(), e);
        }

        filterComboBox = new JComboBox<>(filterModel);
        filterComboBox.setPreferredSize(new Dimension(150, 28));

        // Custom renderer để hiển thị mô tả của enum
        filterComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                if (value instanceof TrangThai) {
                    value = ((TrangThai) value).getValue();
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });

        // Thêm các thành phần vào panel hàng đầu
        firstRowPanel.add(dateLabel);
        firstRowPanel.add(dateChooser);
        firstRowPanel.add(filterLabel);
        firstRowPanel.add(filterComboBox);

        // Panel cho hàng thứ hai (ga đi, ga đến, giờ đi)
        JPanel secondRowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        // Thành phần tìm kiếm theo ga đi
        JLabel depStationLabel = new JLabel("Ga đi:");

        // Sử dụng model cho autocomplete ComboBox
        DefaultComboBoxModel<String> depStationModel = new DefaultComboBoxModel<>();
        depStationModel.addElement("Tất cả");

        // Tạo ComboBox cho ga đi với AutoComplete
        JComboBox<String> depStationComboBox = new JComboBox<>(depStationModel);
        depStationComboBox.setPreferredSize(new Dimension(150, 28));
        depStationComboBox.setEditable(true);

        // Thêm AutoComplete cho ComboBox ga đi
        setupAutoComplete(depStationComboBox);

        // Thành phần tìm kiếm theo ga đến
        JLabel arrStationLabel = new JLabel("Ga đến:");

        // Sử dụng model cho autocomplete ComboBox
        DefaultComboBoxModel<String> arrStationModel = new DefaultComboBoxModel<>();
        arrStationModel.addElement("Tất cả");

        // Tạo ComboBox cho ga đến với AutoComplete
        JComboBox<String> arrStationComboBox = new JComboBox<>(arrStationModel);
        arrStationComboBox.setPreferredSize(new Dimension(150, 28));
        arrStationComboBox.setEditable(true);

        // Thêm AutoComplete cho ComboBox ga đến
        setupAutoComplete(arrStationComboBox);

        // Tải danh sách ga từ cơ sở dữ liệu và thêm vào ComboBox
        loadStationList(depStationComboBox, arrStationComboBox);

        // Thành phần tìm kiếm theo giờ đi - THAY ĐỔI: Dùng spinner thay cho TextField
        JLabel depTimeLabel = new JLabel("Giờ đi:");

        // Tạo mô hình cho giờ (0-23) và phút (0-59)
        SpinnerNumberModel hourModel = new SpinnerNumberModel(0, 0, 23, 1);
        SpinnerNumberModel minuteModel = new SpinnerNumberModel(0, 0, 59, 1);

        JSpinner hourSpinner = new JSpinner(hourModel);
        JSpinner minuteSpinner = new JSpinner(minuteModel);

        // Thiết lập editor để hiển thị đúng định dạng
        JSpinner.NumberEditor hourEditor = new JSpinner.NumberEditor(hourSpinner, "00");
        hourSpinner.setEditor(hourEditor);
        JSpinner.NumberEditor minuteEditor = new JSpinner.NumberEditor(minuteSpinner, "00");
        minuteSpinner.setEditor(minuteEditor);

        // Panel chứa các spinner giờ và phút
        JPanel timeSpinnerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

        // Thêm nhãn ":" giữa giờ và phút
        JLabel separator = new JLabel(" : ");
        separator.setFont(new Font("SansSerif", Font.BOLD, 14));

        timeSpinnerPanel.add(hourSpinner);
        timeSpinnerPanel.add(separator);
        timeSpinnerPanel.add(minuteSpinner);

        // Checkbox để người dùng có thể chọn tìm theo giờ hay không
        JCheckBox useTimeCheckBox = new JCheckBox("Tìm theo giờ");
        useTimeCheckBox.addActionListener(e -> {
            boolean selected = useTimeCheckBox.isSelected();
            hourSpinner.setEnabled(selected);
            minuteSpinner.setEnabled(selected);
        });

        // Mặc định không tìm kiếm theo giờ
        useTimeCheckBox.setSelected(false);
        hourSpinner.setEnabled(false);
        minuteSpinner.setEnabled(false);

        // Thêm các thành phần vào panel hàng thứ hai
        secondRowPanel.add(depStationLabel);
        secondRowPanel.add(depStationComboBox);
        secondRowPanel.add(arrStationLabel);
        secondRowPanel.add(arrStationComboBox);
        secondRowPanel.add(depTimeLabel);
        secondRowPanel.add(timeSpinnerPanel);
        secondRowPanel.add(useTimeCheckBox);

        // Panel cho các nút tìm kiếm và làm mới
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        // Nút tìm kiếm với biểu tượng tùy chỉnh
        searchButton = new JButton("Tìm Kiếm");
        searchButton.setIcon(createSearchIcon(16, 16));
        searchButton.addActionListener(e -> {
            try {
                // Lấy các giá trị tìm kiếm
                Date selectedDate = dateChooser.getDate();
                if (selectedDate == null) {
                    throw new IllegalArgumentException("Vui lòng chọn ngày hợp lệ.");
                }

                LocalDate localDate = selectedDate.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();

                String gaDi = depStationComboBox.getSelectedItem().toString();
                if (gaDi.equals("Tất cả")) gaDi = null;

                String gaDen = arrStationComboBox.getSelectedItem().toString();
                if (gaDen.equals("Tất cả")) gaDen = null;

                // Lấy giá trị giờ nếu checkbox được chọn
                String gioDi = null;
                if (useTimeCheckBox.isSelected()) {
                    int hour = (int) hourSpinner.getValue();
                    int minute = (int) minuteSpinner.getValue();
                    gioDi = String.format("%02d:%02d", hour, minute);
                }

                // Thực hiện tìm kiếm dựa trên các trường đã nhập
                searchSchedules(localDate, gaDi, gaDen, gioDi);

            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(LichTrinhTauPanel.this,
                        ex.getMessage(), "Lỗi tìm kiếm", JOptionPane.ERROR_MESSAGE);
            } catch (RemoteException ex) {
                LOGGER.log(Level.SEVERE, "Lỗi khi tìm kiếm lịch trình", ex);
                showErrorMessage("Lỗi khi tìm kiếm lịch trình", ex);
            }
        });

        // Nút làm mới với biểu tượng tùy chỉnh
        refreshButton = new JButton("Làm Mới");
        refreshButton.setIcon(createRefreshIcon(16, 16));
        refreshButton.addActionListener(e -> {
            // Đặt lại các trường tìm kiếm về giá trị mặc định
            dateChooser.setDate(new Date());
            filterComboBox.setSelectedItem("Tất cả");
            depStationComboBox.setSelectedItem("Tất cả");
            arrStationComboBox.setSelectedItem("Tất cả");
            useTimeCheckBox.setSelected(false);
            hourSpinner.setValue(0);
            minuteSpinner.setValue(0);
            hourSpinner.setEnabled(false);
            minuteSpinner.setEnabled(false);

            // Làm mới dữ liệu
            refreshData();
        });

        // Thêm các nút vào panel nút
        buttonPanel.add(searchButton);
        buttonPanel.add(refreshButton);

        // Tổng hợp tất cả các panel vào panel chính
        mainSearchPanel.add(firstRowPanel, BorderLayout.NORTH);
        mainSearchPanel.add(secondRowPanel, BorderLayout.CENTER);
        mainSearchPanel.add(buttonPanel, BorderLayout.SOUTH);

        outerPanel.add(mainSearchPanel, BorderLayout.CENTER);
        return outerPanel;
    }

    /**
     * Thiết lập chức năng AutoComplete cho JComboBox
     * @param comboBox JComboBox cần thêm chức năng AutoComplete
     */
    private void setupAutoComplete(JComboBox<String> comboBox) {
        final JTextField editor = (JTextField) comboBox.getEditor().getEditorComponent();

        // Tạo một ArrayList để lưu các mục ban đầu
        final List<String> originalItems = new ArrayList<>();

        // Đảm bảo comboBox luôn có "Tất cả" là lựa chọn mặc định đầu tiên
        comboBox.addItem("Tất cả");
        originalItems.add("Tất cả");

        // Tạo một lọc văn bản để xử lý sự kiện bàn phím thay vì dùng DocumentListener
        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                // Bỏ qua các phím đặc biệt
                if (e.getKeyCode() == KeyEvent.VK_ENTER ||
                        e.getKeyCode() == KeyEvent.VK_ESCAPE ||
                        e.getKeyCode() == KeyEvent.VK_UP ||
                        e.getKeyCode() == KeyEvent.VK_DOWN) {
                    return;
                }

                // Lấy văn bản hiện tại trong editor
                String text = editor.getText();

                // Không thực hiện lọc nếu văn bản quá ngắn
                if (text.length() < 1) {
                    return;
                }

                // Sử dụng SwingUtilities.invokeLater để tránh lỗi khi sửa đổi mô hình trong lúc xử lý sự kiện
                SwingUtilities.invokeLater(() -> {
                    filterItems(comboBox, text, originalItems);
                });
            }
        });
    }
    private void filterItems(JComboBox<String> comboBox, String text, List<String> originalItems) {
        // Lưu lại các lựa chọn hiện tại
        Object selectedItem = comboBox.getSelectedItem();
        String typedText = text.toLowerCase();

        // Đóng popup trong khi thay đổi mục
        boolean isPopupVisible = comboBox.isPopupVisible();
        if (isPopupVisible) {
            comboBox.hidePopup();
        }

        // Tạo danh sách các mục phù hợp
        List<String> matchingItems = originalItems.stream()
                .filter(item -> item.toLowerCase().contains(typedText) || item.equals("Tất cả"))
                .collect(Collectors.toList());

        // Làm mới mô hình chỉ khi cần thiết
        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) comboBox.getModel();
        model.removeAllElements();

        // Luôn thêm "Tất cả" trước tiên
        model.addElement("Tất cả");

        // Thêm các mục phù hợp (trừ "Tất cả" đã thêm)
        matchingItems.stream()
                .filter(item -> !item.equals("Tất cả"))
                .forEach(model::addElement);

        // Đặt lại văn bản cho editor
        comboBox.getEditor().setItem(text);

        // Hiển thị lại popup nếu trước đó đã mở
        if (isPopupVisible && model.getSize() > 0) {
            comboBox.showPopup();
        }
    }
    /**
     * Tải danh sách ga vào các JComboBox
     */
    private void loadStationList(JComboBox<String> depComboBox, JComboBox<String> arrComboBox) {
        try {
            if (isConnected && lichTrinhTauDAO != null) {
                // Lấy danh sách ga từ cơ sở dữ liệu
                List<String> stations = lichTrinhTauDAO.getAllStations();

                // Xóa tất cả các mục hiện có
                depComboBox.removeAllItems();
                arrComboBox.removeAllItems();

                // Luôn thêm "Tất cả" trước tiên
                depComboBox.addItem("Tất cả");
                arrComboBox.addItem("Tất cả");

                if (stations != null && !stations.isEmpty()) {
                    // Thêm các ga vào ComboBox một cách an toàn
                    for (String station : stations) {
                        depComboBox.addItem(station);
                        arrComboBox.addItem(station);
                    }

                    // Thiết lập AutoComplete cho các ComboBox sau khi đã thêm tất cả các mục
                    setupComboBoxFiltering(depComboBox);
                    setupComboBoxFiltering(arrComboBox);

                    LOGGER.info("Đã tải thành công " + stations.size() + " ga từ cơ sở dữ liệu");
                } else {
                    LOGGER.warning("Không tìm thấy ga nào trong cơ sở dữ liệu");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tải danh sách ga: " + e.getMessage(), e);
        }
    }
    private void setupComboBoxFiltering(JComboBox<String> comboBox) {
        // Đảm bảo ComboBox có thể chỉnh sửa
        comboBox.setEditable(true);

        // Lấy tất cả các mục hiện tại
        List<String> allItems = new ArrayList<>();
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            allItems.add(comboBox.getItemAt(i));
        }

        // Thiết lập renderer đặc biệt để highlight từ khóa tìm kiếm
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (c instanceof JLabel && value != null) {
                    ((JLabel) c).setText(value.toString());
                }

                return c;
            }
        });

        // Sử dụng KeyAdapter để xử lý sự kiện gõ phím
        JTextField editor = (JTextField) comboBox.getEditor().getEditorComponent();
        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                // Không xử lý các phím đặc biệt
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_UP ||
                        e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    return;
                }

                String text = editor.getText().toLowerCase();

                // Sử dụng SwingUtilities.invokeLater để tránh lỗi khi thay đổi mô hình trong sự kiện
                SwingUtilities.invokeLater(() -> {
                    // Lưu lại trạng thái popup
                    boolean wasVisible = comboBox.isPopupVisible();
                    comboBox.hidePopup();

                    // Tạo model mới
                    DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();

                    // Luôn thêm "Tất cả" vào đầu tiên
                    model.addElement("Tất cả");

                    // Thêm các mục phù hợp
                    for (String item : allItems) {
                        if (!item.equals("Tất cả") && item.toLowerCase().contains(text)) {
                            model.addElement(item);
                        }
                    }

                    // Áp dụng model mới
                    comboBox.setModel(model);

                    // Đặt lại văn bản
                    comboBox.getEditor().setItem(text);
                    editor.setCaretPosition(text.length());

                    // Hiển thị lại popup nếu trước đó đã mở và có kết quả
                    if ((wasVisible || !text.isEmpty()) && model.getSize() > 0) {
                        comboBox.showPopup();
                    }
                });
            }
        });
    }

    private void searchSchedules(LocalDate date, String gaDi, String gaDen, String gioDi) throws RemoteException {
        if (!isConnected || lichTrinhTauDAO == null) {
            reconnectAndLoadData(date);
            if (!isConnected) {
                throw new RemoteException("Không thể kết nối đến server");
            }
        }

        tableModel.setRowCount(0);
        List<LichTrinhTau> schedules;

        try {
            // Quyết định phương thức tìm kiếm dựa trên các tham số
            if (gaDi == null && gaDen == null && gioDi == null) {
                // Chỉ tìm theo ngày
                schedules = lichTrinhTauDAO.getListLichTrinhTauByDate(date);
            } else if (gaDi != null && gaDen == null && gioDi == null) {
                // Tìm theo ngày và ga đi
                schedules = lichTrinhTauDAO.getListLichTrinhTauByDateAndGaDi(date, gaDi);
            } else if (gaDi != null && gaDen != null && gioDi == null) {
                // Tìm theo ngày, ga đi và ga đến
                schedules = lichTrinhTauDAO.getListLichTrinhTauByDateAndGaDiGaDen(date, gaDi, gaDen);
            } else if (gaDi != null && gaDen != null && gioDi != null) {
                // Tìm theo tất cả các trường
                schedules = lichTrinhTauDAO.getListLichTrinhTauByDateAndGaDiGaDenAndGioDi(date, gaDi, gaDen, gioDi);
            } else {
                // Trường hợp còn lại: có ga đến nhưng không có ga đi, hoặc có giờ đi nhưng thiếu ga đi/đến
                JOptionPane.showMessageDialog(this,
                        "Để tìm kiếm với ga đến, bạn cần chọn ga đi trước.\n" +
                                "Để tìm kiếm với giờ đi, bạn cần chọn cả ga đi và ga đến.",
                        "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            if (schedules == null || schedules.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Không tìm thấy lịch trình nào phù hợp với tiêu chí tìm kiếm.",
                        "Thông báo",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Áp dụng bộ lọc trạng thái nếu được chọn
            Object selectedItem = filterComboBox.getSelectedItem();

            for (LichTrinhTau schedule : schedules) {
                // Kiểm tra xem lịch trình có phù hợp với bộ lọc trạng thái hay không
                if (matchesStatusFilter(schedule, selectedItem)) {
                    tableModel.addRow(createTableRow(schedule));
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi chi tiết khi tìm kiếm: " + e.getMessage(), e);
            throw new RemoteException("Lỗi khi tìm kiếm: " + e.getMessage(), e);
        }
    }

    // Phương thức mới để kiểm tra trạng thái
    private boolean matchesStatusFilter(LichTrinhTau schedule, Object filterValue) {
        // Nếu là "Tất cả" hoặc null, hiển thị tất cả
        if (filterValue == null || "Tất cả".equals(filterValue)) {
            return true;
        }

        // Nếu lịch trình không có trạng thái, không phù hợp với bất kỳ bộ lọc nào ngoại trừ "Tất cả"
        if (schedule.getTrangThai() == null) {
            return false;
        }

        // Nếu filterValue là một TrangThai enum
        if (filterValue instanceof TrangThai) {
            return schedule.getTrangThai() == filterValue;
        }

        // Nếu filterValue là một chuỗi (giá trị hiển thị)
        String filterString = filterValue.toString();

        // So sánh với giá trị hiển thị của trạng thái
        return schedule.getTrangThai().getValue().equals(filterString);
    }
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Danh Sách Lịch Trình"));

        // Create table model with non-editable cells
        String[] columns = {"ID", "Ngày Đi", "Mã Tàu - Tên Tàu", "Tuyến Đường", "Giờ Đi", "Giờ Đến", "Trạng Thái"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Create and configure table
        scheduleTable = new JTable(tableModel);
        scheduleTable.setRowHeight(25);
        scheduleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scheduleTable.setAutoCreateRowSorter(true);

        // Thiết lập màu nền cho hàng lẻ và hàng chẵn
        scheduleTable.setDefaultRenderer(Object.class, new CustomTableCellRenderer());

        // Style the table header
        JTableHeader header = scheduleTable.getTableHeader();
        header.setFont(new Font("Arial", Font.BOLD, 12));
        header.setBackground(new Color(41, 128, 185)); // Màu xanh dương cho header
        header.setForeground(Color.WHITE);  // Màu trắng cho chữ

        // Áp dụng custom UI cho bảng để có hiệu ứng hover
        setupTableUI();

        // Thiết lập phím tắt và menu ngữ cảnh
        setupKeyBindings();
        setupContextMenu();

        // Add table to scroll pane
        JScrollPane scrollPane = new JScrollPane(scheduleTable);
        scrollPane.getViewport().setBackground(Color.WHITE);

        // Đảm bảo hiển thị header đúng màu sắc
        scrollPane.setColumnHeaderView(header);

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void setupTableUI() {
        // Đặt một số thuộc tính cho bảng
        scheduleTable.setShowHorizontalLines(true);
        scheduleTable.setShowVerticalLines(true);
        scheduleTable.setGridColor(new Color(230, 230, 230));
        scheduleTable.setBackground(Color.WHITE);
        scheduleTable.setForeground(Color.BLACK);
        scheduleTable.setSelectionBackground(new Color(66, 139, 202)); // Màu khi chọn chính thức
        scheduleTable.setSelectionForeground(Color.WHITE);

        // Biến để lưu trạng thái lựa chọn và hiệu ứng hover
        final int[] permanentSelectedRow = {-1}; // Lựa chọn chính thức
        final int[] hoverRow = {-1}; // Dòng đang hover
        final boolean[] isUserSelection = {false}; // Cờ đánh dấu người dùng đã chọn một dòng

        // Thêm hiệu ứng hover bằng cách sử dụng MouseMotionAdapter
        scheduleTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                // Lấy dòng hiện tại đang hover
                Point point = e.getPoint();
                int currentRow = scheduleTable.rowAtPoint(point);

                // Nếu di chuyển đến một dòng mới
                if (currentRow != hoverRow[0]) {
                    // Cập nhật dòng đang hover
                    hoverRow[0] = currentRow;

                    // Kiểm tra tính hợp lệ của chỉ số hàng
                    boolean isValidRow = currentRow >= 0 && currentRow < scheduleTable.getRowCount();
                    boolean isPermanentSelectionValid = permanentSelectedRow[0] >= 0 && permanentSelectedRow[0] < scheduleTable.getRowCount();

                    // Nếu người dùng đã có lựa chọn chính thức, chỉ hiển thị hiệu ứng hover
                    if (isUserSelection[0] && isPermanentSelectionValid) {
                        // Nạp lại chọn chính thức
                        scheduleTable.setSelectionBackground(new Color(66, 139, 202));
                        scheduleTable.setSelectionForeground(Color.WHITE);
                        scheduleTable.setRowSelectionInterval(permanentSelectedRow[0], permanentSelectedRow[0]);

                        // Vẽ hiệu ứng hover cho dòng hiện tại
                        scheduleTable.repaint();
                    }
                    // Nếu không có lựa chọn chính thức và có hàng hợp lệ, áp dụng hiệu ứng hover
                    else if (isValidRow) {
                        scheduleTable.setSelectionBackground(new Color(173, 216, 230)); // Màu xanh nhạt cho hover
                        scheduleTable.setSelectionForeground(Color.BLACK);
                        scheduleTable.setRowSelectionInterval(currentRow, currentRow);
                    } else {
                        // Không có hàng hợp lệ để hover, xóa lựa chọn
                        scheduleTable.clearSelection();
                    }
                }
            }
        });

        // Xử lý các sự kiện chuột khác
        scheduleTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                hoverRow[0] = -1; // Xóa trạng thái hover

                // Kiểm tra tính hợp lệ của lựa chọn chính thức
                boolean isPermanentSelectionValid = permanentSelectedRow[0] >= 0 &&
                        permanentSelectedRow[0] < scheduleTable.getRowCount();

                // Nếu có lựa chọn chính thức và hợp lệ, giữ nguyên lựa chọn đó
                if (isUserSelection[0] && isPermanentSelectionValid) {
                    scheduleTable.setSelectionBackground(new Color(66, 139, 202));
                    scheduleTable.setSelectionForeground(Color.WHITE);
                    scheduleTable.setRowSelectionInterval(permanentSelectedRow[0], permanentSelectedRow[0]);
                }
                // Nếu chỉ là hover, xóa lựa chọn khi rời khỏi bảng
                else {
                    scheduleTable.clearSelection();
                }
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = scheduleTable.getSelectedRow();
                if (row >= 0 && row < scheduleTable.getRowCount()) {
                    // Lưu lựa chọn chính thức
                    permanentSelectedRow[0] = row;
                    isUserSelection[0] = true;

                    // Đặt màu chọn thành màu xanh đậm
                    scheduleTable.setSelectionBackground(new Color(66, 139, 202));
                    scheduleTable.setSelectionForeground(Color.WHITE);
                }
            }
        });

        // Theo dõi các thay đổi trong lựa chọn
        scheduleTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = scheduleTable.getSelectedRow();

                // Kiểm tra tính hợp lệ của lựa chọn chính thức
                boolean isPermanentSelectionValid = permanentSelectedRow[0] >= 0 &&
                        permanentSelectedRow[0] < scheduleTable.getRowCount();

                // Nếu người dùng đã chọn một dòng nhưng bây giờ không có dòng nào được chọn,
                // và chọn cuối cùng vẫn hợp lệ, thì khôi phục lựa chọn đó
                if (selectedRow == -1 && isUserSelection[0] && isPermanentSelectionValid) {
                    try {
                        scheduleTable.setRowSelectionInterval(permanentSelectedRow[0], permanentSelectedRow[0]);
                    } catch (IllegalArgumentException ex) {
                        // Xử lý trường hợp chỉ số hàng không hợp lệ
                        LOGGER.warning("Không thể khôi phục lựa chọn hàng: " + ex.getMessage());
                        // Đặt lại các biến trạng thái
                        isUserSelection[0] = false;
                        permanentSelectedRow[0] = -1;
                    }
                }
            }
        });

        // Sử dụng custom renderer để hiển thị màu hover
        scheduleTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component comp = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);

                // Nếu dòng này là lựa chọn chính thức
                if (isSelected && row == permanentSelectedRow[0] && isUserSelection[0]) {
                    comp.setBackground(new Color(66, 139, 202)); // Màu xanh đậm cho lựa chọn
                    comp.setForeground(Color.WHITE);
                }
                // Nếu dòng này đang được hover
                else if (isSelected && row == hoverRow[0]) {
                    comp.setBackground(new Color(173, 216, 230)); // Màu xanh nhạt cho hover
                    comp.setForeground(Color.BLACK);
                }
                // Màu sắc thông thường cho các dòng lẻ chẵn
                else {
                    if (row % 2 == 0) {
                        comp.setBackground(Color.WHITE);
                    } else {
                        comp.setBackground(new Color(245, 245, 245)); // Màu xám nhạt
                    }
                    comp.setForeground(Color.BLACK);
                }

                // Canh lề và font
                ((JLabel) comp).setHorizontalAlignment(SwingConstants.CENTER);
                comp.setFont(new Font("Arial", Font.PLAIN, 12));

                return comp;
            }
        });

        // Thiết lập UI cho header (giữ nguyên)
        scheduleTable.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                label.setBackground(new Color(41, 128, 185));
                label.setForeground(Color.WHITE);
                label.setFont(new Font("Arial", Font.BOLD, 12));
                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(200, 200, 200)),
                        BorderFactory.createEmptyBorder(5, 5, 5, 5)
                ));
                label.setHorizontalAlignment(JLabel.CENTER);
                return label;
            }
        });
    }

    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        // Nút trợ lý ảo/chatbot
        JButton chatbotButton = new JButton("Trợ lý ảo");
        chatbotButton.setIcon(createChatbotIcon(16, 16));
        chatbotButton.addActionListener(e -> showChatbot());

        // Thêm nút Dự đoán thời gian
        JButton predictButton = new JButton("Dự đoán thời gian");
        predictButton.setIcon(createPredictIcon(16, 16));
        predictButton.addActionListener(e -> showPrediction());
        // Create action buttons with custom icons
        addButton = new JButton("Thêm Lịch Trình");
        addButton.setIcon(createAddIcon(16, 16));
        addButton.addActionListener(e -> addSchedule());

        // Nút thêm nhiều lịch trình tự động
        JButton batchAddButton = new JButton("Tạo Nhiều Lịch Trình");
        batchAddButton.setIcon(createBatchIcon(16, 16));
        batchAddButton.addActionListener(e -> createBatchSchedules());

        editButton = new JButton("Chỉnh Sửa");
        editButton.setIcon(createEditIcon(16, 16));
        editButton.addActionListener(e -> editSchedule());

        deleteButton = new JButton("Xóa");
        deleteButton.setIcon(createDeleteIcon(16, 16));
        deleteButton.addActionListener(e -> deleteSchedule());

        // Add buttons to panel
        panel.add(chatbotButton);
        panel.add(predictButton);
        panel.add(addButton);
        panel.add(batchAddButton);
        panel.add(editButton);
        panel.add(deleteButton);
        return panel;
    }

    private Icon createChatbotIcon(int width, int height) {
        return new ImageIcon(createIconImage(width, height, new Color(52, 152, 219), g -> {
            // Vẽ hình chat bubble đơn giản
            g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(2, 3, 12, 9, 4, 4);
            g.drawLine(4, 12, 6, 14);
            g.drawLine(6, 14, 8, 12);

            // Vẽ các dấu chấm trong bubble
            g.fillOval(5, 8, 1, 1);
            g.fillOval(8, 8, 1, 1);
            g.fillOval(11, 8, 1, 1);
        }));
    }
    private Icon createPredictIcon(int width, int height) {
        return new ImageIcon(createIconImage(width, height, new Color(155, 89, 182), g -> {
            // Vẽ hình đồng hồ đơn giản
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval(3, 3, 10, 10);
            g.drawLine(8, 8, 8, 5);
            g.drawLine(8, 8, 11, 10);
        }));
    }
    private void showChatbot() {
        // Khởi tạo dialog nếu chưa có
        if (chatbotDialog == null) {
            // Tìm JFrame cha chứa panel này
            JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            chatbotDialog = new ChatbotDialog(parentFrame);
            chatbotDialog.attachToOwner();
        }

        // Hiển thị hoặc ẩn dialog tùy thuộc vào trạng thái hiện tại
        if (chatbotDialog.isVisible()) {
            chatbotDialog.setVisible(false);
        } else {
            chatbotDialog.showAtCorner();
        }
    }

    /**
     * Hiển thị dialog dự đoán thời gian di chuyển
     */
    private void showPrediction() {
        int selectedRow = scheduleTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn lịch trình để xem dự đoán.",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Đảm bảo chuyển đổi chỉ số từ view sang model nếu bảng có sắp xếp
        int modelRow = selectedRow;
        if (scheduleTable.getRowSorter() != null) {
            modelRow = scheduleTable.getRowSorter().convertRowIndexToModel(selectedRow);
        }

        // Lấy mã lịch trình
        String maLich = tableModel.getValueAt(modelRow, 0).toString();

        try {
            // Lấy đối tượng lịch trình
            LichTrinhTau lichTrinh = lichTrinhTauDAO.getById(maLich);
            if (lichTrinh == null) {
                JOptionPane.showMessageDialog(this,
                        "Không tìm thấy thông tin lịch trình.",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Dự đoán thời gian di chuyển
            AITravelTimePredictor.PredictionResult prediction = aiPredictor.predictTravelTime(lichTrinh);

            // Tạo và hiển thị dialog dự đoán
            showPredictionDialog(lichTrinh, prediction);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi dự đoán thời gian: " + e.getMessage(), e);
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi dự đoán thời gian: " + e.getMessage(),
                    "Lỗi dự đoán",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Hiển thị dialog chi tiết dự đoán thời gian
     */
    private void showPredictionDialog(LichTrinhTau lichTrinh, AITravelTimePredictor.PredictionResult prediction) {
        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentFrame, "Dự đoán thời gian di chuyển", true);
        dialog.setSize(550, 550);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setResizable(false);

        // Panel chính
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Tiêu đề
        JLabel titleLabel = new JLabel("Dự đoán thời gian di chuyển", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Panel thông tin
        JPanel infoPanel = new JPanel(new GridLayout(6, 2, 10, 10));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Thông tin lịch trình"));

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        infoPanel.add(new JLabel("Mã lịch trình:"));
        infoPanel.add(new JLabel(lichTrinh.getMaLich()));

        infoPanel.add(new JLabel("Tàu:"));
        infoPanel.add(new JLabel(lichTrinh.getTau().getMaTau() + " - " + lichTrinh.getTau().getTenTau()));

        infoPanel.add(new JLabel("Tuyến đường:"));
        infoPanel.add(new JLabel(lichTrinh.getTau().getTuyenTau().getGaDi() + " → " + lichTrinh.getTau().getTuyenTau().getGaDen()));

        infoPanel.add(new JLabel("Ngày đi:"));
        infoPanel.add(new JLabel(lichTrinh.getNgayDi().format(dateFormatter)));

        infoPanel.add(new JLabel("Giờ khởi hành:"));
        infoPanel.add(new JLabel(lichTrinh.getGioDi().format(timeFormatter)));

        infoPanel.add(new JLabel("Trạng thái:"));
        infoPanel.add(new JLabel(lichTrinh.getTrangThai().getValue()));

        // Panel dự đoán
        JPanel predictionPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        predictionPanel.setBorder(BorderFactory.createTitledBorder("Kết quả dự đoán"));

        predictionPanel.add(new JLabel("Thời gian di chuyển dự kiến:"));
        predictionPanel.add(new JLabel(prediction.getFormattedTravelTime()));

        predictionPanel.add(new JLabel("Giờ đến dự kiến:"));
        predictionPanel.add(new JLabel(prediction.getEstimatedArrivalTime(lichTrinh.getGioDi()).format(timeFormatter)));

        predictionPanel.add(new JLabel("Độ chính xác dự đoán:"));

        // Tạo thanh độ chính xác
        JProgressBar accuracyBar = new JProgressBar(0, 100);
        accuracyBar.setValue(prediction.getAccuracyPercentage());
        accuracyBar.setStringPainted(true);
        accuracyBar.setString(prediction.getAccuracyPercentage() + "%");

        // Đặt màu cho thanh độ chính xác
        if (prediction.getAccuracyPercentage() >= 85) {
            accuracyBar.setForeground(new Color(46, 204, 113)); // Xanh lá
        } else if (prediction.getAccuracyPercentage() >= 70) {
            accuracyBar.setForeground(new Color(241, 196, 15)); // Vàng
        } else {
            accuracyBar.setForeground(new Color(231, 76, 60)); // Đỏ
        }

        predictionPanel.add(accuracyBar);

        // Panel giải thích
        JPanel explanationPanel = new JPanel(new BorderLayout(5, 5));
        explanationPanel.setBorder(BorderFactory.createTitledBorder("Giải thích"));

        JTextArea explanationArea = new JTextArea(prediction.getExplanation());
        explanationArea.setEditable(false);
        explanationArea.setLineWrap(true);
        explanationArea.setWrapStyleWord(true);
        explanationArea.setFont(new Font("Arial", Font.PLAIN, 12));
        explanationArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JScrollPane scrollPane = new JScrollPane(explanationArea);
        scrollPane.setPreferredSize(new Dimension(500, 120));
        explanationPanel.add(scrollPane, BorderLayout.CENTER);

        // Panel nút
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton("Đóng");
        closeButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(closeButton);

        // Thêm các thành phần vào panel chính
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        mainPanel.add(infoPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(predictionPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(explanationPanel);

        // Thêm vào dialog
        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Hiển thị dialog
        dialog.setVisible(true);
    }

    // Phương thức tạo icon cho nút Batch
    private Icon createBatchIcon(int width, int height) {
        return new ImageIcon(createIconImage(width, height, new Color(41, 128, 185), icon -> {
            // Vẽ biểu tượng nhiều dòng xếp chồng
            Graphics2D g2 = icon;
            g2.setStroke(new BasicStroke(1.5f));

            // Vẽ 3 tài liệu chồng lên nhau
            g2.drawRect(3, 2, 8, 10);
            g2.drawRect(5, 4, 8, 10);
            g2.drawRect(7, 6, 8, 10);

            // Vẽ dấu cộng nhỏ
            g2.drawLine(12, 3, 14, 3);
            g2.drawLine(13, 2, 13, 4);
        }));
    }
    // Method to create a search icon
    private Icon createSearchIcon(int width, int height) {
        return new ImageIcon(createIconImage(width, height, new Color(41, 128, 185), icon -> {
            // Draw a magnifying glass
            Graphics2D g2 = icon;
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(3, 3, 7, 7);
            g2.drawLine(9, 9, 13, 13);
        }));
    }

    // Method to create a refresh icon
    private Icon createRefreshIcon(int width, int height) {
        return new ImageIcon(createIconImage(width, height, new Color(41, 128, 185), icon -> {
            // Draw circular arrows
            Graphics2D g2 = icon;
            g2.setStroke(new BasicStroke(2));
            g2.drawArc(3, 3, 10, 10, 0, 270);
            g2.drawLine(13, 8, 13, 3);
            g2.drawLine(13, 3, 8, 3);
        }));
    }

    // Method to create an add icon (plus sign)
    private Icon createAddIcon(int width, int height) {
        return new ImageIcon(createIconImage(width, height, new Color(46, 204, 113), icon -> {
            // Draw a plus sign
            Graphics2D g2 = icon;
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(8, 4, 8, 12);
            g2.drawLine(4, 8, 12, 8);
        }));
    }

    // Method to create an edit icon
    private Icon createEditIcon(int width, int height) {
        return new ImageIcon(createIconImage(width, height, new Color(243, 156, 18), icon -> {
            // Draw a pencil
            Graphics2D g2 = icon;
            g2.setStroke(new BasicStroke(1));
            g2.drawLine(3, 13, 13, 3);
            g2.drawLine(13, 3, 11, 1);
            g2.drawLine(3, 13, 1, 11);
            g2.drawLine(1, 11, 3, 3);
        }));
    }

    // Method to create a delete icon
    private Icon createDeleteIcon(int width, int height) {
        return new ImageIcon(createIconImage(width, height, new Color(231, 76, 60), icon -> {
            // Draw a trash can
            Graphics2D g2 = icon;
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRect(4, 5, 8, 10);
            g2.drawLine(3, 5, 13, 5);
            g2.drawLine(6, 3, 10, 3);
            g2.drawLine(6, 5, 6, 3);
            g2.drawLine(10, 5, 10, 3);
            g2.drawLine(6, 8, 6, 12);
            g2.drawLine(10, 8, 10, 12);
        }));
    }

    // Helper method to create icon images
    private Image createIconImage(int width, int height, Color color, DrawIcon drawer) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        // Set up high quality rendering
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // Draw the icon
        g2.setColor(color);
        drawer.draw(g2);
        g2.dispose();

        return image;
    }

    // Interface for drawing icons
    private interface DrawIcon {
        void draw(Graphics2D g2);
    }

    private void searchButtonClicked(ActionEvent e) {
        try {
            Date selectedDate = dateChooser.getDate();
            if (selectedDate == null) {
                throw new IllegalArgumentException("Vui lòng chọn ngày hợp lệ.");
            }

            // Convert java.util.Date to java.time.LocalDate
            LocalDate localDate = selectedDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            System.out.println("Đang tìm kiếm cho ngày: " + localDate);

            if (isConnected) {
                // Nếu đang ở chế độ xem bảng, tải dữ liệu vào bảng
                if (viewTabbedPane.getSelectedIndex() == 0) {
                    loadScheduleData(localDate);
                }
                // Nếu đang ở chế độ xem lịch, chuyển đến ngày được chọn
                else if (viewTabbedPane.getSelectedIndex() == 1 && calendarPanel != null) {
                    calendarPanel.setSelectedDate(localDate);
                }
            } else {
                reconnectAndLoadData(localDate);
            }
        } catch (IllegalArgumentException ex) {
            LOGGER.log(Level.WARNING, "Invalid date selection", ex);
            showErrorMessage("Vui lòng chọn ngày hợp lệ.", null);
        } catch (RemoteException ex) {
            LOGGER.log(Level.SEVERE, "Error loading schedule data", ex);
            showErrorMessage("Lỗi khi tải dữ liệu", ex);
        }
    }

    private void loadScheduleData(LocalDate date) throws RemoteException {
        if (!isConnected || lichTrinhTauDAO == null) {
            reconnectAndLoadData(date);
            if (!isConnected) {
                throw new RemoteException("Not connected to RMI server");
            }
        }

        tableModel.setRowCount(0);

        try {
            System.out.println("Đang tìm kiếm với ngày: " + date);
            List<LichTrinhTau> schedules = lichTrinhTauDAO.getListLichTrinhTauByDate(date);
            System.out.println("Kết quả trả về: " + (schedules != null ? schedules.size() + " lịch" : "null"));

            if (schedules == null || schedules.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Không có lịch trình nào cho ngày: " + date,
                        "Thông báo",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Apply filter if selected
            String filterOption = (String) filterComboBox.getSelectedItem();
            for (LichTrinhTau schedule : schedules) {
                if (matchesFilter(schedule, filterOption)) {
                    tableModel.addRow(createTableRow(schedule));
                }
            }

        } catch (Exception e) {
            System.out.println("Lỗi chi tiết: " + e.getMessage());
            e.printStackTrace();
            throw new RemoteException("Lỗi khi tìm kiếm: " + e.getMessage(), e);
        }
    }

    private boolean matchesFilter(LichTrinhTau schedule, String filterOption) {
        if (filterOption == null || filterOption.equals("Tất cả")) {
            return true;
        }

        // So sánh trực tiếp với trạng thái
        return filterOption.equals(schedule.getTrangThai());
    }

    private Object[] createTableRow(LichTrinhTau schedule) {
        // Định dạng ngày đi
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String formattedDate = schedule.getNgayDi().format(dateFormatter);

        // Định dạng giờ đi
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String departureTime = schedule.getGioDi().format(timeFormatter);

        // Tính và định dạng giờ đến dự kiến
        String arrivalTime;
        try {
            // Sử dụng AI để dự đoán thời gian di chuyển
            AITravelTimePredictor.PredictionResult prediction = aiPredictor.predictTravelTime(schedule);
            LocalTime estimatedArrival = prediction.getEstimatedArrivalTime(schedule.getGioDi());
            arrivalTime = estimatedArrival.format(timeFormatter);
        } catch (Exception e) {
            // Nếu gặp lỗi, sử dụng phương pháp ước tính đơn giản
            LOGGER.warning("Không thể dự đoán thời gian đến, sử dụng ước tính đơn giản: " + e.getMessage());
            LocalTime estimatedArrival = schedule.getGioDi().plusHours(estimateTravelTime(schedule));
            arrivalTime = estimatedArrival.format(timeFormatter);
        }

        // Lấy chuỗi hiển thị của trạng thái từ getValue()
        String statusDisplay = schedule.getTrangThai().getValue();

        return new Object[]{
                schedule.getMaLich(),
                formattedDate,
                schedule.getTau().getMaTau() + " - " + schedule.getTau().getTenTau(),
                "TT" + schedule.getTau().getMaTau() + " - " +
                        schedule.getTau().getTuyenTau().getGaDi() + " - " +
                        schedule.getTau().getTuyenTau().getGaDen(),
                departureTime,
                arrivalTime,
                statusDisplay
        };
    }

    /**
     * Hiển thị giao diện tạo lịch trình tự động trong khoảng thời gian
     */
    private void createBatchSchedules() {
        // Tạo dialog cho việc thêm nhiều lịch trình
        JDialog batchDialog = new JDialog();
        batchDialog.setTitle("Tạo nhiều lịch trình tự động");
        batchDialog.setSize(650, 600);
        batchDialog.setLocationRelativeTo(this);
        batchDialog.setModal(true);
        batchDialog.setLayout(new BorderLayout(10, 10));
        batchDialog.setResizable(false);

        // Panel chính
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Tiêu đề
        JLabel titleLabel = new JLabel("TẠO NHIỀU LỊCH TRÌNH TỰ ĐỘNG", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(new Color(41, 128, 185));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        mainPanel.add(titleLabel);

        // 1. Panel chọn tàu
        JPanel trainPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JLabel trainLabel = new JLabel("Tàu:");
        trainLabel.setFont(new Font("Arial", Font.BOLD, 14));
        trainLabel.setPreferredSize(new Dimension(120, 28));

        JComboBox<Tau> trainComboBox = new JComboBox<>();
        trainComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        trainComboBox.setPreferredSize(new Dimension(450, 28));

        // Renderer cho combobox tàu
        trainComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Tau) {
                    Tau tau = (Tau) value;
                    setText("Tàu " + tau.getMaTau() + " - " + tau.getTenTau());
                }
                return c;
            }
        });

        trainPanel.add(trainLabel);
        trainPanel.add(trainComboBox);

        // 2. Panel chọn ngày bắt đầu
        JPanel startDatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JLabel startDateLabel = new JLabel("Ngày bắt đầu:");
        startDateLabel.setFont(new Font("Arial", Font.BOLD, 14));
        startDateLabel.setPreferredSize(new Dimension(120, 28));

        JDateChooser startDateChooser = new JDateChooser();
        startDateChooser.setDateFormatString("dd/MM/yyyy");
        startDateChooser.setPreferredSize(new Dimension(450, 28));
        startDateChooser.setFont(new Font("Arial", Font.PLAIN, 14));
        startDateChooser.setDate(new Date());

        startDatePanel.add(startDateLabel);
        startDatePanel.add(startDateChooser);

        // 3. Panel chọn ngày kết thúc
        JPanel endDatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JLabel endDateLabel = new JLabel("Ngày kết thúc:");
        endDateLabel.setFont(new Font("Arial", Font.BOLD, 14));
        endDateLabel.setPreferredSize(new Dimension(120, 28));

        JDateChooser endDateChooser = new JDateChooser();
        endDateChooser.setDateFormatString("dd/MM/yyyy");
        endDateChooser.setPreferredSize(new Dimension(450, 28));
        endDateChooser.setFont(new Font("Arial", Font.PLAIN, 14));

        // Mặc định ngày kết thúc là 7 ngày sau ngày bắt đầu
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 7);
        endDateChooser.setDate(calendar.getTime());

        endDatePanel.add(endDateLabel);
        endDatePanel.add(endDateChooser);

        // 4. Panel chọn giờ đi
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JLabel timeLabel = new JLabel("Giờ đi:");
        timeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        timeLabel.setPreferredSize(new Dimension(120, 28));

        // Tạo spinner cho giờ và phút
        SpinnerNumberModel hourModel = new SpinnerNumberModel(8, 0, 23, 1);
        SpinnerNumberModel minuteModel = new SpinnerNumberModel(0, 0, 59, 1);

        JSpinner hourSpinner = new JSpinner(hourModel);
        hourSpinner.setFont(new Font("Arial", Font.PLAIN, 14));
        hourSpinner.setPreferredSize(new Dimension(70, 28));

        JLabel colonLabel = new JLabel(":");
        colonLabel.setFont(new Font("Arial", Font.BOLD, 14));

        JSpinner minuteSpinner = new JSpinner(minuteModel);
        minuteSpinner.setFont(new Font("Arial", Font.PLAIN, 14));
        minuteSpinner.setPreferredSize(new Dimension(70, 28));

        // Cố gắng thiết lập editors với định dạng hai chữ số
        try {
            JSpinner.NumberEditor hourEditor = new JSpinner.NumberEditor(hourSpinner, "00");
            hourSpinner.setEditor(hourEditor);

            JSpinner.NumberEditor minuteEditor = new JSpinner.NumberEditor(minuteSpinner, "00");
            minuteSpinner.setEditor(minuteEditor);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Không thể thiết lập editor cho spinners", e);
        }

        JPanel timeSpinnerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        timeSpinnerPanel.add(hourSpinner);
        timeSpinnerPanel.add(colonLabel);
        timeSpinnerPanel.add(minuteSpinner);
        timeSpinnerPanel.add(new JLabel("  (giờ:phút)"));

        timePanel.add(timeLabel);
        timePanel.add(timeSpinnerPanel);

        // 5. Panel chọn trạng thái
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JLabel statusLabel = new JLabel("Trạng thái:");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setPreferredSize(new Dimension(120, 28));

        JComboBox<String> statusComboBox = new JComboBox<>();
        statusComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        statusComboBox.setPreferredSize(new Dimension(450, 28));

        // Tải danh sách trạng thái
        try {
            if (isConnected && lichTrinhTauDAO != null) {
                List<TrangThai> statuses = lichTrinhTauDAO.getTrangThai();
                if (statuses != null && !statuses.isEmpty()) {
                    // Thêm tất cả các trạng thái từ cơ sở dữ liệu
                    for (TrangThai status : statuses) {
                        statusComboBox.addItem(status.getValue());
                    }
                    // Đặt giá trị mặc định là "Chưa khởi hành" nếu có
                    statusComboBox.setSelectedItem("Chưa khởi hành");
                } else {
                    // Thêm các trạng thái mặc định
                    statusComboBox.addItem("Chưa khởi hành");
                    statusComboBox.addItem("Đã khởi hành");
                    statusComboBox.addItem("Đã hủy");
                    statusComboBox.addItem("Hoạt động");
                }
            } else {
                // Thêm các trạng thái mặc định
                statusComboBox.addItem("Chưa khởi hành");
                statusComboBox.addItem("Đã khởi hành");
                statusComboBox.addItem("Đã hủy");
                statusComboBox.addItem("Hoạt động");
            }
        } catch (RemoteException ex) {
            LOGGER.log(Level.WARNING, "Không thể tải danh sách trạng thái", ex);
            // Thêm các trạng thái mặc định
            statusComboBox.addItem("Chưa khởi hành");
            statusComboBox.addItem("Đã khởi hành");
            statusComboBox.addItem("Đã hủy");
            statusComboBox.addItem("Hoạt động");
        }

        statusPanel.add(statusLabel);
        statusPanel.add(statusComboBox);

        // 6. Panel hiển thị thông tin tuyến đường
        JPanel routePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JLabel routeLabel = new JLabel("Tuyến đường:");
        routeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        routeLabel.setPreferredSize(new Dimension(120, 28));

        JTextField routeTextField = new JTextField();
        routeTextField.setFont(new Font("Arial", Font.PLAIN, 14));
        routeTextField.setPreferredSize(new Dimension(450, 28));
        routeTextField.setEditable(false);
        routeTextField.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        routePanel.add(routeLabel);
        routePanel.add(routeTextField);

        // 7. Panel hiển thị thông tin về lịch trình sẽ được tạo
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JLabel infoLabel = new JLabel("Dự kiến tạo:");
        infoLabel.setFont(new Font("Arial", Font.BOLD, 14));
        infoLabel.setPreferredSize(new Dimension(120, 28));

        JLabel countInfoLabel = new JLabel("0 lịch trình");
        countInfoLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        countInfoLabel.setForeground(new Color(41, 128, 185));

        infoPanel.add(infoLabel);
        infoPanel.add(countInfoLabel);

        // 8. Panel chọn ngày trong tuần
        JPanel weekdayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JLabel weekdayLabel = new JLabel("Chỉ tạo vào:");
        weekdayLabel.setFont(new Font("Arial", Font.BOLD, 14));
        weekdayLabel.setPreferredSize(new Dimension(120, 28));

        JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        String[] weekdays = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật"};
        JCheckBox[] weekdayCheckboxes = new JCheckBox[7];

        for (int i = 0; i < weekdays.length; i++) {
            weekdayCheckboxes[i] = new JCheckBox(weekdays[i], true);
            weekdayCheckboxes[i].setFont(new Font("Arial", Font.PLAIN, 12));
            checkboxPanel.add(weekdayCheckboxes[i]);
        }

        // Nút chọn tất cả/bỏ chọn tất cả
        JButton selectAllButton = new JButton("Chọn tất cả");
        selectAllButton.setFont(new Font("Arial", Font.PLAIN, 12));
        selectAllButton.addActionListener(e -> {
            boolean allSelected = true;
            for (JCheckBox cb : weekdayCheckboxes) {
                if (!cb.isSelected()) {
                    allSelected = false;
                    break;
                }
            }

            for (JCheckBox cb : weekdayCheckboxes) {
                cb.setSelected(!allSelected);
            }

            selectAllButton.setText(allSelected ? "Chọn tất cả" : "Bỏ chọn tất cả");
            updateScheduleCount(startDateChooser, endDateChooser, weekdayCheckboxes, countInfoLabel);
        });

        checkboxPanel.add(Box.createHorizontalStrut(20));
        checkboxPanel.add(selectAllButton);

        weekdayPanel.add(weekdayLabel);
        weekdayPanel.add(checkboxPanel);

        // Thêm tất cả các panel con vào panel chính
        mainPanel.add(trainPanel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(startDatePanel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(endDatePanel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(timePanel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(statusPanel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(routePanel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(weekdayPanel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(infoPanel);

        // Panel chứa các nút thao tác
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton createButton = new JButton("Tạo lịch trình");
        createButton.setFont(new Font("Arial", Font.BOLD, 14));
        createButton.setPreferredSize(new Dimension(130, 35));

        JButton cancelButton = new JButton("Hủy");
        cancelButton.setFont(new Font("Arial", Font.BOLD, 14));
        cancelButton.setPreferredSize(new Dimension(100, 35));

        // Thêm icon cho nút
        try {
            createButton.setIcon(createAddIcon(16, 16));
            cancelButton.setIcon(createDeleteIcon(16, 16));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Không thể thiết lập icon cho nút", e);
        }

        buttonsPanel.add(createButton);
        buttonsPanel.add(cancelButton);

        // Thêm panel chính và panel nút vào dialog
        batchDialog.add(new JScrollPane(mainPanel), BorderLayout.CENTER);
        batchDialog.add(buttonsPanel, BorderLayout.SOUTH);

        // Tải danh sách tàu từ TauDAO
        try {
            // Tạo kết nối đến RMI server
            Registry registry = LocateRegistry.getRegistry(RMI_SERVER_IP, RMI_SERVER_PORT);
            TauDAO tauDAO = (TauDAO) registry.lookup("tauDAO");

            // Lấy danh sách tàu và thêm vào combobox
            List<Tau> trains = tauDAO.getAllListT();
            if (trains != null && !trains.isEmpty()) {
                DefaultComboBoxModel<Tau> model = new DefaultComboBoxModel<>();
                for (Tau train : trains) {
                    model.addElement(train);
                }
                trainComboBox.setModel(model);

                // Hiển thị thông tin tuyến đường của tàu được chọn
                updateRouteInfo(trainComboBox, routeTextField);
            } else {
                JOptionPane.showMessageDialog(batchDialog,
                        "Không có tàu nào trong hệ thống. Vui lòng tạo tàu trước.",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                batchDialog.dispose();
                return;
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tải danh sách tàu", ex);
            JOptionPane.showMessageDialog(batchDialog,
                    "Không thể tải danh sách tàu: " + ex.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            batchDialog.dispose();
            return;
        }

        // Cập nhật thông tin tuyến đường khi chọn tàu khác
        trainComboBox.addActionListener(e -> {
            updateRouteInfo(trainComboBox, routeTextField);
        });

        // Cập nhật số lượng lịch trình dự kiến khi thay đổi ngày hoặc checkbox
        PropertyChangeListener dateListener = evt -> {
            if ("date".equals(evt.getPropertyName())) {
                updateScheduleCount(startDateChooser, endDateChooser, weekdayCheckboxes, countInfoLabel);
            }
        };

        startDateChooser.addPropertyChangeListener(dateListener);
        endDateChooser.addPropertyChangeListener(dateListener);

        for (JCheckBox checkbox : weekdayCheckboxes) {
            checkbox.addActionListener(e -> {
                updateScheduleCount(startDateChooser, endDateChooser, weekdayCheckboxes, countInfoLabel);
            });
        }

        // Cập nhật ban đầu
        updateScheduleCount(startDateChooser, endDateChooser, weekdayCheckboxes, countInfoLabel);

        // Xử lý sự kiện khi nhấn nút "Hủy"
        cancelButton.addActionListener(e -> batchDialog.dispose());

        // Xử lý sự kiện khi nhấn nút "Tạo lịch trình"
        createButton.addActionListener(e -> {
            try {
                // Kiểm tra thông tin đầu vào
                Tau selectedTrain = (Tau) trainComboBox.getSelectedItem();
                if (selectedTrain == null) {
                    JOptionPane.showMessageDialog(batchDialog,
                            "Vui lòng chọn tàu!",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Kiểm tra ngày bắt đầu và ngày kết thúc
                Date startDate = startDateChooser.getDate();
                Date endDate = endDateChooser.getDate();

                if (startDate == null) {
                    JOptionPane.showMessageDialog(batchDialog,
                            "Vui lòng chọn ngày bắt đầu!",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (endDate == null) {
                    JOptionPane.showMessageDialog(batchDialog,
                            "Vui lòng chọn ngày kết thúc!",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Kiểm tra ngày kết thúc phải sau ngày bắt đầu
                if (endDate.before(startDate)) {
                    JOptionPane.showMessageDialog(batchDialog,
                            "Ngày kết thúc phải sau ngày bắt đầu!",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Kiểm tra xem có ít nhất một ngày trong tuần được chọn
                boolean anyDaySelected = false;
                for (JCheckBox cb : weekdayCheckboxes) {
                    if (cb.isSelected()) {
                        anyDaySelected = true;
                        break;
                    }
                }

                if (!anyDaySelected) {
                    JOptionPane.showMessageDialog(batchDialog,
                            "Vui lòng chọn ít nhất một ngày trong tuần để tạo lịch trình!",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Chuyển đổi ngày từ Date sang LocalDate
                LocalDate startLocalDate = startDate.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();

                LocalDate endLocalDate = endDate.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();

                // Lấy giờ và phút
                int hour = (int) hourSpinner.getValue();
                int minute = (int) minuteSpinner.getValue();
                LocalTime departureTime = LocalTime.of(hour, minute);

                // Lấy trạng thái
                String statusValue = statusComboBox.getSelectedItem().toString();
                TrangThai status = TrangThai.fromValue(statusValue);

                // Tạo danh sách các ngày trong tuần được chọn (0 = Thứ 2, 6 = Chủ nhật)
                List<Integer> selectedDays = new ArrayList<>();
                for (int i = 0; i < weekdayCheckboxes.length; i++) {
                    if (weekdayCheckboxes[i].isSelected()) {
                        selectedDays.add((i + 1) % 7); // Chuyển đổi sang DayOfWeek (0 = Chủ nhật, 1-6 = Thứ 2 - Thứ 7)
                    }
                }

                // Kiểm tra kết nối tới RMI server
                if (!isConnected || lichTrinhTauDAO == null) {
                    connectToRMIServer();
                    if (!isConnected) {
                        JOptionPane.showMessageDialog(batchDialog,
                                "Không thể kết nối đến server để tạo lịch trình.",
                                "Lỗi kết nối",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                // Hiển thị xác nhận
                int confirm = JOptionPane.showConfirmDialog(
                        batchDialog,
                        "Bạn sắp tạo " + countInfoLabel.getText() + " cho tàu " + selectedTrain.getTenTau() +
                                ".\nBạn có chắc chắn muốn tiếp tục không?",
                        "Xác nhận",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }

                // Hiển thị dialog tiến trình
                ProgressDialog progressDialog = new ProgressDialog(batchDialog, "Đang tạo lịch trình...");

                // Tạo thread để xử lý tạo lịch trình
                Thread createThread = new Thread(() -> {
                    try {
                        // Gọi phương thức tạo nhiều lịch trình
                        List<LichTrinhTau> createdSchedules = createMultipleSchedules(
                                selectedTrain, startLocalDate, endLocalDate, departureTime, status, selectedDays, progressDialog);

                        // Cập nhật UI sau khi hoàn thành
                        SwingUtilities.invokeLater(() -> {
                            progressDialog.dispose();

                            // Hiển thị thông báo thành công
                            JOptionPane.showMessageDialog(batchDialog,
                                    "Đã tạo thành công " + createdSchedules.size() + " lịch trình!",
                                    "Thành công",
                                    JOptionPane.INFORMATION_MESSAGE);

                            // Đóng dialog và làm mới dữ liệu
                            batchDialog.dispose();
                            refreshData();
                        });
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> {
                            progressDialog.dispose();
                            LOGGER.log(Level.SEVERE, "Lỗi khi tạo lịch trình tự động", ex);
                            JOptionPane.showMessageDialog(batchDialog,
                                    "Lỗi khi tạo lịch trình: " + ex.getMessage(),
                                    "Lỗi",
                                    JOptionPane.ERROR_MESSAGE);
                        });
                    }
                });

                createThread.start();
                progressDialog.setVisible(true);

            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Lỗi khi tạo lịch trình tự động", ex);
                JOptionPane.showMessageDialog(batchDialog,
                        "Lỗi: " + ex.getMessage(),
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        // Hiển thị dialog
        batchDialog.setVisible(true);
    }

    private List<LichTrinhTau> createMultipleSchedules(
            Tau train, LocalDate startDate, LocalDate endDate, LocalTime departureTime,
            TrangThai status, List<Integer> selectedDays, ProgressDialog progressDialog) throws RemoteException {

        List<LichTrinhTau> createdSchedules = new ArrayList<>();

        // Tính toán tổng số lịch trình sẽ tạo để cập nhật tiến trình
        int totalSchedules = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            int dayOfWeek = current.getDayOfWeek().getValue() % 7;
            if (selectedDays.contains(dayOfWeek)) {
                totalSchedules++;
            }
            current = current.plusDays(1);
        }

        if (totalSchedules == 0) {
            return createdSchedules;
        }

        // Reset biến current
        current = startDate;
        int processedCount = 0;

        while (!current.isAfter(endDate)) {
            int dayOfWeek = current.getDayOfWeek().getValue() % 7;

            if (selectedDays.contains(dayOfWeek)) {
                // Tạo mã lịch trình mới
                String maLich = generateLichTrinhCode();

                // Tạo đối tượng LichTrinhTau mới
                LichTrinhTau newSchedule = new LichTrinhTau();
                newSchedule.setMaLich(maLich);
                newSchedule.setNgayDi(current);
                newSchedule.setGioDi(departureTime);
                newSchedule.setTrangThai(status);
                newSchedule.setTau(train);

                // Lưu vào cơ sở dữ liệu
                boolean saved = lichTrinhTauDAO.save(newSchedule);
                if (saved) {
                    createdSchedules.add(newSchedule);
                    LOGGER.info("Đã tạo lịch trình " + maLich + " cho ngày " + current);
                } else {
                    LOGGER.warning("Không thể tạo lịch trình cho ngày " + current);
                }

                // Cập nhật tiến trình
                processedCount++;
                updateProgressSafely(progressDialog, processedCount, totalSchedules);
//                final int finalProcessedCount = processedCount;  // Thêm từ khóa final ở đây
//                SwingUtilities.invokeLater(() -> {
//                    progressDialog.updateProgress(finalProcessedCount, totalSchedules);
//                });

                // Tạm dừng một chút để tránh quá tải server
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RemoteException("Quá trình tạo lịch trình bị gián đoạn", e);
                }
            }

            current = current.plusDays(1);
        }

        return createdSchedules;
    }

    /**
     * Cập nhật thông tin tuyến đường của tàu được chọn
     * @param trainComboBox ComboBox chứa thông tin tàu
     * @param routeTextField TextField để hiển thị thông tin tuyến đường
     */
    private void updateRouteInfo(JComboBox<Tau> trainComboBox, JTextField routeTextField) {
        Tau selectedTrain = (Tau) trainComboBox.getSelectedItem();

        if (selectedTrain != null) {
            try {
                if (selectedTrain.getTuyenTau() != null) {
                    String routeInfo = String.format("Tàu %s: %s → %s",
                            selectedTrain.getMaTau(),
                            selectedTrain.getTuyenTau().getGaDi(),
                            selectedTrain.getTuyenTau().getGaDen());
                    routeTextField.setText(routeInfo);
                } else {
                    routeTextField.setText("Tàu " + selectedTrain.getMaTau() + " - Không có thông tin tuyến đường");
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Lỗi khi hiển thị thông tin tuyến đường", ex);
                routeTextField.setText("Tàu " + selectedTrain.getMaTau() + " - Lỗi tải thông tin tuyến đường");
            }
        } else {
            routeTextField.setText("");
        }
    }

    /**
     * Cập nhật và hiển thị số lượng lịch trình dự kiến sẽ được tạo
     */
    private void updateScheduleCount(JDateChooser startDateChooser, JDateChooser endDateChooser,
                                     JCheckBox[] weekdayCheckboxes, JLabel countInfoLabel) {
        try {
            Date startDate = startDateChooser.getDate();
            Date endDate = endDateChooser.getDate();

            if (startDate == null || endDate == null) {
                countInfoLabel.setText("0 lịch trình");
                return;
            }

            if (endDate.before(startDate)) {
                countInfoLabel.setText("Ngày không hợp lệ");
                return;
            }

            // Chuyển đổi thành LocalDate
            LocalDate start = startDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            LocalDate end = endDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            // Tạo danh sách các ngày trong tuần được chọn (0 = Chủ nhật, 1-6 = Thứ 2 - Thứ 7)
            List<Integer> selectedDays = new ArrayList<>();
            for (int i = 0; i < weekdayCheckboxes.length; i++) {
                if (weekdayCheckboxes[i].isSelected()) {
                    selectedDays.add((i + 1) % 7); // Chuyển đổi sang DayOfWeek (0 = Chủ nhật, 1-6 = Thứ 2 - Thứ 7)
                }
            }

            // Đếm số ngày thỏa mãn
            int count = 0;
            LocalDate current = start;

            while (!current.isAfter(end)) {
                int dayOfWeek = current.getDayOfWeek().getValue() % 7; // Chuyển đổi sang định dạng 0-6
                if (selectedDays.contains(dayOfWeek)) {
                    count++;
                }
                current = current.plusDays(1);
            }

            countInfoLabel.setText(count + " lịch trình");

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Lỗi khi tính toán số lượng lịch trình", e);
            countInfoLabel.setText("Lỗi tính toán");
        }
    }


    /**
     * Dialog hiển thị tiến trình xử lý
     */
    private class ProgressDialog extends JDialog {
        private final JProgressBar progressBar;
        private final JLabel statusLabel;

        public ProgressDialog(Window owner, String title) {
            super(owner, title, ModalityType.APPLICATION_MODAL);
            setSize(400, 150);
            setLocationRelativeTo(owner);
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            setResizable(false);

            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            statusLabel = new JLabel("Đang xử lý...");
            statusLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

            progressBar = new JProgressBar(0, 100);
            progressBar.setStringPainted(true);
            progressBar.setFont(new Font("Arial", Font.BOLD, 12));

            panel.add(statusLabel, BorderLayout.NORTH);
            panel.add(progressBar, BorderLayout.CENTER);

            add(panel);
        }

        /**
         * Cập nhật tiến trình
         * @param current Số lượng hiện tại
         * @param total Tổng số lượng
         */
        public void updateProgress(int current, int total) {
            int percentage = (int) ((current * 100.0) / total);
            progressBar.setValue(percentage);
            statusLabel.setText("Đang xử lý " + current + "/" + total + " lịch trình...");
        }
    }

    private void updateProgressSafely(final ProgressDialog dialog, final int current, final int total) {
        SwingUtilities.invokeLater(() -> {
            dialog.updateProgress(current, total);
        });
    }

    /**
     * Ước tính thời gian di chuyển cho một lịch trình (giờ)
     */
    private int estimateTravelTime(LichTrinhTau schedule) {
        try {
            // Sử dụng AI để dự đoán thời gian
            AITravelTimePredictor.PredictionResult prediction = aiPredictor.predictTravelTime(schedule);

            // Chuyển từ phút sang giờ (làm tròn lên)
            return (int) Math.ceil(prediction.getPredictedMinutes() / 60.0);
        } catch (Exception e) {
            LOGGER.warning("Không thể dự đoán thời gian di chuyển, sử dụng ước tính đơn giản: " + e.getMessage());

            // Sử dụng phương pháp ước tính cũ nếu AI thất bại
            try {
                String routeId = schedule.getTau().getTuyenTau().getMaTuyen();

                if (routeId.contains("HN-SG") || routeId.contains("SG-HN")) {
                    return 27; // 27 giờ cho tuyến Hà Nội - Sài Gòn
                } else if (routeId.contains("HN-DN") || routeId.contains("DN-HN")) {
                    return 13; // 13 giờ cho tuyến Hà Nội - Đà Nẵng
                } else if (routeId.contains("DN-SG") || routeId.contains("SG-DN")) {
                    return 16; // 16 giờ cho tuyến Đà Nẵng - Sài Gòn
                } else {
                    return 8;  // Mặc định 8 giờ cho các tuyến khác
                }
            } catch (Exception ex) {
                return 8; // Mặc định 8 giờ nếu không thể xác định tuyến
            }
        }
    }

    private void reconnectAndLoadData(LocalDate localDate) {
        connectToRMIServer();
        if (isConnected) {
            try {
                loadScheduleData(localDate);
            } catch (RemoteException ex) {
                LOGGER.log(Level.SEVERE, "Failed to load data after reconnection", ex);
                showErrorMessage("Không thể tải dữ liệu sau khi kết nối lại", ex);
            }
        } else {
            showErrorMessage("Không thể kết nối đến server", null);
        }
    }

    private void refreshData() {
        try {
            // Sử dụng StatusManager để cập nhật trạng thái
            if (statusManager != null) {
                statusManager.updateDepartedSchedules();
            }

            // Tiếp tục phương thức làm mới dữ liệu hiện tại
            if (isConnected) {
                // Nếu đang ở chế độ xem bảng, tải lại dữ liệu bảng
                if (viewTabbedPane.getSelectedIndex() == 0) {
                    loadAllScheduleData();
                }
                // Nếu đang ở chế độ xem lịch, làm mới lịch
                else if (viewTabbedPane.getSelectedIndex() == 1 && calendarPanel != null) {
                    calendarPanel.refreshCalendar();
                }
            } else {
                connectToRMIServer();
                if (isConnected) {
                    loadAllScheduleData();
                } else {
                    showErrorMessage("Không thể kết nối đến server", null);
                }
            }
        } catch (RemoteException ex) {
            LOGGER.log(Level.SEVERE, "Error refreshing data", ex);
            showErrorMessage("Lỗi khi làm mới dữ liệu", ex);
        }
    }

    private void loadAllScheduleData() throws RemoteException {
        if (!isConnected || lichTrinhTauDAO == null) {
            connectToRMIServer();
            if (!isConnected) {
                throw new RemoteException("Not connected to RMI server");
            }
        }

        tableModel.setRowCount(0);

        try {
            List<LichTrinhTau> schedules = lichTrinhTauDAO.getAllList();

            if (schedules == null || schedules.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Không có lịch trình nào để hiển thị.",
                        "Thông báo",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Apply filter if selected
            String filterOption = (String) filterComboBox.getSelectedItem();
            for (LichTrinhTau schedule : schedules) {
                if (matchesFilter(schedule, filterOption)) {
                    tableModel.addRow(createTableRow(schedule));
                }
            }

        } catch (Exception e) {
            System.out.println("Lỗi chi tiết khi tải tất cả dữ liệu: " + e.getMessage());
            e.printStackTrace();
            throw new RemoteException("Lỗi khi tải dữ liệu: " + e.getMessage(), e);
        }
    }

    private void addSchedule() {
        // Tạo dialog cho việc thêm lịch trình
        JDialog addDialog = new JDialog();
        addDialog.setTitle("Thêm lịch trình tàu");
        addDialog.setSize(600, 450);
        addDialog.setLocationRelativeTo(this);
        addDialog.setModal(true);
        addDialog.setLayout(new BorderLayout(10, 10));
        addDialog.setResizable(false);

        // Panel chính để chứa các thành phần
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Tiêu đề form
        JLabel titleLabel = new JLabel("THÊM LỊCH TRÌNH TÀU", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(new Color(41, 128, 185));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        mainPanel.add(titleLabel);

        // 1. Panel chọn tàu
        JPanel trainPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JLabel trainLabel = new JLabel("Tàu:");
        trainLabel.setFont(new Font("Arial", Font.BOLD, 14));
        trainLabel.setPreferredSize(new Dimension(120, 28));

        JComboBox<Tau> trainComboBox = new JComboBox<>();
        trainComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        trainComboBox.setPreferredSize(new Dimension(400, 28));

        // Renderer đơn giản cho tàu
        trainComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Tau) {
                    Tau tau = (Tau) value;
                    setText("Tàu " + tau.getMaTau() + " - " + tau.getTenTau());
                }
                return c;
            }
        });

        trainPanel.add(trainLabel);
        trainPanel.add(trainComboBox);

        // 2. Panel chọn ngày đi
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JLabel dateLabel = new JLabel("Ngày đi:");
        dateLabel.setFont(new Font("Arial", Font.BOLD, 14));
        dateLabel.setPreferredSize(new Dimension(120, 28));

        JDateChooser departureDateChooser = new JDateChooser();
        departureDateChooser.setDateFormatString("dd/MM/yyyy");
        departureDateChooser.setPreferredSize(new Dimension(400, 28));
        departureDateChooser.setFont(new Font("Arial", Font.PLAIN, 14));
        departureDateChooser.setDate(new Date());

        datePanel.add(dateLabel);
        datePanel.add(departureDateChooser);

        // 3. Panel chọn giờ đi
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JLabel timeLabel = new JLabel("Giờ đi:");
        timeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        timeLabel.setPreferredSize(new Dimension(120, 28));

        // Tạo spinner cho giờ và phút
        SpinnerNumberModel hourModel = new SpinnerNumberModel(0, 0, 23, 1);
        SpinnerNumberModel minuteModel = new SpinnerNumberModel(0, 0, 59, 1);

        JSpinner hourSpinner = new JSpinner(hourModel);
        hourSpinner.setFont(new Font("Arial", Font.PLAIN, 14));
        hourSpinner.setPreferredSize(new Dimension(70, 28));

        JLabel colonLabel = new JLabel(":");
        colonLabel.setFont(new Font("Arial", Font.BOLD, 14));

        JSpinner minuteSpinner = new JSpinner(minuteModel);
        minuteSpinner.setFont(new Font("Arial", Font.PLAIN, 14));
        minuteSpinner.setPreferredSize(new Dimension(70, 28));

        // Cố gắng thiết lập editors với định dạng hai chữ số
        try {
            JSpinner.NumberEditor hourEditor = new JSpinner.NumberEditor(hourSpinner, "00");
            hourSpinner.setEditor(hourEditor);

            JSpinner.NumberEditor minuteEditor = new JSpinner.NumberEditor(minuteSpinner, "00");
            minuteSpinner.setEditor(minuteEditor);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Không thể thiết lập editor cho spinners", e);
        }

        JPanel timeSpinnerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        timeSpinnerPanel.add(hourSpinner);
        timeSpinnerPanel.add(colonLabel);
        timeSpinnerPanel.add(minuteSpinner);
        timeSpinnerPanel.add(new JLabel("  (giờ:phút)"));

        timePanel.add(timeLabel);
        timePanel.add(timeSpinnerPanel);

        // 4. Panel chọn trạng thái
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JLabel statusLabel = new JLabel("Trạng thái:");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setPreferredSize(new Dimension(120, 28));

        JComboBox<String> statusComboBox = new JComboBox<>();
        statusComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        statusComboBox.setPreferredSize(new Dimension(400, 28));

        // Tải danh sách trạng thái
        try {
            if (isConnected && lichTrinhTauDAO != null) {
                List<TrangThai> statuses = lichTrinhTauDAO.getTrangThai();
                if (statuses != null && !statuses.isEmpty()) {
                    // Xóa các item cũ trước khi thêm mới
                    statusComboBox.removeAllItems();

                    // Thêm tất cả các trạng thái từ cơ sở dữ liệu
                    for (TrangThai status : statuses) {
                        statusComboBox.addItem(status.getValue());
                    }
                    // Đặt giá trị mặc định là "Chưa khởi hành" nếu có
                    statusComboBox.setSelectedItem("Chưa khởi hành");
                } else {
                    // Thêm các trạng thái mặc định nếu không tìm thấy từ cơ sở dữ liệu
                    statusComboBox.addItem("Chưa khởi hành");
                    statusComboBox.addItem("Đã khởi hành");
                    statusComboBox.addItem("Đã hủy");
                    statusComboBox.addItem("Hoạt động");
                }
            } else {
                // Thêm các trạng thái mặc định nếu không kết nối được
                statusComboBox.addItem("Chưa khởi hành");
                statusComboBox.addItem("Đã khởi hành");
                statusComboBox.addItem("Đã hủy");
                statusComboBox.addItem("Hoạt động");
            }
        } catch (RemoteException ex) {
            LOGGER.log(Level.WARNING, "Không thể tải danh sách trạng thái", ex);
            // Thêm các trạng thái mặc định
            statusComboBox.addItem("Chưa khởi hành");
            statusComboBox.addItem("Đã khởi hành");
            statusComboBox.addItem("Đã hủy");
            statusComboBox.addItem("Hoạt động");
        }

        statusPanel.add(statusLabel);
        statusPanel.add(statusComboBox);

        // 5. Panel hiển thị thông tin tuyến đường
        JPanel routePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JLabel routeLabel = new JLabel("Tuyến đường:");
        routeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        routeLabel.setPreferredSize(new Dimension(120, 28));

        JTextField routeTextField = new JTextField();
        routeTextField.setFont(new Font("Arial", Font.PLAIN, 14));
        routeTextField.setPreferredSize(new Dimension(400, 28));
        routeTextField.setEditable(false);
        routeTextField.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        routePanel.add(routeLabel);
        routePanel.add(routeTextField);

        // Thêm tất cả các panel con vào panel chính
        mainPanel.add(trainPanel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(datePanel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(timePanel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(statusPanel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(routePanel);

        // Panel chứa các nút thao tác
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton saveButton = new JButton("Lưu");
        saveButton.setFont(new Font("Arial", Font.BOLD, 14));
        saveButton.setPreferredSize(new Dimension(100, 35));

        JButton cancelButton = new JButton("Hủy");
        cancelButton.setFont(new Font("Arial", Font.BOLD, 14));
        cancelButton.setPreferredSize(new Dimension(100, 35));

        // Thêm icon cho nút (nếu phương thức tạo icon hoạt động)
        try {
            saveButton.setIcon(createAddIcon(16, 16));
            cancelButton.setIcon(createDeleteIcon(16, 16));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Không thể thiết lập icon cho nút", e);
        }

        buttonsPanel.add(saveButton);
        buttonsPanel.add(cancelButton);

        // Thêm panel chính và panel nút vào dialog
        addDialog.add(mainPanel, BorderLayout.CENTER);
        addDialog.add(buttonsPanel, BorderLayout.SOUTH);

        // Tải danh sách tàu từ TauDAO
        try {
            // Tạo kết nối đến RMI server
            Registry registry = LocateRegistry.getRegistry(RMI_SERVER_IP, RMI_SERVER_PORT);
            TauDAO tauDAO = (TauDAO) registry.lookup("tauDAO");

            // Lấy danh sách tàu và thêm vào combobox
            List<Tau> trains = tauDAO.getAllListT();
            if (trains != null && !trains.isEmpty()) {
                DefaultComboBoxModel<Tau> model = new DefaultComboBoxModel<>();
                for (Tau train : trains) {
                    model.addElement(train);
                }
                trainComboBox.setModel(model);

                // Hiển thị thông tin tuyến đường của tàu được chọn
                Tau selectedTrain = (Tau) trainComboBox.getSelectedItem();
                if (selectedTrain != null) {
                    try {
                        if (selectedTrain.getTuyenTau() != null) {
                            String routeInfo = "Tàu " + selectedTrain.getMaTau() + ": " +
                                    selectedTrain.getTuyenTau().getGaDi() + " → " +
                                    selectedTrain.getTuyenTau().getGaDen();
                            routeTextField.setText(routeInfo);
                        } else {
                            routeTextField.setText("Tàu " + selectedTrain.getMaTau() + " - Không có thông tin tuyến đường");
                        }
                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, "Lỗi khi hiển thị thông tin tuyến đường", ex);
                        routeTextField.setText("Tàu " + selectedTrain.getMaTau() + " - Lỗi tải thông tin tuyến đường");
                    }
                }
            } else {
                JOptionPane.showMessageDialog(addDialog,
                        "Không có tàu nào trong hệ thống. Vui lòng tạo tàu trước.",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                addDialog.dispose();
                return;
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tải danh sách tàu", ex);
            JOptionPane.showMessageDialog(addDialog,
                    "Không thể tải danh sách tàu: " + ex.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            addDialog.dispose();
            return;
        }

        // Cập nhật thông tin tuyến đường khi chọn tàu khác
        trainComboBox.addActionListener(e -> {
            Tau selectedTrain = (Tau) trainComboBox.getSelectedItem();
            if (selectedTrain != null) {
                try {
                    if (selectedTrain.getTuyenTau() != null) {
                        String routeInfo = "Tàu " + selectedTrain.getMaTau() + ": " +
                                selectedTrain.getTuyenTau().getGaDi() + " → " +
                                selectedTrain.getTuyenTau().getGaDen();
                        routeTextField.setText(routeInfo);
                    } else {
                        routeTextField.setText("Tàu " + selectedTrain.getMaTau() + " - Không có thông tin tuyến đường");
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Lỗi khi hiển thị thông tin tuyến đường", ex);
                    routeTextField.setText("Tàu " + selectedTrain.getMaTau() + " - Lỗi tải thông tin tuyến đường");
                }
            } else {
                routeTextField.setText("");
            }
        });

        // Xử lý sự kiện khi người dùng nhấn nút "Hủy"
        cancelButton.addActionListener(e -> addDialog.dispose());

        // Xử lý sự kiện khi người dùng nhấn nút "Lưu"
        saveButton.addActionListener(e -> {
            try {
                // Kiểm tra các thông tin đầu vào
                Tau selectedTrain = (Tau) trainComboBox.getSelectedItem();
                if (selectedTrain == null) {
                    JOptionPane.showMessageDialog(addDialog, "Vui lòng chọn tàu!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Date departureDate = departureDateChooser.getDate();
                if (departureDate == null) {
                    JOptionPane.showMessageDialog(addDialog, "Vui lòng chọn ngày đi!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Chuyển đổi java.util.Date sang java.time.LocalDate
                LocalDate localDate = departureDate.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();

                // Lấy giờ và phút
                int hour = (int) hourSpinner.getValue();
                int minute = (int) minuteSpinner.getValue();
                LocalTime departureTime = LocalTime.of(hour, minute);

                // Lấy trạng thái
                String statusValue = (String) statusComboBox.getSelectedItem();
                if (statusValue == null || statusValue.isEmpty()) {
                    JOptionPane.showMessageDialog(addDialog, "Vui lòng chọn trạng thái!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Tạo mã lịch trình mới sử dụng phương thức đã thêm
                String maLich = generateLichTrinhCode();

                // Tạo đối tượng LichTrinhTau mới
                LichTrinhTau newSchedule = new LichTrinhTau();
                newSchedule.setMaLich(maLich);
                newSchedule.setNgayDi(localDate);
                newSchedule.setGioDi(departureTime);
                newSchedule.setTrangThai(TrangThai.fromValue(statusValue));
                newSchedule.setTau(selectedTrain);

                // Xác nhận thêm
                int confirm = JOptionPane.showConfirmDialog(
                        addDialog,
                        "Xác nhận thêm lịch trình mới?\n\n" +
                                "- Mã lịch trình: " + maLich + "\n" +
                                "- Tàu: " + selectedTrain.getMaTau() + " - " + selectedTrain.getTenTau() + "\n" +
                                "- Ngày đi: " + localDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n" +
                                "- Giờ đi: " + String.format("%02d:%02d", hour, minute) + "\n" +
                                "- Trạng thái: " + statusValue,
                        "Xác nhận",
                        JOptionPane.YES_NO_OPTION
                );

                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }

                // Kiểm tra kết nối tới RMI server trước khi lưu
                if (!isConnected || lichTrinhTauDAO == null) {
                    connectToRMIServer();
                    if (!isConnected) {
                        JOptionPane.showMessageDialog(addDialog,
                                "Không thể kết nối đến máy chủ để lưu lịch trình.",
                                "Lỗi kết nối",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                // Lưu lịch trình mới vào cơ sở dữ liệu
                boolean saved = lichTrinhTauDAO.save(newSchedule);
                if (saved) {
                    JOptionPane.showMessageDialog(addDialog,
                            "Thêm lịch trình thành công!\nMã lịch trình: " + maLich,
                            "Thông báo",
                            JOptionPane.INFORMATION_MESSAGE);

                    // Đóng dialog
                    addDialog.dispose();

                    // Làm mới dữ liệu trên bảng
                    refreshData();
                } else {
                    JOptionPane.showMessageDialog(addDialog,
                            "Không thể lưu lịch trình. Vui lòng thử lại.",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Lỗi khi lưu lịch trình tàu", ex);
                JOptionPane.showMessageDialog(addDialog,
                        "Lỗi: " + ex.getMessage(),
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        // Hiển thị dialog
        addDialog.setVisible(true);
    }

    /**
     * Phương thức để chỉnh sửa lịch trình đã chọn
     */
    private void editSchedule() {
        int selectedRow = scheduleTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn lịch trình để chỉnh sửa.",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Đảm bảo chuyển đổi chỉ số từ view sang model nếu bảng có sắp xếp
        int modelRow = selectedRow;
        if (scheduleTable.getRowSorter() != null) {
            modelRow = scheduleTable.getRowSorter().convertRowIndexToModel(selectedRow);
        }

        // Lấy thông tin từ bảng
        String scheduleId = tableModel.getValueAt(modelRow, 0).toString();
        String statusStr = tableModel.getValueAt(modelRow, 6).toString();

        try {
            // Lấy thông tin chi tiết của lịch trình từ cơ sở dữ liệu để có thông tin chính xác về trạng thái
            LichTrinhTau lichTrinh = lichTrinhTauDAO.getById(scheduleId);
            if (lichTrinh == null) {
                JOptionPane.showMessageDialog(this,
                        "Không tìm thấy thông tin lịch trình.",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Kiểm tra trạng thái trực tiếp từ đối tượng LichTrinhTau
            TrangThai trangThai = lichTrinh.getTrangThai();
            if (trangThai == TrangThai.DA_KHOI_HANH || trangThai == TrangThai.HOAT_DONG ||
                    "Đã khởi hành".equals(statusStr) || "Đang hoạt động".equals(statusStr)) {

                JOptionPane.showMessageDialog(this,
                        "Không thể chỉnh sửa lịch trình đã khởi hành hoặc đang hoạt động.",
                        "Cảnh báo",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Tạo dialog chỉnh sửa
            JDialog editDialog = new JDialog();
            editDialog.setTitle("Chỉnh sửa lịch trình");
            editDialog.setModal(true);
            editDialog.setSize(550, 500);
            editDialog.setLocationRelativeTo(this);
            editDialog.setResizable(false);
            editDialog.setLayout(new BorderLayout());

            // Panel chính
            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            // Tiêu đề
            JLabel titleLabel = new JLabel("CHỈNH SỬA LỊCH TRÌNH", JLabel.CENTER);
            titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
            titleLabel.setForeground(new Color(41, 128, 185));
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
            mainPanel.add(titleLabel);

            // 1. Mã lịch trình (không cho phép chỉnh sửa)
            JPanel idPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
            JLabel idLabel = new JLabel("Mã lịch trình:");
            idLabel.setFont(new Font("Arial", Font.BOLD, 14));
            idLabel.setPreferredSize(new Dimension(120, 28));

            JTextField idField = new JTextField(scheduleId);
            idField.setFont(new Font("Arial", Font.PLAIN, 14));
            idField.setPreferredSize(new Dimension(380, 28));
            idField.setEditable(false);  // Không cho phép chỉnh sửa
            idField.setBackground(new Color(240, 240, 240));

            idPanel.add(idLabel);
            idPanel.add(idField);
            mainPanel.add(idPanel);
            mainPanel.add(Box.createVerticalStrut(15));

            // 2. Chọn tàu
            JPanel trainPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
            JLabel trainLabel = new JLabel("Tàu:");
            trainLabel.setFont(new Font("Arial", Font.BOLD, 14));
            trainLabel.setPreferredSize(new Dimension(120, 28));

            JComboBox<Tau> trainComboBox = new JComboBox<>();
            trainComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
            trainComboBox.setPreferredSize(new Dimension(380, 28));

            // Renderer cho combobox tàu
            trainComboBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                              boolean isSelected, boolean cellHasFocus) {
                    Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Tau) {
                        Tau tau = (Tau) value;
                        setText("Tàu " + tau.getMaTau() + " - " + tau.getTenTau());
                    }
                    return c;
                }
            });

            trainPanel.add(trainLabel);
            trainPanel.add(trainComboBox);
            mainPanel.add(trainPanel);
            mainPanel.add(Box.createVerticalStrut(15));

            // 3. Chọn ngày đi
            JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
            JLabel dateLabel = new JLabel("Ngày đi:");
            dateLabel.setFont(new Font("Arial", Font.BOLD, 14));
            dateLabel.setPreferredSize(new Dimension(120, 28));

            JDateChooser dateChooser = new JDateChooser();
            dateChooser.setDateFormatString("yyyy-MM-dd");
            dateChooser.setPreferredSize(new Dimension(380, 28));
            dateChooser.setFont(new Font("Arial", Font.PLAIN, 14));

            // Đặt ngày hiện tại của lịch trình
            try {
                LocalDate scheduleDate = lichTrinh.getNgayDi();
                Date date = Date.from(scheduleDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                dateChooser.setDate(date);
            } catch (Exception e) {
                LOGGER.warning("Không thể chuyển đổi ngày: " + e.getMessage());
            }

            datePanel.add(dateLabel);
            datePanel.add(dateChooser);
            mainPanel.add(datePanel);
            mainPanel.add(Box.createVerticalStrut(15));

            // 4. Chọn giờ đi
            JPanel departTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
            JLabel departTimeLabel = new JLabel("Giờ đi:");
            departTimeLabel.setFont(new Font("Arial", Font.BOLD, 14));
            departTimeLabel.setPreferredSize(new Dimension(120, 28));

            // Lấy giờ và phút từ lịch trình
            LocalTime departTime = lichTrinh.getGioDi();
            SpinnerNumberModel hourModel = new SpinnerNumberModel(departTime.getHour(), 0, 23, 1);
            SpinnerNumberModel minuteModel = new SpinnerNumberModel(departTime.getMinute(), 0, 59, 1);

            JSpinner hourSpinner = new JSpinner(hourModel);
            hourSpinner.setFont(new Font("Arial", Font.PLAIN, 14));
            hourSpinner.setPreferredSize(new Dimension(70, 28));

            JLabel colonLabel = new JLabel(":");
            colonLabel.setFont(new Font("Arial", Font.BOLD, 14));

            JSpinner minuteSpinner = new JSpinner(minuteModel);
            minuteSpinner.setFont(new Font("Arial", Font.PLAIN, 14));
            minuteSpinner.setPreferredSize(new Dimension(70, 28));

            // Định dạng spinner với số 0 phía trước
            JSpinner.NumberEditor hourEditor = new JSpinner.NumberEditor(hourSpinner, "00");
            hourSpinner.setEditor(hourEditor);

            JSpinner.NumberEditor minuteEditor = new JSpinner.NumberEditor(minuteSpinner, "00");
            minuteSpinner.setEditor(minuteEditor);

            JPanel timeSpinnerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            timeSpinnerPanel.add(hourSpinner);
            timeSpinnerPanel.add(colonLabel);
            timeSpinnerPanel.add(minuteSpinner);
            timeSpinnerPanel.add(new JLabel("  (giờ:phút)"));

            departTimePanel.add(departTimeLabel);
            departTimePanel.add(timeSpinnerPanel);
            mainPanel.add(departTimePanel);
            mainPanel.add(Box.createVerticalStrut(15));

            // 5. Chọn trạng thái
            JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
            JLabel statusLabel = new JLabel("Trạng thái:");
            statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
            statusLabel.setPreferredSize(new Dimension(120, 28));

            JComboBox<TrangThai> statusComboBox = new JComboBox<>();
            statusComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
            statusComboBox.setPreferredSize(new Dimension(380, 28));

// Tùy chỉnh renderer để hiển thị giá trị dễ đọc của TrangThai
            statusComboBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                              boolean isSelected, boolean cellHasFocus) {
                    Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof TrangThai) {
                        TrangThai trangThai = (TrangThai) value;
                        setText(trangThai.getValue());
                    }
                    return c;
                }
            });

            try {
                // Chỉ thêm các trạng thái được phép (loại bỏ "Đã khởi hành" và "Đang hoạt động")
                for (TrangThai status : TrangThai.values()) {
                    if (status != TrangThai.DA_KHOI_HANH && status != TrangThai.HOAT_DONG) {
                        statusComboBox.addItem(status);
                    }
                }

                // Đặt giá trị mặc định là trạng thái hiện tại (nếu được phép)
                if (lichTrinh.getTrangThai() != TrangThai.DA_KHOI_HANH &&
                        lichTrinh.getTrangThai() != TrangThai.HOAT_DONG) {
                    statusComboBox.setSelectedItem(lichTrinh.getTrangThai());
                } else {
                    // Nếu trạng thái hiện tại không được phép, chọn trạng thái mặc định khác
                    statusComboBox.setSelectedItem(TrangThai.CHUA_KHOI_HANH);
                }

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Không thể tải danh sách trạng thái", e);
                // Trong trường hợp lỗi, thêm trạng thái mặc định
                statusComboBox.removeAllItems();
                statusComboBox.addItem(TrangThai.CHUA_KHOI_HANH);
                statusComboBox.addItem(TrangThai.DA_HUY);
            }

            statusPanel.add(statusLabel);
            statusPanel.add(statusComboBox);
            mainPanel.add(statusPanel);
            mainPanel.add(Box.createVerticalStrut(15));

            // 6. Panel thông tin tuyến đường (chỉ hiển thị, không cho sửa)
            JPanel routeInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
            JLabel routeInfoLabel = new JLabel("Tuyến đường:");
            routeInfoLabel.setFont(new Font("Arial", Font.BOLD, 14));
            routeInfoLabel.setPreferredSize(new Dimension(120, 28));

            JTextField routeInfoField = new JTextField();
            routeInfoField.setFont(new Font("Arial", Font.PLAIN, 14));
            routeInfoField.setPreferredSize(new Dimension(380, 28));
            routeInfoField.setEditable(false);
            routeInfoField.setBackground(new Color(240, 240, 240));

            routeInfoPanel.add(routeInfoLabel);
            routeInfoPanel.add(routeInfoField);
            mainPanel.add(routeInfoPanel);

            // Tải danh sách tàu
            try {
                Registry registry = LocateRegistry.getRegistry(RMI_SERVER_IP, RMI_SERVER_PORT);
                TauDAO tauDAO = (TauDAO) registry.lookup("tauDAO");
                List<Tau> trainList = tauDAO.getAllListT();

                // Thêm tàu vào combobox
                DefaultComboBoxModel<Tau> trainModel = new DefaultComboBoxModel<>();
                for (Tau tau : trainList) {
                    trainModel.addElement(tau);
                }
                trainComboBox.setModel(trainModel);

                // Đặt tàu hiện tại
                for (int i = 0; i < trainModel.getSize(); i++) {
                    Tau tau = trainModel.getElementAt(i);
                    if (tau.getMaTau().equals(lichTrinh.getTau().getMaTau())) {
                        trainComboBox.setSelectedIndex(i);
                        break;
                    }
                }

                // Cập nhật thông tin tuyến đường khi chọn tàu
                updateRouteInfo(trainComboBox.getSelectedItem(), routeInfoField);

                // Thêm listener để cập nhật thông tin tuyến đường khi thay đổi tàu
                trainComboBox.addActionListener(e -> {
                    updateRouteInfo(trainComboBox.getSelectedItem(), routeInfoField);
                });

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Lỗi khi tải danh sách tàu", e);
                JOptionPane.showMessageDialog(this,
                        "Không thể tải danh sách tàu: " + e.getMessage(),
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }

            // Panel chứa các nút thao tác
            JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

            JButton saveButton = new JButton("Lưu");
            saveButton.setFont(new Font("Arial", Font.BOLD, 14));
            saveButton.setPreferredSize(new Dimension(100, 35));
            saveButton.setIcon(createSaveIcon(16, 16));

            JButton cancelButton = new JButton("Hủy");
            cancelButton.setFont(new Font("Arial", Font.BOLD, 14));
            cancelButton.setPreferredSize(new Dimension(100, 35));
            cancelButton.setIcon(createCancelIcon(16, 16));

            // Sự kiện nút hủy
            cancelButton.addActionListener(e -> editDialog.dispose());

            // Sự kiện nút lưu
            saveButton.addActionListener(e -> {
                try {
                    // Kiểm tra các trường thông tin
                    if (dateChooser.getDate() == null) {
                        JOptionPane.showMessageDialog(editDialog,
                                "Vui lòng chọn ngày đi.",
                                "Lỗi",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    // Lấy thông tin từ form
                    Tau selectedTrain = (Tau) trainComboBox.getSelectedItem();

                    // Chuyển đổi từ Date sang LocalDate
                    Date selectedDate = dateChooser.getDate();
                    LocalDate departureDate = selectedDate.toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();

                    // Lấy giờ và phút từ spinner
                    int hour = (int) hourSpinner.getValue();
                    int minute = (int) minuteSpinner.getValue();
                    LocalTime departureTime = LocalTime.of(hour, minute);

                    // Lấy trạng thái
                    String selectedStatus = statusComboBox.getSelectedItem().toString();
                    TrangThai status = (TrangThai) statusComboBox.getSelectedItem();

                    // Cập nhật thông tin lịch trình
                    lichTrinh.setTau(selectedTrain);
                    lichTrinh.setNgayDi(departureDate);
                    lichTrinh.setGioDi(departureTime);
                    lichTrinh.setTrangThai(status);

                    // Lưu vào cơ sở dữ liệu
                    boolean updated = lichTrinhTauDAO.update(lichTrinh);

                    if (updated) {
                        JOptionPane.showMessageDialog(editDialog,
                                "Cập nhật lịch trình thành công.",
                                "Thông báo",
                                JOptionPane.INFORMATION_MESSAGE);

                        // Đóng dialog và làm mới dữ liệu
                        editDialog.dispose();
                        refreshData();
                    } else {
                        JOptionPane.showMessageDialog(editDialog,
                                "Không thể cập nhật lịch trình.",
                                "Lỗi",
                                JOptionPane.ERROR_MESSAGE);
                    }

                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Lỗi khi cập nhật lịch trình", ex);
                    JOptionPane.showMessageDialog(editDialog,
                            "Lỗi khi cập nhật lịch trình: " + ex.getMessage(),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                }
            });

            buttonsPanel.add(saveButton);
            buttonsPanel.add(cancelButton);

            // Thêm các panel vào dialog
            editDialog.add(new JScrollPane(mainPanel), BorderLayout.CENTER);
            editDialog.add(buttonsPanel, BorderLayout.SOUTH);

            // Hiển thị dialog
            editDialog.setVisible(true);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi mở form chỉnh sửa", e);
            JOptionPane.showMessageDialog(this,
                    "Không thể mở form chỉnh sửa: " + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Cập nhật thông tin tuyến đường dựa trên tàu được chọn
     * @param selectedItem Tàu được chọn
     * @param routeInfoField TextField hiển thị thông tin tuyến đường
     */
    private void updateRouteInfo(Object selectedItem, JTextField routeInfoField) {
        if (selectedItem instanceof Tau) {
            Tau tau = (Tau) selectedItem;
            try {
                if (tau.getTuyenTau() != null) {
                    String routeInfo = String.format("Tuyến %s: %s → %s",
                            tau.getTuyenTau().getMaTuyen(),
                            tau.getTuyenTau().getGaDi(),
                            tau.getTuyenTau().getGaDen());
                    routeInfoField.setText(routeInfo);
                } else {
                    routeInfoField.setText("Tàu " + tau.getMaTau() + " - Không có thông tin tuyến đường");
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Lỗi khi hiển thị thông tin tuyến đường", ex);
                routeInfoField.setText("Tàu " + tau.getMaTau() + " - Lỗi tải thông tin tuyến đường");
            }
        } else {
            routeInfoField.setText("");
        }
    }

    /**
     * Tạo biểu tượng lưu
     */
    private Icon createSaveIcon(int width, int height) {
        return new ImageIcon(createIconImage(width, height, new Color(39, 174, 96), icon -> {
            Graphics2D g2 = icon;
            g2.setStroke(new BasicStroke(1.5f));

            // Vẽ hình chữ nhật
            g2.drawRect(2, 2, 12, 12);

            // Vẽ dấu tích
            g2.drawLine(4, 8, 7, 11);
            g2.drawLine(7, 11, 12, 5);
        }));
    }

    /**
     * Tạo biểu tượng hủy
     */
    private Icon createCancelIcon(int width, int height) {
        return new ImageIcon(createIconImage(width, height, new Color(231, 76, 60), icon -> {
            Graphics2D g2 = icon;
            g2.setStroke(new BasicStroke(2));

            // Vẽ dấu X
            g2.drawLine(5, 5, 11, 11);
            g2.drawLine(11, 5, 5, 11);
        }));
    }

    /**
     * Xử lý sự kiện khi người dùng nhấn nút "Xóa" lịch trình
     */
    private void deleteSchedule() {
        // Lấy dòng được chọn, nếu có
        int selectedRow = scheduleTable.getSelectedRow();

        // Nếu không có dòng nào được chọn
        if (selectedRow == -1) {
            // Hiển thị thông báo yêu cầu chọn một lịch trình
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn một lịch trình trong bảng để xóa.",
                    "Chưa chọn lịch trình",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Chuyển đổi chỉ số dòng từ view sang model (nếu bảng có sắp xếp)
        int modelRow = selectedRow;
        if (scheduleTable.getRowSorter() != null) {
            modelRow = scheduleTable.getRowSorter().convertRowIndexToModel(selectedRow);
        }

        // Lấy thông tin chi tiết của lịch trình từ hàng được chọn
        String scheduleId = tableModel.getValueAt(modelRow, 0).toString();
        String scheduleTrain = tableModel.getValueAt(modelRow, 2).toString();
        String scheduleDate = tableModel.getValueAt(modelRow, 1).toString();
        String scheduleTime = tableModel.getValueAt(modelRow, 4).toString();
        String scheduleStatus = tableModel.getValueAt(modelRow, 6).toString();

        // Kiểm tra trạng thái lịch trình - chỉ cho phép xóa lịch trình có trạng thái "Chưa khởi hành"
        if (!scheduleStatus.equals("Chưa khởi hành")) {
            JOptionPane.showMessageDialog(this,
                    "Chỉ được phép xóa các lịch trình có trạng thái 'Chưa khởi hành'.\n" +
                            "Lịch trình đã chọn có trạng thái: " + scheduleStatus,
                    "Không thể xóa",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Tạo thông báo xác nhận với đầy đủ thông tin chi tiết
        StringBuilder confirmMsg = new StringBuilder();
        confirmMsg.append("Bạn có chắc chắn muốn xóa lịch trình sau?\n\n");
        confirmMsg.append("Mã lịch trình: ").append(scheduleId).append("\n");
        confirmMsg.append("Tàu: ").append(scheduleTrain).append("\n");
        confirmMsg.append("Ngày đi: ").append(scheduleDate).append("\n");
        confirmMsg.append("Giờ đi: ").append(scheduleTime).append("\n");
        confirmMsg.append("Trạng thái: ").append(scheduleStatus).append("\n\n");
        confirmMsg.append("Dữ liệu sẽ bị xóa vĩnh viễn và không thể khôi phục.");

        // Hiển thị hộp thoại xác nhận với biểu tượng cảnh báo
        int option = JOptionPane.showConfirmDialog(this,
                confirmMsg.toString(),
                "Xác nhận xóa lịch trình",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        // Nếu người dùng xác nhận xóa
        if (option == JOptionPane.YES_OPTION) {
            try {
                // Kiểm tra kết nối đến server
                if (!isConnected || lichTrinhTauDAO == null) {
                    connectToRMIServer();
                    if (!isConnected) {
                        throw new Exception("Không thể kết nối đến máy chủ");
                    }
                }

                // Hiển thị thông báo đang xử lý
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                // Thực hiện xóa lịch trình
                boolean deleted = lichTrinhTauDAO.delete(scheduleId);

                // Khôi phục con trỏ
                setCursor(Cursor.getDefaultCursor());

                // Xử lý kết quả xóa
                if (deleted) {
                    // Hiển thị thông báo thành công
                    JOptionPane.showMessageDialog(this,
                            "Đã xóa thành công lịch trình: " + scheduleId,
                            "Xóa thành công",
                            JOptionPane.INFORMATION_MESSAGE);

                    // Làm mới dữ liệu trên bảng
                    refreshData();
                } else {
                    // Hiển thị thông báo thất bại
                    JOptionPane.showMessageDialog(this,
                            "Không thể xóa lịch trình " + scheduleId + ".\n" +
                                    "Lịch trình này có thể đang được sử dụng bởi các bản ghi khác.",
                            "Xóa không thành công",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                // Khôi phục con trỏ
                setCursor(Cursor.getDefaultCursor());

                // Ghi log lỗi
                LOGGER.log(Level.SEVERE, "Lỗi khi xóa lịch trình: " + scheduleId, ex);

                // Hiển thị thông báo lỗi
                JOptionPane.showMessageDialog(this,
                        "Lỗi khi xóa lịch trình: " + ex.getMessage(),
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    public void shutdown() {
        if (statusManager != null) {
            statusManager.shutdown();
        }
    }
    // Thêm phương thức xem chi tiết lịch trình
    private void viewScheduleDetails() {
        int selectedRow = scheduleTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn lịch trình để xem chi tiết.",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Chuyển đổi chỉ số dòng từ view sang model (nếu bảng có sắp xếp)
        int modelRow = selectedRow;
        if (scheduleTable.getRowSorter() != null) {
            modelRow = scheduleTable.getRowSorter().convertRowIndexToModel(selectedRow);
        }

        // Lấy thông tin chi tiết của lịch trình từ hàng được chọn
        String scheduleId = tableModel.getValueAt(modelRow, 0).toString();
        String scheduleDate = tableModel.getValueAt(modelRow, 1).toString();
        String scheduleTrain = tableModel.getValueAt(modelRow, 2).toString();
        String scheduleRoute = tableModel.getValueAt(modelRow, 3).toString();
        String scheduleDepartTime = tableModel.getValueAt(modelRow, 4).toString();
        String scheduleArriveTime = tableModel.getValueAt(modelRow, 5).toString();
        String scheduleStatus = tableModel.getValueAt(modelRow, 6).toString();

        // Kiểm tra và cập nhật trạng thái nếu đã qua thời gian khởi hành
        boolean statusUpdated = false;
        try {
            // Chỉ kiểm tra nếu trạng thái không phải "Đã khởi hành" hoặc "Đã hủy"
            if (!scheduleStatus.equals("Đã khởi hành") && !scheduleStatus.equals("Đã hủy")) {
                // Chuyển đổi chuỗi ngày và giờ đi thành LocalDateTime
                LocalDate date = null;
                DateTimeFormatter[] formatters = new DateTimeFormatter[] {
                        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                        DateTimeFormatter.ofPattern("MM/dd/yyyy")
                };

                for (DateTimeFormatter formatter : formatters) {
                    try {
                        date = LocalDate.parse(scheduleDate, formatter);
                        break; // Thoát khỏi vòng lặp nếu phân tích thành công
                    } catch (DateTimeParseException e) {
                        // Tiếp tục thử với định dạng tiếp theo
                    }
                }

                if (date == null) {
                    LOGGER.warning("Không thể phân tích chuỗi ngày: " + scheduleDate);
                    // Xử lý trường hợp không thể phân tích ngày
                    return;
                }
                LocalTime time = LocalTime.parse(scheduleDepartTime, DateTimeFormatter.ofPattern("HH:mm"));
                LocalDateTime departureDateTime = LocalDateTime.of(date, time);

                // Kiểm tra nếu thời gian khởi hành đã qua
                if (departureDateTime.isBefore(LocalDateTime.now())) {
                    // Cập nhật trạng thái trong cơ sở dữ liệu
                    if (isConnected && lichTrinhTauDAO != null) {
                        LichTrinhTau schedule = lichTrinhTauDAO.getById(scheduleId);
                        if (schedule != null) {
                            schedule.setTrangThai(TrangThai.DA_KHOI_HANH);
                            boolean updated = lichTrinhTauDAO.update(schedule);

                            if (updated) {
                                LOGGER.info("Đã tự động cập nhật trạng thái lịch trình " + scheduleId + " thành Đã khởi hành");
                                scheduleStatus = "Đã khởi hành";
                                tableModel.setValueAt(scheduleStatus, modelRow, 6);
                                statusUpdated = true;
                            } else {
                                LOGGER.warning("Không thể cập nhật trạng thái lịch trình " + scheduleId);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Lỗi khi kiểm tra và cập nhật trạng thái lịch trình: " + ex.getMessage(), ex);
        }

        // Tạo dialog hiển thị thông tin chi tiết
        JDialog detailDialog = new JDialog();
        detailDialog.setTitle("Chi tiết lịch trình");
        detailDialog.setSize(500, 400);
        detailDialog.setLocationRelativeTo(this);
        detailDialog.setModal(true);
        detailDialog.setLayout(new BorderLayout(10, 10));

        // Tạo panel chứa thông tin chi tiết
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new GridLayout(7, 2, 10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Tạo các label thông tin với font tốt hơn
        Font labelFont = new Font("Arial", Font.BOLD, 14);
        Font valueFont = new Font("Arial", Font.PLAIN, 14);

        // Thêm các thông tin vào panel
        JLabel idLabel = new JLabel("Mã lịch trình:");
        idLabel.setFont(labelFont);
        contentPanel.add(idLabel);

        JLabel idValueLabel = new JLabel(scheduleId);
        idValueLabel.setFont(valueFont);
        contentPanel.add(idValueLabel);

        JLabel dateLabel = new JLabel("Ngày đi:");
        dateLabel.setFont(labelFont);
        contentPanel.add(dateLabel);

        JLabel dateValueLabel = new JLabel(scheduleDate);
        dateValueLabel.setFont(valueFont);
        contentPanel.add(dateValueLabel);

        JLabel trainLabel = new JLabel("Tàu:");
        trainLabel.setFont(labelFont);
        contentPanel.add(trainLabel);

        JLabel trainValueLabel = new JLabel(scheduleTrain);
        trainValueLabel.setFont(valueFont);
        contentPanel.add(trainValueLabel);

        JLabel routeLabel = new JLabel("Tuyến đường:");
        routeLabel.setFont(labelFont);
        contentPanel.add(routeLabel);

        JLabel routeValueLabel = new JLabel(scheduleRoute);
        routeValueLabel.setFont(valueFont);
        contentPanel.add(routeValueLabel);

        JLabel departTimeLabel = new JLabel("Giờ đi:");
        departTimeLabel.setFont(labelFont);
        contentPanel.add(departTimeLabel);

        JLabel departTimeValueLabel = new JLabel(scheduleDepartTime);
        departTimeValueLabel.setFont(valueFont);
        contentPanel.add(departTimeValueLabel);

        JLabel arriveTimeLabel = new JLabel("Giờ đến (dự kiến):");
        arriveTimeLabel.setFont(labelFont);
        contentPanel.add(arriveTimeLabel);

        JLabel arriveTimeValueLabel = new JLabel(scheduleArriveTime);
        arriveTimeValueLabel.setFont(valueFont);
        contentPanel.add(arriveTimeValueLabel);

        JLabel statusLabel = new JLabel("Trạng thái:");
        statusLabel.setFont(labelFont);
        contentPanel.add(statusLabel);

        // Hiển thị trạng thái với màu sắc tương ứng
        JLabel statusValueLabel = new JLabel(scheduleStatus);
        statusValueLabel.setFont(new Font("Arial", Font.BOLD, 14));

        if ("Đã khởi hành".equals(scheduleStatus)) {
            statusValueLabel.setForeground(new Color(46, 204, 113)); // Xanh lá
        } else if ("Đã hủy".equals(scheduleStatus)) {
            statusValueLabel.setForeground(new Color(231, 76, 60)); // Đỏ
        } else {
            statusValueLabel.setForeground(new Color(52, 152, 219)); // Xanh dương
        }
        contentPanel.add(statusValueLabel);

        // Tạo một panel riêng cho thông báo cập nhật (nếu có)
        JPanel notificationPanel = new JPanel(new BorderLayout());
        notificationPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 20));

        // Nếu trạng thái đã được cập nhật, hiển thị thông báo
        if (statusUpdated) {
            JLabel notificationLabel = new JLabel(
                    "Lịch trình này đã qua thời gian khởi hành và đã được tự động cập nhật trạng thái.");
            notificationLabel.setFont(new Font("Arial", Font.ITALIC, 12));
            notificationLabel.setForeground(new Color(231, 76, 60)); // Màu đỏ nhạt
            notificationPanel.add(notificationLabel, BorderLayout.CENTER);
        }

        // Panel chứa nút đóng
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton("Đóng");
        closeButton.setFont(new Font("Arial", Font.PLAIN, 14));
        closeButton.setPreferredSize(new Dimension(100, 30));
        closeButton.addActionListener(e -> detailDialog.dispose());
        buttonPanel.add(closeButton);

        // Thêm các panel vào dialog
        detailDialog.add(contentPanel, BorderLayout.CENTER);
        detailDialog.add(notificationPanel, BorderLayout.NORTH);
        detailDialog.add(buttonPanel, BorderLayout.SOUTH);

        // Hiển thị dialog
        detailDialog.setVisible(true);

        // Nếu trạng thái đã được cập nhật, làm mới bảng dữ liệu sau khi đóng dialog
        if (statusUpdated) {
            SwingUtilities.invokeLater(this::refreshData);
        }
    }

    // Thêm vào phương thức khởi tạo hoặc một phương thức riêng biệt
    private void setupKeyBindings() {
        InputMap inputMap = scheduleTable.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = scheduleTable.getActionMap();

        // Thêm phím tắt Delete để xóa lịch trình
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        actionMap.put("delete", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSchedule();
            }
        });

        // Thêm phím tắt Enter để xem chi tiết
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "view");
        actionMap.put("view", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                viewScheduleDetails();
            }
        });
    }

    private void setupContextMenu() {
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem viewItem = new JMenuItem("Xem chi tiết");
        viewItem.setIcon(createViewIcon(16, 16));
        viewItem.addActionListener(e -> viewScheduleDetails());

        JMenuItem editItem = new JMenuItem("Chỉnh sửa");
        editItem.setIcon(createEditIcon(16, 16));
        editItem.addActionListener(e -> editSchedule());

        JMenuItem deleteItem = new JMenuItem("Xóa lịch trình");
        deleteItem.setIcon(createDeleteIcon(16, 16));
        deleteItem.addActionListener(e -> deleteSchedule());

        popupMenu.add(viewItem);
        popupMenu.add(editItem);
        popupMenu.add(new JSeparator());
        popupMenu.add(deleteItem);

        scheduleTable.setComponentPopupMenu(popupMenu);
    }

    // Thêm phương thức tạo icon "Xem chi tiết"
    private Icon createViewIcon(int width, int height) {
        return new ImageIcon(createIconImage(width, height, new Color(41, 128, 185), icon -> {
            // Draw an eye-like icon
            Graphics2D g2 = icon;
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(4, 6, 8, 5);
            g2.drawOval(7, 6, 2, 2);
        }));
    }

    private void showErrorMessage(String message, Exception ex) {
        String fullMessage = message;
        if (ex != null) {
            fullMessage += ": " + ex.getMessage();
        }

        JOptionPane.showMessageDialog(this,
                fullMessage,
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Custom renderer để hiển thị màu sắc cho hàng lẻ và hàng chẵn
     */
    private class CustomTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component comp = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            if (isSelected) {
                // Khi hàng được chọn
                comp.setBackground(table.getSelectionBackground());
                comp.setForeground(table.getSelectionForeground());
            } else {
                // Màu nền cho các hàng khác nhau
                if (row % 2 == 0) {
                    comp.setBackground(Color.WHITE);
                } else {
                    comp.setBackground(new Color(245, 245, 245)); // Màu xám rất nhạt
                }
                comp.setForeground(Color.BLACK);
            }

            // Canh lề và font
            ((JLabel) comp).setHorizontalAlignment(SwingConstants.CENTER);
            comp.setFont(new Font("Arial", Font.PLAIN, 12));

            return comp;
        }
    }

    /**
     * Tạo mã lịch trình ngẫu nhiên đảm bảo không trùng lặp
     * @return String mã lịch trình
     */
    private String generateLichTrinhCode() throws RemoteException {
        // Kiểm tra kết nối
        if (!isConnected || lichTrinhTauDAO == null) {
            connectToRMIServer();
            if (!isConnected) {
                throw new RemoteException("Không thể kết nối đến server");
            }
        }

        // Sử dụng ngày và giờ hiện tại (thay vì ngẫu nhiên) khi tạo lịch trình mới
        LocalDateTime now = LocalDateTime.now();

        // Định dạng ngày tháng giờ phút giây
        String dateTimePart = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        // Nếu là ngày mới, đặt lại số đếm về 0
        if (!now.toLocalDate().isEqual(lastGeneratedDate)) {
            count = 0;
            lastGeneratedDate = now.toLocalDate();
        }

        // Tăng số đếm để đảm bảo mã lịch trình không trùng
        if (count >= 999) {
            count = 1; // Đặt lại số đếm nếu vượt quá 999
        } else {
            count++; // Tăng số đếm
        }

        // Tạo phần số đếm, đảm bảo có tối đa 3 chữ số
        String countPart = String.format("%03d", count);

        // Tạo mã lịch trình với cấu trúc: LLT + ngày tháng giờ phút giây + số đếm
        String lichTrinhCode = "LLT" + dateTimePart + "-" + countPart;

        // Kiểm tra trùng lặp trong cơ sở dữ liệu
        try {
            LichTrinhTau existingLichTrinh = lichTrinhTauDAO.getById(lichTrinhCode);
            if (existingLichTrinh != null) {
                // Nếu mã đã tồn tại, gọi lại hàm để tạo mã khác
                return generateLichTrinhCode();
            }
        } catch (Exception e) {
            // Nếu không tìm thấy (thường sẽ gây ra ngoại lệ), thì coi như mã này chưa tồn tại
            LOGGER.log(Level.FINE, "Mã lịch trình chưa tồn tại, có thể sử dụng: " + lichTrinhCode);
        }

        return lichTrinhCode;
    }
}