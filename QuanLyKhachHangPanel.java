package guiClient;

import dao.*;
import dao.impl.KhachHangDAOImpl;
import dao.impl.LoaiKhachHangDAOImpl;
import dao.impl.VeTauDAOImpl;
import guiClient.format.DateLabelFormatter;
import model.*;
import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;
import service.AITravelTimePredictor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QuanLyKhachHangPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(QuanLyKhachHangPanel.class.getName());
    private final JButton deleteButton, updateButton;
    private final JButton addButton;
    private AITravelTimePredictor aiPredictor;
    private JTable customerTable, invoiceTable, ticketTable;
    private DefaultTableModel customerTableModel, invoiceTableModel, ticketTableModel;
    private JTextField searchField;
    private JComboBox<String> customerTypeFilter;
    private JButton searchButton, resetFilterButton;
    private List<KhachHang> customerList;
    private List<LoaiKhachHang> customerTypeList;
    private LoaiKhachHangDAO loaiKhachHangDAO;
    private VeTauDAO veTauDAO;
    private KhachHangDAO khachHangDAO;
    private List<HoaDon> invoiceList;
    private List<VeTau> ticketList;
    private boolean isConnected = false;
    private HoaDonDAO hoaDonDAO;


    private void connectToRMIServer() {
        try {
            // Get the registry
            Registry registry = LocateRegistry.getRegistry("localhost", 9090);

            // Look up the remote objects
            khachHangDAO = (KhachHangDAO) registry.lookup("khachHangDAO");
            loaiKhachHangDAO = (LoaiKhachHangDAO) registry.lookup("loaiKhachHangDAO");
            hoaDonDAO = (HoaDonDAO) registry.lookup("hoaDonDAO");
            veTauDAO = (VeTauDAO) registry.lookup("veTauDAO");

            // Test the connection
            if (khachHangDAO.testConnection()) {
                isConnected = true;
                LOGGER.info("Kết nối RMI server thành công");
            } else {
                isConnected = false;
                LOGGER.warning("Kết nối RMI server thất bại trong quá trình kiểm tra");
            }
        } catch (Exception e) {
            isConnected = false;
            LOGGER.log(Level.SEVERE, "Lỗi kết nối RMI server: " + e.getMessage(), e);
        }
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
                        try {
                            loadCustomerTypes();
                            loadCustomers();
                        } catch (RemoteException ex) {
                            LOGGER.log(Level.SEVERE, "Lỗi khi tải dữ liệu khách hàng", ex);
                            showErrorMessage("Không thể tải dữ liệu khách hàng", ex);
                        }
                    } else {
                        customerTableModel.setRowCount(0);
                        customerTableModel.addRow(new Object[]{"Lỗi kết nối", "Không thể kết nối tới server", "", ""});
                        showErrorMessage("Không thể kết nối đến máy chủ RMI", null);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Lỗi khi tải dữ liệu khách hàng", e);
                    customerTableModel.setRowCount(0);
                    customerTableModel.addRow(new Object[]{"Lỗi: " + e.getMessage(), "", "", ""});
                    showErrorMessage("Không thể tải dữ liệu khách hàng", e);
                }
            }
        };

        worker.execute();
    }

    private void reconnectAndLoadData() {
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                connectToRMIServer();
                return isConnected;
            }

            @Override
            protected void done() {
                try {
                    boolean connected = get();
                    if (connected) {
                        try {
                            loadCustomers();
                        } catch (RemoteException ex) {
                            LOGGER.log(Level.SEVERE, "Lỗi khi tải lại dữ liệu khách hàng", ex);
                            showErrorMessage("Không thể tải lại dữ liệu khách hàng", ex);
                        }
                    } else {
                        showErrorMessage("Không thể kết nối đến máy chủ RMI", null);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Lỗi trong quá trình tái kết nối", e);
                    showErrorMessage("Lỗi trong quá trình tái kết nối", e);
                }
            }
        };

        worker.execute();
    }

    private void showErrorMessage(String message, Exception ex) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                    this,
                    message + (ex != null ? "\nChi tiết: " + ex.getMessage() : ""),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            );
        });
    }

    public QuanLyKhachHangPanel() throws RemoteException {
        setLayout(new BorderLayout());
        this.loaiKhachHangDAO = new LoaiKhachHangDAOImpl();
        this.khachHangDAO = new KhachHangDAOImpl();
        this.veTauDAO = new VeTauDAOImpl();

        // Top Panel: Search and Filter
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));


        searchField = new JTextField(15);
        searchButton = new JButton("Tìm kiếm");
        customerTypeFilter = new JComboBox<>();
        resetFilterButton = new JButton("Đặt lại");
        leftPanel.add(new JLabel("Tìm kiếm bằng số điện thoại:"));
        leftPanel.add(searchField);
        leftPanel.add(searchButton);
        leftPanel.add(new JLabel("Lọc theo loại:"));
        leftPanel.add(customerTypeFilter);
        leftPanel.add(resetFilterButton);



        rightPanel.add(Box.createHorizontalStrut(20)); // Khoảng cách giữa reset và 2 nút mới

        // Thêm vào sau khi tạo các nút update và delete trong rightPanel
        addButton = new JButton("Thêm mới");
        updateButton = new JButton("Cập nhật");
        deleteButton = new JButton("Xóa");
        rightPanel.add(addButton);
        rightPanel.add(updateButton);
        rightPanel.add(deleteButton);

        topPanel.add(leftPanel, BorderLayout.WEST);
        topPanel.add(rightPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        Dimension buttonSize = new Dimension(80, 20);

        searchButton.setPreferredSize(buttonSize);
        searchButton.setBackground(new Color(30, 144, 255));
        searchButton.setForeground(Color.BLACK);

        resetFilterButton.setPreferredSize(buttonSize);
        resetFilterButton.setBackground(new Color(255, 165, 0));
        resetFilterButton.setForeground(Color.BLACK);

        addButton.setPreferredSize(buttonSize);
        addButton.setBackground(new Color(65, 105, 225)); // Royal Blue
        addButton.setForeground(Color.BLACK);

        updateButton.setPreferredSize(buttonSize);
        updateButton.setBackground(new Color(60, 179, 113));
        updateButton.setForeground(Color.BLACK);

        deleteButton.setPreferredSize(buttonSize);
        deleteButton.setBackground(new Color(220, 20, 60));
        deleteButton.setForeground(Color.BLACK);


        // Center Panel: Customer Table
        customerTableModel = new DefaultTableModel(new String[]{"ID", "Tên khách hàng", "Số điện thoại", "Loại"}, 0);
        customerTable = new JTable(customerTableModel);
        add(new JScrollPane(customerTable), BorderLayout.CENTER);

        // Bottom Panel: Invoice and Ticket Tables
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2));
        invoiceTableModel = new DefaultTableModel(new String[]{"Mã hóa đơn", "Ngày lập", "Tổng"}, 0);
        invoiceTable = new JTable(invoiceTableModel);
//        bottomPanel.add(new JScrollPane(invoiceTable));

        ticketTableModel = new DefaultTableModel(new String[]{"Mã vé", "Ghế", "Giá"}, 0);
        ticketTable = new JTable(ticketTableModel);
//        bottomPanel.add(new JScrollPane(ticketTable));
//        add(bottomPanel, BorderLayout.SOUTH);

        // Center Panel: Combine customer, invoice, and ticket tables
        // Bottom Panel: Chứa bảng hóa đơn và bảng vé song song
        bottomPanel.add(new JScrollPane(invoiceTable));
        bottomPanel.add(new JScrollPane(ticketTable));
        add(bottomPanel, BorderLayout.SOUTH);


        // Connect to RMI server
        connectToRMIServer();

        // Add components to the panel
        // [existing UI setup code...]

        // Load initial data
        if (isConnected) {
            try {
                loadCustomerTypes();
                loadCustomers();
            } catch (RemoteException ex) {
                LOGGER.log(Level.SEVERE, "Error loading customer data", ex);
                showErrorMessage("Không thể tải dữ liệu khách hàng", ex);
            }
        } else {
            showErrorMessage("Không thể kết nối đến máy chủ", null);
        }

        // Start background loading task
        loadDataInBackground();

        // Load Data
        loadCustomerTypes();
        loadCustomers();

        // Event Listeners
        searchButton.addActionListener(e -> {
            try {
                // 1. Tìm khách hàng theo số điện thoại
                searchCustomerByPhone();

                // 2. Nếu có kết quả, tự động chọn hàng đầu tiên
                if (customerTable.getRowCount() > 0) {
                    customerTable.setRowSelectionInterval(0, 0);

                    // 3. Load hóa đơn theo khách hàng đó
                    loadInvoicesForCustomer();

                    // 4. Nếu có hóa đơn, load vé của hóa đơn đầu tiên
                    if (invoiceTable.getRowCount() > 0) {
                        String invoiceId = (String) invoiceTableModel.getValueAt(0, 0);
                        loadTicketsForInvoice(invoiceId);
                    } else {
                        ticketTableModel.setRowCount(0); // Xóa vé nếu không có hóa đơn
                    }
                } else {
                    // Không tìm thấy khách hàng → clear bảng hóa đơn & vé
                    invoiceTableModel.setRowCount(0);
                    ticketTableModel.setRowCount(0);
                }

            } catch (RemoteException ex) {
                throw new RuntimeException(ex);
            }
        });
        resetFilterButton.addActionListener(e -> {
            try {
                resetFilters();
            } catch (RemoteException ex) {
                throw new RuntimeException(ex);
            }
        });
        customerTypeFilter.addActionListener(e -> {
            try {
                filterCustomersByType();
            } catch (RemoteException ex) {
                throw new RuntimeException(ex);
            }
        });
        customerTable.getSelectionModel().addListSelectionListener(e -> {
            try {
                loadInvoicesForCustomer();
            } catch (RemoteException ex) {
                throw new RuntimeException(ex);
            }
        });
        invoiceTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                try {
                    int selectedRow = invoiceTable.getSelectedRow();
                    if (selectedRow >= 0) {
                        String invoiceId = (String) invoiceTableModel.getValueAt(selectedRow, 0);
                        loadTicketsForInvoice(invoiceId);
                    }
                } catch (RemoteException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        // Thêm xử lý sự kiện cho nút Thêm mới
        addButton.addActionListener(e -> {
            try {
                showAddCustomerForm();
            } catch (RemoteException ex) {
                LOGGER.log(Level.SEVERE, "Lỗi khi thêm khách hàng mới", ex);
                showErrorMessage("Không thể thêm khách hàng mới", ex);
            }
        });


        deleteButton.addActionListener(e -> {
            try {
                int selectedRow = customerTable.getSelectedRow();
                if (selectedRow >= 0) {
                    String customerId = (String) customerTableModel.getValueAt(selectedRow, 0);

                    int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa khách hàng?", "Confirm", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        boolean success = khachHangDAO.delete(customerId);
                        if (success) {
                            JOptionPane.showMessageDialog(this, "Xóa khách hàng thành công.");
                            loadCustomers();
                            invoiceTableModel.setRowCount(0);
                            ticketTableModel.setRowCount(0);
                        } else {
                            JOptionPane.showMessageDialog(this, "Xóa khách hàng thất bại.");
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Chọn khách hàng để xóa.");
                }
            } catch (RemoteException ex) {
                throw new RuntimeException(ex);
            }
        });


        updateButton.addActionListener(e -> {
            try {
                int selectedRow = customerTable.getSelectedRow();
                if (selectedRow >= 0) {
                    String id = (String) customerTableModel.getValueAt(selectedRow, 0);

                    KhachHang selectedCustomer = customerList.stream()
                            .filter(c -> c.getMaKhachHang().equals(id))
                            .findFirst()
                            .orElse(null);

                    if (selectedCustomer == null) {
                        JOptionPane.showMessageDialog(this, "Không tìm thấy thông tin khách hàng.");
                        return;
                    }

                    // Fields
                    JTextField nameField = new JTextField(selectedCustomer.getTenKhachHang(), 20);
                    JTextField phoneField = new JTextField(selectedCustomer.getSoDienThoai(), 20);
                    JTextField addressField = new JTextField(selectedCustomer.getDiaChi(), 20);
                    JTextField cmndField = new JTextField(selectedCustomer.getGiayTo(), 20);

                    // Date Picker
                    UtilDateModel dateModel = new UtilDateModel();
                    LocalDate dob = selectedCustomer.getNgaySinh();
                    dateModel.setDate(dob.getYear(), dob.getMonthValue() - 1, dob.getDayOfMonth());
                    dateModel.setSelected(true);

                    Properties p = new Properties();
                    p.put("text.today", "Hôm nay");
                    p.put("text.month", "Tháng");
                    p.put("text.year", "Năm");

                    JDatePanelImpl datePanel = new JDatePanelImpl(dateModel, p);
                    JDatePickerImpl datePicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());
                    datePicker.getJFormattedTextField().setPreferredSize(new Dimension(160, 28));

                    // ComboBox loại khách
                    JComboBox<String> typeComboBox = new JComboBox<>();
                    for (LoaiKhachHang t : customerTypeList) {
                        typeComboBox.addItem(t.getTenLoaiKhachHang());
                    }
                    typeComboBox.setSelectedItem(selectedCustomer.getLoaiKhachHang().getTenLoaiKhachHang());

                    // Title
                    JLabel titleLabel = new JLabel("CHỈNH SỬA KHÁCH HÀNG", SwingConstants.CENTER);
                    titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
                    titleLabel.setForeground(Color.BLUE);

                    // Form Panel
                    JPanel formPanel = new JPanel(new GridBagLayout());
                    GridBagConstraints gbc = new GridBagConstraints();
                    gbc.insets = new Insets(10, 10, 10, 10);
                    gbc.anchor = GridBagConstraints.WEST;

                    JLabel[] labels = {
                            new JLabel("Tên khách hàng:"),
                            new JLabel("Số điện thoại:"),
                            new JLabel("Địa chỉ:"),
                            new JLabel("CMND/CCCD:"),
                            new JLabel("Ngày sinh:"),
                            new JLabel("Hạng thành viên:")
                    };
                    for (JLabel lbl : labels) {
                        lbl.setFont(new Font("Arial", Font.BOLD, 14));
                    }

                    // Dòng 1
                    gbc.gridx = 0;
                    gbc.gridy = 0;
                    formPanel.add(labels[0], gbc);
                    gbc.gridx = 1;
                    formPanel.add(nameField, gbc);
                    // Dòng 2
                    gbc.gridx = 0;
                    gbc.gridy = 1;
                    formPanel.add(labels[1], gbc);
                    gbc.gridx = 1;
                    formPanel.add(phoneField, gbc);
                    // Dòng 3
                    gbc.gridx = 0;
                    gbc.gridy = 2;
                    formPanel.add(labels[2], gbc);
                    gbc.gridx = 1;
                    formPanel.add(addressField, gbc);
                    // Dòng 4
                    gbc.gridx = 0;
                    gbc.gridy = 3;
                    formPanel.add(labels[3], gbc);
                    gbc.gridx = 1;
                    formPanel.add(cmndField, gbc);
                    // Dòng 5
                    gbc.gridx = 0;
                    gbc.gridy = 4;
                    formPanel.add(labels[4], gbc);
                    gbc.gridx = 1;
                    formPanel.add(datePicker, gbc);
                    // Dòng 6
                    gbc.gridx = 0;
                    gbc.gridy = 5;
                    formPanel.add(labels[5], gbc);
                    gbc.gridx = 1;
                    formPanel.add(typeComboBox, gbc);

                    JPanel panel = new JPanel();
                    panel.setLayout(new BorderLayout());
                    panel.add(titleLabel, BorderLayout.NORTH);
                    panel.add(formPanel, BorderLayout.CENTER);

                    int result = JOptionPane.showConfirmDialog(this, panel, "Cập nhật thông tin khách hàng", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                    if (result == JOptionPane.OK_OPTION) {
                        selectedCustomer.setTenKhachHang(nameField.getText());
                        selectedCustomer.setSoDienThoai(phoneField.getText());
                        selectedCustomer.setDiaChi(addressField.getText());
                        selectedCustomer.setGiayTo(cmndField.getText());

                        Date selectedDate = (Date) datePicker.getModel().getValue();
                        if (selectedDate != null) {
                            Instant instant = selectedDate.toInstant();
                            ZoneId zone = ZoneId.systemDefault();
                            LocalDate newDob = instant.atZone(zone).toLocalDate();
                            selectedCustomer.setNgaySinh(newDob);
                        }




                        String selectedType = (String) typeComboBox.getSelectedItem();
                        LoaiKhachHang selectedLoai = customerTypeList.stream()
                                .filter(t -> t.getTenLoaiKhachHang().equals(selectedType))
                                .findFirst()
                                .orElse(null);
                        selectedCustomer.setLoaiKhachHang(selectedLoai);

                        boolean success = khachHangDAO.update(selectedCustomer);
                        if (success) {
                            JOptionPane.showMessageDialog(this, "Cập nhật khách hàng thành công.");
                            loadCustomers();
                        } else {
                            JOptionPane.showMessageDialog(this, "Cập nhật thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Vui lòng chọn khách hàng cần cập nhật.");
                }
            } catch (RemoteException ex) {
                throw new RuntimeException(ex);
            }
        });


    }

    private void showAddCustomerForm() throws RemoteException{
        // Fields
        JTextField nameField = new JTextField(20);
        JTextField phoneField = new JTextField(20);
        JTextField addressField = new JTextField(20);
        JTextField cmndField = new JTextField(20);

        // Date Picker cho ngày sinh [// Các trường
//        JTextField nameField = new JTextField(20); JTextField phoneField = new JTextField(20); JTextField addressField = new JTextField(20); JTextField cmndField = new JTextField(20); // Bộ chọn ngày cho ngày sinh
        UtilDateModel dateModel = new UtilDateModel();
        // Thiết lập ngày mặc định là hôm nay
        Calendar today = Calendar.getInstance();
        dateModel.setDate(
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
        );
        dateModel.setSelected(true);

        Properties p = new Properties();
        p.put("text.today", "Hôm nay");
        p.put("text.month", "Tháng");
        p.put("text.year", "Năm");

        JDatePanelImpl datePanel = new JDatePanelImpl(dateModel, p);
        JDatePickerImpl datePicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());
        datePicker.getJFormattedTextField().setPreferredSize(new Dimension(160, 28));

        // ComboBox loại khách hàng
        JComboBox<String> typeComboBox = new JComboBox<>();
        for (LoaiKhachHang type : customerTypeList) {
            typeComboBox.addItem(type.getTenLoaiKhachHang());
        }
        if (typeComboBox.getItemCount() > 0) {
            typeComboBox.setSelectedIndex(0);
        }

        // Title
        JLabel titleLabel = new JLabel("THÊM KHÁCH HÀNG MỚI", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(Color.BLUE);

        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Labels
        JLabel[] labels = {
                new JLabel("Tên khách hàng:"),
                new JLabel("Số điện thoại:"),
                new JLabel("Địa chỉ:"),
                new JLabel("CMND/CCCD:"),
                new JLabel("Ngày sinh:"),
                new JLabel("Hạng thành viên:")
        };
        for (JLabel lbl : labels) {
            lbl.setFont(new Font("Arial", Font.BOLD, 14));
        }

        // Dòng 1
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(labels[0], gbc);
        gbc.gridx = 1;
        formPanel.add(nameField, gbc);
        // Dòng 2
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(labels[1], gbc);
        gbc.gridx = 1;
        formPanel.add(phoneField, gbc);
        // Dòng 3
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(labels[2], gbc);
        gbc.gridx = 1;
        formPanel.add(addressField, gbc);
        // Dòng 4
        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(labels[3], gbc);
        gbc.gridx = 1;
        formPanel.add(cmndField, gbc);
        // Dòng 5
        gbc.gridx = 0;
        gbc.gridy = 4;
        formPanel.add(labels[4], gbc);
        gbc.gridx = 1;
        formPanel.add(datePicker, gbc);
        // Dòng 6
        gbc.gridx = 0;
        gbc.gridy = 5;
        formPanel.add(labels[5], gbc);
        gbc.gridx = 1;
        formPanel.add(typeComboBox, gbc);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

        JButton saveButton = new JButton("Lưu");
        saveButton.setBackground(new Color(60, 179, 113)); // Medium Sea Green
        saveButton.setForeground(Color.BLACK);

        JButton clearButton = new JButton("Xóa trắng");
        clearButton.setBackground(new Color(255, 165, 0)); // Orange
        clearButton.setForeground(Color.BLACK);

        JButton cancelButton = new JButton("Hủy");
        cancelButton.setBackground(new Color(220, 20, 60)); // Crimson
        cancelButton.setForeground(Color.BLACK);

        buttonPanel.add(saveButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(cancelButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Dialog
        JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Thêm khách hàng mới", true);
        dialog.setContentPane(mainPanel);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        // Button actions
//        saveButton.addActionListener(evt -> {
//            try {
//                // Validate input
//                String name = nameField.getText().trim();
//                String phone = phoneField.getText().trim();
//                String address = addressField.getText().trim();
//                String cmnd = cmndField.getText().trim();
//
//                if (name.isEmpty() || phone.isEmpty()) {
//                    JOptionPane.showMessageDialog(dialog,
//                            "Tên khách hàng và số điện thoại không được để trống!",
//                            "Lỗi nhập liệu",
//                            JOptionPane.ERROR_MESSAGE);
//                    return;
//                }
//
//                // Create new customer object
//                KhachHang newCustomer = new KhachHang();
//                newCustomer.setTenKhachHang(name);
//                newCustomer.setSoDienThoai(phone);
//                newCustomer.setDiaChi(address);
//                newCustomer.setGiayTo(cmnd);
//
//                // Set date of birth
//                Date selectedDate = (Date) datePicker.getModel().getValue();
//                if (selectedDate != null) {
//                    Instant instant = selectedDate.toInstant();
//                    ZoneId zone = ZoneId.systemDefault();
//                    LocalDate dob = instant.atZone(zone).toLocalDate();
//                    newCustomer.setNgaySinh(dob);
//                }
//
//                // Set customer type
//                String selectedType = (String) typeComboBox.getSelectedItem();
//                LoaiKhachHang customerType = customerTypeList.stream()
//                        .filter(t -> t.getTenLoaiKhachHang().equals(selectedType))
//                        .findFirst()
//                        .orElse(null);
//                newCustomer.setLoaiKhachHang(customerType);
//                System.out.println(newCustomer);
//
//                // Save to database
//                boolean success = khachHangDAO.add(newCustomer);
//                if (success) {
//                    JOptionPane.showMessageDialog(dialog,
//                            "Thêm khách hàng thành công!",
//                            "Thông báo",
//                            JOptionPane.INFORMATION_MESSAGE);
//                    dialog.dispose();
//
//                    // Reload customer list
//                    loadCustomers();
//
//                    // Tìm kiếm khách hàng vừa tạo
//                    searchField.setText(phone);
//                    searchButton.doClick();
//                } else {
//                    JOptionPane.showMessageDialog(dialog,
//                            "Không thể thêm khách hàng! Vui lòng thử lại.",
//                            "Lỗi",
//                            JOptionPane.ERROR_MESSAGE);
//                }
//            } catch (RemoteException e) {
//                LOGGER.log(Level.SEVERE, "Lỗi khi thêm khách hàng mới", e);
//                JOptionPane.showMessageDialog(dialog,
//                        "Lỗi kết nối: " + e.getMessage(),
//                        "Lỗi",
//                        JOptionPane.ERROR_MESSAGE);
//            }
//        });

        saveButton.addActionListener(evt -> {
            try {
                // Validate input
                String name = nameField.getText().trim();
                String phone = phoneField.getText().trim();
                String address = addressField.getText().trim();
                String cmnd = cmndField.getText().trim();

                if (name.isEmpty() || phone.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog,
                            "Tên khách hàng và số điện thoại không được để trống!",
                            "Lỗi nhập liệu",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Create new customer object
                KhachHang newCustomer = new KhachHang();
                newCustomer.setTenKhachHang(name);
                newCustomer.setSoDienThoai(phone);
                newCustomer.setDiaChi(address);
                newCustomer.setGiayTo(cmnd);

                // Thiết lập ngày tham gia là ngày hiện tại
                newCustomer.setNgayThamgGia(LocalDate.now());

                // Thiết lập điểm tích lũy ban đầu là 0
                newCustomer.setDiemTichLuy(0.0);

                // Set date of birth
                Date selectedDate = (Date) datePicker.getModel().getValue();
                if (selectedDate != null) {
                    Instant instant = selectedDate.toInstant();
                    ZoneId zone = ZoneId.systemDefault();
                    LocalDate dob = instant.atZone(zone).toLocalDate();
                    newCustomer.setNgaySinh(dob);
                }

                // Set customer type
                String selectedType = (String) typeComboBox.getSelectedItem();
                LoaiKhachHang customerType = customerTypeList.stream()
                        .filter(t -> t.getTenLoaiKhachHang().equals(selectedType))
                        .findFirst()
                        .orElse(null);

                if (customerType != null) {
                    newCustomer.setLoaiKhachHang(customerType);

                    // Thiết lập hạng thành viên dựa trên loại khách hàng
                    if (selectedType.toLowerCase().contains("vip")) {
                        newCustomer.setHangThanhVien("VIP");
                    }  else {
                        newCustomer.setHangThanhVien("Vãng lai");  // Mặc định
                    }
                } else {
                    // Nếu không tìm thấy loại khách hàng, đặt mặc định là Vãng lai
                    newCustomer.setHangThanhVien("Vãng lai");
                }

//                System.out.println("Trước khi lưu: " + newCustomer);  // Debug log

                // Save to database
                boolean success = khachHangDAO.add(newCustomer);
                if (success) {
                    JOptionPane.showMessageDialog(dialog,
                            "Thêm khách hàng thành công!",
                            "Thông báo",
                            JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();

                    // Reload customer list
                    loadCustomers();

                    // Tìm kiếm khách hàng vừa tạo
                    searchField.setText(phone);
                    searchButton.doClick();
                } else {
                    JOptionPane.showMessageDialog(dialog,
                            "Không thể thêm khách hàng! Vui lòng thử lại.",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (RemoteException e) {
                LOGGER.log(Level.SEVERE, "Lỗi khi thêm khách hàng mới", e);
                JOptionPane.showMessageDialog(dialog,
                        "Lỗi kết nối: " + e.getMessage(),
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        clearButton.addActionListener(evt -> {
            nameField.setText("");
            phoneField.setText("");
            addressField.setText("");
            cmndField.setText("");
            dateModel.setValue(null);
            if (typeComboBox.getItemCount() > 0) {
                typeComboBox.setSelectedIndex(0);
            }
            nameField.requestFocus();
        });

        cancelButton.addActionListener(evt -> dialog.dispose());

        // Show dialog
        dialog.setVisible(true);
    }


    private void loadCustomerTypes() throws RemoteException {
//        // Load customer types into the filter and form combo box
//        customerTypeList = loaiKhachHangDAO.getAll();
//        customerTypeFilter.addItem("All");
//        for (LoaiKhachHang type : customerTypeList) {
//            customerTypeFilter.addItem(type.getTenLoaiKhachHang());
//        }

        // Load customer types into the filter and form combo box
        customerTypeList = loaiKhachHangDAO.getAll();
        customerTypeFilter.removeAllItems();

        // Use Set to store unique customer type names
        Set<String> uniqueTypes = new HashSet<>();
//        customerTypeFilter.addItem("All");

        for (LoaiKhachHang type : customerTypeList) {
            String typeName = type.getTenLoaiKhachHang();
            if (uniqueTypes.add(typeName)) {  // add() returns true if the element wasn't present
                customerTypeFilter.addItem(typeName);
            }
        }
    }

//    private void loadCustomers() throws RemoteException {
//        // Load all customers into the table
//        customerList = khachHangDAO.getAll();
//        customerTableModel.setRowCount(0);
//        for (KhachHang customer : customerList) {
//            customerTableModel.addRow(new Object[]{
//                    customer.getMaKhachHang(),
//                    customer.getTenKhachHang(),
//                    customer.getSoDienThoai(),
//                    customer.getLoaiKhachHang().getTenLoaiKhachHang()
//            });
//        }
//    }

    private void loadCustomers(String searchPhone, String customerType) throws RemoteException {
        List<KhachHang> filteredCustomers;

        // Xóa dữ liệu cũ trước khi tải dữ liệu mới
        customerTableModel.setRowCount(0);

        // Nếu không có tiêu chí tìm kiếm, hiển thị bảng trống và trả về
        if ((searchPhone == null || searchPhone.trim().isEmpty()) &&
                (customerType == null || customerType.trim().isEmpty())) {
            // Có thể hiển thị thông báo hoặc để bảng trống
            return;
        }

        // Tìm kiếm theo số điện thoại
        if (searchPhone != null && !searchPhone.trim().isEmpty()) {
            filteredCustomers = khachHangDAO.searchByPhone(searchPhone);
        }
        // Tìm kiếm theo loại khách hàng
        else if (customerType != null && !customerType.trim().isEmpty()) {
            filteredCustomers = khachHangDAO.filterByType(customerType);
        }
        // Trường hợp nào đó cả hai tham số đều null (không nên xảy ra theo logic trên)
        else {
            return;
        }

        // Lưu trữ danh sách khách hàng đã lọc
        customerList = filteredCustomers;

        // Hiển thị kết quả tìm kiếm lên bảng
        for (KhachHang customer : customerList) {
            customerTableModel.addRow(new Object[]{
                    customer.getMaKhachHang(),
                    customer.getTenKhachHang(),
                    customer.getSoDienThoai(),
                    customer.getLoaiKhachHang().getTenLoaiKhachHang()
            });
        }
    }

    private void loadCustomers() throws RemoteException {
        // Gọi phương thức có tham số với các tham số null
        loadCustomers(null, null);
    }


    private void searchCustomerByPhone() throws RemoteException {
        // Search customers by phone number
        String phone = searchField.getText();
        customerList = khachHangDAO.searchByPhone(phone);
        customerTableModel.setRowCount(0);
        for (KhachHang customer : customerList) {
            customerTableModel.addRow(new Object[]{
                    customer.getMaKhachHang(),
                    customer.getTenKhachHang(),
                    customer.getSoDienThoai(),
                    customer.getLoaiKhachHang().getTenLoaiKhachHang()
            });
        }
    }

    private void resetFilters() throws RemoteException {
        // Reset filters and reload all customers
        searchField.setText("");
        customerTypeFilter.setSelectedIndex(0);
        loadCustomers();
    }

    private void filterCustomersByType() throws RemoteException {
        // Filter customers by selected type
        String selectedType = (String) customerTypeFilter.getSelectedItem();
        if ("All".equals(selectedType)) {
            loadCustomers();
        } else {
            customerList = khachHangDAO.filterByType(selectedType);
            customerTableModel.setRowCount(0);
            for (KhachHang customer : customerList) {
                customerTableModel.addRow(new Object[]{
                        customer.getMaKhachHang(),
                        customer.getTenKhachHang(),
                        customer.getSoDienThoai(),
                        customer.getLoaiKhachHang().getTenLoaiKhachHang()
                });
            }
        }
    }

    private void loadInvoicesForCustomer() throws RemoteException {
//        // Load invoices for the selected customer
//        int selectedRow = customerTable.getSelectedRow();
//        if (selectedRow >= 0) {
//            String customerId = (String) customerTableModel.getValueAt(selectedRow, 0);
//            invoiceList = hoaDonDAO.getByCustomerId(customerId);
//            invoiceTableModel.setRowCount(0);
//            for (HoaDon invoice : invoiceList) {
//                invoiceTableModel.addRow(new Object[]{
//                        invoice.getMaHD(),
//                        invoice.getNgayLap(),
//                        invoice.getTongTien()
//                });
//            }
//        }

        int selectedRow = customerTable.getSelectedRow();
        if (selectedRow >= 0) {
            String customerId = (String) customerTableModel.getValueAt(selectedRow, 0);
            invoiceList = hoaDonDAO.getByCustomerId(customerId);
            invoiceTableModel.setRowCount(0);

            for (HoaDon invoice : invoiceList) {
                invoiceTableModel.addRow(new Object[]{
                        invoice.getMaHD(),
                        invoice.getNgayLap(),
                        invoice.getTongTien()
                });
            }

            // Load vé cho hóa đơn đầu tiên nếu có
            if (!invoiceList.isEmpty()) {
                HoaDon firstInvoice = invoiceList.get(0);
                loadTicketsForInvoice(firstInvoice.getMaHD());
            } else {
                ticketTableModel.setRowCount(0); // Clear table if no invoice
            }
        }
    }

    // Thêm overload cho loadTicketsForInvoice() truyền mã hóa đơn trực tiếp
    private void loadTicketsForInvoice(String invoiceId) throws RemoteException {
        ticketList = veTauDAO.getByInvoiceId(invoiceId);
        ticketTableModel.setRowCount(0);
        for (VeTau ticket : ticketList) {
            ticketTableModel.addRow(new Object[]{
                    ticket.getMaVe(),
                    ticket.getChoNgoi().getMaCho(),
                    ticket.getGiaVe()
            });
        }
    }

//    private void loadTicketsForInvoice() throws RemoteException {
//        // Load tickets for the selected invoice
//        int selectedRow = invoiceTable.getSelectedRow();
//        if (selectedRow >= 0) {
//            String invoiceId = (String) invoiceTableModel.getValueAt(selectedRow, 0);
//            ticketList = veTauDAO.getByInvoiceId(invoiceId);
//            ticketTableModel.setRowCount(0);
//            for (VeTau ticket : ticketList) {
//                ticketTableModel.addRow(new Object[]{
//                        ticket.getMaVe(),
//                        ticket.getChoNgoi().getMaCho(),
//                        ticket.getGiaVe()
//                });
//            }
//        }
//    }

    private void updateCustomer(JTextField idField, JTextField nameField, JTextField phoneField, JComboBox<String> typeComboBox) throws RemoteException {
        // Update customer information
        String id = idField.getText();
        String name = nameField.getText();
        String phone = phoneField.getText();
        String type = (String) typeComboBox.getSelectedItem();

        if (id.isEmpty() || name.isEmpty() || phone.isEmpty() || type.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        KhachHang customer = new KhachHang();
        customer.setMaKhachHang(id);
        customer.setTenKhachHang(name);
        customer.setSoDienThoai(phone);
        customer.setLoaiKhachHang(customerTypeList.stream()
                .filter(t -> t.getTenLoaiKhachHang().equals(type))
                .findFirst()
                .orElse(null));

        boolean success = khachHangDAO.update(customer);
        if (success) {
            JOptionPane.showMessageDialog(this, "Customer updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadCustomers();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update customer.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


}

