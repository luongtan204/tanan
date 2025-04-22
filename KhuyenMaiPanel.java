package guiClient;

import com.toedter.calendar.JDateChooser;
import dao.KhuyenMaiDAO;
import model.DoiTuongApDung;
import model.KhuyenMai;
import model.TrangThaiKM;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KhuyenMaiPanel extends JPanel {

    private static final Logger LOGGER = Logger.getLogger(KhuyenMaiPanel.class.getName());
    // Địa chỉ IP và port của RMI server
    private static final String RMI_SERVER_IP = "127.0.0.1";
    private static final int RMI_SERVER_PORT = 9090;
    private JTable promotionTable;
    private DefaultTableModel tableModel;
    private JTextField nameTextField;
    private JDateChooser startDateChooser;
    private JDateChooser endDateChooser;
    private JComboBox<Object> targetComboBox;
    private JComboBox<Object> statusComboBox;
    private JButton searchButton;
    private JButton refreshButton;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;

    private JTabbedPane viewTabbedPane; // Tab để chuyển đổi giữa dạng bảng và lịch
    private PromotionCalendarPanel calendarPanel; // Panel dạng lịch
    private JPanel tableViewPanel; // Panel chứa bảng

    private KhuyenMaiDAO khuyenMaiDAO;
    private boolean isConnected = false;

    // Biến để theo dõi số lượng mã khuyến mãi đã tạo trong ngày hiện tại
    private static LocalDate lastGeneratedDate = LocalDate.now();
    private static int count = 0;

    public KhuyenMaiPanel() {
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
                loadAllPromotionData();
            } catch (RemoteException ex) {
                LOGGER.log(Level.SEVERE, "Error loading promotion data", ex);
                showErrorMessage("Không thể tải dữ liệu khuyến mãi", ex);
            }
        } else {
            showErrorMessage("Không thể kết nối đến máy chủ", null);
        }
        loadDataInBackground();
    }

    private void connectToRMIServer() {
        try {
            // Kết nối đến RMI registry
            Registry registry = LocateRegistry.getRegistry(RMI_SERVER_IP, RMI_SERVER_PORT);

            // Tìm kiếm đối tượng KhuyenMaiDAO từ registry
            khuyenMaiDAO = (KhuyenMaiDAO) registry.lookup("KhuyenMaiDAO");

            // Kiểm tra kết nối
            if (khuyenMaiDAO != null && khuyenMaiDAO.testConnection()) {
                isConnected = true;
                LOGGER.info("Kết nối thành công đến RMI server");
            } else {
                isConnected = false;
                LOGGER.warning("Không thể kết nối đến RMI server");
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Không thể kết nối đến RMI server", ex);
            isConnected = false;
            showErrorMessage("Không thể kết nối đến RMI server: " + ex.getMessage(), ex);
        }
    }

    private void showErrorMessage(String message, Exception ex) {
        String detailMessage = message;
        if (ex != null) {
            detailMessage += "\n\nChi tiết lỗi: " + ex.getMessage();
        }
        JOptionPane.showMessageDialog(this, detailMessage, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    private class CustomTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
            Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            // Màu nền cho hàng chẵn và lẻ
            if (!isSelected) {
                if (row % 2 == 0) {
                    comp.setBackground(Color.WHITE);
                } else {
                    comp.setBackground(new Color(240, 240, 240));
                }
            }

            return comp;
        }
    }

    private void loadAllPromotionData() throws RemoteException {
        if (!isConnected || khuyenMaiDAO == null) {
            connectToRMIServer();
            if (!isConnected) {
                throw new RemoteException("Not connected to RMI server");
            }
        }

        tableModel.setRowCount(0);

        try {
            List<KhuyenMai> promotions = khuyenMaiDAO.findAll();

            if (promotions == null || promotions.isEmpty()) {
                LOGGER.info("Không có khuyến mãi nào để hiển thị.");
                return;
            }

            for (KhuyenMai promotion : promotions) {
                tableModel.addRow(createTableRow(promotion));
            }

            // Cập nhật view lịch nếu đang hiển thị
            if (calendarPanel != null) {
                calendarPanel.refreshCalendar();
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi chi tiết khi tải dữ liệu: " + e.getMessage(), e);
            throw new RemoteException("Lỗi khi tải dữ liệu: " + e.getMessage(), e);
        }
    }

    private Object[] createTableRow(KhuyenMai promotion) {
        return new Object[]{
            promotion.getMaKM(),
            promotion.getTenKM(),
            promotion.getThoiGianBatDau().toString(),
            promotion.getThoiGianKetThuc().toString(),
            promotion.getNoiDungKM(),
            promotion.getChietKhau(),
            promotion.getDoiTuongApDung().getValue(),
            promotion.getTrangThai().getValue()
        };
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
                        // Tải dữ liệu khuyến mãi
                        loadAllPromotionData();
                    } else {
                        tableModel.setRowCount(0);
                        tableModel.addRow(new Object[]{"Không thể kết nối đến máy chủ", "", "", "", "", "", "", ""});
                        showErrorMessage("Không thể kết nối đến máy chủ", null);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error loading promotion data", e);
                    tableModel.setRowCount(0);
                    tableModel.addRow(new Object[]{"Lỗi: " + e.getMessage(), "", "", "", "", "", "", ""});
                    showErrorMessage("Không thể tải dữ liệu khuyến mãi", e);
                }
            }
        };

        worker.execute();
    }

    private JPanel createTitlePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 0, 15, 0));

        // Create a gradient panel for the title
        JPanel gradientPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Create gradient from top to bottom
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(41, 128, 185),
                    0, getHeight(), new Color(52, 152, 219)
                );
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
            }
        };
        gradientPanel.setLayout(new BorderLayout());
        gradientPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // Create title with shadow effect
        JLabel titleLabel = new JLabel("QUẢN LÝ KHUYẾN MÃI", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);

        // Add a subtle icon to the title
        ImageIcon icon = createTitleIcon(32, 32);
        titleLabel.setIcon(icon);
        titleLabel.setIconTextGap(15);

        gradientPanel.add(titleLabel, BorderLayout.CENTER);
        panel.add(gradientPanel, BorderLayout.CENTER);

        return panel;
    }

    private ImageIcon createTitleIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw a gift box or discount icon
        g2.setColor(Color.WHITE);

        // Draw a discount tag
        int[] xPoints = {5, width-5, width-10, width-5, 5};
        int[] yPoints = {5, 5, height/2, height-5, height-5};
        g2.fillPolygon(xPoints, yPoints, 5);

        // Draw % symbol
        g2.setColor(new Color(41, 128, 185));
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.drawString("%", width/2-6, height/2+5);

        g2.dispose();
        return new ImageIcon(image);
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        panel.add(createSearchPanel(), BorderLayout.NORTH);

        // Tạo TabbedPane để chứa cả chế độ xem bảng và lịch
        viewTabbedPane = new JTabbedPane();

        // Tạo panel chế độ xem bảng
        tableViewPanel = new JPanel(new BorderLayout());
        tableViewPanel.add(createTablePanel(), BorderLayout.CENTER);

        // Tạo panel lịch
        calendarPanel = new PromotionCalendarPanel(isConnected ? khuyenMaiDAO : null);

        // Nếu đã kết nối, thiết lập listener cho sự kiện click ngày
        if (isConnected && khuyenMaiDAO != null) {
            // Thiết lập listener cho sự kiện click ngày
            calendarPanel.setDayPanelClickListener((date, promotions) -> {
                // Hiển thị danh sách khuyến mãi của ngày được chọn
                if (!promotions.isEmpty()) {
                    showPromotionDetailsDialog(date, promotions);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Không có khuyến mãi nào cho ngày " + date,
                            "Thông tin",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            });
        }

        // Thêm các tab vào TabbedPane
        viewTabbedPane.addTab("Dạng Bảng", new ImageIcon(), tableViewPanel, "Hiển thị dạng bảng");
        viewTabbedPane.addTab("Dạng Lịch", new ImageIcon(), calendarPanel, "Hiển thị dạng lịch");

        panel.add(viewTabbedPane, BorderLayout.CENTER);
        panel.add(createActionPanel(), BorderLayout.SOUTH);

        return panel;
    }

    // Hiện hộp thoại chi tiết khuyến mãi khi click vào một ngày trong lịch
    private void showPromotionDetailsDialog(LocalDate date, List<KhuyenMai> promotions) {
        JDialog dialog = new JDialog();
        dialog.setTitle("Khuyến mãi ngày " + date);
        dialog.setSize(800, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setModal(true);
        dialog.setLayout(new BorderLayout());

        // Tạo panel chứa bảng khuyến mãi
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Tạo model cho bảng khuyến mãi
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Mã KM");
        model.addColumn("Tên KM");
        model.addColumn("Ngày Bắt Đầu");
        model.addColumn("Ngày Kết Thúc");
        model.addColumn("Nội Dung");
        model.addColumn("Chiết Khấu");
        model.addColumn("Đối Tượng");
        model.addColumn("Trạng Thái");

        // Thêm dữ liệu vào bảng
        for (KhuyenMai khuyenMai : promotions) {
            model.addRow(new Object[]{
                    khuyenMai.getMaKM(),
                    khuyenMai.getTenKM(),
                    khuyenMai.getThoiGianBatDau().toString(),
                    khuyenMai.getThoiGianKetThuc().toString(),
                    khuyenMai.getNoiDungKM(),
                    String.format("%.1f%%", khuyenMai.getChietKhau() * 100),
                    khuyenMai.getDoiTuongApDung().getValue(),
                    khuyenMai.getTrangThai().getValue()
            });
        }

        // Tạo bảng hiển thị khuyến mãi
        JTable promotionTable = new JTable(model);
        promotionTable.setRowHeight(25);
        promotionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        promotionTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));

        // Set renderer cho cột trạng thái để hiển thị màu tương ứng
        promotionTable.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                String status = value.toString();

                // Thiết lập màu nền tương ứng với trạng thái
                if ("Đang diễn ra".equals(status)) {
                    comp.setBackground(new Color(46, 204, 113)); // Xanh lá
                    comp.setForeground(Color.WHITE);
                } else if ("Hết hạn".equals(status)) {
                    comp.setBackground(new Color(231, 76, 60)); // Đỏ
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
        JScrollPane scrollPane = new JScrollPane(promotionTable);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        // Thêm các nút thao tác
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton editButton = new JButton("Chỉnh sửa", createEditIcon(16, 16));
        JButton deleteButton = new JButton("Xóa", createDeleteIcon(16, 16));
        JButton closeButton = new JButton("Đóng");

        // Sự kiện cho nút chỉnh sửa
        editButton.addActionListener(e -> {
            int row = promotionTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(dialog,
                        "Vui lòng chọn một khuyến mãi để chỉnh sửa",
                        "Thông báo",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Lấy mã khuyến mãi đã chọn
            String maKM = (String) promotionTable.getValueAt(row, 0);

            // Đóng dialog hiện tại
            dialog.dispose();

            // Gọi hàm chỉnh sửa với mã khuyến mãi
            editPromotionById(maKM);
        });

        // Sự kiện cho nút xóa
        deleteButton.addActionListener(e -> {
            int row = promotionTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(dialog,
                        "Vui lòng chọn một khuyến mãi để xóa",
                        "Thông báo",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Lấy mã khuyến mãi đã chọn
            String maKM = (String) promotionTable.getValueAt(row, 0);

            // Xác nhận xóa
            int option = JOptionPane.showConfirmDialog(dialog,
                    "Bạn có chắc chắn muốn xóa khuyến mãi " + maKM + "?",
                    "Xác nhận xóa",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (option == JOptionPane.YES_OPTION) {
                try {
                    // Thực hiện xóa
                    boolean deleted = khuyenMaiDAO.delete(maKM);

                    if (deleted) {
                        JOptionPane.showMessageDialog(dialog,
                                "Đã xóa thành công khuyến mãi " + maKM,
                                "Thành công",
                                JOptionPane.INFORMATION_MESSAGE);

                        // Cập nhật lại dữ liệu
                        try {
                            loadAllPromotionData();
                            // Cập nhật view lịch nếu đang hiển thị
                            if (calendarPanel != null) {
                                calendarPanel.refreshCalendar();
                            }
                        } catch (RemoteException ex) {
                            LOGGER.log(Level.SEVERE, "Lỗi khi làm mới dữ liệu", ex);
                            showErrorMessage("Không thể làm mới dữ liệu", ex);
                        }

                        // Đóng dialog
                        dialog.dispose();
                    } else {
                        JOptionPane.showMessageDialog(dialog,
                                "Không thể xóa khuyến mãi " + maKM,
                                "Lỗi",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Lỗi khi xóa khuyến mãi", ex);
                    JOptionPane.showMessageDialog(dialog,
                            "Lỗi khi xóa khuyến mãi: " + ex.getMessage(),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Sự kiện cho nút đóng
        closeButton.addActionListener(e -> dialog.dispose());

        // Thêm các nút vào panel
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(closeButton);

        // Thêm các panel vào dialog
        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Hiển thị dialog
        dialog.setVisible(true);
    }

    private void editPromotionById(String maKM) {
        try {
            // Lấy thông tin chi tiết khuyến mãi
            KhuyenMai khuyenMai = khuyenMaiDAO.findById(maKM);
            if (khuyenMai == null) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy thông tin khuyến mãi", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Create a dialog for editing the promotion
            JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Chỉnh Sửa Khuyến Mãi", true);
            dialog.setSize(500, 500);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new BorderLayout());

            // Create a panel for the form
            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 5, 5, 5);

            // Mã KM (Promotion ID) - readonly
            gbc.gridx = 0;
            gbc.gridy = 0;
            formPanel.add(new JLabel("Mã khuyến mãi:"), gbc);

            gbc.gridx = 1;
            gbc.gridy = 0;
            JTextField maKMField = new JTextField(20);
            maKMField.setText(khuyenMai.getMaKM());
            maKMField.setEditable(false); // Make it readonly
            formPanel.add(maKMField, gbc);

            // Tên KM (Promotion Name)
            gbc.gridx = 0;
            gbc.gridy = 1;
            formPanel.add(new JLabel("Tên khuyến mãi:"), gbc);

            gbc.gridx = 1;
            gbc.gridy = 1;
            JTextField tenKMField = new JTextField(20);
            tenKMField.setText(khuyenMai.getTenKM());
            formPanel.add(tenKMField, gbc);

            // Thời gian bắt đầu (Start Date)
            gbc.gridx = 0;
            gbc.gridy = 2;
            formPanel.add(new JLabel("Ngày bắt đầu:"), gbc);

            gbc.gridx = 1;
            gbc.gridy = 2;
            JDateChooser startDateChooser = new JDateChooser();
            startDateChooser.setDateFormatString("yyyy-MM-dd");
            // Convert LocalDate to Date
            Date startDate = Date.from(khuyenMai.getThoiGianBatDau().atStartOfDay(ZoneId.systemDefault()).toInstant());
            startDateChooser.setDate(startDate);
            formPanel.add(startDateChooser, gbc);

            // Thời gian kết thúc (End Date)
            gbc.gridx = 0;
            gbc.gridy = 3;
            formPanel.add(new JLabel("Ngày kết thúc:"), gbc);

            gbc.gridx = 1;
            gbc.gridy = 3;
            JDateChooser endDateChooser = new JDateChooser();
            endDateChooser.setDateFormatString("yyyy-MM-dd");
            // Convert LocalDate to Date
            Date endDate = Date.from(khuyenMai.getThoiGianKetThuc().atStartOfDay(ZoneId.systemDefault()).toInstant());
            endDateChooser.setDate(endDate);
            formPanel.add(endDateChooser, gbc);

            // Nội dung KM (Promotion Content)
            gbc.gridx = 0;
            gbc.gridy = 4;
            formPanel.add(new JLabel("Nội dung:"), gbc);

            gbc.gridx = 1;
            gbc.gridy = 4;
            JTextField noiDungField = new JTextField(20);
            noiDungField.setText(khuyenMai.getNoiDungKM());
            formPanel.add(noiDungField, gbc);

            // Chiết khấu (Discount Rate)
            gbc.gridx = 0;
            gbc.gridy = 5;
            formPanel.add(new JLabel("Chiết khấu (%):"), gbc);

            gbc.gridx = 1;
            gbc.gridy = 5;
            JSpinner chietKhauSpinner = new JSpinner(new SpinnerNumberModel(khuyenMai.getChietKhau() * 100, 0.0, 100.0, 0.1));
            JSpinner.NumberEditor editor = new JSpinner.NumberEditor(chietKhauSpinner, "##0.0");
            chietKhauSpinner.setEditor(editor);
            formPanel.add(chietKhauSpinner, gbc);

            // Đối tượng áp dụng (Target Audience)
            gbc.gridx = 0;
            gbc.gridy = 6;
            formPanel.add(new JLabel("Đối tượng áp dụng:"), gbc);

            gbc.gridx = 1;
            gbc.gridy = 6;
            DefaultComboBoxModel<Object> targetModel = new DefaultComboBoxModel<>();
            for (DoiTuongApDung target : DoiTuongApDung.values()) {
                targetModel.addElement(target);
            }
            JComboBox<Object> targetComboBox = new JComboBox<>(targetModel);
            targetComboBox.setSelectedItem(khuyenMai.getDoiTuongApDung());
            targetComboBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                    if (value instanceof DoiTuongApDung) {
                        value = ((DoiTuongApDung) value).getValue();
                    }
                    return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                }
            });
            formPanel.add(targetComboBox, gbc);

            // Trạng thái (Status)
            gbc.gridx = 0;
            gbc.gridy = 7;
            formPanel.add(new JLabel("Trạng thái:"), gbc);

            gbc.gridx = 1;
            gbc.gridy = 7;
            DefaultComboBoxModel<Object> statusModel = new DefaultComboBoxModel<>();
            for (TrangThaiKM status : TrangThaiKM.values()) {
                statusModel.addElement(status);
            }
            JComboBox<Object> statusComboBox = new JComboBox<>(statusModel);
            statusComboBox.setSelectedItem(khuyenMai.getTrangThai());
            statusComboBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                    if (value instanceof TrangThaiKM) {
                        value = ((TrangThaiKM) value).getValue();
                    }
                    return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                }
            });
            formPanel.add(statusComboBox, gbc);

            // Add the form panel to the dialog
            dialog.add(formPanel, BorderLayout.CENTER);

            // Create a panel for the buttons
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

            // Save button
            JButton saveButton = new JButton("Lưu");
            saveButton.addActionListener(e -> {
                // Validate the input
                if (tenKMField.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Vui lòng nhập tên khuyến mãi", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (startDateChooser.getDate() == null) {
                    JOptionPane.showMessageDialog(dialog, "Vui lòng chọn ngày bắt đầu", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (endDateChooser.getDate() == null) {
                    JOptionPane.showMessageDialog(dialog, "Vui lòng chọn ngày kết thúc", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Check if end date is after start date
                if (endDateChooser.getDate().before(startDateChooser.getDate())) {
                    JOptionPane.showMessageDialog(dialog, "Ngày kết thúc phải sau ngày bắt đầu", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (noiDungField.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Vui lòng nhập nội dung khuyến mãi", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Update the KhuyenMai object
                khuyenMai.setTenKM(tenKMField.getText().trim());
                khuyenMai.setThoiGianBatDau(startDateChooser.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                khuyenMai.setThoiGianKetThuc(endDateChooser.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                khuyenMai.setNoiDungKM(noiDungField.getText().trim());
                khuyenMai.setChietKhau(((Number) chietKhauSpinner.getValue()).doubleValue() / 100.0); // Convert from percentage to decimal
                khuyenMai.setDoiTuongApDung((DoiTuongApDung) targetComboBox.getSelectedItem());
                khuyenMai.setTrangThai((TrangThaiKM) statusComboBox.getSelectedItem());

                try {
                    // Save the promotion
                    boolean saved = khuyenMaiDAO.save(khuyenMai);

                    if (saved) {
                        JOptionPane.showMessageDialog(dialog, "Đã cập nhật khuyến mãi thành công", "Thành công", JOptionPane.INFORMATION_MESSAGE);

                        // Refresh the table
                        loadAllPromotionData();

                        // Cập nhật view lịch nếu đang hiển thị
                        if (calendarPanel != null) {
                            calendarPanel.refreshCalendar();
                        }

                        // Close the dialog
                        dialog.dispose();
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Không thể cập nhật khuyến mãi", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Error updating promotion", ex);
                    showErrorMessage("Lỗi khi cập nhật khuyến mãi", ex);
                }
            });

            // Cancel button
            JButton cancelButton = new JButton("Hủy");
            cancelButton.addActionListener(e -> dialog.dispose());

            // Add the buttons to the panel
            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);

            // Add the button panel to the dialog
            dialog.add(buttonPanel, BorderLayout.SOUTH);

            // Show the dialog
            dialog.setVisible(true);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tải thông tin khuyến mãi để chỉnh sửa", ex);
            showErrorMessage("Lỗi khi tải thông tin khuyến mãi để chỉnh sửa", ex);
        }
    }

    private JPanel createSearchPanel() {
        // Create a panel with a modern, rounded border and subtle shadow effect
        JPanel outerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw a subtle shadow
                g2d.setColor(new Color(0, 0, 0, 20));
                g2d.fillRoundRect(3, 3, getWidth() - 6, getHeight() - 6, 15, 15);

                // Draw the main background
                g2d.setColor(new Color(245, 245, 250));
                g2d.fillRoundRect(0, 0, getWidth() - 3, getHeight() - 3, 15, 15);
            }
        };

        // Create a titled border with custom font and color
        Border titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createEmptyBorder(),
            "Tìm Kiếm Khuyến Mãi",
            TitledBorder.DEFAULT_JUSTIFICATION,
            TitledBorder.DEFAULT_POSITION,
            new Font("Arial", Font.BOLD, 14),
            new Color(41, 128, 185)
        );

        outerPanel.setBorder(BorderFactory.createCompoundBorder(
            titledBorder,
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        // Panel chứa tất cả các điều khiển tìm kiếm
        JPanel mainSearchPanel = new JPanel();
        mainSearchPanel.setLayout(new BoxLayout(mainSearchPanel, BoxLayout.Y_AXIS));
        mainSearchPanel.setOpaque(false);

        // Panel cho hàng đầu tiên (tên khuyến mãi và đối tượng áp dụng)
        JPanel firstRowPanel = new JPanel(new GridBagLayout());
        firstRowPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Thành phần tìm kiếm theo tên
        JLabel nameLabel = createStyledLabel("Tên khuyến mãi:");
        nameTextField = createStyledTextField(20);

        // Add search icon to text field
        nameTextField.putClientProperty("JTextField.leadingIcon", createSearchIcon(16, 16));

        // Thành phần lọc theo đối tượng áp dụng
        JLabel targetLabel = createStyledLabel("Đối tượng áp dụng:");
        DefaultComboBoxModel<Object> targetModel = new DefaultComboBoxModel<>();
        targetModel.addElement("Tất cả");

        for (DoiTuongApDung target : DoiTuongApDung.values()) {
            targetModel.addElement(target);
        }

        targetComboBox = createStyledComboBox(targetModel);

        // Custom renderer để hiển thị mô tả của enum
        targetComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
                if (value instanceof DoiTuongApDung) {
                    value = ((DoiTuongApDung) value).getValue();
                }
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
                return label;
            }
        });

        // Add components to first row with GridBagLayout
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.1;
        firstRowPanel.add(nameLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.4;
        firstRowPanel.add(nameTextField, gbc);

        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0.1;
        firstRowPanel.add(targetLabel, gbc);

        gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 0.4;
        firstRowPanel.add(targetComboBox, gbc);

        // Panel cho hàng thứ hai (ngày bắt đầu, ngày kết thúc, trạng thái)
        JPanel secondRowPanel = new JPanel(new GridBagLayout());
        secondRowPanel.setOpaque(false);

        // Thành phần tìm kiếm theo ngày bắt đầu
        JLabel startDateLabel = createStyledLabel("Ngày bắt đầu:");
        startDateChooser = createStyledDateChooser();

        // Thành phần tìm kiếm theo ngày kết thúc
        JLabel endDateLabel = createStyledLabel("Ngày kết thúc:");
        endDateChooser = createStyledDateChooser();

        // Thành phần lọc theo trạng thái
        JLabel statusLabel = createStyledLabel("Trạng thái:");
        DefaultComboBoxModel<Object> statusModel = new DefaultComboBoxModel<>();
        statusModel.addElement("Tất cả");

        for (TrangThaiKM status : TrangThaiKM.values()) {
            statusModel.addElement(status);
        }

        statusComboBox = createStyledComboBox(statusModel);

        // Custom renderer để hiển thị mô tả của enum
        statusComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
                if (value instanceof TrangThaiKM) {
                    value = ((TrangThaiKM) value).getValue();
                }
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
                return label;
            }
        });

        // Add components to second row with GridBagLayout
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.1;
        secondRowPanel.add(startDateLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.3;
        secondRowPanel.add(startDateChooser, gbc);

        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0.1;
        secondRowPanel.add(endDateLabel, gbc);

        gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 0.3;
        secondRowPanel.add(endDateChooser, gbc);

        gbc.gridx = 4; gbc.gridy = 0; gbc.weightx = 0.1;
        secondRowPanel.add(statusLabel, gbc);

        gbc.gridx = 5; gbc.gridy = 0; gbc.weightx = 0.3;
        secondRowPanel.add(statusComboBox, gbc);

        // Panel cho hàng thứ ba (nút tìm kiếm và làm mới)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setOpaque(false);

        // Nút tìm kiếm
        searchButton = createStyledButton("Tìm Kiếm", createSearchIcon(16, 16), new Color(52, 152, 219));
        searchButton.addActionListener(this::searchButtonClicked);

        // Nút làm mới
        refreshButton = createStyledButton("Làm Mới", createRefreshIcon(16, 16), new Color(46, 204, 113));
        refreshButton.addActionListener(e -> {
            // Xóa các trường tìm kiếm
            nameTextField.setText("");
            startDateChooser.setDate(null);
            endDateChooser.setDate(null);
            targetComboBox.setSelectedIndex(0);
            statusComboBox.setSelectedIndex(0);

            // Tải lại tất cả dữ liệu
            try {
                loadAllPromotionData();
            } catch (RemoteException ex) {
                LOGGER.log(Level.SEVERE, "Error refreshing data", ex);
                showErrorMessage("Không thể làm mới dữ liệu", ex);
            }
        });

        // Thêm các nút vào panel
        buttonPanel.add(searchButton);
        buttonPanel.add(refreshButton);

        // Add spacing between rows
        mainSearchPanel.add(firstRowPanel);
        mainSearchPanel.add(Box.createVerticalStrut(10));
        mainSearchPanel.add(secondRowPanel);
        mainSearchPanel.add(Box.createVerticalStrut(5));
        mainSearchPanel.add(buttonPanel);

        outerPanel.add(mainSearchPanel, BorderLayout.CENTER);
        return outerPanel;
    }

    // Helper methods for creating styled components
    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        label.setForeground(new Color(70, 70, 70));
        return label;
    }

    private JTextField createStyledTextField(int columns) {
        JTextField textField = new JTextField(columns);
        textField.setPreferredSize(new Dimension(150, 30));
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(204, 204, 204)),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        textField.setFont(new Font("Arial", Font.PLAIN, 12));
        return textField;
    }

    private JComboBox<Object> createStyledComboBox(DefaultComboBoxModel<Object> model) {
        JComboBox<Object> comboBox = new JComboBox<>(model);
        comboBox.setPreferredSize(new Dimension(150, 30));
        comboBox.setFont(new Font("Arial", Font.PLAIN, 12));
        comboBox.setBorder(BorderFactory.createLineBorder(new Color(204, 204, 204)));
        return comboBox;
    }

    private JDateChooser createStyledDateChooser() {
        JDateChooser dateChooser = new JDateChooser();
        dateChooser.setDateFormatString("yyyy-MM-dd");
        dateChooser.setPreferredSize(new Dimension(150, 30));
        dateChooser.setFont(new Font("Arial", Font.PLAIN, 12));

        // Style the text field inside the date chooser
        JTextField textField = (JTextField) dateChooser.getDateEditor().getUiComponent();
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(204, 204, 204)),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));

        return dateChooser;
    }

    private JButton createStyledButton(String text, Icon icon, Color color) {
        JButton button = new JButton(text);
        button.setIcon(icon);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        // Add hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(color.brighter());
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(color);
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                button.setBackground(color.darker());
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                button.setBackground(color);
            }
        });

        return button;
    }

    private Icon createSearchIcon(int width, int height) {
        return new ImageIcon(createIconImage(width, height, new Color(41, 128, 185), g -> {
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval(4, 4, 7, 7);
            g.drawLine(10, 10, 13, 13);
        }));
    }

    private Icon createRefreshIcon(int width, int height) {
        return new ImageIcon(createIconImage(width, height, new Color(41, 128, 185), g -> {
            g.setStroke(new BasicStroke(1.5f));
            int centerX = width / 2;
            int centerY = height / 2;
            g.drawArc(centerX - 5, centerY - 5, 10, 10, 45, 270);
            g.drawLine(centerX, centerY - 5, centerX - 2, centerY - 7);
            g.drawLine(centerX, centerY - 5, centerX + 2, centerY - 7);
        }));
    }

    private Icon createAddIcon(int width, int height) {
        return new ImageIcon(createIconImage(width, height, new Color(46, 204, 113), g -> {
            g.setStroke(new BasicStroke(2f));
            g.drawLine(width / 2, 4, width / 2, height - 4);
            g.drawLine(4, height / 2, width - 4, height / 2);
        }));
    }

    private Icon createEditIcon(int width, int height) {
        return new ImageIcon(createIconImage(width, height, new Color(230, 126, 34), g -> {
            g.setStroke(new BasicStroke(1.5f));
            // Vẽ hình bút chì
            g.drawLine(4, 12, 12, 4);
            g.drawLine(12, 4, 14, 6);
            g.drawLine(4, 12, 2, 14);
            g.drawLine(2, 14, 4, 14);
        }));
    }

    private Icon createDeleteIcon(int width, int height) {
        return new ImageIcon(createIconImage(width, height, new Color(231, 76, 60), g -> {
            g.setStroke(new BasicStroke(1.5f));
            // Vẽ hình thùng rác
            g.drawRect(4, 3, 8, 2);
            g.drawLine(6, 3, 6, 2);
            g.drawLine(10, 3, 10, 2);
            g.drawLine(6, 2, 10, 2);
            g.drawRect(5, 5, 6, 9);
            g.drawLine(7, 7, 7, 12);
            g.drawLine(9, 7, 9, 12);
        }));
    }

    private Image createIconImage(int width, int height, Color color, DrawIcon drawer) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        drawer.draw(g2);
        g2.dispose();
        return image;
    }

    private interface DrawIcon {
        void draw(Graphics2D g2);
    }

    private void searchButtonClicked(ActionEvent e) {
        try {
            // Lấy các giá trị tìm kiếm
            String name = nameTextField.getText().trim();
            LocalDate startDate = null;
            if (startDateChooser.getDate() != null) {
                startDate = startDateChooser.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
            LocalDate endDate = null;
            if (endDateChooser.getDate() != null) {
                endDate = endDateChooser.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
            Object targetObj = targetComboBox.getSelectedItem();
            Object statusObj = statusComboBox.getSelectedItem();

            // Tìm kiếm khuyến mãi
            searchPromotions(name, startDate, endDate, targetObj, statusObj);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error searching promotions", ex);
            showErrorMessage("Lỗi khi tìm kiếm khuyến mãi", ex);
        }
    }

    private void searchPromotions(String name, LocalDate startDate, LocalDate endDate, Object targetObj, Object statusObj) throws RemoteException {
        if (!isConnected || khuyenMaiDAO == null) {
            connectToRMIServer();
            if (!isConnected) {
                throw new RemoteException("Not connected to RMI server");
            }
        }

        tableModel.setRowCount(0);

        try {
            List<KhuyenMai> promotions;

            // Nếu có tên, tìm theo tên
            if (name != null && !name.isEmpty()) {
                promotions = khuyenMaiDAO.findByName(name);
            } else {
                // Nếu không có tên, lấy tất cả
                promotions = khuyenMaiDAO.findAll();
            }

            if (promotions == null || promotions.isEmpty()) {
                LOGGER.info("Không có khuyến mãi nào để hiển thị.");
                return;
            }

            // Lọc theo các điều kiện khác
            for (KhuyenMai promotion : promotions) {
                boolean matchesStartDate = startDate == null || !promotion.getThoiGianBatDau().isBefore(startDate);
                boolean matchesEndDate = endDate == null || !promotion.getThoiGianKetThuc().isAfter(endDate);
                boolean matchesTarget = "Tất cả".equals(targetObj.toString()) || 
                                       (targetObj instanceof DoiTuongApDung && promotion.getDoiTuongApDung() == targetObj) ||
                                       promotion.getDoiTuongApDung().getValue().equals(targetObj.toString());
                boolean matchesStatus = "Tất cả".equals(statusObj.toString()) || 
                                       (statusObj instanceof TrangThaiKM && promotion.getTrangThai() == statusObj) ||
                                       promotion.getTrangThai().getValue().equals(statusObj.toString());

                if (matchesStartDate && matchesEndDate && matchesTarget && matchesStatus) {
                    tableModel.addRow(createTableRow(promotion));
                }
            }

            // Cập nhật view lịch nếu đang hiển thị
            if (calendarPanel != null) {
                calendarPanel.refreshCalendar();
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi chi tiết khi tìm kiếm: " + e.getMessage(), e);
            throw new RemoteException("Lỗi khi tìm kiếm: " + e.getMessage(), e);
        }
    }

    private JPanel createTablePanel() {
        // Create a panel with a modern, rounded border and subtle shadow effect
        JPanel panel = new JPanel(new BorderLayout()) {
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
            "Danh Sách Khuyến Mãi",
            TitledBorder.DEFAULT_JUSTIFICATION,
            TitledBorder.DEFAULT_POSITION,
            new Font("Arial", Font.BOLD, 14),
            new Color(41, 128, 185)
        );

        panel.setBorder(BorderFactory.createCompoundBorder(
            titledBorder,
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        panel.setOpaque(false);

        // Create table model with non-editable cells
        String[] columns = {"Mã KM", "Tên KM", "Ngày Bắt Đầu", "Ngày Kết Thúc", "Nội Dung", "Chiết Khấu", "Đối Tượng", "Trạng Thái"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Create and configure table
        promotionTable = new JTable(tableModel);
        promotionTable.setRowHeight(30); // Increased row height for better readability
        promotionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        promotionTable.setAutoCreateRowSorter(true);
        promotionTable.setShowGrid(true);
        promotionTable.setGridColor(new Color(230, 230, 230));
        promotionTable.setIntercellSpacing(new Dimension(10, 5)); // Add more space between cells
        promotionTable.setFont(new Font("Arial", Font.PLAIN, 12));

        // Remove the default table border
        promotionTable.setBorder(BorderFactory.createEmptyBorder());

        // Thiết lập màu nền cho hàng lẻ và hàng chẵn với màu sắc hiện đại hơn
        promotionTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // Add padding to cell content
                label.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));

                // Center align the text for better readability
                label.setHorizontalAlignment(column == 0 ? JLabel.CENTER : JLabel.LEFT);

                if (isSelected) {
                    label.setBackground(new Color(66, 139, 202, 180)); // Semi-transparent selection color
                    label.setForeground(Color.WHITE);
                } else {
                    // Alternate row colors
                    if (row % 2 == 0) {
                        label.setBackground(Color.WHITE);
                    } else {
                        label.setBackground(new Color(245, 245, 250));
                    }
                    label.setForeground(Color.BLACK);

                    // Special formatting for specific columns
                    if (column == 5) { // Chiết khấu column
                        if (value instanceof Double) {
                            label.setText(String.format("%.1f%%", (Double) value * 100));
                        }
                    } else if (column == 7) { // Trạng thái column
                        String status = value.toString();
                        if ("Đang diễn ra".equals(status)) {
                            label.setForeground(new Color(46, 204, 113));
                            label.setFont(new Font("Arial", Font.BOLD, 12));
                        } else if ("Hết hạn".equals(status)) {
                            label.setForeground(new Color(231, 76, 60));
                            label.setFont(new Font("Arial", Font.BOLD, 12));
                        }
                    }
                }

                return label;
            }
        });

        // Style the table header with a modern gradient look
        JTableHeader header = promotionTable.getTableHeader();
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                         boolean isSelected, boolean hasFocus,
                                                         int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // Set up the header appearance
                label.setBackground(new Color(41, 128, 185));
                label.setForeground(Color.WHITE);
                label.setFont(new Font("Arial", Font.BOLD, 12));
                label.setHorizontalAlignment(JLabel.CENTER);
                label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(33, 103, 153)),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)
                ));

                return label;
            }
        });

        header.setPreferredSize(new Dimension(header.getWidth(), 40)); // Taller header
        header.setResizingAllowed(true);
        header.setReorderingAllowed(false);

        // Áp dụng custom UI cho bảng để có hiệu ứng hover
        setupTableUI();

        // Thiết lập phím tắt và menu ngữ cảnh
        setupKeyBindings();
        setupContextMenu();

        // Create a scroll pane with clean styling
        JScrollPane scrollPane = new JScrollPane(promotionTable);

        // Style the scrollbars
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        scrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 10));

        // Set scrollbar colors
        scrollPane.getVerticalScrollBar().setBackground(Color.WHITE);
        scrollPane.getHorizontalScrollBar().setBackground(Color.WHITE);

        // Remove borders from the scroll pane
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }


    private void setupTableUI() {
        // Đặt một số thuộc tính cho bảng
        promotionTable.setShowHorizontalLines(true);
        promotionTable.setShowVerticalLines(true);
        promotionTable.setGridColor(new Color(230, 230, 230));
        promotionTable.setBackground(Color.WHITE);
        promotionTable.setForeground(Color.BLACK);
        promotionTable.setSelectionBackground(new Color(66, 139, 202)); // Màu khi chọn chính thức
        promotionTable.setSelectionForeground(Color.WHITE);

        // Biến để lưu trạng thái lựa chọn và hiệu ứng hover
        final int[] permanentSelectedRow = {-1}; // Lựa chọn chính thức
        final int[] hoverRow = {-1}; // Dòng đang hover
        final boolean[] isUserSelection = {false}; // Cờ đánh dấu người dùng đã chọn một dòng

        // Thêm hiệu ứng hover bằng cách sử dụng MouseMotionAdapter
        promotionTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                // Lấy dòng hiện tại đang hover
                Point point = e.getPoint();
                int currentRow = promotionTable.rowAtPoint(point);

                // Nếu di chuyển đến một dòng mới
                if (currentRow != hoverRow[0]) {
                    // Cập nhật dòng đang hover
                    hoverRow[0] = currentRow;

                    // Kiểm tra tính hợp lệ của chỉ số hàng
                    boolean isValidRow = currentRow >= 0 && currentRow < promotionTable.getRowCount();
                    boolean isPermanentSelectionValid = permanentSelectedRow[0] >= 0 && permanentSelectedRow[0] < promotionTable.getRowCount();

                    // Nếu người dùng đã có lựa chọn chính thức, chỉ hiển thị hiệu ứng hover
                    if (isUserSelection[0] && isPermanentSelectionValid) {
                        // Nạp lại chọn chính thức
                        promotionTable.setSelectionBackground(new Color(66, 139, 202));
                        promotionTable.setSelectionForeground(Color.WHITE);
                        promotionTable.setRowSelectionInterval(permanentSelectedRow[0], permanentSelectedRow[0]);

                        // Vẽ hiệu ứng hover cho dòng hiện tại
                        promotionTable.repaint();
                    }
                    // Nếu không có lựa chọn chính thức và có hàng hợp lệ, áp dụng hiệu ứng hover
                    else if (isValidRow) {
                        promotionTable.setSelectionBackground(new Color(173, 216, 230)); // Màu xanh nhạt cho hover
                        promotionTable.setSelectionForeground(Color.BLACK);
                        promotionTable.setRowSelectionInterval(currentRow, currentRow);
                    } else {
                        // Không có hàng hợp lệ để hover, xóa lựa chọn
                        promotionTable.clearSelection();
                    }
                }
            }
        });

        // Xử lý các sự kiện chuột khác
        promotionTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoverRow[0] = -1; // Xóa trạng thái hover

                // Kiểm tra tính hợp lệ của lựa chọn chính thức
                boolean isPermanentSelectionValid = permanentSelectedRow[0] >= 0 &&
                        permanentSelectedRow[0] < promotionTable.getRowCount();

                // Nếu có lựa chọn chính thức hợp lệ, giữ nguyên lựa chọn đó
                if (isUserSelection[0] && isPermanentSelectionValid) {
                    promotionTable.setSelectionBackground(new Color(66, 139, 202));
                    promotionTable.setSelectionForeground(Color.WHITE);
                    promotionTable.setRowSelectionInterval(permanentSelectedRow[0], permanentSelectedRow[0]);
                } else {
                    // Không có lựa chọn chính thức, xóa tất cả lựa chọn
                    promotionTable.clearSelection();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int row = promotionTable.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    // Cập nhật lựa chọn chính thức
                    permanentSelectedRow[0] = row;
                    isUserSelection[0] = true;

                    // Áp dụng màu sắc cho lựa chọn chính thức
                    promotionTable.setSelectionBackground(new Color(66, 139, 202));
                    promotionTable.setSelectionForeground(Color.WHITE);
                    promotionTable.setRowSelectionInterval(row, row);
                }
            }
        });

        // Thiết lập renderer cho cột trạng thái để hiển thị màu tương ứng
        promotionTable.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
                Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                String status = value.toString();

                // Thiết lập màu nền tương ứng với trạng thái
                if ("Đang diễn ra".equals(status)) {
                    comp.setBackground(new Color(46, 204, 113)); // Xanh lá
                    comp.setForeground(Color.WHITE);
                } else if ("Hết hạn".equals(status)) {
                    comp.setBackground(new Color(231, 76, 60)); // Đỏ
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

        // Thiết lập renderer cho cột chiết khấu để hiển thị dưới dạng phần trăm
        promotionTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
                if (value instanceof Double) {
                    value = String.format("%.1f%%", (Double) value * 100);
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });
    }

    private void setupKeyBindings() {
        // Thiết lập phím tắt cho các thao tác
        InputMap inputMap = promotionTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = promotionTable.getActionMap();

        // Phím tắt F5 để làm mới dữ liệu
        inputMap.put(KeyStroke.getKeyStroke("F5"), "refresh");
        actionMap.put("refresh", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    loadAllPromotionData();
                } catch (RemoteException ex) {
                    LOGGER.log(Level.SEVERE, "Error refreshing data", ex);
                    showErrorMessage("Không thể làm mới dữ liệu", ex);
                }
            }
        });

        // Phím tắt Delete để xóa khuyến mãi
        inputMap.put(KeyStroke.getKeyStroke("DELETE"), "delete");
        actionMap.put("delete", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deletePromotion();
            }
        });
    }

    private void setupContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();

        JMenuItem viewItem = new JMenuItem("Xem chi tiết");
        JMenuItem editItem = new JMenuItem("Chỉnh sửa");
        JMenuItem deleteItem = new JMenuItem("Xóa");

        viewItem.addActionListener(e -> viewPromotionDetails());
        editItem.addActionListener(e -> editPromotion());
        deleteItem.addActionListener(e -> deletePromotion());

        contextMenu.add(viewItem);
        contextMenu.add(editItem);
        contextMenu.add(deleteItem);

        promotionTable.setComponentPopupMenu(contextMenu);
    }

    private JPanel createActionPanel() {
        // Create a panel with a modern, rounded border and subtle shadow effect
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw a subtle shadow
                g2d.setColor(new Color(0, 0, 0, 10));
                g2d.fillRoundRect(3, 3, getWidth() - 6, getHeight() - 6, 15, 15);

                // Draw the main background
                g2d.setColor(new Color(250, 250, 255));
                g2d.fillRoundRect(0, 0, getWidth() - 3, getHeight() - 3, 15, 15);
            }
        };

        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.setOpaque(false);

        // Create a button panel with right alignment
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        buttonPanel.setOpaque(false);

        // Create action buttons with modern styling
        addButton = createStyledButton("Thêm Khuyến Mãi", createAddIcon(16, 16), new Color(46, 204, 113));
        addButton.addActionListener(e -> addPromotion());

        editButton = createStyledButton("Chỉnh Sửa", createEditIcon(16, 16), new Color(52, 152, 219));
        editButton.addActionListener(e -> editPromotion());

        deleteButton = createStyledButton("Xóa", createDeleteIcon(16, 16), new Color(231, 76, 60));
        deleteButton.addActionListener(e -> deletePromotion());

        // Add buttons to panel
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);

        panel.add(buttonPanel, BorderLayout.EAST);

        // Add a hint label on the left side
        JLabel hintLabel = new JLabel("Chọn một khuyến mãi để chỉnh sửa hoặc xóa");
        hintLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        hintLabel.setForeground(new Color(150, 150, 150));

        panel.add(hintLabel, BorderLayout.WEST);

        return panel;
    }

    // Methods for CRUD operations
    private void addPromotion() {
        // Create a dialog for adding a new promotion
        JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Thêm Khuyến Mãi Mới", true);
        dialog.setSize(500, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        // Create a panel for the form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Mã KM (Promotion ID) - tự động sinh
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Mã khuyến mãi:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        JTextField maKMField = new JTextField(20);
        try {
            // Tự động sinh mã khuyến mãi
            String generatedCode = generatePromotionCode();
            maKMField.setText(generatedCode);
            maKMField.setEditable(false); // Không cho phép chỉnh sửa
            maKMField.setBackground(new Color(240, 240, 240)); // Màu nền xám nhạt
        } catch (RemoteException ex) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tạo mã khuyến mãi tự động", ex);
            showErrorMessage("Không thể tạo mã khuyến mãi tự động", ex);
        }
        formPanel.add(maKMField, gbc);

        // Tên KM (Promotion Name)
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Tên khuyến mãi:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        JTextField tenKMField = new JTextField(20);
        formPanel.add(tenKMField, gbc);

        // Thời gian bắt đầu (Start Date)
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Ngày bắt đầu:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        JDateChooser startDateChooser = new JDateChooser();
        startDateChooser.setDateFormatString("yyyy-MM-dd");
        startDateChooser.setDate(new Date()); // Set to current date
        formPanel.add(startDateChooser, gbc);

        // Thời gian kết thúc (End Date)
        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("Ngày kết thúc:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        JDateChooser endDateChooser = new JDateChooser();
        endDateChooser.setDateFormatString("yyyy-MM-dd");
        // Set default end date to 30 days from now
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 30);
        endDateChooser.setDate(calendar.getTime());
        formPanel.add(endDateChooser, gbc);

        // Nội dung KM (Promotion Content)
        gbc.gridx = 0;
        gbc.gridy = 4;
        formPanel.add(new JLabel("Nội dung:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 4;
        JTextField noiDungField = new JTextField(20);
        formPanel.add(noiDungField, gbc);

        // Chiết khấu (Discount Rate)
        gbc.gridx = 0;
        gbc.gridy = 5;
        formPanel.add(new JLabel("Chiết khấu (%):"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 5;
        JSpinner chietKhauSpinner = new JSpinner(new SpinnerNumberModel(10.0, 0.0, 100.0, 0.1));
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(chietKhauSpinner, "##0.0");
        chietKhauSpinner.setEditor(editor);
        formPanel.add(chietKhauSpinner, gbc);

        // Đối tượng áp dụng (Target Audience)
        gbc.gridx = 0;
        gbc.gridy = 6;
        formPanel.add(new JLabel("Đối tượng áp dụng:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 6;
        DefaultComboBoxModel<Object> targetModel = new DefaultComboBoxModel<>();
        for (DoiTuongApDung target : DoiTuongApDung.values()) {
            targetModel.addElement(target);
        }
        JComboBox<Object> targetComboBox = new JComboBox<>(targetModel);
        targetComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
                if (value instanceof DoiTuongApDung) {
                    value = ((DoiTuongApDung) value).getValue();
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        formPanel.add(targetComboBox, gbc);

        // Trạng thái (Status)
        gbc.gridx = 0;
        gbc.gridy = 7;
        formPanel.add(new JLabel("Trạng thái:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 7;
        DefaultComboBoxModel<Object> statusModel = new DefaultComboBoxModel<>();
        for (TrangThaiKM status : TrangThaiKM.values()) {
            statusModel.addElement(status);
        }
        JComboBox<Object> statusComboBox = new JComboBox<>(statusModel);
        statusComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
                if (value instanceof TrangThaiKM) {
                    value = ((TrangThaiKM) value).getValue();
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        formPanel.add(statusComboBox, gbc);

        // Add the form panel to the dialog
        dialog.add(formPanel, BorderLayout.CENTER);

        // Create a panel for the buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        // Save button
        JButton saveButton = new JButton("Lưu");
        saveButton.addActionListener(e -> {
            // Validate the input

            if (tenKMField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng nhập tên khuyến mãi", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (startDateChooser.getDate() == null) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng chọn ngày bắt đầu", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (endDateChooser.getDate() == null) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng chọn ngày kết thúc", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Check if end date is after start date
            if (endDateChooser.getDate().before(startDateChooser.getDate())) {
                JOptionPane.showMessageDialog(dialog, "Ngày kết thúc phải sau ngày bắt đầu", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (noiDungField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng nhập nội dung khuyến mãi", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Create a new KhuyenMai object
            KhuyenMai khuyenMai = new KhuyenMai();
            khuyenMai.setMaKM(maKMField.getText().trim());
            khuyenMai.setTenKM(tenKMField.getText().trim());
            khuyenMai.setThoiGianBatDau(startDateChooser.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            khuyenMai.setThoiGianKetThuc(endDateChooser.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            khuyenMai.setNoiDungKM(noiDungField.getText().trim());
            khuyenMai.setChietKhau(((Number) chietKhauSpinner.getValue()).doubleValue() / 100.0); // Convert from percentage to decimal
            khuyenMai.setDoiTuongApDung((DoiTuongApDung) targetComboBox.getSelectedItem());
            khuyenMai.setTrangThai((TrangThaiKM) statusComboBox.getSelectedItem());

            try {
                // Save the promotion
                boolean saved = khuyenMaiDAO.save(khuyenMai);

                if (saved) {
                    JOptionPane.showMessageDialog(dialog, "Đã thêm khuyến mãi thành công", "Thành công", JOptionPane.INFORMATION_MESSAGE);

                    // Refresh the table
                    loadAllPromotionData();

                    // Cập nhật view lịch nếu đang hiển thị
                    if (calendarPanel != null) {
                        calendarPanel.refreshCalendar();
                    }

                    // Close the dialog
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Không thể thêm khuyến mãi", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error adding promotion", ex);
                showErrorMessage("Lỗi khi thêm khuyến mãi", ex);
            }
        });

        // Cancel button
        JButton cancelButton = new JButton("Hủy");
        cancelButton.addActionListener(e -> dialog.dispose());

        // Add the buttons to the panel
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        // Add the button panel to the dialog
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Show the dialog
        dialog.setVisible(true);
    }

    private void editPromotion() {
        int selectedRow = promotionTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một khuyến mãi để chỉnh sửa", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Lấy mã khuyến mãi từ dòng đã chọn
        int modelRow = promotionTable.convertRowIndexToModel(selectedRow);
        String maKM = tableModel.getValueAt(modelRow, 0).toString();

        try {
            // Lấy thông tin chi tiết khuyến mãi
            KhuyenMai khuyenMai = khuyenMaiDAO.findById(maKM);
            if (khuyenMai == null) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy thông tin khuyến mãi", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Create a dialog for editing the promotion
            JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Chỉnh Sửa Khuyến Mãi", true);
            dialog.setSize(500, 500);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new BorderLayout());

            // Create a panel for the form
            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 5, 5, 5);

            // Mã KM (Promotion ID) - readonly
            gbc.gridx = 0;
            gbc.gridy = 0;
            formPanel.add(new JLabel("Mã khuyến mãi:"), gbc);

            gbc.gridx = 1;
            gbc.gridy = 0;
            JTextField maKMField = new JTextField(20);
            maKMField.setText(khuyenMai.getMaKM());
            maKMField.setEditable(false); // Make it readonly
            formPanel.add(maKMField, gbc);

            // Tên KM (Promotion Name)
            gbc.gridx = 0;
            gbc.gridy = 1;
            formPanel.add(new JLabel("Tên khuyến mãi:"), gbc);

            gbc.gridx = 1;
            gbc.gridy = 1;
            JTextField tenKMField = new JTextField(20);
            tenKMField.setText(khuyenMai.getTenKM());
            formPanel.add(tenKMField, gbc);

            // Thời gian bắt đầu (Start Date)
            gbc.gridx = 0;
            gbc.gridy = 2;
            formPanel.add(new JLabel("Ngày bắt đầu:"), gbc);

            gbc.gridx = 1;
            gbc.gridy = 2;
            JDateChooser startDateChooser = new JDateChooser();
            startDateChooser.setDateFormatString("yyyy-MM-dd");
            // Convert LocalDate to Date
            Date startDate = Date.from(khuyenMai.getThoiGianBatDau().atStartOfDay(ZoneId.systemDefault()).toInstant());
            startDateChooser.setDate(startDate);
            formPanel.add(startDateChooser, gbc);

            // Thời gian kết thúc (End Date)
            gbc.gridx = 0;
            gbc.gridy = 3;
            formPanel.add(new JLabel("Ngày kết thúc:"), gbc);

            gbc.gridx = 1;
            gbc.gridy = 3;
            JDateChooser endDateChooser = new JDateChooser();
            endDateChooser.setDateFormatString("yyyy-MM-dd");
            // Convert LocalDate to Date
            Date endDate = Date.from(khuyenMai.getThoiGianKetThuc().atStartOfDay(ZoneId.systemDefault()).toInstant());
            endDateChooser.setDate(endDate);
            formPanel.add(endDateChooser, gbc);

            // Nội dung KM (Promotion Content)
            gbc.gridx = 0;
            gbc.gridy = 4;
            formPanel.add(new JLabel("Nội dung:"), gbc);

            gbc.gridx = 1;
            gbc.gridy = 4;
            JTextField noiDungField = new JTextField(20);
            noiDungField.setText(khuyenMai.getNoiDungKM());
            formPanel.add(noiDungField, gbc);

            // Chiết khấu (Discount Rate)
            gbc.gridx = 0;
            gbc.gridy = 5;
            formPanel.add(new JLabel("Chiết khấu (%):"), gbc);

            gbc.gridx = 1;
            gbc.gridy = 5;
            JSpinner chietKhauSpinner = new JSpinner(new SpinnerNumberModel(khuyenMai.getChietKhau() * 100, 0.0, 100.0, 0.1));
            JSpinner.NumberEditor editor = new JSpinner.NumberEditor(chietKhauSpinner, "##0.0");
            chietKhauSpinner.setEditor(editor);
            formPanel.add(chietKhauSpinner, gbc);

            // Đối tượng áp dụng (Target Audience)
            gbc.gridx = 0;
            gbc.gridy = 6;
            formPanel.add(new JLabel("Đối tượng áp dụng:"), gbc);

            gbc.gridx = 1;
            gbc.gridy = 6;
            DefaultComboBoxModel<Object> targetModel = new DefaultComboBoxModel<>();
            for (DoiTuongApDung target : DoiTuongApDung.values()) {
                targetModel.addElement(target);
            }
            JComboBox<Object> targetComboBox = new JComboBox<>(targetModel);
            targetComboBox.setSelectedItem(khuyenMai.getDoiTuongApDung());
            targetComboBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                    if (value instanceof DoiTuongApDung) {
                        value = ((DoiTuongApDung) value).getValue();
                    }
                    return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                }
            });
            formPanel.add(targetComboBox, gbc);

            // Trạng thái (Status)
            gbc.gridx = 0;
            gbc.gridy = 7;
            formPanel.add(new JLabel("Trạng thái:"), gbc);

            gbc.gridx = 1;
            gbc.gridy = 7;
            DefaultComboBoxModel<Object> statusModel = new DefaultComboBoxModel<>();
            for (TrangThaiKM status : TrangThaiKM.values()) {
                statusModel.addElement(status);
            }
            JComboBox<Object> statusComboBox = new JComboBox<>(statusModel);
            statusComboBox.setSelectedItem(khuyenMai.getTrangThai());
            statusComboBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                    if (value instanceof TrangThaiKM) {
                        value = ((TrangThaiKM) value).getValue();
                    }
                    return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                }
            });
            formPanel.add(statusComboBox, gbc);

            // Add the form panel to the dialog
            dialog.add(formPanel, BorderLayout.CENTER);

            // Create a panel for the buttons
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

            // Save button
            JButton saveButton = new JButton("Lưu");
            saveButton.addActionListener(e -> {
                // Validate the input
                if (tenKMField.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Vui lòng nhập tên khuyến mãi", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (startDateChooser.getDate() == null) {
                    JOptionPane.showMessageDialog(dialog, "Vui lòng chọn ngày bắt đầu", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (endDateChooser.getDate() == null) {
                    JOptionPane.showMessageDialog(dialog, "Vui lòng chọn ngày kết thúc", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Check if end date is after start date
                if (endDateChooser.getDate().before(startDateChooser.getDate())) {
                    JOptionPane.showMessageDialog(dialog, "Ngày kết thúc phải sau ngày bắt đầu", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (noiDungField.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Vui lòng nhập nội dung khuyến mãi", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Update the KhuyenMai object
                khuyenMai.setTenKM(tenKMField.getText().trim());
                khuyenMai.setThoiGianBatDau(startDateChooser.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                khuyenMai.setThoiGianKetThuc(endDateChooser.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                khuyenMai.setNoiDungKM(noiDungField.getText().trim());
                khuyenMai.setChietKhau(((Number) chietKhauSpinner.getValue()).doubleValue() / 100.0); // Convert from percentage to decimal
                khuyenMai.setDoiTuongApDung((DoiTuongApDung) targetComboBox.getSelectedItem());
                khuyenMai.setTrangThai((TrangThaiKM) statusComboBox.getSelectedItem());

                try {
                    // Save the promotion
                    boolean saved = khuyenMaiDAO.save(khuyenMai);

                    if (saved) {
                        JOptionPane.showMessageDialog(dialog, "Đã cập nhật khuyến mãi thành công", "Thành công", JOptionPane.INFORMATION_MESSAGE);

                        // Refresh the table
                        loadAllPromotionData();

                        // Close the dialog
                        dialog.dispose();
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Không thể cập nhật khuyến mãi", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Error updating promotion", ex);
                    showErrorMessage("Lỗi khi cập nhật khuyến mãi", ex);
                }
            });

            // Cancel button
            JButton cancelButton = new JButton("Hủy");
            cancelButton.addActionListener(e -> dialog.dispose());

            // Add the buttons to the panel
            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);

            // Add the button panel to the dialog
            dialog.add(buttonPanel, BorderLayout.SOUTH);

            // Show the dialog
            dialog.setVisible(true);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error loading promotion details for editing", ex);
            showErrorMessage("Lỗi khi tải thông tin khuyến mãi để chỉnh sửa", ex);
        }
    }

    private void deletePromotion() {
        // TODO: Implement delete promotion
        int selectedRow = promotionTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một khuyến mãi để xóa", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Lấy mã khuyến mãi từ dòng đã chọn
        int modelRow = promotionTable.convertRowIndexToModel(selectedRow);
        String maKM = tableModel.getValueAt(modelRow, 0).toString();

        // Xác nhận xóa
        int option = JOptionPane.showConfirmDialog(this, 
                "Bạn có chắc chắn muốn xóa khuyến mãi " + maKM + "?", 
                "Xác nhận xóa", 
                JOptionPane.YES_NO_OPTION, 
                JOptionPane.WARNING_MESSAGE);

        if (option == JOptionPane.YES_OPTION) {
            try {
                boolean deleted = khuyenMaiDAO.delete(maKM);
                if (deleted) {
                    JOptionPane.showMessageDialog(this, 
                            "Đã xóa thành công khuyến mãi " + maKM, 
                            "Thành công", 
                            JOptionPane.INFORMATION_MESSAGE);

                    // Cập nhật lại dữ liệu
                    loadAllPromotionData();

                    // Cập nhật view lịch nếu đang hiển thị
                    if (calendarPanel != null) {
                        calendarPanel.refreshCalendar();
                    }
                } else {
                    JOptionPane.showMessageDialog(this, 
                            "Không thể xóa khuyến mãi " + maKM, 
                            "Lỗi", 
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error deleting promotion", ex);
                showErrorMessage("Lỗi khi xóa khuyến mãi", ex);
            }
        }
    }

    /**
     * Tạo mã khuyến mãi tự động theo định dạng KMXXXXXXXXXXX (KM + 11 chữ số)
     * @return Mã khuyến mãi duy nhất
     * @throws RemoteException nếu có lỗi khi kiểm tra mã trong cơ sở dữ liệu
     */
    private String generatePromotionCode() throws RemoteException {
        // Kiểm tra nếu ngày hiện tại khác với ngày tạo mã cuối cùng, reset count
        LocalDate today = LocalDate.now();
        if (!today.equals(lastGeneratedDate)) {
            lastGeneratedDate = today;
            count = 0;
        }

        // Tăng count và tạo mã mới
        count++;

        // Format: KM + năm (4 chữ số) + tháng (2 chữ số) + ngày (2 chữ số) + count (3 chữ số)
        String code = String.format("KM%04d%02d%02d%03d", 
                today.getYear(), 
                today.getMonthValue(), 
                today.getDayOfMonth(),
                count);

        // Kiểm tra xem mã đã tồn tại chưa
        while (khuyenMaiDAO.findById(code) != null) {
            // Nếu đã tồn tại, tăng count và tạo mã mới
            count++;
            code = String.format("KM%04d%02d%02d%03d", 
                    today.getYear(), 
                    today.getMonthValue(), 
                    today.getDayOfMonth(),
                    count);
        }

        return code;
    }

    private void viewPromotionDetails() {
        int selectedRow = promotionTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một khuyến mãi để xem chi tiết", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Lấy mã khuyến mãi từ dòng đã chọn
        int modelRow = promotionTable.convertRowIndexToModel(selectedRow);
        String maKM = tableModel.getValueAt(modelRow, 0).toString();

        try {
            // Lấy thông tin chi tiết khuyến mãi
            KhuyenMai khuyenMai = khuyenMaiDAO.findById(maKM);
            if (khuyenMai == null) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy thông tin khuyến mãi", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Hiển thị thông tin chi tiết
            StringBuilder details = new StringBuilder();
            details.append("Mã khuyến mãi: ").append(khuyenMai.getMaKM()).append("\n");
            details.append("Tên khuyến mãi: ").append(khuyenMai.getTenKM()).append("\n");
            details.append("Thời gian bắt đầu: ").append(khuyenMai.getThoiGianBatDau()).append("\n");
            details.append("Thời gian kết thúc: ").append(khuyenMai.getThoiGianKetThuc()).append("\n");
            details.append("Nội dung: ").append(khuyenMai.getNoiDungKM()).append("\n");
            details.append("Chiết khấu: ").append(String.format("%.1f%%", khuyenMai.getChietKhau() * 100)).append("\n");
            details.append("Đối tượng áp dụng: ").append(khuyenMai.getDoiTuongApDung().getValue()).append("\n");
            details.append("Trạng thái: ").append(khuyenMai.getTrangThai().getValue());

            JOptionPane.showMessageDialog(this, details.toString(), "Chi tiết khuyến mãi", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error viewing promotion details", ex);
            showErrorMessage("Lỗi khi xem chi tiết khuyến mãi", ex);
        }
    }
}
