package guiClient;

import dao.ChoNgoiCallback;
import dao.ChoNgoiDoiVeDAO;
import dao.LichTrinhTauDAO;
import dao.ToaTauDoiVeDAO;
import model.ChoNgoi;
import model.KhuyenMai;
import model.LichTrinhTau;
import model.ToaTau;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.rmi.RemoteException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class LichTrinhSelectorDialog extends JDialog {
    private JPanel pnlLichTrinh;
    private JScrollPane scrollPane;
    private JPanel pnlSearch;
    private JTextField txtSearch;
    private JComboBox<String> cboGaDi;
    private JComboBox<String> cboGaDen;
    private JDatePicker datePicker;
    private JButton btnTimKiem;
    private JButton btnHuy;
    private JButton btnXacNhan;

    // Định nghĩa các màu chính
    private Color primaryColor = new Color(41, 128, 185); // Xanh dương
    private Color secondaryColor = new Color(231, 76, 60); // Đỏ cam
    private Color accentColor = new Color(46, 204, 113); // Xanh lá
    private Color lightGray = new Color(245, 245, 245);
    private Color darkText = new Color(52, 73, 94);

    private List<LichTrinhTau> dsLichTrinh;
    private List<LichTrinhTau> dsLichTrinhLoc;
    private LichTrinhTau lichTrinhDaChon;
    private LichTrinhTauDAO lichTrinhDAO;
    private LichTrinhSelectorCallback callback;
    private Locale locale = new Locale("vi", "VN");
    private NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(locale);
    private ToaTauDoiVeDAO toaTauDAO;
    private ChoNgoiDoiVeDAO choNgoiDAO;
    private LocalDate currentDate;
    private LocalTime currentTime;
    // Thêm constructor mới chấp nhận dữ liệu đã preload
    public LichTrinhSelectorDialog(Frame owner, LichTrinhTauDAO lichTrinhDAO,
                                   ToaTauDoiVeDAO toaTauDAO, ChoNgoiDoiVeDAO choNgoiDAO,
                                   LichTrinhSelectorCallback callback) {
        super(owner, "Chọn lịch trình tàu", true);
        this.lichTrinhDAO = lichTrinhDAO;
        this.toaTauDAO = toaTauDAO;
        this.choNgoiDAO = choNgoiDAO;
        this.callback = callback;
        this.dsLichTrinh = new ArrayList<>();
        this.dsLichTrinhLoc = new ArrayList<>();

        // Lưu thời gian hiện tại để so sánh
        this.currentDate = LocalDate.now();
        this.currentTime = LocalTime.now();

        // Đảm bảo datePicker không cho chọn ngày trong quá khứ
        initComponents();

        // Thiết lập ngày bắt đầu cho datePicker không sớm hơn ngày hiện tại
        datePicker.setDate(LocalDate.now());

        // Tải dữ liệu trong thread riêng để không block UI
        loadDataInBackground();
    }

    private void loadFullDataInBackground() {
        SwingWorker<List<LichTrinhTau>, Void> worker = new SwingWorker<List<LichTrinhTau>, Void>() {
            @Override
            protected List<LichTrinhTau> doInBackground() throws Exception {
                // Nếu cần lấy thêm dữ liệu đầy đủ
                return lichTrinhDAO.getAllList();
            }

            @Override
            protected void done() {
                try {
                    List<LichTrinhTau> fullData = get();

                    // Nếu dữ liệu preload không đầy đủ, cập nhật lại
                    if (fullData.size() > dsLichTrinh.size()) {
                        dsLichTrinh = fullData;
                        filterLichTrinh(); // Lọc lại theo các điều kiện hiện tại
                        displayLichTrinh(dsLichTrinhLoc);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        worker.execute();
    }


    private void loadDataInBackground() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                loadLichTrinh(); // Hàm loadLichTrinh đã được cập nhật để chỉ lấy lịch trình tương lai
                loadComboBoxGa();
                return null;
            }

            @Override
            protected void done() {
                try {
                    // Cập nhật UI khi tải xong
                    displayLichTrinh(dsLichTrinhLoc);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(LichTrinhSelectorDialog.this,
                            "Lỗi tải dữ liệu: " + e.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setSize(1000, 600);
        setLocationRelativeTo(getOwner());

        // Thiết lập giao diện với Look and Feel hiện đại
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Panel tìm kiếm
        pnlSearch = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        pnlSearch.setBorder(new EmptyBorder(5, 5, 5, 5));
        pnlSearch.setBackground(lightGray);

        // Tìm kiếm theo từ khóa
        JPanel pnlKeyword = new JPanel(new BorderLayout(5, 0));
        pnlKeyword.setOpaque(false);
        JLabel lblSearch = new JLabel("Tìm kiếm:");
        lblSearch.setFont(new Font("Arial", Font.BOLD, 12));
        pnlKeyword.add(lblSearch, BorderLayout.WEST);

        txtSearch = new JTextField(15);
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterLichTrinh();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterLichTrinh();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterLichTrinh();
            }
        });
        pnlKeyword.add(txtSearch, BorderLayout.CENTER);
        pnlSearch.add(pnlKeyword);

        // Tìm kiếm theo ga đi
        JPanel pnlGaDi = new JPanel(new BorderLayout(5, 0));
        pnlGaDi.setOpaque(false);
        JLabel lblGaDi = new JLabel("Ga đi:");
        lblGaDi.setFont(new Font("Arial", Font.BOLD, 12));
        pnlGaDi.add(lblGaDi, BorderLayout.WEST);

        cboGaDi = new JComboBox<>();
        cboGaDi.setPreferredSize(new Dimension(150, 25));
        cboGaDi.addActionListener(e -> filterLichTrinh());
        pnlGaDi.add(cboGaDi, BorderLayout.CENTER);
        pnlSearch.add(pnlGaDi);

        // Tìm kiếm theo ga đến
        JPanel pnlGaDen = new JPanel(new BorderLayout(5, 0));
        pnlGaDen.setOpaque(false);
        JLabel lblGaDen = new JLabel("Ga đến:");
        lblGaDen.setFont(new Font("Arial", Font.BOLD, 12));
        pnlGaDen.add(lblGaDen, BorderLayout.WEST);

        cboGaDen = new JComboBox<>();
        cboGaDen.setPreferredSize(new Dimension(150, 25));
        cboGaDen.addActionListener(e -> filterLichTrinh());
        pnlGaDen.add(cboGaDen, BorderLayout.CENTER);
        pnlSearch.add(pnlGaDen);

        // Tìm kiếm theo ngày đi
        JPanel pnlNgayDi = new JPanel(new BorderLayout(5, 0));
        pnlNgayDi.setOpaque(false);
        JLabel lblNgayDi = new JLabel("Ngày đi:");
        lblNgayDi.setFont(new Font("Arial", Font.BOLD, 12));
        pnlNgayDi.add(lblNgayDi, BorderLayout.WEST);

        datePicker = new JDatePicker();
        datePicker.setDate(LocalDate.now());
        datePicker.setPreferredSize(new Dimension(120, 25));
        datePicker.addDateChangeListener(e -> filterLichTrinh());
        pnlNgayDi.add(datePicker, BorderLayout.CENTER);
        pnlSearch.add(pnlNgayDi);

        // Nút tìm kiếm
        btnTimKiem = new JButton("Tìm kiếm") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (isEnabled()) {
                    if (getModel().isPressed()) {
                        g2.setColor(primaryColor.darker().darker());
                    } else if (getModel().isRollover()) {
                        g2.setColor(primaryColor.darker());
                    } else {
                        g2.setColor(primaryColor);
                    }
                } else {
                    g2.setColor(new Color(200, 200, 200)); // Màu khi nút bị vô hiệu hóa
                }

                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();

                super.paintComponent(g);
            }
        };
        btnTimKiem.setIcon(createSearchIcon(16, 16, Color.WHITE));
        btnTimKiem.setForeground(Color.WHITE);
        btnTimKiem.setFocusPainted(false);
        btnTimKiem.setContentAreaFilled(false);
        btnTimKiem.setBorderPainted(false);
        btnTimKiem.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnTimKiem.addActionListener(e -> filterLichTrinh());

        pnlSearch.add(btnTimKiem);

        add(pnlSearch, BorderLayout.NORTH);

        // Panel hiển thị lịch trình
        pnlLichTrinh = new JPanel();
        pnlLichTrinh.setLayout(new WrapLayout(FlowLayout.LEFT, 15, 15));
        pnlLichTrinh.setBackground(Color.WHITE);

        scrollPane = new JScrollPane(pnlLichTrinh);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // Panel nút điều khiển
        JPanel pnlControls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlControls.setBackground(Color.WHITE);

        btnXacNhan = new JButton("Xác nhận") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (isEnabled()) {
                    if (getModel().isPressed()) {
                        g2.setColor(accentColor.darker().darker());
                    } else if (getModel().isRollover()) {
                        g2.setColor(accentColor.darker());
                    } else {
                        g2.setColor(accentColor);
                    }
                } else {
                    g2.setColor(new Color(200, 200, 200)); // Màu khi nút bị vô hiệu hóa
                }

                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();

                super.paintComponent(g);
            }
        };

        btnXacNhan.setIcon(createConfirmIcon(16, 16));
        btnXacNhan.setForeground(Color.WHITE);
        btnXacNhan.setEnabled(false);
        btnXacNhan.setFocusPainted(false);
        btnXacNhan.setContentAreaFilled(false);
        btnXacNhan.setBorderPainted(false);
        btnXacNhan.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnXacNhan.addActionListener(e -> xacNhanChonLichTrinh());

        btnHuy = new JButton("Hủy") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2.setColor(secondaryColor.darker().darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(secondaryColor.darker());
                } else {
                    g2.setColor(secondaryColor);
                }

                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();

                super.paintComponent(g);
            }
        };
        btnHuy.setIcon(createCancelIcon(16, 16));
        btnHuy.setForeground(Color.WHITE);
        btnHuy.setFocusPainted(false);
        btnHuy.setContentAreaFilled(false);
        btnHuy.setBorderPainted(false);
        btnHuy.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnHuy.addActionListener(e -> dispose());

        pnlControls.add(btnXacNhan);
        pnlControls.add(Box.createHorizontalStrut(10));
        pnlControls.add(btnHuy);

        // Thêm padding cho panel điều khiển
        pnlControls.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(pnlControls, BorderLayout.SOUTH);

        // Panel thông tin chi tiết
        JPanel pnlDetail = new JPanel();
        pnlDetail.setLayout(new BoxLayout(pnlDetail, BoxLayout.Y_AXIS));
        pnlDetail.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(primaryColor),
                "Thông tin chi tiết",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14),
                primaryColor));
        pnlDetail.setPreferredSize(new Dimension(280, 0));
        pnlDetail.setBackground(Color.WHITE);

        // Thay thế đoạn code tạo pnlInfo
        JPanel pnlInfo = new JPanel();
        pnlInfo.setLayout(new BoxLayout(pnlInfo, BoxLayout.Y_AXIS));
        pnlInfo.setBorder(new EmptyBorder(15, 15, 15, 15));
        pnlInfo.setBackground(Color.WHITE);

        // Tạo các label với style thống nhất
        Font labelHeaderFont = new Font("Arial", Font.BOLD, 12);
        Font labelDataFont = new Font("Arial", Font.PLAIN, 12);

        JLabel lblMaLich = new JLabel("Mã lịch: ");
        lblMaLich.setFont(labelHeaderFont);
        lblMaLich.setHorizontalAlignment(SwingConstants.LEFT);
        lblMaLich.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblNgayDiH = new JLabel("Ngày đi: ");
        lblNgayDiH.setFont(labelHeaderFont);
        lblNgayDiH.setHorizontalAlignment(SwingConstants.LEFT);
        lblNgayDiH.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblGioDi = new JLabel("Giờ đi: ");
        lblGioDi.setFont(labelHeaderFont);
        lblGioDi.setHorizontalAlignment(SwingConstants.LEFT);
        lblGioDi.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTuyen = new JLabel("Tuyến: ");
        lblTuyen.setFont(labelHeaderFont);
        lblTuyen.setHorizontalAlignment(SwingConstants.LEFT);
        lblTuyen.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTau = new JLabel("Tàu: ");
        lblTau.setFont(labelHeaderFont);
        lblTau.setHorizontalAlignment(SwingConstants.LEFT);
        lblTau.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblGiaVeCoBan = new JLabel("Giá vé cơ bản: ");
        lblGiaVeCoBan.setFont(labelHeaderFont);
        lblGiaVeCoBan.setForeground(new Color(231, 76, 60)); // Đỏ cho giá
        lblGiaVeCoBan.setHorizontalAlignment(SwingConstants.LEFT);
        lblGiaVeCoBan.setAlignmentX(Component.LEFT_ALIGNMENT);
        

// Thêm các label vào pnlInfo với căn trái
        JPanel pnlMaLich = createAlignedPanel(lblMaLich);
        JPanel pnlNgayDiH = createAlignedPanel(lblNgayDiH);
        JPanel pnlGioDi = createAlignedPanel(lblGioDi);
        JPanel pnlTuyen = createAlignedPanel(lblTuyen);
        JPanel pnlTau = createAlignedPanel(lblTau);
        JPanel pnlGiaVeCoBan = createAlignedPanel(lblGiaVeCoBan);

        pnlInfo.add(pnlMaLich);
        pnlInfo.add(Box.createVerticalStrut(5));
        pnlInfo.add(pnlNgayDiH);
        pnlInfo.add(Box.createVerticalStrut(5));
        pnlInfo.add(pnlGioDi);
        pnlInfo.add(Box.createVerticalStrut(5));
        pnlInfo.add(pnlTuyen);
        pnlInfo.add(Box.createVerticalStrut(5));
        pnlInfo.add(pnlTau);
        pnlInfo.add(Box.createVerticalStrut(5));
        pnlInfo.add(Box.createVerticalStrut(5));
        pnlInfo.add(pnlGiaVeCoBan);
        pnlInfo.add(Box.createVerticalStrut(10));

        // Phần code cho Bảng giá vé theo loại chỗ trong initComponents()
        JLabel lblBangGiaVe = new JLabel("Bảng giá vé theo loại chỗ:");
        lblBangGiaVe.setFont(labelHeaderFont);
        pnlInfo.add(lblBangGiaVe, BorderLayout.WEST);


        DefaultTableModel modelGiaVe = new DefaultTableModel(
                new Object[][] {},
                new String[] {"Loại chỗ", "Giá vé"}
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable tblGiaVe = new JTable(modelGiaVe);
        tblGiaVe.setRowHeight(25);
        tblGiaVe.setBackground(Color.WHITE);

// Tùy chỉnh header của bảng
        JTableHeader header = tblGiaVe.getTableHeader();
        header.setBackground(primaryColor);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Arial", Font.BOLD, 12));
        header.setReorderingAllowed(false);
        header.setResizingAllowed(true);

// Thiết lập renderer mặc định cho header để đảm bảo màu sắc
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setBackground(primaryColor);
                label.setForeground(Color.WHITE);
                label.setHorizontalAlignment(JLabel.CENTER);
                label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
                label.setOpaque(true);
                return label;
            }
        });

// Đảm bảo header luôn hiển thị cùng với bảng
        UIManager.put("TableHeader.background", primaryColor);
        UIManager.put("TableHeader.foreground", Color.WHITE);

// Căn phải cột giá vé
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer() {
            {
                setHorizontalAlignment(JLabel.RIGHT);
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                if (value instanceof Double) {
                    value = currencyFormatter.format((Double) value);
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        };
        tblGiaVe.getColumnModel().getColumn(1).setCellRenderer(rightRenderer);

// Căn giữa cột loại chỗ
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        tblGiaVe.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

// Đặt kích thước cột
        tblGiaVe.getColumnModel().getColumn(0).setPreferredWidth(120);
        tblGiaVe.getColumnModel().getColumn(1).setPreferredWidth(100);

// Thêm một số dữ liệu mẫu để đảm bảo bảng hiển thị đúng
        modelGiaVe.addRow(new Object[]{"Hạng nhất", 500000.0});
        modelGiaVe.addRow(new Object[]{"Hạng hai", 300000.0});
        modelGiaVe.addRow(new Object[]{"Ghế ngồi", 200000.0});

// Đặt scrollPane với kích thước cố định
        JScrollPane scrollGiaVe = new JScrollPane(tblGiaVe);
        scrollGiaVe.setPreferredSize(new Dimension(230, 150));
        scrollGiaVe.setMinimumSize(new Dimension(230, 150));
        scrollGiaVe.setBackground(Color.WHITE);
        scrollGiaVe.getViewport().setBackground(Color.WHITE);
        scrollGiaVe.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        scrollGiaVe.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollGiaVe.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        pnlInfo.add(scrollGiaVe);

        // Thêm một phần mô tả
        JPanel pnlDescription = new JPanel();
        pnlDescription.setLayout(new BoxLayout(pnlDescription, BoxLayout.Y_AXIS));
        pnlDescription.setBackground(new Color(249, 249, 249));
        pnlDescription.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 230, 230)),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        JLabel lblDescTitle = new JLabel("Hướng dẫn");
        lblDescTitle.setFont(new Font("Arial", Font.BOLD, 14));
        lblDescTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea txtDesc = new JTextArea(
                "1. Chọn một lịch trình từ danh sách bên trái\n" +
                        "2. Xem thông tin chi tiết tại đây\n" +
                        "3. Nhấn 'Xác nhận' để tiếp tục hoặc 'Hủy' để quay lại"
        );
        txtDesc.setFont(new Font("Arial", Font.PLAIN, 12));
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        txtDesc.setEditable(false);
        txtDesc.setBackground(new Color(249, 249, 249));
        txtDesc.setAlignmentX(Component.LEFT_ALIGNMENT);

        pnlDescription.add(lblDescTitle);
        pnlDescription.add(Box.createVerticalStrut(5));
        pnlDescription.add(txtDesc);

        pnlDetail.add(pnlInfo);
        pnlDetail.add(pnlDescription);

        add(pnlDetail, BorderLayout.EAST);
    }

    // Thêm phương thức hỗ trợ này vào class LichTrinhSelectorDialog
    private JPanel createAlignedPanel(JComponent component) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);
        panel.add(component);
        return panel;
    }

    // Tạo label thông tin với định dạng nhất quán
    private JLabel createInfoLabel(String labelText, Font headerFont, Font dataFont) {
        JLabel label = new JLabel(labelText);
        label.setFont(headerFont);
        return label;
    }

    // Tạo label thông tin giá với định dạng nhất quán và màu đặc biệt
    private JLabel createInfoPriceLabel(String labelText, Font headerFont, Font dataFont) {
        JLabel label = new JLabel(labelText);
        label.setFont(headerFont);
        label.setForeground(new Color(231, 76, 60)); // Đỏ cho giá
        return label;
    }

    private void loadLichTrinh() {
        try {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            // Thay vì lấy tất cả lịch trình, chỉ lấy lịch trình từ thời điểm hiện tại trở đi
            LocalDate currentDate = LocalDate.now();

            // Lấy danh sách lịch trình từ server (từ ngày hiện tại đến 30 ngày sau)
            dsLichTrinh = lichTrinhDAO.getListLichTrinhTauByDateRange(
                    currentDate,
                    currentDate.plusDays(30)
            );

            // Lọc thêm theo giờ hiện tại
            LocalTime currentTime = LocalTime.now();
            dsLichTrinh = dsLichTrinh.stream()
                    .filter(lt -> {
                        if (lt.getNgayDi().isAfter(currentDate)) {
                            return true;  // Ngày đi sau ngày hiện tại
                        } else if (lt.getNgayDi().isEqual(currentDate)) {
                            return lt.getGioDi().isAfter(currentTime);  // Ngày đi là ngày hiện tại, kiểm tra giờ đi
                        } else {
                            return false; // Ngày đi trước ngày hiện tại
                        }
                    })
                    .collect(Collectors.toList());

            dsLichTrinhLoc = new ArrayList<>(dsLichTrinh);

            // Lấy danh sách ga để đổ vào combobox
            loadComboBoxGa();

            // Hiển thị danh sách lịch trình
            displayLichTrinh(dsLichTrinhLoc);

        } catch (RemoteException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Không thể tải danh sách lịch trình: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private void loadComboBoxGa() {
        try {
            List<String> dsGa = lichTrinhDAO.getAllStations();

            cboGaDi.addItem("Tất cả");
            cboGaDen.addItem("Tất cả");

            for (String ga : dsGa) {
                cboGaDi.addItem(ga);
                cboGaDen.addItem(ga);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void displayLichTrinh(List<LichTrinhTau> danhSach) {
        pnlLichTrinh.removeAll();

        if (danhSach.isEmpty()) {
            JLabel lblNoData = new JLabel("Không tìm thấy lịch trình phù hợp từ thời điểm hiện tại",
                    createSearchIcon(48, 48, new Color(200, 200, 200)), JLabel.CENTER);
            lblNoData.setFont(new Font("Arial", Font.ITALIC, 16));
            lblNoData.setForeground(Color.GRAY);
            lblNoData.setHorizontalAlignment(SwingConstants.CENTER);
            lblNoData.setVerticalAlignment(SwingConstants.CENTER);
            pnlLichTrinh.add(lblNoData);
        } else {
            for (LichTrinhTau lichTrinh : danhSach) {
                JPanel pnlItem = createLichTrinhPanel(lichTrinh);
                pnlLichTrinh.add(pnlItem);
            }
        }

        pnlLichTrinh.revalidate();
        pnlLichTrinh.repaint();
    }

    private JPanel createLichTrinhPanel(LichTrinhTau lichTrinh) {
        // Panel chính
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setPreferredSize(new Dimension(300, 180));
        panel.setBorder(new LineBorder(lichTrinh.equals(lichTrinhDaChon) ? primaryColor : new Color(220, 220, 220), lichTrinh.equals(lichTrinhDaChon) ? 2 : 1));
        panel.setBackground(Color.WHITE);

        // Panel tiêu đề
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(lichTrinh.equals(lichTrinhDaChon) ? primaryColor.darker() : new Color(240, 240, 240));
        pnlHeader.setBorder(new EmptyBorder(8, 10, 8, 10));

        JLabel lblMaLich = new JLabel("Mã lịch: " + lichTrinh.getMaLich());
        lblMaLich.setFont(new Font("Arial", Font.BOLD, 12));
        lblMaLich.setForeground(lichTrinh.equals(lichTrinhDaChon) ? Color.WHITE : darkText);
        pnlHeader.add(lblMaLich, BorderLayout.WEST);

        JLabel lblNgayDi = new JLabel(lichTrinh.getNgayDi().toString());
        lblNgayDi.setFont(new Font("Arial", Font.PLAIN, 12));
        lblNgayDi.setForeground(lichTrinh.equals(lichTrinhDaChon) ? Color.WHITE : darkText);
        pnlHeader.add(lblNgayDi, BorderLayout.EAST);

        panel.add(pnlHeader, BorderLayout.NORTH);

        // Panel nội dung
        JPanel pnlContent = new JPanel();
        pnlContent.setLayout(new BoxLayout(pnlContent, BoxLayout.Y_AXIS));
        pnlContent.setBackground(Color.WHITE);
        pnlContent.setBorder(new EmptyBorder(15, 15, 10, 15));

        // Tuyến đường
        JPanel pnlTuyen = new JPanel(new BorderLayout(10, 0));
        pnlTuyen.setOpaque(false);

        JLabel lblFrom = new JLabel(lichTrinh.getTau().getTuyenTau().getGaDi());
        lblFrom.setFont(new Font("Arial", Font.BOLD, 14));
        lblFrom.setForeground(darkText);
        pnlTuyen.add(lblFrom, BorderLayout.WEST);

        JLabel lblArrow = new JLabel("→");
        lblArrow.setFont(new Font("Arial", Font.BOLD, 16));
        lblArrow.setHorizontalAlignment(SwingConstants.CENTER);
        lblArrow.setForeground(primaryColor);
        pnlTuyen.add(lblArrow, BorderLayout.CENTER);

        JLabel lblTo = new JLabel(lichTrinh.getTau().getTuyenTau().getGaDen());
        lblTo.setFont(new Font("Arial", Font.BOLD, 14));
        lblTo.setForeground(darkText);
        pnlTuyen.add(lblTo, BorderLayout.EAST);

        pnlContent.add(pnlTuyen);
        pnlContent.add(Box.createVerticalStrut(15));

        // Giờ đi
        JPanel pnlGioDi = new JPanel(new BorderLayout(10, 0));
        pnlGioDi.setOpaque(false);

        JLabel lblTimeIcon = new JLabel(createClockIcon(16, 16, primaryColor));
        pnlGioDi.add(lblTimeIcon, BorderLayout.WEST);

        JLabel lblTime = new JLabel("Giờ đi: " + lichTrinh.getGioDi().format(DateTimeFormatter.ofPattern("HH:mm")));
        lblTime.setFont(new Font("Arial", Font.PLAIN, 13));
        lblTime.setForeground(darkText);
        pnlGioDi.add(lblTime, BorderLayout.CENTER);

        pnlContent.add(pnlGioDi);
        pnlContent.add(Box.createVerticalStrut(8));

        // Thông tin tàu
        JPanel pnlTau = new JPanel(new BorderLayout(10, 0));
        pnlTau.setOpaque(false);

        JLabel lblTrainIcon = new JLabel(createTrainIcon(16, 16, primaryColor));
        pnlTau.add(lblTrainIcon, BorderLayout.WEST);

        JLabel lblTrain = new JLabel("Tàu: " + lichTrinh.getTau().getMaTau());
        lblTrain.setFont(new Font("Arial", Font.PLAIN, 13));
        lblTrain.setForeground(darkText);
        pnlTau.add(lblTrain, BorderLayout.CENTER);

        pnlContent.add(pnlTau);
        pnlContent.add(Box.createVerticalStrut(8));

        // Giá vé cơ bản
        JPanel pnlGiaVe = new JPanel(new BorderLayout(10, 0));
        pnlGiaVe.setOpaque(false);

        JLabel lblPriceIcon = new JLabel(createPriceIcon(16, 16, secondaryColor));
        pnlGiaVe.add(lblPriceIcon, BorderLayout.WEST);

        // Tìm giá vé thấp nhất từ các chỗ ngồi
        double giaVeThapNhat = timGiaVeThapNhat(lichTrinh);
        JLabel lblPrice = new JLabel("Giá từ: " + currencyFormatter.format(giaVeThapNhat));
        lblPrice.setFont(new Font("Arial", Font.BOLD, 13));
        lblPrice.setForeground(secondaryColor);
        pnlGiaVe.add(lblPrice, BorderLayout.CENTER);

        pnlContent.add(pnlGiaVe);

        panel.add(pnlContent, BorderLayout.CENTER);

        // Panel nút chọn
        JPanel pnlAction = new JPanel(new BorderLayout());
        pnlAction.setBackground(new Color(248, 248, 248));
        pnlAction.setBorder(new EmptyBorder(8, 15, 8, 15));

        JButton btnSelect = new JButton("Chọn") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2.setColor(primaryColor.darker().darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(primaryColor.darker());
                } else {
                    g2.setColor(primaryColor);
                }

                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();

                super.paintComponent(g);
            }
        };
        btnSelect.setForeground(Color.WHITE);
        btnSelect.setFont(new Font("Arial", Font.BOLD, 12));
        btnSelect.setBorderPainted(false);
        btnSelect.setContentAreaFilled(false);
        btnSelect.setFocusPainted(false);
        btnSelect.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSelect.setIcon(createSelectIcon(16, 16));

        btnSelect.addActionListener(e -> selectLichTrinh(lichTrinh));

        pnlAction.add(btnSelect, BorderLayout.CENTER);

        panel.add(pnlAction, BorderLayout.SOUTH);

        // Xử lý sự kiện click vào panel
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectLichTrinh(lichTrinh);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!lichTrinh.equals(lichTrinhDaChon)) {
                    panel.setBorder(new LineBorder(primaryColor, 1));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!lichTrinh.equals(lichTrinhDaChon)) {
                    panel.setBorder(new LineBorder(new Color(220, 220, 220), 1));
                }
            }
        });

        return panel;
    }

    /**
     * Tạo icon tìm kiếm
     */
    private ImageIcon createSearchIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);

        // Vẽ hình tròn
        g2.setStroke(new BasicStroke(2));
        g2.drawOval(3, 3, width - 10, height - 10);

        // Vẽ cán của kính lúp
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(width - 5, height - 5, width - 8, height - 8);

        g2.dispose();
        return new ImageIcon(image);
    }

    /**
     * Tạo icon đồng hồ
     */
    private ImageIcon createClockIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);

        // Vẽ đường viền đồng hồ
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(2, 2, width - 4, height - 4);

        // Vẽ kim giờ
        g2.drawLine(width/2, height/2, width/2, height/2 - height/4);

        // Vẽ kim phút
        g2.drawLine(width/2, height/2, width/2 + width/3, height/2);

        g2.dispose();
        return new ImageIcon(image);
    }

    /**
     * Tạo icon tàu
     */
    private ImageIcon createTrainIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);

        // Vẽ thân tàu
        g2.fillRect(4, height/3, width - 8, height/3);

        // Vẽ đầu tàu
        g2.fillRect(width - 6, height/3 - 2, 2, height/3 + 4);

        // Vẽ bánh xe
        g2.fillOval(5, height - 5, 4, 4);
        g2.fillOval(width - 9, height - 5, 4, 4);

        g2.dispose();
        return new ImageIcon(image);
    }

    /**
     * Tạo icon giá tiền
     */
    private ImageIcon createPriceIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);

        // Vẽ ký hiệu đồng VNĐ
        Font priceFont = new Font("SansSerif", Font.BOLD, height - 4);
        g2.setFont(priceFont);
        g2.drawString("₫", 3, height - 4);

        g2.dispose();
        return new ImageIcon(image);
    }

    /**
     * Tạo icon nút chọn
     */
    private ImageIcon createSelectIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Vẽ dấu tích
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(4, height/2, width/2 - 2, height - 5);
        g2.drawLine(width/2 - 2, height - 5, width - 3, 5);

        g2.dispose();
        return new ImageIcon(image);
    }

    /**
     * Tạo icon xác nhận
     */
    private ImageIcon createConfirmIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Vẽ hình tròn
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(1, 1, width - 3, height - 3);

        // Vẽ dấu tích
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(4, height/2, width/2 - 2, height - 6);
        g2.drawLine(width/2 - 2, height - 6, width - 4, 4);

        g2.dispose();
        return new ImageIcon(image);
    }

    /**
     * Tạo icon hủy
     */
    private ImageIcon createCancelIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Vẽ dấu X
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(4, 4, width - 4, height - 4);
        g2.drawLine(4, height - 4, width - 4, 4);

        g2.dispose();
        return new ImageIcon(image);
    }

    /**
     * Tìm giá vé thấp nhất cho một lịch trình từ tất cả các chỗ ngồi
     * @param lichTrinh Lịch trình cần tìm giá
     * @return Giá vé thấp nhất
     */
    private double timGiaVeThapNhat(LichTrinhTau lichTrinh) {
        try {
            List<ToaTau> dsToaTau = toaTauDAO.getToaTauByMaTau(lichTrinh.getTau().getMaTau());
            double giaThapNhat = Double.MAX_VALUE;
            boolean coGia = false;

            for (ToaTau toaTau : dsToaTau) {
                List<ChoNgoi> dsChoNgoi = choNgoiDAO.getChoNgoiByToaTau(toaTau.getMaToa());

                for (ChoNgoi choNgoi : dsChoNgoi) {
                    // Chỉ xét chỗ ngồi có thể sử dụng
                    if (choNgoi.isTinhTrang() && choNgoi.getGiaTien() < giaThapNhat) {
                        giaThapNhat = choNgoi.getGiaTien();
                        coGia = true;
                    }
                }
            }

            return coGia ? giaThapNhat : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private void selectLichTrinh(LichTrinhTau lichTrinh) {
        lichTrinhDaChon = lichTrinh;
        btnXacNhan.setEnabled(true);
        displayLichTrinh(dsLichTrinhLoc);
        hienThiThongTinChiTiet(lichTrinh);
    }

    private void hienThiThongTinChiTiet(LichTrinhTau lichTrinh) {
        try {
            // Lấy panel chi tiết và panel thông tin
            JPanel pnlDetail = (JPanel) getContentPane().getComponent(3);
            JPanel pnlInfo = (JPanel) pnlDetail.getComponent(0);

            // Duyệt qua tất cả các thành phần của pnlInfo để tìm các label
            int index = 0;
            for (Component comp : pnlInfo.getComponents()) {
                // Bỏ qua các component không phải panel hoặc các khoảng cách (Box)
                if (!(comp instanceof JPanel) || comp instanceof Box.Filler) {
                    continue;
                }

                // Xử lý từng JPanel chứa label
                if (comp instanceof JPanel) {
                    JPanel panel = (JPanel) comp;
                    // Lấy label từ panel (thường là component đầu tiên)
                    if (panel.getComponentCount() > 0 && panel.getComponent(0) instanceof JLabel) {
                        JLabel label = (JLabel) panel.getComponent(0);

                        // Cập nhật nội dung label tùy theo vị trí index
                        switch (index) {
                            case 0:  // Mã lịch
                                label.setText("Mã lịch: " + lichTrinh.getMaLich());
                                break;
                            case 1:  // Ngày đi
                                label.setText("Ngày đi: " + lichTrinh.getNgayDi());
                                break;
                            case 2:  // Giờ đi
                                label.setText("Giờ đi: " + lichTrinh.getGioDi().format(DateTimeFormatter.ofPattern("HH:mm")));
                                break;
                            case 3:  // Tuyến
                                label.setText("Tuyến: " + lichTrinh.getTau().getTuyenTau().getGaDi() +
                                        " → " + lichTrinh.getTau().getTuyenTau().getGaDen());
                                break;
                            case 4:  // Tàu
                                label.setText("Tàu: " + lichTrinh.getTau().getMaTau());
                                break;
                            case 6:  // Giá vé cơ bản
                                // Giá vé sẽ được cập nhật sau khi tính toán
                                break;
                        }

                        // Đảm bảo label luôn căn trái
                        if (label != null) {
                            label.setHorizontalAlignment(SwingConstants.LEFT);
                        }

                        index++;
                    }
                }
            }

            // Tìm JScrollPane chứa bảng giá vé
            JScrollPane scrollPane = null;
            JTable table = null;

            for (Component comp : pnlInfo.getComponents()) {
                if (comp instanceof JScrollPane) {
                    scrollPane = (JScrollPane) comp;
                    if (scrollPane.getViewport().getView() instanceof JTable) {
                        table = (JTable) scrollPane.getViewport().getView();
                        break;
                    }
                }
            }

            if (scrollPane == null || table == null) {
                throw new RuntimeException("Không tìm thấy bảng giá vé trong panel thông tin");
            }

            // Đảm bảo header của bảng hiển thị đúng màu
            JTableHeader header = table.getTableHeader();
            header.setBackground(primaryColor);
            header.setForeground(Color.WHITE);

            // Tùy chỉnh header renderer để đảm bảo màu đúng
            header.setDefaultRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                                                               boolean isSelected, boolean hasFocus, int row, int column) {
                    JLabel label = (JLabel) super.getTableCellRendererComponent(
                            table, value, isSelected, hasFocus, row, column);
                    label.setBackground(primaryColor);
                    label.setForeground(Color.WHITE);
                    label.setHorizontalAlignment(JLabel.CENTER);
                    label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
                    label.setOpaque(true);
                    return label;
                }
            });

            // Xóa và cập nhật dữ liệu bảng
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);

            // Lấy danh sách toa tàu và tính giá vé
            List<ToaTau> dsToaTau = toaTauDAO.getToaTauByMaTau(lichTrinh.getTau().getMaTau());
            Map<String, List<Double>> giaVeTheoLoai = new HashMap<>();

            for (ToaTau toaTau : dsToaTau) {
                List<ChoNgoi> dsChoNgoi = choNgoiDAO.getChoNgoiByToaTau(toaTau.getMaToa());

                for (ChoNgoi choNgoi : dsChoNgoi) {
                    if (choNgoi.getLoaiCho() != null) {
                        String tenLoaiCho = choNgoi.getLoaiCho().getTenLoai();
                        double giaTien = choNgoi.getGiaTien();

                        if (!giaVeTheoLoai.containsKey(tenLoaiCho)) {
                            giaVeTheoLoai.put(tenLoaiCho, new ArrayList<>());
                        }

                        giaVeTheoLoai.get(tenLoaiCho).add(giaTien);
                    }
                }
            }

            // Hiển thị giá vé theo loại chỗ
            double giaVeCoBan = Double.MAX_VALUE;
            for (Map.Entry<String, List<Double>> entry : giaVeTheoLoai.entrySet()) {
                String tenLoaiCho = entry.getKey();
                List<Double> dsGiaTien = entry.getValue();

                if (!dsGiaTien.isEmpty()) {
                    double giaThapNhat = Collections.min(dsGiaTien);
                    double giaCaoNhat = Collections.max(dsGiaTien);

                    if (giaThapNhat < giaVeCoBan) {
                        giaVeCoBan = giaThapNhat;
                    }

                    if (Math.abs(giaThapNhat - giaCaoNhat) < 0.01) {
                        model.addRow(new Object[]{tenLoaiCho, giaThapNhat});
                    } else {
                        model.addRow(new Object[]{tenLoaiCho, giaThapNhat + " - " + giaCaoNhat});
                    }
                }
            }

            // Thêm dòng mẫu nếu không có dữ liệu
            if (model.getRowCount() == 0) {
                model.addRow(new Object[]{"Không có dữ liệu", 0.0});
            }

            // Cập nhật giá vé cơ bản
            if (giaVeCoBan == Double.MAX_VALUE) {
                giaVeCoBan = 0;
            }

            // Cập nhật label giá vé
            for (Component comp : pnlInfo.getComponents()) {
                if (comp instanceof JPanel) {
                    JPanel panel = (JPanel) comp;
                    if (panel.getComponentCount() > 0 && panel.getComponent(0) instanceof JLabel) {
                        JLabel label = (JLabel) panel.getComponent(0);
                        if (label.getText().startsWith("Giá vé cơ bản:")) {
                            label.setText("Giá vé cơ bản: " + currencyFormatter.format(giaVeCoBan));
                            break;
                        }
                    }
                }
            }

            // Cập nhật UI
            table.repaint();
            scrollPane.repaint();
            pnlInfo.revalidate();
            pnlInfo.repaint();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Không thể hiển thị thông tin chi tiết: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void filterLichTrinh() {
        String keyword = txtSearch.getText().toLowerCase();
        String gaDi = cboGaDi.getSelectedItem() != null ? cboGaDi.getSelectedItem().toString() : "Tất cả";
        String gaDen = cboGaDen.getSelectedItem() != null ? cboGaDen.getSelectedItem().toString() : "Tất cả";
        LocalDate ngayDi = datePicker.getDate();

        // Cập nhật thời gian hiện tại khi lọc
        LocalDate currentDate = LocalDate.now();
        LocalTime currentTime = LocalTime.now();

        dsLichTrinhLoc = dsLichTrinh.stream()
                .filter(lt -> {
                    // Kiểm tra xem lịch trình có trong tương lai hay không
                    boolean isFuture;
                    if (lt.getNgayDi().isAfter(currentDate)) {
                        isFuture = true;  // Ngày đi sau ngày hiện tại
                    } else if (lt.getNgayDi().isEqual(currentDate)) {
                        isFuture = lt.getGioDi().isAfter(currentTime);  // Ngày đi là ngày hiện tại, kiểm tra giờ đi
                    } else {
                        isFuture = false; // Ngày đi trước ngày hiện tại
                    }

                    // Lọc theo từ khóa
                    boolean matchKeyword = keyword.isEmpty() ||
                            lt.getMaLich().toLowerCase().contains(keyword) ||
                            lt.getTau().getMaTau().toLowerCase().contains(keyword) ||
                            lt.getTau().getTuyenTau().getGaDi().toLowerCase().contains(keyword) ||
                            lt.getTau().getTuyenTau().getGaDen().toLowerCase().contains(keyword);

                    // Lọc theo ga đi
                    boolean matchGaDi = "Tất cả".equals(gaDi) ||
                            lt.getTau().getTuyenTau().getGaDi().equals(gaDi);

                    // Lọc theo ga đến
                    boolean matchGaDen = "Tất cả".equals(gaDen) ||
                            lt.getTau().getTuyenTau().getGaDen().equals(gaDen);

                    // Lọc theo ngày đi
                    boolean matchNgayDi = ngayDi == null ||
                            lt.getNgayDi().equals(ngayDi);

                    return isFuture && matchKeyword && matchGaDi && matchGaDen && matchNgayDi;
                })
                .collect(Collectors.toList());

        displayLichTrinh(dsLichTrinhLoc);
    }

    private void xacNhanChonLichTrinh() {
        if (lichTrinhDaChon == null) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn một lịch trình.",
                    "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (callback != null) {
            callback.onLichTrinhSelected(lichTrinhDaChon);
        }

        dispose();
    }

    /**
     * Tính lại giá vé khi đổi vé
     * @param choNgoi Chỗ ngồi được chọn
     * @param khuyenMai Khuyến mãi áp dụng (có thể null)
     * @param doiTuong Đối tượng khách hàng (để tính giảm giá)
     * @return Giá vé sau khi tính toán
     */
    public double tinhGiaVe(ChoNgoi choNgoi, KhuyenMai khuyenMai, String doiTuong) {
        if (choNgoi == null) {
            return 0;
        }

        // Lấy giá tiền trực tiếp từ chỗ ngồi
        double giaVe = choNgoi.getGiaTien();

        // Áp dụng khuyến mãi nếu có
        if (khuyenMai != null) {
            giaVe *= (1 - khuyenMai.getChietKhau());
        }

        // Áp dụng chiết khấu theo đối tượng
        if (doiTuong != null) {
            switch (doiTuong) {
                case "Trẻ em":
                    giaVe *= 0.5; // Giảm 50% cho trẻ em
                    break;
                case "Người cao tuổi":
                    giaVe *= 0.7; // Giảm 30% cho người cao tuổi
                    break;
                case "Sinh viên":
                    giaVe *= 0.8; // Giảm 20% cho sinh viên
                    break;
                default:
                    // Không giảm cho đối tượng bình thường
                    break;
            }
        }

        return giaVe;
    }

    // Interface callback để thông báo về lịch trình đã chọn
    public interface LichTrinhSelectorCallback {
        void onLichTrinhSelected(LichTrinhTau lichTrinh);
    }

    // Class JDatePicker đơn giản
    private class JDatePicker extends JPanel {
        private JComboBox<Integer> cboDay;
        private JComboBox<String> cboMonth;
        private JComboBox<Integer> cboYear;
        private LocalDate date;
        private List<DateChangeListener> listeners = new ArrayList<>();

        private final String[] MONTHS = {"Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
                "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"};

        public JDatePicker() {
            setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));

            cboDay = new JComboBox<>();
            cboDay.setPreferredSize(new Dimension(45, 25));

            cboMonth = new JComboBox<>(MONTHS);
            cboMonth.setPreferredSize(new Dimension(80, 25));

            cboYear = new JComboBox<>();
            cboYear.setPreferredSize(new Dimension(60, 25));

            // Thêm năm từ hiện tại đến +5 năm
            int currentYear = LocalDate.now().getYear();
            for (int i = 0; i <= 5; i++) {
                cboYear.addItem(currentYear + i);
            }

            // Cập nhật ngày khi tháng hoặc năm thay đổi
            cboMonth.addActionListener(e -> updateDays());
            cboYear.addActionListener(e -> updateDays());

            // Cập nhật ngày khi chọn ngày
            cboDay.addActionListener(e -> updateDate());

            add(cboDay);
            add(cboMonth);
            add(cboYear);

            // Chỉ cho phép chọn từ ngày hiện tại trở đi
            LocalDate currentDate = LocalDate.now();

            // Chỉ cho phép chọn tháng/năm hiện tại và tương lai
            cboYear.addActionListener(e -> {
                if (cboYear.getSelectedItem() != null && cboMonth.getSelectedIndex() != -1) {
                    int selectedYear = (Integer) cboYear.getSelectedItem();
                    int selectedMonth = cboMonth.getSelectedIndex() + 1;

                    // Nếu chọn năm hiện tại, chỉ cho phép chọn tháng hiện tại trở đi
                    if (selectedYear == currentDate.getYear() && selectedMonth < currentDate.getMonthValue()) {
                        cboMonth.setSelectedIndex(currentDate.getMonthValue() - 1);
                    }
                }
            });

            cboMonth.addActionListener(e -> {
                if (cboYear.getSelectedItem() != null && cboMonth.getSelectedIndex() != -1) {
                    int selectedYear = (Integer) cboYear.getSelectedItem();
                    int selectedMonth = cboMonth.getSelectedIndex() + 1;

                    // Nếu chọn năm và tháng hiện tại, chỉ cho phép chọn ngày hiện tại trở đi
                    if (selectedYear == currentDate.getYear() && selectedMonth == currentDate.getMonthValue()) {
                        updateDaysWithMinimum(currentDate.getDayOfMonth());
                    } else {
                        updateDays();
                    }
                }
            });
        }

        private void updateDaysWithMinimum(int minimumDay) {
            if (cboMonth.getSelectedIndex() == -1 || cboYear.getSelectedItem() == null) {
                return;
            }

            int month = cboMonth.getSelectedIndex() + 1;
            int year = (Integer) cboYear.getSelectedItem();
            int day = date != null ? date.getDayOfMonth() : minimumDay;

            int daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth();

            cboDay.removeAllItems();
            for (int i = minimumDay; i <= daysInMonth; i++) {
                cboDay.addItem(i);
            }

            if (day < minimumDay) {
                cboDay.setSelectedItem(minimumDay);
            } else if (day <= daysInMonth) {
                cboDay.setSelectedItem(day);
            } else {
                cboDay.setSelectedItem(daysInMonth);
            }

            updateDate();
        }

        private void updateDays() {
            if (cboMonth.getSelectedIndex() == -1 || cboYear.getSelectedItem() == null) {
                return;
            }

            int month = cboMonth.getSelectedIndex() + 1;
            int year = (Integer) cboYear.getSelectedItem();
            int day = date != null ? date.getDayOfMonth() : 1;

            int daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth();

            cboDay.removeAllItems();
            for (int i = 1; i <= daysInMonth; i++) {
                cboDay.addItem(i);
            }

            if (day <= daysInMonth) {
                cboDay.setSelectedItem(day);
            } else {
                cboDay.setSelectedItem(daysInMonth);
            }

            updateDate();
        }

        private void updateDate() {
            if (cboDay.getSelectedItem() == null || cboMonth.getSelectedIndex() == -1 || cboYear.getSelectedItem() == null) {
                return;
            }

            int day = (Integer) cboDay.getSelectedItem();
            int month = cboMonth.getSelectedIndex() + 1;
            int year = (Integer) cboYear.getSelectedItem();

            date = LocalDate.of(year, month, day);

            // Thông báo cho các listeners
            for (DateChangeListener listener : listeners) {
                listener.dateChanged(date);
            }
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            // Đảm bảo ngày không sớm hơn ngày hiện tại
            LocalDate currentDate = LocalDate.now();
            if (date.isBefore(currentDate)) {
                date = currentDate;
            }

            this.date = date;

            cboYear.setSelectedItem(date.getYear());
            cboMonth.setSelectedIndex(date.getMonthValue() - 1);

            // updateDays sẽ được gọi qua sự kiện của cboMonth
            // và cboDay sẽ được cập nhật sau đó
        }


        public void addDateChangeListener(DateChangeListener listener) {
            listeners.add(listener);
        }

        public interface DateChangeListener {
            void dateChanged(LocalDate newDate);
        }
    }

    // Class WrapLayout để hiển thị các item dạng lưới, tự động xuống dòng
    private class WrapLayout extends FlowLayout {
        private Dimension preferredLayoutSize;

        public WrapLayout() {
            super();
        }

        public WrapLayout(int align) {
            super(align);
        }

        public WrapLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            Dimension minimum = layoutSize(target, false);
            minimum.width -= (getHgap() + 1);
            return minimum;
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getWidth();

                if (targetWidth == 0)
                    targetWidth = Integer.MAX_VALUE;

                int hgap = getHgap();
                int vgap = getVgap();
                Insets insets = target.getInsets();
                int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
                int maxWidth = targetWidth - horizontalInsetsAndGap;

                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0;
                int rowHeight = 0;

                int nmembers = target.getComponentCount();

                for (int i = 0; i < nmembers; i++) {
                    Component m = target.getComponent(i);

                    if (m.isVisible()) {
                        Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();

                        if (rowWidth + d.width > maxWidth) {
                            addRow(dim, rowWidth, rowHeight);
                            rowWidth = 0;
                            rowHeight = 0;
                        }

                        if (rowWidth != 0) {
                            rowWidth += hgap;
                        }

                        rowWidth += d.width;
                        rowHeight = Math.max(rowHeight, d.height);
                    }
                }

                addRow(dim, rowWidth, rowHeight);

                dim.width += horizontalInsetsAndGap;
                dim.height += insets.top + insets.bottom + vgap * 2;

                Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);

                if (scrollPane != null && target.isValid()) {
                    dim.width -= (hgap + 1);
                }

                return dim;
            }
        }

        private void addRow(Dimension dim, int rowWidth, int rowHeight) {
            dim.width = Math.max(dim.width, rowWidth);

            if (dim.height > 0) {
                dim.height += getVgap();
            }

            dim.height += rowHeight;
        }
    }
}