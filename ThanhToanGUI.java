package guiClient;

import dao.*;
import dao.impl.*;
import model.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.RemoteException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Checkout interface for train ticket booking system
 * Displays selected tickets and collects passenger information
 * @author luongtan204
 */
public class ThanhToanGUI extends JFrame {
    // Main panels
    private JPanel mainPanel;
    private JTable ticketTable;
    private DefaultTableModel tableModel;

    // Form fields
    private JTextField nameField;
    private JTextField idCardField;
    private JTextField emailField;
    private JTextField confirmEmailField;
    private JTextField phoneField;
    private JComboBox<KhuyenMai> promotionComboBox;

    // Store the schedule date for promotions
    private LocalDate scheduleDate;

    // Summary fields
    private JLabel totalAmountLabel;
    private double totalAmount = 0.0;

    // Confirmation panel
    private JTextArea confirmationTextArea;

    // Payment fields
    private JTextField amountPaidField;
    private JLabel changeAmountLabel;

    // Data
    private Map<String, String> ticketsMap; // Map of seat IDs to schedule IDs
    private Color primaryColor = new Color(0, 136, 204);

    // Employee information
    private NhanVien nhanVien;

    // DAOs
    private LichTrinhTauDAO lichTrinhTauDAO;
    private ChoNgoiDAO choNgoiDAO;
    private ToaTauDAO toaTauDAO;
    private TauDAO tauDAO;
    private KhuyenMaiDAO khuyenMaiDAO;
    private KhachHangDAO khachHangDAO;
    private dao.LoaiKhachHangDAO loaiKhachHangDAO;
    private VeTauDAO veTauDAO;
    private HoaDonDAO hoaDonDAO;
    private ChiTietHoaDonDAO chiTietHoaDonDAO;
    private LoaiHoaDonDAO loaiHoaDonDAO;

    /**
     * Constructor
     * @param ticketsMap Map of seat IDs to schedule IDs
     * @param nv The employee who is processing the payment
     */
    public ThanhToanGUI(Map<String, String> ticketsMap, NhanVien nv) throws RemoteException {
        this.ticketsMap = ticketsMap;
        this.nhanVien = nv;

        // Initialize DAOs
        lichTrinhTauDAO = new LichTrinhTauDAOImpl();
        choNgoiDAO = new ChoNgoiDAOImpl();
        toaTauDAO = new ToaTauDAOImpl();
        tauDAO = new TauDAOImpl();
        khuyenMaiDAO = new KhuyenMaiDAOImpl();
        khachHangDAO = new KhachHangDAOImpl();
        loaiKhachHangDAO = new LoaiKhachHangDAOImpl();
        veTauDAO = new VeTauDAOImpl();
        hoaDonDAO = new HoaDonDAOImpl();
        chiTietHoaDonDAO = new ChiTietHoaDonDAOImpl();
        loaiHoaDonDAO = new LoaiHoaDonDAOImpl();

        setTitle("Thanh toán vé tàu");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        calculateTotal();
    }

    /**
     * Initialize components
     */
    private void initComponents() {
        // Main container with padding
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Title panel
        JPanel titlePanel = createTitlePanel();
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // Initialize promotionComboBox before creating tickets table
        promotionComboBox = new JComboBox<>();
        promotionComboBox.setPreferredSize(new Dimension(200, 30));

        // Center panel containing tickets table and bottom controls
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));

        // Create tickets table
        createTicketsTable();
        JScrollPane tableScrollPane = new JScrollPane(ticketTable);
        tableScrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        centerPanel.add(tableScrollPane, BorderLayout.CENTER);

        // Bottom panel for buttons and discount
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 0));

        // Left - Remove all tickets button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton removeAllButton = new JButton("Xóa tất cả các vé");
        removeAllButton.setIcon(createTrashIcon());
        removeAllButton.addActionListener(e -> removeAllTickets());
        buttonPanel.add(removeAllButton);
        bottomPanel.add(buttonPanel, BorderLayout.WEST);

        // Center - Promotion selection field
        JPanel promotionPanel = new JPanel(new BorderLayout(5, 0));
        // promotionComboBox is already initialized above
        JLabel promoLabel = new JLabel("Chọn khuyến mãi");
        promoLabel.setBorder(new EmptyBorder(0, 0, 0, 5));
        promotionPanel.add(promoLabel, BorderLayout.WEST);
        promotionPanel.add(promotionComboBox, BorderLayout.CENTER);

        // Apply button
        JButton applyButton = new JButton("Áp dụng");
        applyButton.addActionListener(e -> applyPromotion());
        promotionPanel.add(applyButton, BorderLayout.EAST);
        bottomPanel.add(promotionPanel, BorderLayout.CENTER);

        // Right - Total amount
        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JLabel totalLabel = new JLabel("Tổng tiền");
        totalLabel.setFont(new Font("Arial", Font.BOLD, 16));
        totalAmountLabel = new JLabel();
        totalAmountLabel.setFont(new Font("Arial", Font.BOLD, 16));
        totalPanel.add(totalLabel);
        totalPanel.add(totalAmountLabel);
        bottomPanel.add(totalPanel, BorderLayout.EAST);

        // Add bottom panel to center panel
        centerPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Add center panel to main panel
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Create confirmation panel and add to the right side
        JPanel confirmationPanel = createConfirmationPanel();
        mainPanel.add(confirmationPanel, BorderLayout.EAST);

        // Create south panel for customer info and payment button
        JPanel southPanel = new JPanel(new BorderLayout(0, 10));

        // Customer information section
        JPanel customerPanel = createCustomerInfoPanel();
        southPanel.add(customerPanel, BorderLayout.NORTH);

        // Payment button
        JPanel paymentPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton payButton = new JButton("Thanh toán");
        payButton.setBackground(primaryColor);
        payButton.setForeground(Color.WHITE);
        payButton.setFont(new Font("Arial", Font.BOLD, 14));
        payButton.setPreferredSize(new Dimension(120, 35));
        payButton.addActionListener(e -> processPayment());
        paymentPanel.add(payButton);
        southPanel.add(paymentPanel, BorderLayout.SOUTH);

        // Add south panel to main panel
        mainPanel.add(southPanel, BorderLayout.SOUTH);

        // Add main panel to frame
        add(mainPanel);

        // Set a larger size for the frame to accommodate the new panel
        setSize(1300, 700);
    }

    /**
     * Create title panel
     */
    private JPanel createTitlePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Thanh toán vé tàu", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(primaryColor);
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        panel.add(titleLabel, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Create tickets table
     */
    private void createTicketsTable() {
        // Define table columns
        String[] columns = {
                "Họ tên", "Thông tin chỗ", "Giá vé", "VAT",
                "Giảm đối tượng", "Khuyến mãi", "Thành tiền", ""
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0 || column == 7; // Only name and delete button columns are editable
            }
        };

        // Create table
        ticketTable = new JTable(tableModel);
        ticketTable.setRowHeight(150);
        ticketTable.setShowVerticalLines(true);
        ticketTable.setGridColor(new Color(230, 230, 230));

        // Configure column widths
        ticketTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        ticketTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        ticketTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        ticketTable.getColumnModel().getColumn(3).setPreferredWidth(60);
        ticketTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        ticketTable.getColumnModel().getColumn(5).setPreferredWidth(80);
        ticketTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        ticketTable.getColumnModel().getColumn(7).setPreferredWidth(40);

        // Add table data for each ticket
        try {
            for (Map.Entry<String, String> entry : ticketsMap.entrySet()) {
                String seatId = entry.getKey();
                String lichTrinhId = entry.getValue();

                // Get ticket information from database using DAOs
                addTicketToTable(seatId, lichTrinhId);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Lỗi khi tải thông tin vé: " + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            );
            e.printStackTrace();
        }

        // Set custom renderer and editor for the Họ tên (name) column
        ticketTable.getColumnModel().getColumn(0).setCellRenderer(new PassengerInfoRenderer());
        ticketTable.getColumnModel().getColumn(0).setCellEditor(new PassengerInfoEditor());

        // Add delete button renderer and editor
        ticketTable.getColumnModel().getColumn(7).setCellRenderer(new ButtonRenderer());
        ticketTable.getColumnModel().getColumn(7).setCellEditor(new ButtonEditor(new JCheckBox()));
    }

    /**
     * Add a ticket to the table using seatId and scheduleId
     */
    private void addTicketToTable(String seatId, String scheduleId) {
        try {
            // Get seat information
            ChoNgoi choNgoi = choNgoiDAO.getById(seatId);
            if (choNgoi == null) {
                System.err.println("Không tìm thấy thông tin ghế: " + seatId);
                return;
            }

            // Get schedule information
            LichTrinhTau lichTrinh = lichTrinhTauDAO.getById(scheduleId);
            if (lichTrinh == null) {
                System.err.println("Không tìm thấy thông tin lịch trình: " + scheduleId);
                return;
            }

            // Get toa information (from seat's toaId)
            String toaId = choNgoi.getToaTau().getMaToa();
            ToaTau toaTau = toaTauDAO.getToaTauById(toaId);
            if (toaTau == null) {
                System.err.println("Không tìm thấy thông tin toa: " + toaId);
                return;
            }

            // Get train information
            Tau tau = tauDAO.getTauByLichTrinhTau(lichTrinh);
            if (tau == null) {
                System.err.println("Không tìm thấy thông tin tàu cho lịch trình: " + scheduleId);
                return;
            }

            // Get seat name/number
            String seatName = choNgoi.getTenCho() != null ? choNgoi.getTenCho() : seatId;

            // Get price based on seat class and route
            double price = getSeatPrice(choNgoi, toaTau);

            // Get route information
            String from = lichTrinh.getTau().getTuyenTau().getGaDi();
            String to = lichTrinh.getTau().getTuyenTau().getGaDen();

            // Format date and time
            String departDate = lichTrinh.getNgayDi().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String departTime = lichTrinh.getGioDi().format(DateTimeFormatter.ofPattern("HH:mm"));

            // Create seat info text matching the image format
            String seatInfo = String.format("<html>" +
                            "Ghế: <b>%s</b><br>" +
                            "Toa: <b>Toa %s</b><br>" +
                            "Tàu: <b>Tàu %s</b><br>" +
                            "Tuyến: <b>%s → %s</b><br>" +
                            "Khởi hành: <b>%s %s</b></html>",
                    seatName, toaTau.getTenToa(), tau.getTenTau(), from, to, departDate, departTime);

            // Format price
            String priceStr = formatCurrency(price);

            // VAT calculation (fixed at 10%)
            String vatStr = "10%";

            // Passenger type discount (empty for now)
            String promotion = "";

            // Promotion discount (empty for now)
            String promotionDiscountStr = "0";

            // Calculate total (price + VAT)
            double totalForTicket = price + (price * 0.1);
            String totalStr = formatCurrency(totalForTicket);

            // Create passenger info component - will be replaced by the custom renderer
            PassengerInfo passengerInfo = new PassengerInfo();

            // Add row to table
            tableModel.addRow(new Object[] {
                    passengerInfo, seatInfo, priceStr, vatStr,
                    promotion, promotionDiscountStr, totalStr, "X"
            });

            // Populate promotion combo box with promotions applicable to all based on train schedule date
            populatePromotionComboBox(lichTrinh.getNgayDi());

        } catch (Exception e) {
            System.err.println("Lỗi khi thêm vé vào bảng: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Copy passenger information to buyer information (for the first passenger)
     */
    /**
     * Copy passenger information to buyer information (for the first passenger)
     */
    private void copyPassengerInfoToBuyerInfo(PassengerInfo info) {
        // Make sure we have valid information to copy
        if (info.name == null || info.name.trim().isEmpty() ||
                info.idNumber == null || info.idNumber.trim().isEmpty()) {
            return;
        }

        // Only copy data if the buyer fields are empty
        if (nameField.getText().trim().isEmpty()) {
            nameField.setText(info.name);
        }

        if (idCardField.getText().trim().isEmpty()) {
            idCardField.setText(info.idNumber);
        }
    }

    /**
     * Passenger information class to hold data
     */
    class PassengerInfo {
        String name = "";
        String idNumber = "";
        String passengerType = "Người lớn"; // Default to adult
        int age = 0;

        public PassengerInfo() {}

        public PassengerInfo(String name, String idNumber, String passengerType) {
            this.name = name;
            this.idNumber = idNumber;
            this.passengerType = passengerType;
        }

        // Get discount percentage based on passenger type
        public double getDiscountPercentage() {
            switch (passengerType) {
                case "Sinh viên":
                    return 0.10; // 10% discount for students
                case "Trẻ em":
                    return 0.25; // 25% discount for children
                case "Người cao tuổi":
                    return 0.15; // 15% discount for elderly
                default:
                    return 0.0;  // No discount for regular adults
            }
        }
    }

    /**
     * Renderer for passenger information cell
     */
    class PassengerInfoRenderer extends JPanel implements TableCellRenderer {
        private JTextField nameField = new JTextField();
        private JComboBox<String> typeCombo = new JComboBox<>(
                new String[]{"Người lớn", "Sinh viên", "Trẻ em", "Người cao tuổi"});
        private JTextField idField = new JTextField();
        private JTextField ageField = new JTextField();

        public PassengerInfoRenderer() {
            setLayout(new GridLayout(4, 1, 0, 2));
            setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

            nameField.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200)),
                    "Họ tên",
                    TitledBorder.DEFAULT_JUSTIFICATION,
                    TitledBorder.TOP,
                    new Font("Arial", Font.PLAIN, 10),
                    new Color(100, 100, 100)
            ));

            typeCombo.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200)),
                    "Đối tượng",
                    TitledBorder.DEFAULT_JUSTIFICATION,
                    TitledBorder.TOP,
                    new Font("Arial", Font.PLAIN, 10),
                    new Color(100, 100, 100)
            ));

            idField.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200)),
                    "Số giấy tờ",
                    TitledBorder.DEFAULT_JUSTIFICATION,
                    TitledBorder.TOP,
                    new Font("Arial", Font.PLAIN, 10),
                    new Color(100, 100, 100)
            ));

            ageField.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200)),
                    "Tuổi (cho trẻ em)",
                    TitledBorder.DEFAULT_JUSTIFICATION,
                    TitledBorder.TOP,
                    new Font("Arial", Font.PLAIN, 10),
                    new Color(100, 100, 100)
            ));

            add(nameField);
            add(typeCombo);
            add(idField);
            add(ageField);

            setBackground(Color.WHITE);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof PassengerInfo) {
                PassengerInfo info = (PassengerInfo) value;
                nameField.setText(info.name);
                idField.setText(info.idNumber);
                typeCombo.setSelectedItem(info.passengerType);
                ageField.setText(info.age > 0 ? String.valueOf(info.age) : "");
            }

            return this;
        }
    }

    /**
     * Editor for passenger information cell
     */
    class PassengerInfoEditor extends AbstractCellEditor implements TableCellEditor {
        private JPanel panel = new JPanel();
        private JTextField nameField = new JTextField();
        private JComboBox<String> typeCombo = new JComboBox<>(
                new String[]{"Người lớn", "Sinh viên", "Trẻ em", "Người cao tuổi"});
        private JTextField idField = new JTextField();
        private JTextField ageField = new JTextField();
        private PassengerInfo currentInfo = new PassengerInfo();
        private int currentRow = -1;

        public PassengerInfoEditor() {
            panel.setLayout(new GridLayout(4, 1, 0, 2));
            panel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

            // Configure UI components (borders, etc.) as before...
            nameField.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200)),
                    "Họ tên",
                    TitledBorder.DEFAULT_JUSTIFICATION,
                    TitledBorder.TOP,
                    new Font("Arial", Font.PLAIN, 10),
                    new Color(100, 100, 100)
            ));

            typeCombo.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200)),
                    "Đối tượng",
                    TitledBorder.DEFAULT_JUSTIFICATION,
                    TitledBorder.TOP,
                    new Font("Arial", Font.PLAIN, 10),
                    new Color(100, 100, 100)
            ));

            idField.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200)),
                    "Số giấy tờ",
                    TitledBorder.DEFAULT_JUSTIFICATION,
                    TitledBorder.TOP,
                    new Font("Arial", Font.PLAIN, 10),
                    new Color(100, 100, 100)
            ));

            ageField.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200)),
                    "Tuổi (cho trẻ em)",
                    TitledBorder.DEFAULT_JUSTIFICATION,
                    TitledBorder.TOP,
                    new Font("Arial", Font.PLAIN, 10),
                    new Color(100, 100, 100)
            ));

            panel.add(nameField);
            panel.add(typeCombo);
            panel.add(idField);
            panel.add(ageField);

            panel.setBackground(Color.WHITE);

            // Add action listeners for pressing Enter
            nameField.addActionListener(e -> updateCurrentInfo());
            idField.addActionListener(e -> updateCurrentInfo());
            ageField.addActionListener(e -> updateCurrentInfo());

            // Add type combo listener
            typeCombo.addActionListener(e -> {
                updateCurrentInfo();
                // Enable age field only for children
                ageField.setEnabled("Trẻ em".equals(typeCombo.getSelectedItem()));

                // Recalculate totals when passenger type changes
                if (ticketTable.isEditing()) {
                    stopEditing();
                    calculateTotal();
                }
            });

            // Add focus listeners - this is how we'll detect when user is done with a field
            nameField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    updateCurrentInfo();
                }
            });

            idField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    updateCurrentInfo();
                }
            });

            ageField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    updateCurrentInfo();
                }
            });

            // Initially disable age field unless "Trẻ em" is selected
            ageField.setEnabled(false);
        }

        private void stopEditing() {
            if (ticketTable.isEditing()) {
                TableCellEditor editor = ticketTable.getCellEditor();
                if (editor != null) {
                    editor.stopCellEditing();
                }
            }
        }

        private void updateCurrentInfo() {
            currentInfo.name = nameField.getText();
            currentInfo.idNumber = idField.getText();
            currentInfo.passengerType = (String) typeCombo.getSelectedItem();

            try {
                currentInfo.age = ageField.getText().isEmpty() ? 0 : Integer.parseInt(ageField.getText());
            } catch (NumberFormatException e) {
                currentInfo.age = 0;
            }
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            currentRow = row;

            if (value instanceof PassengerInfo) {
                currentInfo = (PassengerInfo) value;
                nameField.setText(currentInfo.name);
                idField.setText(currentInfo.idNumber);
                typeCombo.setSelectedItem(currentInfo.passengerType);
                ageField.setText(currentInfo.age > 0 ? String.valueOf(currentInfo.age) : "");
                ageField.setEnabled("Trẻ em".equals(currentInfo.passengerType));
            }

            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            updateCurrentInfo();

            // Now that editing is complete, copy first passenger info to buyer info if needed
            if (currentRow == 0) {
                copyPassengerInfoToBuyerInfo(currentInfo);
            }

            return currentInfo;
        }
    }

    /**
     * Calculate seat price based on seat and car type
     */
    private double getSeatPrice(ChoNgoi choNgoi, ToaTau toaTau) {
        // Base price for regular seats
        double basePrice = 250000.0;

        try {
            // Apply multiplier for VIP cars
            if (isVipCar(toaTau)) {
                basePrice *= 1.5; // 50% premium for VIP seats
            }
        } catch (Exception e) {
            System.err.println("Error getting seat price: " + e.getMessage());
        }

        return basePrice;
    }

    /**
     * Determine if a car is VIP based on loaiToa
     */
    private boolean isVipCar(ToaTau toaTau) {
        if (toaTau.getLoaiToa() == null) return false;

        LoaiToa loaiToa = toaTau.getLoaiToa();
        String tenLoai = loaiToa.getTenLoai();

        // Check if the type name contains VIP or premium indicators
        return tenLoai != null && (
                tenLoai.contains("VIP") ||
                        tenLoai.contains("Cao cấp") ||
                        tenLoai.contains("Đặc biệt")
        );
    }

    /**
     * Create customer information panel
     */
    private JPanel createCustomerInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(10, 0, 0, 0)
        ));

        // Form panel with grid layout to match the image
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Row 1: Full name and ID number
        // Full name label
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.15;
        JLabel nameLabel = new JLabel();
        nameLabel.setText("<html>Họ và tên<span style='color:red'>*</span></html>");
        formPanel.add(nameLabel, gbc);

        // Full name field
        gbc.gridx = 1;
        gbc.weightx = 0.35;
        nameField = new JTextField();
        formPanel.add(nameField, gbc);

        // ID card label
        gbc.gridx = 2;
        gbc.weightx = 0.15;
        JLabel idLabel = new JLabel();
        idLabel.setText("<html>Số CMND/Hộ chiếu<span style='color:red'>*</span></html>");
        formPanel.add(idLabel, gbc);

        // ID card field
        gbc.gridx = 3;
        gbc.weightx = 0.35;
        idCardField = new JTextField();
        formPanel.add(idCardField, gbc);

        // Row 2: Email and Confirm email
        // Email label
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.15;
        JLabel emailLabel = new JLabel("Email để nhận vé điện tử");
        formPanel.add(emailLabel, gbc);

        // Email field
        gbc.gridx = 1;
        gbc.weightx = 0.35;
        emailField = new JTextField();
        formPanel.add(emailField, gbc);

        // Confirm email label
        gbc.gridx = 2;
        gbc.weightx = 0.15;
        JLabel confirmEmailLabel = new JLabel("Xác nhận email");
        formPanel.add(confirmEmailLabel, gbc);

        // Confirm email field
        gbc.gridx = 3;
        gbc.weightx = 0.35;
        confirmEmailField = new JTextField();
        formPanel.add(confirmEmailField, gbc);

        // Row 3: Phone number
        // Phone label
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.15;
        JLabel phoneLabel = new JLabel();
        phoneLabel.setText("<html>Số di động<span style='color:red'>*</span></html>");
        formPanel.add(phoneLabel, gbc);

        // Phone field
        gbc.gridx = 1;
        gbc.weightx = 0.35;
        phoneField = new JTextField();
        formPanel.add(phoneField, gbc);

        // Set consistent font and field sizes
        Font labelFont = new Font("Arial", Font.PLAIN, 12);
        nameLabel.setFont(labelFont);
        idLabel.setFont(labelFont);
        emailLabel.setFont(labelFont);
        confirmEmailLabel.setFont(labelFont);
        phoneLabel.setFont(labelFont);

        Dimension fieldSize = new Dimension(0, 30);
        nameField.setPreferredSize(fieldSize);
        idCardField.setPreferredSize(fieldSize);
        emailField.setPreferredSize(fieldSize);
        confirmEmailField.setPreferredSize(fieldSize);
        phoneField.setPreferredSize(fieldSize);

        panel.add(formPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Create confirmation panel to display ticket information
     */
    private JPanel createConfirmationPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Title for confirmation panel
        JLabel titleLabel = new JLabel("Xác nhận thông tin vé", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(primaryColor);
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        panel.add(titleLabel, BorderLayout.NORTH);

        // Text area for ticket information
        confirmationTextArea = new JTextArea();
        confirmationTextArea.setEditable(false);
        confirmationTextArea.setFont(new Font("Arial", Font.PLAIN, 14));
        confirmationTextArea.setLineWrap(true);
        confirmationTextArea.setWrapStyleWord(true);
        confirmationTextArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        confirmationTextArea.setText("Chưa có thông tin vé để xác nhận.");

        JScrollPane scrollPane = new JScrollPane(confirmationTextArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Payment section
        JPanel paymentPanel = createPaymentPanel();
        panel.add(paymentPanel, BorderLayout.SOUTH);

        // Set preferred size for the confirmation panel
        panel.setPreferredSize(new Dimension(300, 0));

        return panel;
    }

    /**
     * Create payment panel for amount paid and change calculation
     */
    private JPanel createPaymentPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(10, 0, 0, 0)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Title for payment section
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel("Thanh toán", JLabel.LEFT);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setForeground(primaryColor);
        panel.add(titleLabel, gbc);

        // Amount paid label
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.4;
        JLabel amountPaidLabel = new JLabel("Tiền khách đưa:");
        amountPaidLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        panel.add(amountPaidLabel, gbc);

        // Amount paid field
        gbc.gridx = 1;
        gbc.weightx = 0.6;
        amountPaidField = new JTextField();
        amountPaidField.setPreferredSize(new Dimension(0, 30));
        // Add document listener to calculate change when amount is entered
        amountPaidField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                calculateChange();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                calculateChange();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                calculateChange();
            }
        });
        panel.add(amountPaidField, gbc);

        // Change label
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.4;
        JLabel changeLabel = new JLabel("Tiền thối lại:");
        changeLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        panel.add(changeLabel, gbc);

        // Change amount label
        gbc.gridx = 1;
        gbc.weightx = 0.6;
        changeAmountLabel = new JLabel("0");
        changeAmountLabel.setFont(new Font("Arial", Font.BOLD, 14));
        changeAmountLabel.setForeground(new Color(0, 128, 0)); // Green color for change amount
        panel.add(changeAmountLabel, gbc);

        return panel;
    }

    /**
     * Calculate change based on amount paid and total amount
     */
    private void calculateChange() {
        try {
            // Get amount paid from text field
            String amountPaidStr = amountPaidField.getText().trim();
            if (amountPaidStr.isEmpty()) {
                changeAmountLabel.setText("0");
                return;
            }

            // Parse amount paid (remove commas if present)
            double amountPaid = Double.parseDouble(amountPaidStr.replace(",", ""));

            // Calculate change
            double change = amountPaid - totalAmount;

            // Update change amount label
            if (change >= 0) {
                changeAmountLabel.setText(formatCurrency(change));
                changeAmountLabel.setForeground(new Color(0, 128, 0)); // Green for positive change
            } else {
                changeAmountLabel.setText("Thiếu: " + formatCurrency(Math.abs(change)));
                changeAmountLabel.setForeground(Color.RED); // Red for negative change (insufficient payment)
            }
        } catch (NumberFormatException e) {
            // Invalid input, set change to 0
            changeAmountLabel.setText("Nhập không hợp lệ");
            changeAmountLabel.setForeground(Color.RED);
        }
    }

    /**
     * Update confirmation text area with ticket information
     */
    private void updateConfirmationText() {
        StringBuilder sb = new StringBuilder();

        // Add header
        sb.append("THÔNG TIN VÉ\n");
        sb.append("------------------------\n\n");

        // Check if there are tickets
        if (tableModel.getRowCount() == 0) {
            sb.append("Chưa có thông tin vé để xác nhận.");
            confirmationTextArea.setText(sb.toString());
            return;
        }

        // Add ticket information for each ticket
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            // Get passenger info
            PassengerInfo info = (PassengerInfo) tableModel.getValueAt(i, 0);
            String passengerName = info.name.isEmpty() ? "Chưa nhập tên" : info.name;

            // Get seat info
            String seatInfo = (String) tableModel.getValueAt(i, 1);
            // Extract just the seat number and train info from the HTML
            String cleanSeatInfo = seatInfo.replaceAll("<[^>]*>", "")
                    .replaceAll("Ghế:", "Ghế:")
                    .replaceAll("Toa:", "\nToa:")
                    .replaceAll("Tàu:", "\nTàu:")
                    .replaceAll("Tuyến:", "\nTuyến:")
                    .replaceAll("Khởi hành:", "\nKhởi hành:");

            // Get price
            String price = (String) tableModel.getValueAt(i, 2);

            // Get total for this ticket
            String total = (String) tableModel.getValueAt(i, 6);

            // Add to confirmation text
            sb.append("Vé ").append(i + 1).append(":\n");
            sb.append("Hành khách: ").append(passengerName).append("\n");
            sb.append(cleanSeatInfo).append("\n");
            sb.append("Giá vé: ").append(price).append("\n");
            sb.append("Thành tiền: ").append(total).append("\n\n");
        }

        // Add total amount
        sb.append("------------------------\n");
        sb.append("TỔNG TIỀN: ").append(formatCurrency(totalAmount)).append("\n");

        // Update confirmation text area
        confirmationTextArea.setText(sb.toString());
        confirmationTextArea.setCaretPosition(0); // Scroll to top
    }

    /**
     * Format currency
     */
    private String formatCurrency(double amount) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        return format.format(amount);
    }

    /**
     * Calculate total amount and update discount based on passenger type
     */
    private void calculateTotal() {
        totalAmount = 0;

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            // Get base price (without VAT) from the third column
            String priceStr = (String) tableModel.getValueAt(i, 2);
            double basePrice = 0.0;

            try {
                // Parse the formatted number by removing commas
                basePrice = Double.parseDouble(priceStr.replace(",", ""));
            } catch (Exception e) {
                System.err.println("Error parsing price: " + e.getMessage());
                continue;
            }

            // Get passenger info to check for discounts
            PassengerInfo info = (PassengerInfo) tableModel.getValueAt(i, 0);
            double discountPercentage = info.getDiscountPercentage();

            // Calculate passenger type discount amount
            double passengerDiscountAmount = basePrice * discountPercentage;

            // Calculate VAT (10%)
            double vat = basePrice * 0.1;

            // Get promotion discount amount (previously insurance)
            String promotionDiscountStr = (String) tableModel.getValueAt(i, 5);
            double promotionDiscount = 0.0;
            try {
                // Remove negative sign, commas, and then parse
                promotionDiscount = Double.parseDouble(promotionDiscountStr.replace("-", "").replace(",", ""));
            } catch (Exception e) {
                System.err.println("Error parsing promotion discount: " + e.getMessage());
            }

            // Calculate ticket total: base price + VAT - passenger discount - promotion discount
            double ticketTotal = basePrice + vat - passengerDiscountAmount - promotionDiscount;

            // Update the row with new values
            String discountStr = passengerDiscountAmount > 0 ?
                    String.format("-%s (%s%%)", formatCurrency(passengerDiscountAmount),
                            (int)(discountPercentage * 100)) : "";

            tableModel.setValueAt(discountStr, i, 4); // Update passenger discount column
            tableModel.setValueAt(formatCurrency(ticketTotal), i, 6); // Update total column

            totalAmount += ticketTotal;
        }

        totalAmountLabel.setText(formatCurrency(totalAmount));

        // Update confirmation text area with ticket information
        updateConfirmationText();

        // Reset change calculation
        if (amountPaidField != null) {
            calculateChange();
        }
    }

    /**
     * Remove all tickets
     */
    private void removeAllTickets() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc muốn xóa tất cả vé?",
                "Xác nhận",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            tableModel.setRowCount(0);
            ticketsMap.clear();
            calculateTotal();
        }
    }

    /**
     * Populate promotion combo box with promotions applicable to all based on schedule date
     */
    private void populatePromotionComboBox(LocalDate scheduleDate) {
        try {
            // Store the schedule date
            this.scheduleDate = scheduleDate;

            // Get promotions applicable to all based on schedule date
            List<KhuyenMai> promotions = khuyenMaiDAO.findPromotionsForAllByScheduleDate(scheduleDate);

            // Create a combo box model
            DefaultComboBoxModel<KhuyenMai> model = new DefaultComboBoxModel<>();

            // Add a default "No promotion" option
            model.addElement(null);

            // Add promotions to the model
            for (KhuyenMai promotion : promotions) {
                model.addElement(promotion);
            }

            // Set the model to the combo box
            promotionComboBox.setModel(model);

            // Set a custom renderer to display promotion name
            promotionComboBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                    if (value == null) {
                        setText("Chọn khuyến mãi");
                    } else {
                        KhuyenMai promotion = (KhuyenMai) value;
                        setText(promotion.getTenKM() + " (" + promotion.getChietKhau() + "%)");
                    }

                    return this;
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Lỗi khi lấy danh sách khuyến mãi: " + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * Apply promotion code
     */
    private void applyPromotion() {
        KhuyenMai selectedPromotion = (KhuyenMai) promotionComboBox.getSelectedItem();
        if (selectedPromotion == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Vui lòng chọn khuyến mãi",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        // Get the promotion discount percentage
        // The database stores the discount as a decimal (e.g., 0.1 for 10%), so we use it directly
        double discountPercentage = selectedPromotion.getChietKhau();

        // Apply the promotion discount to each row in the table
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            // Get base price from the third column
            String priceStr = (String) tableModel.getValueAt(i, 2);
            double basePrice = 0.0;

            try {
                // Parse the formatted number by removing commas
                basePrice = Double.parseDouble(priceStr.replace(",", ""));
            } catch (Exception e) {
                System.err.println("Error parsing price: " + e.getMessage());
                continue;
            }

            // Calculate promotion discount amount
            double discountAmount = basePrice * discountPercentage;

            // Format the discount amount as a negative number to indicate it's a discount
            String discountStr = "-" + formatCurrency(discountAmount);

            // Replace the insurance amount with the promotion discount
            tableModel.setValueAt(discountStr, i, 5);
        }

        // Show success message
        JOptionPane.showMessageDialog(
                this,
                "Đã áp dụng khuyến mãi: " + selectedPromotion.getTenKM(),
                "Thông báo",
                JOptionPane.INFORMATION_MESSAGE
        );

        // Recalculate total with promotion
        calculateTotal();
    }

    /**
     * Process payment
     */
    private void processPayment() {
        // Validate required fields
        if (nameField.getText().trim().isEmpty() ||
                idCardField.getText().trim().isEmpty() ||
                phoneField.getText().trim().isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Vui lòng điền đầy đủ các trường bắt buộc",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        // Validate email match if provided
        if (!emailField.getText().trim().isEmpty() &&
                !emailField.getText().equals(confirmEmailField.getText())) {

            JOptionPane.showMessageDialog(
                    this,
                    "Email xác nhận không khớp với email đã nhập",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        // Validate passenger information is entered for all tickets
        boolean missingPassengerInfo = false;
        boolean invalidChildAge = false;

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Object cellValue = tableModel.getValueAt(i, 0);
            if (cellValue instanceof PassengerInfo) {
                PassengerInfo info = (PassengerInfo) cellValue;

                // Check if required fields are filled
                if (info.name.trim().isEmpty() || info.idNumber.trim().isEmpty()) {
                    missingPassengerInfo = true;
                    break;
                }

                // Validate child age
                if ("Trẻ em".equals(info.passengerType) && (info.age <= 0 || info.age >= 10)) {
                    invalidChildAge = true;
                    break;
                }
            }
        }

        if (missingPassengerInfo) {
            JOptionPane.showMessageDialog(
                    this,
                    "Vui lòng điền đầy đủ thông tin hành khách cho tất cả các vé",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        if (invalidChildAge) {
            JOptionPane.showMessageDialog(
                    this,
                    "Đối tượng 'Trẻ em' phải có tuổi từ 1-9",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        // Check if amount paid is sufficient
        if (amountPaidField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Vui lòng nhập số tiền khách đưa",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            );
            amountPaidField.requestFocus();
            return;
        }

        try {
            double amountPaid = Double.parseDouble(amountPaidField.getText().trim().replace(",", ""));
            if (amountPaid < totalAmount) {
                JOptionPane.showMessageDialog(
                        this,
                        "Số tiền khách đưa không đủ để thanh toán",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE
                );
                amountPaidField.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Số tiền khách đưa không hợp lệ",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            );
            amountPaidField.requestFocus();
            return;
        }

        // Show payment options dialog
        String[] options = {"Tiền mặt", "Thẻ tín dụng", "Chuyển khoản", "Ví điện tử"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Chọn phương thức thanh toán",
                "Thanh toán",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        // Process based on selected payment method
        if (choice >= 0) {
            // Show processing message
            JOptionPane.showMessageDialog(
                    this,
                    "Đang xử lý thanh toán...",
                    "Thanh toán",
                    JOptionPane.INFORMATION_MESSAGE
            );

            try {
                // Get customer information from form
                String customerName = nameField.getText().trim();
                String idCard = idCardField.getText().trim();
                String phone = phoneField.getText().trim();
                String email = emailField.getText().trim(); // This is the address field as per requirements

                // Check if customer exists by ID card and phone number
                KhachHang existingCustomer = khachHangDAO.findByIdCardAndPhone(idCard, phone);

                if (existingCustomer != null) {
                    // Customer exists
                    if ("VIP".equalsIgnoreCase(existingCustomer.getHangThanhVien())) {
                        // Customer is VIP, add loyalty points (1 point per 10,000)
                        int pointsToAdd = (int) (totalAmount / 10000);
                        existingCustomer.setDiemTichLuy(existingCustomer.getDiemTichLuy() + pointsToAdd);

                        // Update customer in database
                        khachHangDAO.update(existingCustomer);

                        JOptionPane.showMessageDialog(
                                this,
                                "Khách hàng VIP đã được cộng " + pointsToAdd + " điểm tích lũy.",
                                "Thông báo",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    } else {
                        // Customer exists but is not VIP, don't add points
                        JOptionPane.showMessageDialog(
                                this,
                                "Khách hàng không phải VIP, không được cộng điểm tích lũy.",
                                "Thông báo",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    }
                } else {
                    // Customer doesn't exist, create new customer
                    KhachHang newCustomer = new KhachHang();
                    newCustomer.setTenKhachHang(customerName);
                    newCustomer.setGiayTo(idCard);
                    newCustomer.setSoDienThoai(phone);
                    newCustomer.setDiaChi(email); // Email address as per requirements
                    newCustomer.setHangThanhVien("Vãng lai"); // Default member rank
                    newCustomer.setDiemTichLuy(0.0); // Initial loyalty points
                    newCustomer.setNgaySinh(LocalDate.now()); // Default birth date, should be updated later
                    newCustomer.setNgayThamgGia(LocalDate.now()); // Join date is today

                    // Get a default customer type
                    try {
                        // Get all customer types
                        List<LoaiKhachHang> types = loaiKhachHangDAO.getAll();

                        // Try to find a default customer type (assuming "Thường" is the default type)
                        LoaiKhachHang defaultType = null;
                        for (LoaiKhachHang type : types) {
                            if ("Thường".equals(type.getTenLoaiKhachHang())) {
                                defaultType = type;
                                break;
                            }
                        }

                        // If "Thường" type doesn't exist, use the first available type
                        if (defaultType == null && !types.isEmpty()) {
                            defaultType = types.get(0);
                        }

                        if (defaultType != null) {
                            newCustomer.setLoaiKhachHang(defaultType);
                        } else {
                            throw new Exception("Không tìm thấy loại khách hàng nào trong cơ sở dữ liệu");
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Lỗi khi tìm loại khách hàng: " + e.getMessage(),
                                "Lỗi",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                    }

                    // Add new customer to database
                    khachHangDAO.add(newCustomer);

                    JOptionPane.showMessageDialog(
                            this,
                            "Đã thêm khách hàng mới vào cơ sở dữ liệu.",
                            "Thông báo",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }

                // Get customer information
                KhachHang customer = existingCustomer;
                if (existingCustomer == null) {
                    // If we're here, we must have created a new customer
                    try {
                        // Get all customers to find the newly added one
                        List<KhachHang> customers = khachHangDAO.searchByPhone(phoneField.getText().trim());
                        for (KhachHang kh : customers) {
                            if (kh.getGiayTo().equals(idCardField.getText().trim())) {
                                customer = kh;
                                break;
                            }
                        }

                        if (customer == null) {
                            throw new Exception("Không thể tìm thấy thông tin khách hàng sau khi thêm mới");
                        }
                    } catch (Exception e) {
                        throw new Exception("Lỗi khi lấy thông tin khách hàng: " + e.getMessage());
                    }
                }

                // 1. Create and save tickets
                List<VeTau> tickets = new ArrayList<>();
                Map<String, Double> ticketPrices = new HashMap<>(); // Store base prices for invoice details
                Map<String, Double> ticketVATs = new HashMap<>(); // Store VAT amounts for invoice details

                // Process each ticket in the table
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    // Get seat ID and schedule ID
                    String seatId = ticketsMap.keySet().toArray(new String[0])[i];
                    String scheduleId = ticketsMap.get(seatId);

                    // Get passenger info
                    PassengerInfo info = (PassengerInfo) tableModel.getValueAt(i, 0);

                    // Get base price from the table
                    String priceStr = (String) tableModel.getValueAt(i, 2);
                    double basePrice = Double.parseDouble(priceStr.replace(",", ""));

                    // Calculate VAT (10%)
                    double vat = basePrice * 0.1;

                    // Get the seat and schedule
                    ChoNgoi seat = choNgoiDAO.getById(seatId);
                    LichTrinhTau schedule = lichTrinhTauDAO.getById(scheduleId);

                    // Generate ticket ID (using date and random number)
                    String ticketId = veTauDAO.generateMaVe();
                    // Create ticket object
                    VeTau ticket = new VeTau();
                    ticket.setMaVe(ticketId);
                    ticket.setTenKhachHang(info.name);
                    ticket.setGiayTo(info.idNumber);
                    ticket.setNgayDi(schedule.getNgayDi());
                    ticket.setDoiTuong(info.passengerType);
                    ticket.setGiaVe(basePrice);
                    ticket.setTrangThai(TrangThaiVeTau.DA_THANH_TOAN);
                    ticket.setLichTrinhTau(schedule);
                    ticket.setChoNgoi(seat);

                    // Set promotion if applied
                    if (promotionComboBox.getSelectedItem() != null) {
                        ticket.setKhuyenMai((KhuyenMai) promotionComboBox.getSelectedItem());
                    }

                    // Save ticket to database
                    if (!veTauDAO.save(ticket)) {
                        throw new Exception("Lỗi khi lưu vé: " + ticketId);
                    }

                    // Add to list for invoice details
                    tickets.add(ticket);
                    ticketPrices.put(ticketId, basePrice);
                    ticketVATs.put(ticketId, vat);
                }

                // 2. Create and save invoice
                // Generate invoice ID
                String invoiceId = hoaDonDAO.generateMaHoaDon(LocalDate.now());

                // Calculate total discount (sum of passenger discounts and promotion discounts)
                double totalDiscount = 0;
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    // Get passenger discount
                    String discountStr = (String) tableModel.getValueAt(i, 4);
                    if (!discountStr.isEmpty()) {
                        try {
                            // Parse discount amount (remove minus sign, percentage, and commas)
                            String amountPart = discountStr.split(" ")[0].replace("-", "").replace(",", "");
                            totalDiscount += Double.parseDouble(amountPart);
                        } catch (Exception e) {
                            System.err.println("Error parsing passenger discount: " + e.getMessage());
                        }
                    }

                    // Get promotion discount
                    String promoDiscountStr = (String) tableModel.getValueAt(i, 5);
                    if (!promoDiscountStr.isEmpty()) {
                        try {
                            // Parse promotion discount (remove minus sign and commas)
                            totalDiscount += Double.parseDouble(promoDiscountStr.replace("-", "").replace(",", ""));
                        } catch (Exception e) {
                            System.err.println("Error parsing promotion discount: " + e.getMessage());
                        }
                    }
                }

                // Create invoice object
                HoaDon invoice = new HoaDon();
                invoice.setMaHD(invoiceId);
                invoice.setNgayLap(java.time.LocalDateTime.now());
                invoice.setTienGiam(totalDiscount);
                invoice.setTongTien(totalAmount);
                invoice.setKhachHang(customer);
                invoice.setNv(nhanVien);

                // Get invoice type (assuming "Vé tàu" is the default type with ID "LHD001")
                try {
                    LoaiHoaDon invoiceType = loaiHoaDonDAO.findById("LHD001");
                    if (invoiceType == null) {
                        throw new Exception("Không tìm thấy loại hóa đơn mặc định");
                    }
                    invoice.setLoaiHoaDon(invoiceType);
                } catch (Exception e) {
                    throw new Exception("Lỗi khi lấy loại hóa đơn: " + e.getMessage());
                }

                // Save invoice to database
                if (!hoaDonDAO.saveHoaDon(invoice)) {
                    throw new Exception("Lỗi khi lưu hóa đơn: " + invoiceId);
                }

                // 3. Create and save invoice details
                for (VeTau ticket : tickets) {
                    // Create invoice detail ID
                    ChiTietHoaDonId detailId = new ChiTietHoaDonId();
                    detailId.setMaHD(invoiceId);
                    detailId.setMaVe(ticket.getMaVe());

                    // Get base price and VAT for this ticket
                    double basePrice = ticketPrices.get(ticket.getMaVe());
                    double vat = ticketVATs.get(ticket.getMaVe());

                    // Create invoice detail object
                    ChiTietHoaDon detail = new ChiTietHoaDon();
                    detail.setId(detailId);
                    detail.setHoaDon(invoice);
                    detail.setVeTau(ticket);
                    detail.setSoLuong(1); // Always 1 for train tickets
                    detail.setVAT(0.1); // 10% VAT
                    detail.setThanhTien(basePrice + vat); // Base price + VAT
                    detail.setTienThue(vat); // VAT amount

                    // Save invoice detail to database
                    if (!chiTietHoaDonDAO.save(detail)) {
                        throw new Exception("Lỗi khi lưu chi tiết hóa đơn cho vé: " + ticket.getMaVe());
                    }
                }

                // Calculate change for cash payment
                String changeMessage = "";
                if (choice == 0) { // Cash payment
                    double amountPaid = Double.parseDouble(amountPaidField.getText().trim().replace(",", ""));
                    double change = amountPaid - totalAmount;
                    if (change > 0) {
                        changeMessage = "\nTiền thối lại: " + formatCurrency(change);
                    }
                }

                // Show success message
                JOptionPane.showMessageDialog(
                        this,
                        "Thanh toán thành công! Vé của bạn đã được đặt." + changeMessage,
                        "Thành công",
                        JOptionPane.INFORMATION_MESSAGE
                );

                // Close checkout window
                dispose();

            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        this,
                        "Lỗi khi xử lý thanh toán: " + e.getMessage(),
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE
                );
                e.printStackTrace();
            }
        }
    }

    /**
     * Create trash icon for buttons
     */
    private ImageIcon createTrashIcon() {
        return new ImageIcon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();

                // Draw simple trash icon
                g2d.setColor(Color.RED);

                // Draw trash can
                g2d.fillRect(x + 3, y + 6, 10, 10);
                g2d.setColor(Color.WHITE);
                g2d.fillRect(x + 5, y + 8, 6, 6);

                // Draw lid
                g2d.setColor(Color.RED);
                g2d.fillRect(x + 1, y + 3, 14, 3);

                g2d.dispose();
            }

            @Override
            public int getIconWidth() {
                return 16;
            }

            @Override
            public int getIconHeight() {
                return 16;
            }
        };
    }

    /**
     * Button renderer for the delete column
     */
    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setBorderPainted(false);
            setBackground(Color.WHITE);
            setIcon(createTrashIcon());
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }

    /**
     * Button editor for the delete column
     */
    class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private int currentRow;
        private boolean isPushed;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.setBorderPainted(false);
            button.setBackground(Color.WHITE);
            button.setIcon(createTrashIcon());

            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            currentRow = row;
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                // Remove the ticket at the specified row
                if (currentRow >= 0 && currentRow < tableModel.getRowCount()) {
                    tableModel.removeRow(currentRow);
                    calculateTotal();
                }
            }
            isPushed = false;
            return "";
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }
}
