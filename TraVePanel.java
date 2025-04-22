package guiClient;

/**
 * @Dự án: PhanTanJavaNhomGPT
 * @Class: TraVePanel
 * @Tạo vào ngày: 19/04/2025
 * @Tác giả: Nguyen Huu Sang
 */

import dao.ChiTietHoaDonDAO;
import dao.HoaDonDAO;
import dao.LichTrinhTauDAO;
import dao.VeTauDAO;
import dao.impl.LichLamViecDAOImpl;
import dao.impl.LichTrinhTauDAOImpl;
import model.*;
//import utils.PrintPDF;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class TraVePanel extends JPanel {
    private VeTauDAO veTauDAO;
    private JTextField txtMaVe;
    private JTextField txtTenKhachHang;
    private JTextField txtGiayTo;
    private JTextField txtNgayDi;
    private JComboBox<String> cboDoiTuong;
    private JButton btnTimKiem;
    private JButton btnTraVe;
    private JButton btnLamMoi;
    private JButton btnThoat;
    private JLabel lblLichTrinh;
    private JLabel lblChoNgoi;
    private JLabel lblTrangThai;
    private JLabel lblGiaVe;
    private JButton btnChonLichTrinh;
    private JButton btnChonChoNgoi;
    private JTextField txtPhiTraVe;
    private JLabel lblTienTraLai;
    private LichTrinhTauDAO lichTrinhTauDAO;
    // Thêm các biến thành viên mới
    private HoaDonDAO hoaDonDAO;
    private ChiTietHoaDonDAO chiTietHoaDonDAO;
    private NhanVien nhanVien; // Đã có từ constructor



    // Màu sắc chính
    private final Color primaryColor = new Color(41, 128, 185);
    private Locale locale;
    private NumberFormat currencyFormatter;
    private JTextField txtDieuKienTraVe;



    // Cập nhật constructor để lưu tham chiếu đến nhanVien
    public TraVePanel(NhanVien nv) {
        this.nhanVien = nv;
        locale = new Locale("vi", "VN");
        currencyFormatter = NumberFormat.getCurrencyInstance(locale);

        // Thiết lập layout và border
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(Color.WHITE);

        // Khởi tạo giao diện
        initializeUI(nv);

        // Kết nối đến RMI server
        connectToServer();

        // Thêm các event listener sau khi giao diện đã được khởi tạo
        addEventListeners(nv);
    }


    private void initializeUI(NhanVien nv) {
        // Panel chính
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(Color.WHITE);

        // Thêm tiêu đề
        JPanel titlePanel = createTitlePanel();
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // Panel chứa nội dung chính
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPanel.setBackground(Color.WHITE);

        // Panel tìm kiếm vé
        JPanel searchPanel = createSearchPanel();
        contentPanel.add(searchPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Panel thông tin vé
        JPanel infoPanel = createInfoPanel();
        contentPanel.add(infoPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Panel nút thao tác
        JPanel buttonPanel = createButtonPanel();
        contentPanel.add(buttonPanel);

        mainPanel.add(contentPanel, BorderLayout.CENTER);
        add(mainPanel);

        // Thêm event listeners
        addEventListeners(nv);
    }

    private JPanel createTitlePanel() {
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(primaryColor);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel titleLabel = new JLabel("QUẢN LÝ TRẢ VÉ TÀU HỎA", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setPreferredSize(new Dimension(0, 30));

        titlePanel.add(titleLabel, BorderLayout.CENTER);

        return titlePanel;
    }

    private JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                "Tìm Kiếm Vé",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14),
                new Color(41, 128, 185)));
        searchPanel.setBackground(Color.WHITE);

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputPanel.setBackground(Color.WHITE);

        JLabel lblMaVe = new JLabel("Mã vé:");
        lblMaVe.setIcon(createTicketIcon(16, 16, primaryColor));
        lblMaVe.setFont(new Font("Arial", Font.BOLD, 12));
        inputPanel.add(lblMaVe);

        txtMaVe = new JTextField(20);
        txtMaVe.setFont(new Font("Arial", Font.PLAIN, 12));
        inputPanel.add(txtMaVe);

        btnTimKiem = new JButton("Tìm Kiếm");
        btnTimKiem.setIcon(createSearchIcon(16, 16));
        btnTimKiem.setFont(new Font("Arial", Font.BOLD, 12));
        btnTimKiem.setBackground(primaryColor);
        btnTimKiem.setForeground(Color.WHITE);
        btnTimKiem.setBorderPainted(false);
        btnTimKiem.setFocusPainted(false);
        inputPanel.add(btnTimKiem);

        searchPanel.add(inputPanel, BorderLayout.CENTER);

        return searchPanel;
    }

    private JPanel createInfoPanel() {
    JPanel infoPanel = new JPanel(new BorderLayout());
    infoPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            "Thông Tin Vé",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14),
            new Color(41, 128, 185)));
    infoPanel.setBackground(Color.WHITE);

    JPanel formPanel = new JPanel(new GridBagLayout());
    formPanel.setBackground(Color.WHITE);

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(8, 5, 8, 5);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.anchor = GridBagConstraints.WEST;

    // Row 1: Tên khách hàng
    gbc.gridx = 0;
    gbc.gridy = 0;
    JLabel lblTenKhachHang = new JLabel("Tên khách hàng:");
    lblTenKhachHang.setFont(new Font("Arial", Font.BOLD, 12));
    formPanel.add(lblTenKhachHang, gbc);

    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.gridwidth = 3;
    txtTenKhachHang = new JTextField(25);
    formPanel.add(txtTenKhachHang, gbc);

    gbc.gridx = 4;
    gbc.gridy = 0;
    gbc.gridwidth = 1;
    JLabel lblGiayTo = new JLabel("Giấy tờ:");
    lblGiayTo.setFont(new Font("Arial", Font.BOLD, 12));
    formPanel.add(lblGiayTo, gbc);

    gbc.gridx = 5;
    gbc.gridy = 0;
    txtGiayTo = new JTextField(15);
    formPanel.add(txtGiayTo, gbc);

    // Row 2: Ngày đi & Đối tượng
    gbc.gridx = 0;
    gbc.gridy = 1;
    JLabel lblNgayDi = new JLabel("Ngày đi:");
    lblNgayDi.setFont(new Font("Arial", Font.BOLD, 12));
    formPanel.add(lblNgayDi, gbc);

    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.gridwidth = 3;
    txtNgayDi = new JTextField(25);
    formPanel.add(txtNgayDi, gbc);

    gbc.gridx = 4;
    gbc.gridy = 1;
    gbc.gridwidth = 1;
    JLabel lblDoiTuong = new JLabel("Đối tượng:");
    lblDoiTuong.setFont(new Font("Arial", Font.BOLD, 12));
    formPanel.add(lblDoiTuong, gbc);

    gbc.gridx = 5;
    gbc.gridy = 1;
    cboDoiTuong = new JComboBox<>(new String[]{"Người lớn", "Trẻ em", "Người cao tuổi"});
    formPanel.add(cboDoiTuong, gbc);

    // Row 3: Lịch trình
    gbc.gridx = 0;
    gbc.gridy = 2;
    JLabel lblLichTrinhText = new JLabel("Lịch trình:");
    lblLichTrinhText.setFont(new Font("Arial", Font.BOLD, 12));
    formPanel.add(lblLichTrinhText, gbc);

    gbc.gridx = 1;
    gbc.gridy = 2;
    lblLichTrinh = new JLabel("Chưa chọn");
    formPanel.add(lblLichTrinh, gbc);

    gbc.gridx = 4;
    gbc.gridy = 2;
    JLabel lblChoNgoiText = new JLabel("Chỗ ngồi:");
    lblChoNgoiText.setFont(new Font("Arial", Font.BOLD, 12));
    formPanel.add(lblChoNgoiText, gbc);

    gbc.gridx = 5;
    gbc.gridy = 2;
    lblChoNgoi = new JLabel("Chưa chọn");
    formPanel.add(lblChoNgoi, gbc);

    // Row 5: Trạng thái & Giá vé
    gbc.gridx = 0;
    gbc.gridy = 4;
    JLabel lblTrangThaiText = new JLabel("Trạng thái:");
    lblTrangThaiText.setFont(new Font("Arial", Font.BOLD, 12));
    formPanel.add(lblTrangThaiText, gbc);

    gbc.gridx = 1;
    gbc.gridy = 4;
    lblTrangThai = new JLabel("---");
    formPanel.add(lblTrangThai, gbc);

    gbc.gridx = 4;
    gbc.gridy = 4;
    JLabel lblGiaVeText = new JLabel("Giá vé:");
    lblGiaVeText.setFont(new Font("Arial", Font.BOLD, 12));
    formPanel.add(lblGiaVeText, gbc);

    gbc.gridx = 5;
    gbc.gridy = 4;
    lblGiaVe = new JLabel("0 VND");
    lblGiaVe.setForeground(new Color(0, 180, 0));
    lblGiaVe.setFont(new Font("Arial", Font.BOLD, 14));
    formPanel.add(lblGiaVe, gbc);

    // Row 6: Phí trả vé
    gbc.gridx = 0;
    gbc.gridy = 6;
    JLabel lblPhiTraVeText = new JLabel("Phí trả vé:");
    lblPhiTraVeText.setFont(new Font("Arial", Font.BOLD, 12));
    formPanel.add(lblPhiTraVeText, gbc);

    gbc.gridx = 1;
    gbc.gridy = 6;
    txtPhiTraVe = new JTextField("0 VND");
    txtPhiTraVe.setEditable(true);
    txtPhiTraVe.setFont(new Font("Arial", Font.PLAIN, 12));
    formPanel.add(txtPhiTraVe, gbc);

    // Row 8: Điều kiện trả vé (THÊM MỚI)
    gbc.gridx = 0;
    gbc.gridy = 5;
    JLabel lblDieuKienTraVeText = new JLabel("Điều kiện trả vé:");
    lblDieuKienTraVeText.setFont(new Font("Arial", Font.BOLD, 12));
    formPanel.add(lblDieuKienTraVeText, gbc);

    gbc.gridx = 1;
    gbc.gridy = 5;
    gbc.gridwidth = 5; // Kéo dài để chiếm toàn bộ hàng ngang
    txtDieuKienTraVe = new JTextField();
    txtDieuKienTraVe.setEditable(false);
    txtDieuKienTraVe.setFont(new Font("Arial", Font.PLAIN, 12));
    txtDieuKienTraVe.setVisible(false); // Ban đầu ẩn đi, chỉ hiển thị khi có vé
    formPanel.add(txtDieuKienTraVe, gbc);

    // Row 7: Tiền trả lại khách
    gbc.gridx = 4;
    gbc.gridy = 6;
    JLabel lblTienTraLaiText = new JLabel("Tiền trả lại:");
    lblTienTraLaiText.setFont(new Font("Arial", Font.BOLD, 12));
    formPanel.add(lblTienTraLaiText, gbc);

    gbc.gridx = 5;
    gbc.gridy = 6;
    lblTienTraLai = new JLabel("0 VND");
    lblTienTraLai.setForeground(new Color(0, 128, 0));
    lblTienTraLai.setFont(new Font("Arial", Font.BOLD, 14));
    formPanel.add(lblTienTraLai, gbc);



    infoPanel.add(formPanel, BorderLayout.CENTER);
    return infoPanel;
}

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(Color.WHITE);

        btnTraVe = new JButton("Trả Vé");
        btnTraVe.setFont(new Font("Arial", Font.BOLD, 12));
        btnTraVe.setBackground(primaryColor);
        btnTraVe.setForeground(Color.WHITE);
        btnTraVe.setBorderPainted(false);
        btnTraVe.setFocusPainted(false);
        btnTraVe.setPreferredSize(new Dimension(120, 35));
        btnTraVe.setIcon(createIconArrow(16, 16, Color.WHITE));
        btnTraVe.setEnabled(false);

        btnLamMoi = new JButton("Làm Mới");
        btnLamMoi.setFont(new Font("Arial", Font.BOLD, 12));
        btnLamMoi.setBackground(new Color(108, 122, 137));
        btnLamMoi.setForeground(Color.WHITE);
        btnLamMoi.setBorderPainted(false);
        btnLamMoi.setFocusPainted(false);
        btnLamMoi.setPreferredSize(new Dimension(120, 35));
        btnLamMoi.setIcon(createIconRefresh(16, 16, Color.WHITE));

        btnThoat = new JButton("Thoát");
        btnThoat.setFont(new Font("Arial", Font.BOLD, 12));
        btnThoat.setBackground(new Color(231, 76, 60));
        btnThoat.setForeground(Color.WHITE);
        btnThoat.setBorderPainted(false);
        btnThoat.setFocusPainted(false);
        btnThoat.setPreferredSize(new Dimension(120, 35));
        btnThoat.setIcon(createIconExit(16, 16, Color.WHITE));

        buttonPanel.add(btnTraVe);
        buttonPanel.add(btnLamMoi);
        buttonPanel.add(btnThoat);

        return buttonPanel;
    }

    private ImageIcon createSearchIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);

        // Vẽ hình tròn của kính lúp
        g2.drawOval(1, 1, width - 8, height - 8);
        g2.drawLine(width - 4, height - 4, width - 8, height - 8);

        g2.dispose();
        return new ImageIcon(image);
    }

    private ImageIcon createIconArrow(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);

        // Vẽ mũi tên trở lại
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(width / 2, 4, 4, height / 2);
        g2.drawLine(4, height / 2, width / 2, height - 4);
        g2.drawLine(4, height / 2, width - 4, height / 2);

        g2.dispose();
        return new ImageIcon(image);
    }

    private ImageIcon createIconRefresh(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);

        // Vẽ biểu tượng refresh
        g2.setStroke(new BasicStroke(2));
        g2.drawArc(2, 2, width - 4, height - 4, 45, 270);
        // Vẽ mũi tên
        int[] xPoints = {width - 4, width - 8, width};
        int[] yPoints = {2, 6, 8};
        g2.fillPolygon(xPoints, yPoints, 3);

        g2.dispose();
        return new ImageIcon(image);
    }

    private ImageIcon createIconExit(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);

        // Vẽ biểu tượng thoát
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(4, 4, width - 4, height - 4);
        g2.drawLine(width - 4, 4, 4, height - 4);

        g2.dispose();
        return new ImageIcon(image);
    }

//    private void connectToServer() {
//        try {
//            Registry registry = LocateRegistry.getRegistry("localhost", 9090);
//            this.veTauDAO = (VeTauDAO) registry.lookup("veTauDAO");
//        } catch (RemoteException | NotBoundException e) {
//            e.printStackTrace();
//            JOptionPane.showMessageDialog(this,
//                    "Lỗi kết nối đến server: " + e.getMessage(),
//                    "Lỗi", JOptionPane.ERROR_MESSAGE);
//        }
//    }

    // Cập nhật phương thức connectToServer
    private void connectToServer() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 9090);
            this.veTauDAO = (VeTauDAO) registry.lookup("veTauDAO");
            this.hoaDonDAO = (HoaDonDAO) registry.lookup("hoaDonDAO");
            this.chiTietHoaDonDAO = (ChiTietHoaDonDAO) registry.lookup("chiTietHoaDonDAO");
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Lỗi kết nối đến server: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addEventListeners(NhanVien nhanVien) {
        btnTimKiem.addActionListener(e -> timVe());

        btnTraVe.addActionListener(e -> traVe());

        btnLamMoi.addActionListener(e -> {
            // Clear all fields
            txtMaVe.setText("");
            txtTenKhachHang.setText("");
            txtGiayTo.setText("");
            txtNgayDi.setText("");
            cboDoiTuong.setSelectedIndex(0);
            lblLichTrinh.setText("Chưa chọn");
            lblChoNgoi.setText("Chưa chọn");
            lblTrangThai.setText("---");
            lblGiaVe.setText("0 VND");
            txtPhiTraVe.setText("0 VND");
            lblTienTraLai.setText("0 VND");
            btnTraVe.setEnabled(false);
        });

        btnThoat.addActionListener(e -> {
            // Exit functionality
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Bạn có chắc muốn thoát không?",
                    "Xác nhận",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                Window window = SwingUtilities.getWindowAncestor(this);
                if (window instanceof JFrame) {
                    window.dispose();
                }
            }
        });

        // Thêm DocumentListener cho txtPhiTraVe
        if (txtPhiTraVe != null) {
            txtPhiTraVe.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                private void updateTienTraLai() {
                    try {
                        // Lấy giá trị phí trả vé từ trường văn bản, loại bỏ các ký tự không phải số
                        String phiTraVeStr = txtPhiTraVe.getText().replaceAll("[^0-9]", "");
                        double phiTraVe = phiTraVeStr.isEmpty() ? 0 : Double.parseDouble(phiTraVeStr);

                        // Lấy giá trị vé từ nhãn, loại bỏ các ký tự không phải số
                        String giaVeStr = lblGiaVe.getText().replaceAll("[^0-9]", "");
                        double giaVe = giaVeStr.isEmpty() ? 0 : Double.parseDouble(giaVeStr);

                        // Tính tiền trả lại theo quy tắc: giá vé - phí trả vé
                        double tienTraLai = giaVe - phiTraVe;

                        // Nếu tiền trả lại âm, đặt về 0
                        if (tienTraLai < 0) tienTraLai = 0;

                        lblTienTraLai.setText(currencyFormatter.format(tienTraLai));
                    } catch (Exception e) {
                        lblTienTraLai.setText("0 VND");
                    }
                }

                @Override
                public void insertUpdate(javax.swing.event.DocumentEvent e) {
                    updateTienTraLai();
                }

                @Override
                public void removeUpdate(javax.swing.event.DocumentEvent e) {
                    updateTienTraLai();
                }

                @Override
                public void changedUpdate(javax.swing.event.DocumentEvent e) {
                    updateTienTraLai();
                }
            });
        }
    }


    private void timVe() {
        try {
            String maVe = txtMaVe.getText().trim();

            if (maVe.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Vui lòng nhập mã vé!",
                        "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            VeTau veTau = veTauDAO.getById(maVe);

            if (veTau == null) {
                JOptionPane.showMessageDialog(this,
                        "Không tìm thấy vé!",
                        "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Hiển thị thông tin vé
            txtTenKhachHang.setText(veTau.getTenKhachHang());
            txtGiayTo.setText(veTau.getGiayTo());
            txtNgayDi.setText(veTau.getNgayDi().toString());

            if (veTau.getLichTrinhTau() != null) {
                lblLichTrinh.setText(veTau.getLichTrinhTau().getMaLich());
            }

            if (veTau.getChoNgoi() != null) {
                lblChoNgoi.setText(veTau.getChoNgoi().getMaCho());
            }

            lblTrangThai.setText(veTau.getTrangThai().toString());
            double giaVe = veTau.getGiaVe();
            lblGiaVe.setText(currencyFormatter.format(giaVe));

            // Kiểm tra và hiển thị điều kiện trả vé
            kiemTraDieuKienTraVe(veTau);

            // Kiểm tra tất cả các điều kiện để kích hoạt nút trả vé
            // 1. Vé phải ở trạng thái DA_THANH_TOAN
            boolean trangThaiHopLe = (veTau.getTrangThai() == TrangThaiVeTau.DA_THANH_TOAN);

            // Khởi tạo biến để lưu số giờ chênh lệch
            long soGioChenhLech = -1;
            boolean dieuKienThoiGianHopLe = false;

            // 2. Nếu là vé đã thanh toán, phải kiểm tra thêm điều kiện thời gian
            if (trangThaiHopLe) {
                // Kiểm tra điều kiện thời gian
                LichTrinhTau lichTrinh = veTau.getLichTrinhTau();
                if (lichTrinh != null && lichTrinh.getNgayDi() != null && lichTrinh.getGioDi() != null) {
                    LocalDateTime thoiGianTauChay = LocalDateTime.of(lichTrinh.getNgayDi(), lichTrinh.getGioDi());
                    LocalDateTime thoiGianHienTai = LocalDateTime.now();

                    // Tính chênh lệch giờ
                    soGioChenhLech = java.time.Duration.between(thoiGianHienTai, thoiGianTauChay).toHours();

                    // Chỉ cho phép trả vé khi còn ít nhất 4 giờ trước khi tàu chạy
                    dieuKienThoiGianHopLe = (soGioChenhLech >= 4);

                    // Tính phí trả vé dựa vào thời gian chênh lệch
                    double phiTraVe = 0;
                    double tienTraLai = 0;

                    if (dieuKienThoiGianHopLe) {
                        // Nếu đủ điều kiện trả vé, tính phí trả vé và tiền trả lại
                        if (soGioChenhLech >= 24) {
                            // Từ 24 giờ trở lên: phí 10%
                            phiTraVe = giaVe * 0.1;
                        } else {
                            // Từ 4 giờ đến dưới 24 giờ: phí 20%
                            phiTraVe = giaVe * 0.2;
                        }

                        // Tính tiền trả lại
                        tienTraLai = giaVe - phiTraVe;

                        // Hiển thị phí trả vé và tiền trả lại
                        txtPhiTraVe.setText(currencyFormatter.format(phiTraVe));
                        lblTienTraLai.setText(currencyFormatter.format(tienTraLai));
                    } else {
                        // Nếu không đủ điều kiện trả vé, không hiển thị phí và tiền trả
                        txtPhiTraVe.setText("Không áp dụng");
                        lblTienTraLai.setText("Không áp dụng");
                    }
                } else {
                    // Nếu không có đủ thông tin lịch trình, không cho phép trả vé
                    dieuKienThoiGianHopLe = false;
                    txtPhiTraVe.setText("Không áp dụng");
                    lblTienTraLai.setText("Không áp dụng");
                }
            } else {
                // Nếu vé không ở trạng thái Đã thanh toán, không hiển thị phí và tiền trả
                txtPhiTraVe.setText("Không áp dụng");
                lblTienTraLai.setText("Không áp dụng");
            }

            // Chỉ cho phép trả vé khi thỏa mãn cả hai điều kiện
            boolean coTheTraVe = trangThaiHopLe && dieuKienThoiGianHopLe;
            btnTraVe.setEnabled(coTheTraVe);

            // Nếu nút trả vé bị vô hiệu hóa, hiển thị tooltip giải thích lý do
            if (!coTheTraVe) {
                if (!trangThaiHopLe) {
                    btnTraVe.setToolTipText("Vé không ở trạng thái cho phép trả (cần là vé Đã thanh toán)");
                } else if (!dieuKienThoiGianHopLe) {
                    btnTraVe.setToolTipText("Không đủ điều kiện thời gian để trả vé (cần ít nhất 4 giờ trước khi tàu chạy)");
                }
            } else {
                // Hiển thị thông tin chi tiết trong tooltip
                if (soGioChenhLech >= 24) {
                    btnTraVe.setToolTipText("Nhấn để trả vé - Phí trả vé: 10% (trên 24 giờ trước khi tàu chạy)");
                } else {
                    btnTraVe.setToolTipText("Nhấn để trả vé - Phí trả vé: 20% (4-24 giờ trước khi tàu chạy)");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi tìm vé: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }


//    private void traVe() {
//        try {
//            String maVe = txtMaVe.getText().trim();
//
//            if (maVe.isEmpty()) {
//                JOptionPane.showMessageDialog(this,
//                        "Vui lòng tìm vé trước khi trả vé!",
//                        "Thông báo", JOptionPane.WARNING_MESSAGE);
//                return;
//            }
//
//            int confirm = JOptionPane.showConfirmDialog(this,
//                    "Bạn có chắc chắn muốn trả vé này không?",
//                    "Xác nhận trả vé",
//                    JOptionPane.YES_NO_OPTION);
//
//            if (confirm != JOptionPane.YES_OPTION) {
//                return;
//            }
//
//            boolean success = veTauDAO.updateStatusToReturned(maVe);
//
//            if (success) {
//                JOptionPane.showMessageDialog(this,
//                        "Trả vé thành công!",
//                        "Thông báo", JOptionPane.INFORMATION_MESSAGE);
//
//                // Refresh thông tin vé
//                VeTau veTau = veTauDAO.getById(maVe);
//                if (veTau != null) {
//                    lblTrangThai.setText(veTau.getTrangThai().toString());
//                    btnTraVe.setEnabled(false);
//                }
//            } else {
//                JOptionPane.showMessageDialog(this,
//                        "Trả vé không thành công!",
//                        "Lỗi", JOptionPane.ERROR_MESSAGE);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            JOptionPane.showMessageDialog(this,
//                    "Lỗi khi trả vé: " + e.getMessage(),
//                    "Lỗi", JOptionPane.ERROR_MESSAGE);
//        }
//    }

//    private void traVe() {
//        try {
//            String maVe = txtMaVe.getText().trim();
//
//            if (maVe.isEmpty()) {
//                JOptionPane.showMessageDialog(this,
//                        "Vui lòng tìm vé trước khi trả vé!",
//                        "Thông báo", JOptionPane.WARNING_MESSAGE);
//                return;
//            }
//
//            int confirm = JOptionPane.showConfirmDialog(this,
//                    "Bạn có chắc chắn muốn trả vé này không?",
//                    "Xác nhận trả vé",
//                    JOptionPane.YES_NO_OPTION);
//
//            if (confirm != JOptionPane.YES_OPTION) {
//                return;
//            }
//
//            // Lấy thông tin vé hiện tại
//            VeTau veTau = veTauDAO.getById(maVe);
//
//            if (veTau == null) {
//                JOptionPane.showMessageDialog(this,
//                        "Không tìm thấy thông tin vé!",
//                        "Lỗi", JOptionPane.ERROR_MESSAGE);
//                return;
//            }
//
//            // Tính phí trả vé và tiền trả lại
//            double giaVe = veTau.getGiaVe();
//            double phiTraVe = tinhPhiTraVe(veTau);
//            double tienTraLai = giaVe - phiTraVe;
//
//            // Thay đổi trạng thái vé thành "Đã trả"
//            boolean success = veTauDAO.updateStatusToReturned(maVe);
//
//            if (!success) {
//                JOptionPane.showMessageDialog(this,
//                        "Trả vé không thành công!",
//                        "Lỗi", JOptionPane.ERROR_MESSAGE);
//                return;
//            }
//
//            // Tạo và lưu hóa đơn trả vé
//            HoaDon hoaDonTraVe = taoHoaDonTraVe(veTau, phiTraVe, tienTraLai);
//
//            if (hoaDonTraVe == null) {
//                JOptionPane.showMessageDialog(this,
//                        "Không thể tạo hóa đơn trả vé!",
//                        "Lỗi", JOptionPane.ERROR_MESSAGE);
//                return;
//            }
//
//            // Hiển thị hóa đơn trả vé
//            hienThiHoaDonTraVe(hoaDonTraVe, veTau, phiTraVe, tienTraLai);
//
//            // Refresh thông tin vé trên giao diện
//            veTau = veTauDAO.getById(maVe);  // Lấy lại vé với trạng thái mới
//            if (veTau != null) {
//                lblTrangThai.setText(veTau.getTrangThai().toString());
//                btnTraVe.setEnabled(false);
//            }
//
//            JOptionPane.showMessageDialog(this,
//                    "Trả vé thành công!",
//                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            JOptionPane.showMessageDialog(this,
//                    "Lỗi khi trả vé: " + e.getMessage(),
//                    "Lỗi", JOptionPane.ERROR_MESSAGE);
//        }
//    }

    private void traVe() {
        try {
            String maVe = txtMaVe.getText().trim();

            if (maVe.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Vui lòng tìm vé trước khi trả vé!",
                        "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc chắn muốn trả vé này không?",
                    "Xác nhận trả vé",
                    JOptionPane.YES_NO_OPTION);

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            // Lấy thông tin vé hiện tại
            VeTau veTau = veTauDAO.getById(maVe);

            if (veTau == null) {
                JOptionPane.showMessageDialog(this,
                        "Không tìm thấy thông tin vé!",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Lấy hóa đơn thanh toán trước đó của vé này
            // Quan trọng: Lấy thông tin hóa đơn cũ trước khi thay đổi trạng thái vé
            KhachHang khachHang = null;
            HoaDon hoaDonCu = null;

            try {
                // Gọi phương thức DAO chuyên biệt để lấy hóa đơn thanh toán của vé
                hoaDonCu = veTauDAO.getHoaDonThanhToanByMaVe(maVe);
                if (hoaDonCu != null) {
                    khachHang = hoaDonCu.getKhachHang();
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Không thể lấy thông tin hóa đơn thanh toán gốc của vé!",
                        "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            }

            if (khachHang == null) {
                // Nếu không tìm được khách hàng từ hóa đơn, thử phương án dự phòng
                try {
                    khachHang = veTauDAO.getKhachHangByMaVe(maVe);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (khachHang == null) {
                    JOptionPane.showMessageDialog(this,
                            "Không thể xác định khách hàng sở hữu vé này!",
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            // Tính phí trả vé và tiền trả lại
            double giaVe = veTau.getGiaVe();
            double phiTraVe = tinhPhiTraVe(veTau);
            double tienTraLai = giaVe - phiTraVe;

            // Thay đổi trạng thái vé thành "Đã trả"
            boolean success = veTauDAO.updateStatusToReturned(maVe);

            if (!success) {
                JOptionPane.showMessageDialog(this,
                        "Trả vé không thành công!",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Tạo và lưu hóa đơn trả vé
//            System.out.println("Hóa đơn cũ");
//            System.out.println(hoaDonCu);
            HoaDon hoaDonTraVe = taoHoaDonTraVe(veTau, khachHang, phiTraVe, tienTraLai);



            if (hoaDonTraVe == null) {
                JOptionPane.showMessageDialog(this,
                        "Không thể tạo hóa đơn trả vé!",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Trong phương thức traVe() trước khi gọi hienThiHoaDonTraVe()
            Map<String, String> thongTinGa = null;
            try {
                thongTinGa = veTauDAO.getThongTinGaByMaVe(veTau.getMaVe());
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Hiển thị hóa đơn trả vé
            hienThiHoaDonTraVe(hoaDonTraVe, veTau, phiTraVe, tienTraLai, thongTinGa);

            // Refresh thông tin vé trên giao diện
            veTau = veTauDAO.getById(maVe);  // Lấy lại vé với trạng thái mới
            if (veTau != null) {
                lblTrangThai.setText(veTau.getTrangThai().toString());
                btnTraVe.setEnabled(false);
            }

            JOptionPane.showMessageDialog(this,
                    "Trả vé thành công!",
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi trả vé: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Phương thức hiển thị hóa đơn trả vé

//    private void hienThiHoaDonTraVe(HoaDon hoaDon, VeTau veTau, double phiTraVe, double tienTraLai) {
//        // Tạo cửa sổ dialog mới để hiển thị hóa đơn
//        JDialog hoaDonDialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Hóa Đơn Trả Vé", true);
//        hoaDonDialog.setSize(600, 650);
//        hoaDonDialog.setLocationRelativeTo(null);
//        hoaDonDialog.setResizable(false);
//
//        // Panel chứa nội dung hóa đơn
//        JPanel contentPanel = new JPanel();
//        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
//        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
//        contentPanel.setBackground(Color.WHITE);
//
//        // Đường kẻ phía trên
//        JPanel topDivider = new JPanel();
//        topDivider.setBackground(Color.WHITE);
//        topDivider.setMaximumSize(new Dimension(Short.MAX_VALUE, 1));
//        topDivider.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));
//        contentPanel.add(topDivider);
//        contentPanel.add(Box.createVerticalStrut(10));
//
//        // Tiêu đề công ty
//        JLabel lblCompany = new JLabel("CÔNG TY CỔ PHẦN VẬN TẢI ĐƯỜNG SẮT LẠC HỒNG", JLabel.CENTER);
//        lblCompany.setFont(new Font("Arial", Font.BOLD, 14));
//        lblCompany.setAlignmentX(Component.CENTER_ALIGNMENT);
//        contentPanel.add(lblCompany);
//        contentPanel.add(Box.createVerticalStrut(5));
//
//        // Tiêu đề hóa đơn
//        JLabel lblTitle = new JLabel("HÓA ĐƠN TRẢ VÉ TÀU", JLabel.CENTER);
//        lblTitle.setFont(new Font("Arial", Font.BOLD, 18));
//        lblTitle.setForeground(primaryColor);
//        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
//        contentPanel.add(lblTitle);
//        contentPanel.add(Box.createVerticalStrut(5));
//
//        // Thời gian xuất hóa đơn
//        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
//        String formattedTime = hoaDon.getNgayLap().format(timeFormatter);
//        JLabel lblTime = new JLabel(formattedTime, JLabel.CENTER);
//        lblTime.setFont(new Font("Arial", Font.PLAIN, 12));
//        lblTime.setAlignmentX(Component.CENTER_ALIGNMENT);
//        contentPanel.add(lblTime);
//        contentPanel.add(Box.createVerticalStrut(10));
//
//        // Thông tin hóa đơn và khách hàng
//        JPanel infoPanel = new JPanel(new GridLayout(0, 1, 5, 5));
//        infoPanel.setBackground(Color.WHITE);
//
//        // Mã hóa đơn
//        JPanel maHDPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
//        maHDPanel.setBackground(Color.WHITE);
//        JLabel lblMaHD = new JLabel("Mã hóa đơn: ");
//        lblMaHD.setFont(new Font("Arial", Font.BOLD, 12));
//        JLabel lblMaHDValue = new JLabel(hoaDon.getMaHD());
//        lblMaHDValue.setFont(new Font("Arial", Font.PLAIN, 12));
//        maHDPanel.add(lblMaHD);
//        maHDPanel.add(lblMaHDValue);
//        infoPanel.add(maHDPanel);
//
//        // Khách hàng
//        JPanel khachHangPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
//        khachHangPanel.setBackground(Color.WHITE);
//        JLabel lblKhachHang = new JLabel("Khách hàng: ");
//        lblKhachHang.setFont(new Font("Arial", Font.BOLD, 12));
//        JLabel lblKhachHangValue = new JLabel(veTau.getTenKhachHang());
//        lblKhachHangValue.setFont(new Font("Arial", Font.PLAIN, 12));
//        khachHangPanel.add(lblKhachHang);
//        khachHangPanel.add(lblKhachHangValue);
//        infoPanel.add(khachHangPanel);
//
//        // Số điện thoại (nếu có từ KhachHang)
//        JPanel sdtPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
//        sdtPanel.setBackground(Color.WHITE);
//        JLabel lblSDT = new JLabel("Số điện thoại: ");
//        lblSDT.setFont(new Font("Arial", Font.BOLD, 12));
//        String soDienThoai = "";
//        if (hoaDon.getKhachHang() != null && hoaDon.getKhachHang().getSoDienThoai() != null) {
//            soDienThoai = hoaDon.getKhachHang().getSoDienThoai();
//        }
//        JLabel lblSDTValue = new JLabel(soDienThoai);
//        lblSDTValue.setFont(new Font("Arial", Font.PLAIN, 12));
//        sdtPanel.add(lblSDT);
//        sdtPanel.add(lblSDTValue);
//        infoPanel.add(sdtPanel);
//
//        // Địa chỉ (nếu có từ KhachHang)
//        JPanel diaChiPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
//        diaChiPanel.setBackground(Color.WHITE);
//        JLabel lblDiaChi = new JLabel("Địa chỉ: ");
//        lblDiaChi.setFont(new Font("Arial", Font.BOLD, 12));
//        String diaChi = "";
//        if (hoaDon.getKhachHang() != null && hoaDon.getKhachHang().getDiaChi() != null) {
//            diaChi = hoaDon.getKhachHang().getDiaChi();
//        }
//        JLabel lblDiaChiValue = new JLabel(diaChi);
//        lblDiaChiValue.setFont(new Font("Arial", Font.PLAIN, 12));
//        diaChiPanel.add(lblDiaChi);
//        diaChiPanel.add(lblDiaChiValue);
//        infoPanel.add(diaChiPanel);
//
//        contentPanel.add(infoPanel);
//        contentPanel.add(Box.createVerticalStrut(10));
//
//        // Đường kẻ giữa thông tin và bảng chi tiết
//        JPanel middleDivider = new JPanel();
//        middleDivider.setBackground(Color.WHITE);
//        middleDivider.setMaximumSize(new Dimension(Short.MAX_VALUE, 1));
//        middleDivider.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Color.BLACK));
//        contentPanel.add(middleDivider);
//        contentPanel.add(Box.createVerticalStrut(10));
//
//        // Bảng chi tiết vé và thanh toán
//        String[] columnNames = {"STT", "Tên hàng hóa, dịch vụ", "Đơn vị tính", "Số lượng", "Đơn giá", "Phí trả vé", "Thanh tiền"};
//
//        // Model cho JTable để hiển thị dữ liệu
//        DefaultTableModel model = new DefaultTableModel() {
//            @Override
//            public boolean isCellEditable(int row, int column) {
//                return false;
//            }
//        };
//
//        for (String columnName : columnNames) {
//            model.addColumn(columnName);
//        }
//
//        // Tạo thông tin vé an toàn không truy cập các đối tượng lazy loading
//        // FIX: Tránh truy cập sâu vào các đối tượng lazy loading
//        String tenDichVu = "Vé tàu";
//
//        // Lấy thông tin mã lịch trình nếu có
//        if (veTau.getLichTrinhTau() != null) {
//            tenDichVu += ", mã lịch: " + veTau.getLichTrinhTau().getMaLich();
//
//            // Thêm thông tin giờ đi nếu có
//            if (veTau.getLichTrinhTau().getGioDi() != null) {
//                tenDichVu += ", " + veTau.getLichTrinhTau().getGioDi().format(DateTimeFormatter.ofPattern("HH:mm"));
//            }
//        }
//
//        // Thêm ngày đi và đối tượng
//        tenDichVu += ", ngày " + veTau.getNgayDi().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
//        tenDichVu += ", " + veTau.getDoiTuong();
//
//        // Thêm thông tin chỗ ngồi nếu có
//        if (veTau.getChoNgoi() != null) {
//            tenDichVu += ", chỗ " + veTau.getChoNgoi().getMaCho();
//        }
//
//        Object[] rowData = {
//                "1",
//                tenDichVu,
//                "Vé",
//                "1",
//                currencyFormatter.format(veTau.getGiaVe()).replace("₫", "").trim(),
//                currencyFormatter.format(phiTraVe).replace("₫", "").trim(),
//                currencyFormatter.format(tienTraLai).replace("₫", "").trim()
//        };
//        model.addRow(rowData);
//
//        // Tạo JTable với model
//        JTable table = new JTable(model);
//        table.setRowHeight(30);
//        table.setFont(new Font("Arial", Font.PLAIN, 12));
//        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
//        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
//
//        // Thiết lập độ rộng cột
//        TableColumnModel columnModel = table.getColumnModel();
//        columnModel.getColumn(0).setPreferredWidth(30);  // STT
//        columnModel.getColumn(1).setPreferredWidth(200); // Tên hàng hóa
//        columnModel.getColumn(2).setPreferredWidth(60);  // Đơn vị tính
//        columnModel.getColumn(3).setPreferredWidth(50);  // Số lượng
//        columnModel.getColumn(4).setPreferredWidth(80);  // Đơn giá
//        columnModel.getColumn(5).setPreferredWidth(80);  // Phí trả vé
//        columnModel.getColumn(6).setPreferredWidth(100); // Thanh tiền
//
//        // Căn giữa nội dung của các cột
//        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
//        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
//        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); // STT
//        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer); // Đơn vị tính
//        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Số lượng
//
//        // Căn phải cho cột tiền
//        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
//        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
//        table.getColumnModel().getColumn(4).setCellRenderer(rightRenderer); // Đơn giá
//        table.getColumnModel().getColumn(5).setCellRenderer(rightRenderer); // Phí trả vé
//        table.getColumnModel().getColumn(6).setCellRenderer(rightRenderer); // Thanh tiền
//
//        // Thêm JScrollPane để có thanh cuộn khi cần
//        JScrollPane scrollPane = new JScrollPane(table);
//        scrollPane.setPreferredSize(new Dimension(550, 70));
//        contentPanel.add(scrollPane);
//        contentPanel.add(Box.createVerticalStrut(10));
//
//        // Panel cho thông tin tổng tiền
//        JPanel totalPanel = new JPanel();
//        totalPanel.setLayout(new BoxLayout(totalPanel, BoxLayout.Y_AXIS));
//        totalPanel.setBackground(Color.WHITE);
//        totalPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
//
//        // Thêm thông tin khuyến mãi
//        JPanel khuyenMaiPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
//        khuyenMaiPanel.setBackground(Color.WHITE);
//        JLabel lblKhuyenMai = new JLabel("Tiền khuyến mãi: ");
//        lblKhuyenMai.setFont(new Font("Arial", Font.BOLD, 12));
//        JLabel lblKhuyenMaiValue = new JLabel("0 VND");
//        lblKhuyenMaiValue.setFont(new Font("Arial", Font.PLAIN, 12));
//        khuyenMaiPanel.add(lblKhuyenMai);
//        khuyenMaiPanel.add(lblKhuyenMaiValue);
//        totalPanel.add(khuyenMaiPanel);
//
//        // Thêm thông tin tổng tiền
//        JPanel tongTienPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
//        tongTienPanel.setBackground(Color.WHITE);
//        JLabel lblTongTien = new JLabel("Tổng tiền (sau giảm giá): ");
//        lblTongTien.setFont(new Font("Arial", Font.BOLD, 14));
//        JLabel lblTongTienValue = new JLabel(currencyFormatter.format(tienTraLai) + " VND");
//        lblTongTienValue.setFont(new Font("Arial", Font.BOLD, 14));
//        lblTongTienValue.setForeground(new Color(0, 128, 0));
//        tongTienPanel.add(lblTongTien);
//        tongTienPanel.add(lblTongTienValue);
//        totalPanel.add(tongTienPanel);
//
//        contentPanel.add(totalPanel);
//        contentPanel.add(Box.createVerticalStrut(20));
//
//        // Ghi chú
//        JLabel lblNote = new JLabel("Ghi chú: Vé đã trả không thể hoàn lại.", JLabel.LEFT);
//        lblNote.setFont(new Font("Arial", Font.ITALIC, 12));
//        contentPanel.add(lblNote);
//        contentPanel.add(Box.createVerticalStrut(20));
//
//        // Đường kẻ phía dưới
//        JPanel bottomDivider = new JPanel();
//        bottomDivider.setBackground(Color.WHITE);
//        bottomDivider.setMaximumSize(new Dimension(Short.MAX_VALUE, 1));
//        bottomDivider.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));
//        contentPanel.add(bottomDivider);
//        contentPanel.add(Box.createVerticalStrut(15));
//
//        // Nút in hóa đơn và đóng
//        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
//        buttonPanel.setBackground(Color.WHITE);
//
//        JButton btnPrint = new JButton("In Hóa Đơn");
//        btnPrint.setFont(new Font("Arial", Font.BOLD, 12));
//        btnPrint.setBackground(primaryColor);
//        btnPrint.setForeground(Color.WHITE);
//        btnPrint.setBorderPainted(false);
//        btnPrint.setFocusPainted(false);
//        btnPrint.setPreferredSize(new Dimension(120, 35));
//
//        btnPrint.addActionListener(e -> {
//            // Chức năng in hóa đơn
//            JOptionPane.showMessageDialog(hoaDonDialog,
//                    "Đang in hóa đơn...",
//                    "Thông báo",
//                    JOptionPane.INFORMATION_MESSAGE);
//            hoaDonDialog.dispose();
//        });
//
//        JButton btnClose = new JButton("Đóng");
//        btnClose.setFont(new Font("Arial", Font.BOLD, 12));
//        btnClose.setBackground(new Color(108, 122, 137));
//        btnClose.setForeground(Color.WHITE);
//        btnClose.setBorderPainted(false);
//        btnClose.setFocusPainted(false);
//        btnClose.setPreferredSize(new Dimension(120, 35));
//
//        btnClose.addActionListener(e -> hoaDonDialog.dispose());
//
//        buttonPanel.add(btnPrint);
//        buttonPanel.add(btnClose);
//        contentPanel.add(buttonPanel);
//
//        // Thiết lập JScrollPane để có thể cuộn nếu nội dung quá dài
//        JScrollPane dialogScrollPane = new JScrollPane(contentPanel);
//        dialogScrollPane.setBorder(BorderFactory.createEmptyBorder());
//        dialogScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
//        dialogScrollPane.getVerticalScrollBar().setUnitIncrement(16);
//
//        // Thêm panel vào dialog
//        hoaDonDialog.getContentPane().add(dialogScrollPane);
//        hoaDonDialog.setVisible(true);
//    }

    private void hienThiHoaDonTraVe(HoaDon hoaDon, VeTau veTau, double phiTraVe, double tienTraLai, Map<String, String> thongTinGa) {
        // Tạo cửa sổ dialog mới để hiển thị hóa đơn
        JDialog hoaDonDialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Hóa Đơn Trả Vé", true);
        hoaDonDialog.setSize(600, 650);
        hoaDonDialog.setLocationRelativeTo(null);
        hoaDonDialog.setResizable(false);

        // Panel chứa nội dung hóa đơn
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPanel.setBackground(Color.WHITE);

        // Đường kẻ phía trên
        JPanel topDivider = new JPanel();
        topDivider.setBackground(Color.WHITE);
        topDivider.setMaximumSize(new Dimension(Short.MAX_VALUE, 1));
        topDivider.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));
        contentPanel.add(topDivider);
        contentPanel.add(Box.createVerticalStrut(10));

        // Tiêu đề công ty
        JLabel lblCompany = new JLabel("CÔNG TY CỔ PHẦN VẬN TẢI ĐƯỜNG SẮT LẠC HỒNG", JLabel.CENTER);
        lblCompany.setFont(new Font("Arial", Font.BOLD, 14));
        lblCompany.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(lblCompany);
        contentPanel.add(Box.createVerticalStrut(5));

        // Tiêu đề hóa đơn
        JLabel lblTitle = new JLabel("HÓA ĐƠN TRẢ VÉ TÀU", JLabel.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 18));
        lblTitle.setForeground(primaryColor);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(lblTitle);
        contentPanel.add(Box.createVerticalStrut(5));

        // Thời gian xuất hóa đơn
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        String formattedTime = hoaDon.getNgayLap().format(timeFormatter);
        JLabel lblTime = new JLabel(formattedTime, JLabel.CENTER);
        lblTime.setFont(new Font("Arial", Font.PLAIN, 12));
        lblTime.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(lblTime);
        contentPanel.add(Box.createVerticalStrut(10));

        // Thông tin hóa đơn và khách hàng
        JPanel infoPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        infoPanel.setBackground(Color.WHITE);

        // Mã hóa đơn
        JPanel maHDPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        maHDPanel.setBackground(Color.WHITE);
        JLabel lblMaHD = new JLabel("Mã hóa đơn: ");
        lblMaHD.setFont(new Font("Arial", Font.BOLD, 12));
        JLabel lblMaHDValue = new JLabel(hoaDon.getMaHD());
        lblMaHDValue.setFont(new Font("Arial", Font.PLAIN, 12));
        maHDPanel.add(lblMaHD);
        maHDPanel.add(lblMaHDValue);
        infoPanel.add(maHDPanel);

        // Khách hàng
        JPanel khachHangPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        khachHangPanel.setBackground(Color.WHITE);
        JLabel lblKhachHang = new JLabel("Khách hàng: ");
        lblKhachHang.setFont(new Font("Arial", Font.BOLD, 12));
        JLabel lblKhachHangValue = new JLabel(veTau.getTenKhachHang());
        lblKhachHangValue.setFont(new Font("Arial", Font.PLAIN, 12));
        khachHangPanel.add(lblKhachHang);
        khachHangPanel.add(lblKhachHangValue);
        infoPanel.add(khachHangPanel);

        // Số điện thoại (nếu có từ KhachHang)
        JPanel sdtPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        sdtPanel.setBackground(Color.WHITE);
        JLabel lblSDT = new JLabel("Số điện thoại: ");
        lblSDT.setFont(new Font("Arial", Font.BOLD, 12));
        String soDienThoai = "";
        if (hoaDon.getKhachHang() != null && hoaDon.getKhachHang().getSoDienThoai() != null) {
            soDienThoai = hoaDon.getKhachHang().getSoDienThoai();
        }
        JLabel lblSDTValue = new JLabel(soDienThoai);
        lblSDTValue.setFont(new Font("Arial", Font.PLAIN, 12));
        sdtPanel.add(lblSDT);
        sdtPanel.add(lblSDTValue);
        infoPanel.add(sdtPanel);

        // Địa chỉ (nếu có từ KhachHang)
        JPanel diaChiPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        diaChiPanel.setBackground(Color.WHITE);
        JLabel lblDiaChi = new JLabel("Địa chỉ: ");
        lblDiaChi.setFont(new Font("Arial", Font.BOLD, 12));
        String diaChi = "";
        if (hoaDon.getKhachHang() != null && hoaDon.getKhachHang().getDiaChi() != null) {
            diaChi = hoaDon.getKhachHang().getDiaChi();
        }
        JLabel lblDiaChiValue = new JLabel(diaChi);
        lblDiaChiValue.setFont(new Font("Arial", Font.PLAIN, 12));
        diaChiPanel.add(lblDiaChi);
        diaChiPanel.add(lblDiaChiValue);
        infoPanel.add(diaChiPanel);

        contentPanel.add(infoPanel);
        contentPanel.add(Box.createVerticalStrut(10));

        // Đường kẻ giữa thông tin và bảng chi tiết
        JPanel middleDivider = new JPanel();
        middleDivider.setBackground(Color.WHITE);
        middleDivider.setMaximumSize(new Dimension(Short.MAX_VALUE, 1));
        middleDivider.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Color.BLACK));
        contentPanel.add(middleDivider);
        contentPanel.add(Box.createVerticalStrut(10));

        // Bảng chi tiết vé và thanh toán
        String[] columnNames = {"STT", "Tên hàng hóa, dịch vụ", "Đơn vị tính", "Số lượng", "Đơn giá", "Phí trả vé", "Thanh tiền"};

        // Model cho JTable để hiển thị dữ liệu
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (String columnName : columnNames) {
            model.addColumn(columnName);
        }

        // Tạo thông tin vé an toàn không truy cập các đối tượng lazy loading
        // FIX: Tránh truy cập sâu vào các đối tượng lazy loading
        String tenDichVu = "<html>Vé tàu";

        // Thêm ga đi và ga đến nếu có
        if (thongTinGa != null) {
            String gaDi = thongTinGa.get("gaDi");
            String gaDen = thongTinGa.get("gaDen");

            if (gaDi != null && gaDen != null) {
                tenDichVu += "<br>Tuyến: " + gaDi + " - " + gaDen;
            }
        }

        // Lấy thông tin mã lịch trình nếu có
        if (veTau.getLichTrinhTau() != null) {
            tenDichVu += "<br>Mã lịch: " + veTau.getLichTrinhTau().getMaLich();

            // Thêm thông tin giờ đi nếu có
            if (veTau.getLichTrinhTau().getGioDi() != null) {
                tenDichVu += "<br>Giờ khởi hành: " + veTau.getLichTrinhTau().getGioDi().format(DateTimeFormatter.ofPattern("HH:mm"));
            }
        }

        // Thêm ngày đi và đối tượng
        tenDichVu += "<br>Ngày đi: " + veTau.getNgayDi().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        tenDichVu += "<br>Đối tượng: " + veTau.getDoiTuong();

        // Thêm thông tin chỗ ngồi nếu có
        if (veTau.getChoNgoi() != null) {
            tenDichVu += "<br>Chỗ ngồi: " + veTau.getChoNgoi().getMaCho();
        }

        tenDichVu += "</html>";

        Object[] rowData = {
                "1",
                tenDichVu,
                "Vé",
                "1",
                currencyFormatter.format(veTau.getGiaVe()).replace("₫", "").trim(),
                currencyFormatter.format(phiTraVe).replace("₫", "").trim(),
                currencyFormatter.format(tienTraLai).replace("₫", "").trim()
        };
        model.addRow(rowData);

        // Tạo JTable với model
        JTable table = new JTable(model);

        // Thiết lập độ cao hàng cho tự động xuống dòng
        table.setRowHeight(100);  // Tăng độ cao mặc định của hàng

        // Sử dụng interface TableCellRenderer tùy chỉnh để hiển thị text xuống dòng
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (c instanceof JLabel) {
                    JLabel l = (JLabel) c;
                    l.setVerticalAlignment(JLabel.TOP); // Căn lề trên cho text
                }
                return c;
            }
        };

        table.setFont(new Font("Arial", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // Thiết lập độ rộng cột
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(30);  // STT
        columnModel.getColumn(1).setPreferredWidth(220); // Tên hàng hóa
        columnModel.getColumn(2).setPreferredWidth(50);  // Đơn vị tính
        columnModel.getColumn(3).setPreferredWidth(50);  // Số lượng
        columnModel.getColumn(4).setPreferredWidth(70);  // Đơn giá
        columnModel.getColumn(5).setPreferredWidth(70);  // Phí trả vé
        columnModel.getColumn(6).setPreferredWidth(80);  // Thanh tiền

        // Căn giữa nội dung của các cột
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setVerticalAlignment(JLabel.TOP); // Căn lề trên khi căn giữa
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); // STT
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer); // Đơn vị tính
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Số lượng

        // Căn phải cho cột tiền
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        rightRenderer.setVerticalAlignment(JLabel.TOP); // Căn lề trên khi căn phải
        table.getColumnModel().getColumn(4).setCellRenderer(rightRenderer); // Đơn giá
        table.getColumnModel().getColumn(5).setCellRenderer(rightRenderer); // Phí trả vé
        table.getColumnModel().getColumn(6).setCellRenderer(rightRenderer); // Thanh tiền

        // Thiết lập renderer cho cột tên dịch vụ
        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(JLabel.LEFT);
        leftRenderer.setVerticalAlignment(JLabel.TOP); // Căn lề trên
        table.getColumnModel().getColumn(1).setCellRenderer(leftRenderer); // Tên hàng hóa

        // Thêm JScrollPane để có thanh cuộn khi cần
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(550, 120)); // Tăng kích thước cho bảng để hiển thị nội dung nhiều dòng
        contentPanel.add(scrollPane);
        contentPanel.add(Box.createVerticalStrut(10));

        // Panel cho thông tin tổng tiền
        JPanel totalPanel = new JPanel();
        totalPanel.setLayout(new BoxLayout(totalPanel, BoxLayout.Y_AXIS));
        totalPanel.setBackground(Color.WHITE);
        totalPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Thêm thông tin khuyến mãi
        JPanel khuyenMaiPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        khuyenMaiPanel.setBackground(Color.WHITE);
        JLabel lblKhuyenMai = new JLabel("Tiền khuyến mãi: ");
        lblKhuyenMai.setFont(new Font("Arial", Font.BOLD, 12));
        JLabel lblKhuyenMaiValue = new JLabel("0 VND");
        lblKhuyenMaiValue.setFont(new Font("Arial", Font.PLAIN, 12));
        khuyenMaiPanel.add(lblKhuyenMai);
        khuyenMaiPanel.add(lblKhuyenMaiValue);
        totalPanel.add(khuyenMaiPanel);

        // Thêm thông tin tổng tiền
        JPanel tongTienPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        tongTienPanel.setBackground(Color.WHITE);
        JLabel lblTongTien = new JLabel("Tổng tiền (sau giảm giá): ");
        lblTongTien.setFont(new Font("Arial", Font.BOLD, 14));
        JLabel lblTongTienValue = new JLabel(currencyFormatter.format(tienTraLai) + " VND");
        lblTongTienValue.setFont(new Font("Arial", Font.BOLD, 14));
        lblTongTienValue.setForeground(new Color(0, 128, 0));
        tongTienPanel.add(lblTongTien);
        tongTienPanel.add(lblTongTienValue);
        totalPanel.add(tongTienPanel);

        contentPanel.add(totalPanel);
        contentPanel.add(Box.createVerticalStrut(20));

        // Ghi chú
        JLabel lblNote = new JLabel("Ghi chú: Vé đã trả không thể hoàn lại.", JLabel.LEFT);
        lblNote.setFont(new Font("Arial", Font.ITALIC, 12));
        contentPanel.add(lblNote);
        contentPanel.add(Box.createVerticalStrut(20));

        // Đường kẻ phía dưới
        JPanel bottomDivider = new JPanel();
        bottomDivider.setBackground(Color.WHITE);
        bottomDivider.setMaximumSize(new Dimension(Short.MAX_VALUE, 1));
        bottomDivider.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));
        contentPanel.add(bottomDivider);
        contentPanel.add(Box.createVerticalStrut(15));

        // Nút in hóa đơn và đóng
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setBackground(Color.WHITE);

        JButton btnPrint = new JButton("In Hóa Đơn");
        btnPrint.setFont(new Font("Arial", Font.BOLD, 12));
        btnPrint.setBackground(primaryColor);
        btnPrint.setForeground(Color.WHITE);
        btnPrint.setBorderPainted(false);
        btnPrint.setFocusPainted(false);
        btnPrint.setPreferredSize(new Dimension(120, 35));

        btnPrint.addActionListener(e -> {
            // Chức năng in hóa đơn
            JOptionPane.showMessageDialog(hoaDonDialog,
                    "Đang in hóa đơn...",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            hoaDonDialog.dispose();
        });

        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(new Font("Arial", Font.BOLD, 12));
        btnClose.setBackground(new Color(108, 122, 137));
        btnClose.setForeground(Color.WHITE);
        btnClose.setBorderPainted(false);
        btnClose.setFocusPainted(false);
        btnClose.setPreferredSize(new Dimension(120, 35));

        btnClose.addActionListener(e -> hoaDonDialog.dispose());

        buttonPanel.add(btnPrint);
        buttonPanel.add(btnClose);
        contentPanel.add(buttonPanel);

        // Thiết lập JScrollPane để có thể cuộn nếu nội dung quá dài
        JScrollPane dialogScrollPane = new JScrollPane(contentPanel);
        dialogScrollPane.setBorder(BorderFactory.createEmptyBorder());
        dialogScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        dialogScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Thêm panel vào dialog
        hoaDonDialog.getContentPane().add(dialogScrollPane);
        hoaDonDialog.setVisible(true);
    }
    // Phương thức hỗ trợ để thêm label vào panel
    private void addLabelToPanel(JPanel panel, String title, String value, int fontStyle) {
        JLabel lblTitle = new JLabel(title, JLabel.LEFT);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 12));

        JLabel lblValue = new JLabel(value, JLabel.RIGHT);
        lblValue.setFont(new Font("Arial", fontStyle, 12));

        panel.add(lblTitle);
        panel.add(lblValue);
    }

    // Phương thức tạo icon vé tàu
    private ImageIcon createTicketIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);

        // Vẽ hình vé
        g2.fillRoundRect(1, 1, width - 2, height - 2, 4, 4);
        g2.setColor(Color.WHITE);
        g2.drawLine(width/4, 1, width/4, height-1);

        g2.dispose();
        return new ImageIcon(image);
    }

//    private HoaDon taoHoaDonTraVe(VeTau veTau, double phiTraVe, double tienTraLai) {
//        try {
//            // Tìm hóa đơn cũ (đã thanh toán trước đó)
//            ChiTietHoaDon chiTietHoaDonCu = null;
//            if (veTau.getChiTietHoaDons() != null && !veTau.getChiTietHoaDons().isEmpty()) {
//                // Lấy hóa đơn thanh toán đầu tiên
//                for (ChiTietHoaDon ct : veTau.getChiTietHoaDons()) {
//                    if (ct.getHoaDon() != null &&
//                            ct.getHoaDon().getLoaiHoaDon() != null &&
//                            "LHD003".equals(ct.getHoaDon().getLoaiHoaDon().getMaLoaiHoaDon())) {
//                        chiTietHoaDonCu = ct;
//                        break;
//                    }
//                }
//            }
//
//            // Nếu không tìm thấy hóa đơn cũ
//            if (chiTietHoaDonCu == null) {
//                throw new RuntimeException("Không tìm thấy hóa đơn thanh toán cho vé này");
//            }
//
//            // Lấy khách hàng từ hóa đơn cũ
//            KhachHang khachHang = chiTietHoaDonCu.getHoaDon().getKhachHang();
//
//            // Tạo hóa đơn mới
//            HoaDon hoaDon = new HoaDon();
//
//            // Sinh mã hóa đơn theo định dạng HD yyyy/MM/dd XXXX
//            String maHoaDon = hoaDonDAO.generateMaHoaDon(LocalDate.now());
//            hoaDon.setMaHD(maHoaDon);
//
//            // Thiết lập các thông tin hóa đơn
//            hoaDon.setNgayLap(LocalDateTime.now());
//            hoaDon.setTienGiam(0); // Không có giảm giá khi trả vé
//            hoaDon.setTongTien(tienTraLai); // Tổng tiền là số tiền trả lại khách
//            hoaDon.setKhachHang(khachHang);
//            hoaDon.setNv(nhanVien); // Nhân viên đang đăng nhập
//
//            // Thiết lập loại hóa đơn là "Đã trả" (LHD002)
//            LoaiHoaDon loaiHoaDon = hoaDonDAO.getLoaiHoaDonById("LHD002");
//            hoaDon.setLoaiHoaDon(loaiHoaDon);
//
//            // Lưu hóa đơn vào cơ sở dữ liệu
//            boolean success = hoaDonDAO.saveHoaDon(hoaDon);
//            if (!success) {
//                throw new RuntimeException("Không thể lưu hóa đơn");
//            }
//
//            // Tạo và lưu chi tiết hóa đơn
//            ChiTietHoaDon chiTietHoaDon = new ChiTietHoaDon();
//            ChiTietHoaDonId chiTietId = new ChiTietHoaDonId();
//            chiTietId.setMaHD(hoaDon.getMaHD());
//            chiTietId.setMaVe(veTau.getMaVe());
//
//            chiTietHoaDon.setId(chiTietId);
//            chiTietHoaDon.setHoaDon(hoaDon);
//            chiTietHoaDon.setVeTau(veTau);
//            chiTietHoaDon.setSoLuong(1);
//            chiTietHoaDon.setVAT(0); // Không tính thuế cho trả vé
//            chiTietHoaDon.setThanhTien(tienTraLai); // Thành tiền là tiền trả lại
//            chiTietHoaDon.setTienThue(0); // Không có thuế
//
//            // Lưu chi tiết hóa đơn vào cơ sở dữ liệu
//            success = chiTietHoaDonDAO.save(chiTietHoaDon);
//            if (!success) {
//                throw new RuntimeException("Không thể lưu chi tiết hóa đơn");
//            }
//
//            return hoaDon;
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new RuntimeException("Lỗi khi tạo hóa đơn trả vé: " + e.getMessage());
//        }
//    }

    // Phương thức tạo và lưu hóa đơn trả vé - Đã sửa lỗi LazyInitializationException
    private HoaDon taoHoaDonTraVe(VeTau veTau, KhachHang khachHang, double phiTraVe, double tienTraLai) {
        try {
            // Kiểm tra thông tin khách hàng
            if (khachHang == null) {
                throw new RuntimeException("Không tìm thấy thông tin khách hàng");
            }

            // Tạo hóa đơn mới
            HoaDon hoaDon = new HoaDon();

            // Sinh mã hóa đơn theo định dạng HD yyyy/MM/dd XXXX
            try {
                String maHoaDon = hoaDonDAO.generateMaHoaDon(LocalDate.now());
                hoaDon.setMaHD(maHoaDon);
            } catch (Exception e) {
                e.printStackTrace();
                // Nếu có lỗi, tạo mã hóa đơn thủ công với timestamp
                String maHoaDon = "HD" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                         System.currentTimeMillis() % 10000;
                hoaDon.setMaHD(maHoaDon);
            }

            // Thiết lập các thông tin hóa đơn
            hoaDon.setNgayLap(LocalDateTime.now());
            hoaDon.setTienGiam(0); // Không có giảm giá khi trả vé
            hoaDon.setTongTien(tienTraLai); // Tổng tiền là số tiền trả lại khách
            hoaDon.setKhachHang(khachHang);
            hoaDon.setNv(nhanVien); // Nhân viên đang đăng nhập

            // Thiết lập loại hóa đơn là "Đã trả" (LHD002)
            try {
                LoaiHoaDon loaiHoaDon = hoaDonDAO.getLoaiHoaDonById("LHD002");
                if (loaiHoaDon == null) {
                    throw new RuntimeException("Không tìm thấy loại hóa đơn LHD002");
                }
                hoaDon.setLoaiHoaDon(loaiHoaDon);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Lỗi khi lấy thông tin loại hóa đơn: " + e.getMessage());
            }
//            System.out.println(veTau);
//            System.out.println(hoaDon);
//            System.out.println(hoaDon.getLoaiHoaDon());
//            System.out.println(hoaDon.getNv());
//            System.out.println(hoaDon.getKhachHang());

            // Lưu hóa đơn vào cơ sở dữ liệu
            boolean success = hoaDonDAO.saveHoaDon(hoaDon);
            System.out.println(success);
            if (!success) {
                throw new RuntimeException("Không thể lưu hóa đơn");
            }

            // Tạo và lưu chi tiết hóa đơn
            ChiTietHoaDon chiTietHoaDon = new ChiTietHoaDon();
            ChiTietHoaDonId chiTietId = new ChiTietHoaDonId();
            chiTietId.setMaHD(hoaDon.getMaHD());
            chiTietId.setMaVe(veTau.getMaVe());

            chiTietHoaDon.setId(chiTietId);
            chiTietHoaDon.setHoaDon(hoaDon);
            chiTietHoaDon.setVeTau(veTau);
            chiTietHoaDon.setSoLuong(1);
            chiTietHoaDon.setVAT(0); // Không tính thuế cho trả vé
            chiTietHoaDon.setThanhTien(tienTraLai); // Thành tiền là tiền trả lại
            chiTietHoaDon.setTienThue(0); // Không có thuế

            // Lưu chi tiết hóa đơn vào cơ sở dữ liệu
            success = chiTietHoaDonDAO.save(chiTietHoaDon);
            if (!success) {
                throw new RuntimeException("Không thể lưu chi tiết hóa đơn");
            }

            return hoaDon;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi tạo hóa đơn trả vé: " + e.getMessage());
        }
    }


    // Phương thức tính phí trả vé dựa trên thời gian
    private double tinhPhiTraVe(VeTau veTau) {
        double giaVe = veTau.getGiaVe();
        double phiTraVe = 0;

        try {
            LichTrinhTau lichTrinh = veTau.getLichTrinhTau();
            if (lichTrinh != null && lichTrinh.getNgayDi() != null && lichTrinh.getGioDi() != null) {
                LocalDateTime thoiGianTauChay = LocalDateTime.of(lichTrinh.getNgayDi(), lichTrinh.getGioDi());
                LocalDateTime thoiGianHienTai = LocalDateTime.now();

                // Tính chênh lệch giờ
                long soGioChenhLech = java.time.Duration.between(thoiGianHienTai, thoiGianTauChay).toHours();

                // Tính phí trả vé dựa vào thời gian chênh lệch
                if (soGioChenhLech >= 24) {
                    // Từ 24 giờ trở lên: phí 10%
                    phiTraVe = giaVe * 0.1;
                } else if (soGioChenhLech >= 4) {
                    // Từ 4 giờ đến dưới 24 giờ: phí 20%
                    phiTraVe = giaVe * 0.2;
                } else {
                    // Dưới 4 giờ: không cho phép trả vé
                    throw new IllegalStateException("Không thể trả vé vì thời gian không đủ điều kiện");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi tính phí trả vé: " + e.getMessage());
        }

        return phiTraVe;
    }


    private double tinhTienTraLai(double giaVe, double phiTraVe) {
        double tienTraLai = giaVe - phiTraVe;
        return tienTraLai > 0 ? tienTraLai : 0;
    }


    /**
     * Kiểm tra và cập nhật điều kiện trả vé dựa trên trạng thái vé và thời gian tàu chạy
     * @param ve Vé tàu cần kiểm tra
     */
    private void kiemTraDieuKienTraVe(VeTau ve) {
        // Ban đầu ẩn trường điều kiện trả vé
        txtDieuKienTraVe.setVisible(false);

        // Nếu không có vé, không hiển thị gì cả
        if (ve == null) {
            return;
        }

        // Hiển thị trường điều kiện trả vé vì đã có vé
        txtDieuKienTraVe.setVisible(true);

        // Kiểm tra trạng thái vé - Chỉ chấp nhận vé "Đã thanh toán"
        if (ve.getTrangThai() != TrangThaiVeTau.DA_THANH_TOAN) {
            txtDieuKienTraVe.setText("Trạng thái vé không đủ điều kiện trả");
            txtDieuKienTraVe.setForeground(new Color(255, 0, 0)); // Màu đỏ cho thông báo lỗi
            return;
        }

        try {
            // Lấy thông tin lịch trình từ vé
            LichTrinhTau lichTrinh = ve.getLichTrinhTau();
            if (lichTrinh == null) {
                txtDieuKienTraVe.setText("Không tìm thấy thông tin lịch trình");
                txtDieuKienTraVe.setForeground(new Color(255, 0, 0));
                return;
            }

            // Lấy ngày và giờ tàu chạy từ lịch trình
            LocalDate ngayDi = lichTrinh.getNgayDi();
            LocalTime gioDi = lichTrinh.getGioDi();

            if (ngayDi == null || gioDi == null) {
                txtDieuKienTraVe.setText("Không có thông tin ngày hoặc giờ tàu chạy");
                txtDieuKienTraVe.setForeground(new Color(255, 0, 0));
                return;
            }

            // Kết hợp ngày và giờ thành LocalDateTime
            LocalDateTime thoiGianTauChay = LocalDateTime.of(ngayDi, gioDi);

            // Lấy thời gian hiện tại
            LocalDateTime thoiGianHienTai = LocalDateTime.now();
            System.out.println(thoiGianHienTai);
            System.out.println(thoiGianTauChay);

            // Tính toán chênh lệch thời gian theo giờ
            long soGioChenhLech = java.time.Duration.between(thoiGianHienTai, thoiGianTauChay).toHours();
            long soPhutChenhLech = java.time.Duration.between(thoiGianHienTai, thoiGianTauChay).toMinutesPart();

            // Kiểm tra điều kiện: nếu thời gian hiện tại > thời gian tàu chạy - 4 tiếng
            if (soGioChenhLech < 4) {
                // Không đủ điều kiện trả vé
                txtDieuKienTraVe.setText("Quá hạn trả vé (cần trước chuyến tàu ít nhất 4 giờ)");
                txtDieuKienTraVe.setForeground(new Color(255, 0, 0));

                // Hiển thị tooltip với thông tin chi tiết
                String infoText = String.format("Còn %d giờ %d phút trước khi tàu chạy - Không đủ điều kiện trả vé",
                        soGioChenhLech, soPhutChenhLech);
                txtDieuKienTraVe.setToolTipText(infoText);
            } else {
                // Đủ điều kiện trả vé - Vé đã thanh toán và thời gian đủ
                txtDieuKienTraVe.setText("Đủ điều kiện trả vé");
                txtDieuKienTraVe.setForeground(new Color(0, 128, 0)); // Màu xanh lá

                // Hiển thị tooltip với thông tin chi tiết
                String phiTraVe;
                if (soGioChenhLech >= 24) {
                    phiTraVe = "10% giá vé";
                } else {
                    phiTraVe = "20% giá vé";
                }

                String infoText = String.format("Còn %d giờ %d phút trước khi tàu chạy - Phí trả vé: %s",
                        soGioChenhLech, soPhutChenhLech, phiTraVe);
                txtDieuKienTraVe.setToolTipText(infoText);
            }
        } catch (Exception e) {
            txtDieuKienTraVe.setText("Lỗi kiểm tra điều kiện: " + e.getMessage());
            txtDieuKienTraVe.setForeground(new Color(255, 0, 0));
            e.printStackTrace();
        }
    }


}