package guiClient;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import dao.*;
import model.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class DoiVePanel extends JPanel {
    // Địa chỉ IP và port của RMI server
    private static final String RMI_SERVER_IP = "127.0.0.1";
    private static final int RMI_SERVER_PORT = 9090;
    // Thêm các biến cho preloading
    private boolean isPreloadingData = false;
    private SwingWorker<Map<String, List<LichTrinhTau>>, Void> preloadWorker;
    private Map<String, List<LichTrinhTau>> cachedLichTrinh = new ConcurrentHashMap<>();

    private DoiVeDAO doiVeDAO;
    private LichTrinhTauDAO lichTrinhTauDAO;
    private ToaTauDoiVeDAO toaTauDAO;
    private ChoNgoiDoiVeDAO choNgoiDAO;
    private LoaiHoaDonDAO loaiHoaDonDAO;
    private HoaDonDAO hoaDonDAO;
    private ChiTietHoaDonDAO chiTietHoaDonDAO;
    // Màu sắc chính
    private Color primaryColor = new Color(41, 128, 185); // Màu xanh dương
    private Color successColor = new Color(46, 204, 113); // Màu xanh lá
    private Color warningColor = new Color(243, 156, 18); // Màu vàng cam
    private Color dangerColor = new Color(231, 76, 60);   // Màu đỏ
    private Color grayColor = new Color(108, 117, 125);   // Màu xám
    private Color darkTextColor = new Color(52, 73, 94);  // Màu chữ tối
    private Color lightBackground = new Color(240, 240, 240); // Màu nền nhạt

    // Components for UI
    private JTextField txtMaVe;
    private JTextField txtTenKhachHang;
    private JTextField txtGiayTo;
    private JTextField txtNgayDi;
    private JComboBox<String> cboDoiTuong;
    private JButton btnTimVe;
    private JButton btnDoiVe;
    private JButton btnLamMoi;
    private JButton btnChonLichTrinh;
    private JButton btnChonChoNgoi;
    private JLabel lblTrangThai;
    private JLabel lblGiaVe;
    private JLabel lblLichTrinh;
    private JLabel lblChoNgoi;
    private JLabel lblStatus;
    private JTable tblLichSu;
    private DefaultTableModel modelLichSu;
    private JProgressBar progressBar;

    // Lưu trữ dữ liệu
    private VeTau veTauHienTai;
    private LichTrinhTau lichTrinhDaChon;
    private ChoNgoi choNgoiDaChon;
    private KhuyenMai khuyenMaiDaChon;
    private NumberFormat currencyFormatter;
    private Locale locale;
    private NhanVien nhanVienPanel;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private Double giaVeBanDau;
    // Constants
    private final String LOADING_TEXT = "Đang tải dữ liệu...";
    private final String READY_TEXT = "Sẵn sàng";
    private final String ERROR_TEXT = "Đã xảy ra lỗi";
    private final String SUCCESS_TEXT = "Thao tác thành công";

    public DoiVePanel(NhanVien nhanVien) {
        this.nhanVienPanel = nhanVien;
        locale = new Locale("vi", "VN");
        currencyFormatter = NumberFormat.getCurrencyInstance(locale);

        // Đảm bảo các nút hiển thị đúng màu sắc
        UIManager.put("Button.background", Color.WHITE);
        UIManager.put("Button.opaque", Boolean.TRUE);

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(Color.WHITE);

        // Khởi tạo giao diện trước
        initializeUI();

        // Thiết lập trạng thái ban đầu
        updateStatus(READY_TEXT, false);

        // Kết nối đến RMI server
        connectToServer();
        startPreloadingData();
    }

    private void connectToServer() {
        try {
            updateStatus(LOADING_TEXT, true);

            Registry registry = LocateRegistry.getRegistry(RMI_SERVER_IP, RMI_SERVER_PORT);
            doiVeDAO = (DoiVeDAO) registry.lookup("doiVeDAO");
            lichTrinhTauDAO = (LichTrinhTauDAO) registry.lookup("lichTrinhTauDAO");
//            khuyenMaiDAO = (KhuyenMaiDAO) registry.lookup("khuyenMaiDAO");
            toaTauDAO = (ToaTauDoiVeDAO) registry.lookup("toaTauDoiVeDAO");
            choNgoiDAO = (ChoNgoiDoiVeDAO) registry.lookup("choNgoiDoiVeDAO");
            loaiHoaDonDAO = (LoaiHoaDonDAO) registry.lookup("loaiHoaDonDAO");
            hoaDonDAO = (HoaDonDAO) registry.lookup("hoaDonDAO");
            chiTietHoaDonDAO = (ChiTietHoaDonDAO) registry.lookup("chiTietHoaDonDAO");
            // Kiểm tra kết nối
            try {
                if (doiVeDAO.testConnection()) {
                    SwingUtilities.invokeLater(() -> {
                        updateStatus(READY_TEXT, false);
                        startPreloadingData();
                    });
                } else {
                    updateStatus(ERROR_TEXT, false);
                }
            } catch (Exception e) {
                updateStatus(ERROR_TEXT, false);
                e.printStackTrace();
            }

        } catch (RemoteException | NotBoundException e) {
            updateStatus(ERROR_TEXT, false);
            JOptionPane.showMessageDialog(this,
                    "Không thể kết nối đến server RMI: " + e.getMessage(),
                    "Lỗi Kết Nối", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void startPreloadingData() {
        if (isPreloadingData || lichTrinhTauDAO == null) {
            return;
        }

        isPreloadingData = true;

        preloadWorker = new SwingWorker<Map<String, List<LichTrinhTau>>, Void>() {
            @Override
            protected Map<String, List<LichTrinhTau>> doInBackground() throws Exception {
                Map<String, List<LichTrinhTau>> result = new ConcurrentHashMap<>();

                try {
                    // Lấy danh sách lịch trình và nhóm theo các tiêu chí phổ biến
                    List<LichTrinhTau> allLichTrinh = lichTrinhTauDAO.getAllList();

                    // Nhóm lịch trình theo ga đi
                    Map<String, List<LichTrinhTau>> lichTrinhByGaDi = new HashMap<>();
                    for (LichTrinhTau lichTrinh : allLichTrinh) {
                        String gaDi = lichTrinh.getTau().getTuyenTau().getGaDi();
                        lichTrinhByGaDi.computeIfAbsent(gaDi, k -> new ArrayList<>()).add(lichTrinh);
                    }
                    result.put("gaDi", new ArrayList<>(lichTrinhByGaDi.values().stream()
                            .flatMap(List::stream)
                            .limit(100) // Giới hạn số lượng để tối ưu bộ nhớ
                            .toList()));

                    // Nhóm lịch trình theo ga đến
                    Map<String, List<LichTrinhTau>> lichTrinhByGaDen = new HashMap<>();
                    for (LichTrinhTau lichTrinh : allLichTrinh) {
                        String gaDen = lichTrinh.getTau().getTuyenTau().getGaDen();
                        lichTrinhByGaDen.computeIfAbsent(gaDen, k -> new ArrayList<>()).add(lichTrinh);
                    }
                    result.put("gaDen", new ArrayList<>(lichTrinhByGaDen.values().stream()
                            .flatMap(List::stream)
                            .limit(100)
                            .toList()));

                    // Lưu toàn bộ danh sách (có giới hạn)
                    result.put("all", allLichTrinh.stream().limit(200).collect(Collectors.toList()));

                    // Preload thông tin toa tàu cho các lịch trình phổ biến (20 lịch trình đầu tiên)
                    for (int i = 0; i < Math.min(20, allLichTrinh.size()); i++) {
                        String maTau = allLichTrinh.get(i).getTau().getMaTau();
                        toaTauDAO.getToaTauByMaTau(maTau); // Kết quả sẽ được cache bởi ToaTauDoiVeDAO
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                return result;
            }

            @Override
            protected void done() {
                try {
                    cachedLichTrinh = get();
                    isPreloadingData = false;
                } catch (Exception e) {
                    e.printStackTrace();
                    isPreloadingData = false;
                }
            }
        };

        preloadWorker.execute();
    }

    private void initializeUI() {
        // Panel chính chia làm hai phần
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(Color.WHITE);

        // Panel bên trái chứa thông tin và thao tác
        JPanel leftPanel = createLeftPanel();

        // Panel bên phải chứa lịch sử đổi vé
        JPanel rightPanel = createRightPanel();

        // Chia đôi màn hình
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(650);
        splitPane.setDividerSize(5);
        splitPane.setOneTouchExpandable(true);
        splitPane.setBackground(Color.WHITE);

        // Thêm splitPane vào panel chính
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // Thêm thanh trạng thái
        JPanel statusPanel = createStatusPanel();
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        // Thêm tiêu đề
        JPanel titlePanel = createTitlePanel();
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createTitlePanel() {
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(primaryColor);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel titleLabel = new JLabel("QUẢN LÝ ĐỔI VÉ TÀU HỎA", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);

        titlePanel.add(titleLabel, BorderLayout.CENTER);

        // Thêm ngày giờ hiện tại vào bên phải
        JLabel dateLabel = new JLabel();
        dateLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        dateLabel.setForeground(Color.WHITE);

        // Cập nhật ngày giờ
        Timer timer = new Timer(1000, e -> {
            Date now = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            dateLabel.setText(sdf.format(now));
        });
        timer.start();

        titlePanel.add(dateLabel, BorderLayout.EAST);

        return titlePanel;
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout(0, 15));
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel tìm kiếm vé
        JPanel searchPanel = createSearchPanel();
        leftPanel.add(searchPanel, BorderLayout.NORTH);

        // Panel thông tin vé
        JPanel infoPanel = createInfoPanel();
        leftPanel.add(infoPanel, BorderLayout.CENTER);

        // Panel nút thao tác
        JPanel buttonPanel = createButtonPanel();
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);

        return leftPanel;
    }

    private JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel(new BorderLayout(15, 0));
        searchPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(primaryColor, 1),
                        "Tìm Kiếm Vé",
                        TitledBorder.LEFT, TitledBorder.TOP,
                        new Font("Arial", Font.BOLD, 14),
                        primaryColor
                ),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        searchPanel.setBackground(Color.WHITE);

        JPanel searchInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        searchInputPanel.setBackground(Color.WHITE);

        // Thêm icon vào label
        JLabel lblMaVe = new JLabel("Mã vé:");
        lblMaVe.setFont(new Font("Arial", Font.BOLD, 12));
        lblMaVe.setIcon(createTicketIcon(16, 16, primaryColor));
        searchInputPanel.add(lblMaVe);

        txtMaVe = new JTextField(15);
        txtMaVe.setFont(new Font("Arial", Font.PLAIN, 12));
        searchInputPanel.add(txtMaVe);

        btnTimVe = new JButton("Tìm Kiếm");
        btnTimVe.setFont(new Font("Arial", Font.BOLD, 12));
        btnTimVe.setBackground(primaryColor);
        btnTimVe.setForeground(Color.WHITE);
        btnTimVe.setOpaque(true);  // Đảm bảo màu nền được hiển thị
        btnTimVe.setBorderPainted(false);  // Không vẽ viền
        btnTimVe.setFocusPainted(false);
        btnTimVe.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnTimVe.setIcon(createSearchIcon(16, 16, Color.WHITE));
        btnTimVe.addActionListener(e -> timVe());

        // Thêm hiệu ứng hover cho nút tìm kiếm
        btnTimVe.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnTimVe.setBackground(primaryColor.darker());
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnTimVe.setBackground(primaryColor);
            }
        });

        searchInputPanel.add(btnTimVe);

        // Thêm nút quét mã QR
        JButton btnQuetQR = new JButton("Quét QR");
        btnQuetQR.setFont(new Font("Arial", Font.BOLD, 12));
        btnQuetQR.setBackground(new Color(0, 153, 153)); // Màu xanh ngọc
        btnQuetQR.setForeground(Color.WHITE);
        btnQuetQR.setOpaque(true);
        btnQuetQR.setBorderPainted(false);
        btnQuetQR.setFocusPainted(false);
        btnQuetQR.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnQuetQR.setIcon(createQRCodeIcon(16, 16, Color.WHITE));
        btnQuetQR.addActionListener(e -> quetQRTuWebcam());

        // Thêm hiệu ứng hover cho nút quét QR
        btnQuetQR.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnQuetQR.setBackground(new Color(0, 130, 130)); // Màu xanh ngọc đậm hơn khi hover
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnQuetQR.setBackground(new Color(0, 153, 153)); // Trở về màu ban đầu
            }
        });

        searchInputPanel.add(btnQuetQR);

        searchPanel.add(searchInputPanel, BorderLayout.CENTER);
        return searchPanel;
    }

    // Thêm phương thức tạo icon mã QR
    private ImageIcon createQRCodeIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(color);

        // Vẽ hình vuông ngoài
        g2d.drawRect(0, 0, width - 1, height - 1);

        // Vẽ các ô vuông nhỏ tượng trưng cho mã QR
        int cellSize = width / 5;

        // Góc trên trái
        g2d.fillRect(2, 2, 3*cellSize, 3*cellSize);
        g2d.setColor(new Color(0, 153, 153)); // Màu nền của nút
        g2d.fillRect(cellSize, cellSize, cellSize, cellSize);
        g2d.setColor(color);

        // Góc trên phải
        g2d.fillRect(width - 3*cellSize - 2, 2, 3*cellSize, 3*cellSize);
        g2d.setColor(new Color(0, 153, 153)); // Màu nền của nút
        g2d.fillRect(width - 2*cellSize - 2, cellSize, cellSize, cellSize);
        g2d.setColor(color);

        // Góc dưới trái
        g2d.fillRect(2, height - 3*cellSize - 2, 3*cellSize, 3*cellSize);
        g2d.setColor(new Color(0, 153, 153)); // Màu nền của nút
        g2d.fillRect(cellSize, height - 2*cellSize - 2, cellSize, cellSize);

        // Một số ô ngẫu nhiên trong mã QR
        g2d.setColor(color);
        g2d.fillRect(width/2, height/2, cellSize, cellSize);
        g2d.fillRect(width/2 - cellSize, height/2 + cellSize, cellSize, cellSize);

        g2d.dispose();

        return new ImageIcon(image);
    }

    // Phương thức quét mã QR
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
                        JDialog qrDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(DoiVePanel.this), "Quét mã QR", true);
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
                                                    txtMaVe.setText(qrText.trim());


                                                    // Thông báo kết quả quét
                                                    JOptionPane.showMessageDialog(DoiVePanel.this,
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
                        qrDialog.setLocationRelativeTo(DoiVePanel.this);
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
                            JOptionPane.showMessageDialog(DoiVePanel.this,
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

    private JPanel createInfoPanel() {
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(primaryColor, 1),
                        "Thông Tin Vé",
                        TitledBorder.LEFT, TitledBorder.TOP,
                        new Font("Arial", Font.BOLD, 14),
                        primaryColor
                ),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        infoPanel.setBackground(Color.WHITE);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 5, 8, 5);

        // Sử dụng bold cho labels
        Font labelFont = new Font("Arial", Font.BOLD, 12);
        Font fieldFont = new Font("Arial", Font.PLAIN, 12);

        // Hàng 1: Tên khách hàng và Giấy tờ
        addFormRow(formPanel, gbc, 0, "Tên khách hàng:", "Giấy tờ:", labelFont);

        txtTenKhachHang = new JTextField(20);
        txtTenKhachHang.setFont(fieldFont);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        formPanel.add(txtTenKhachHang, gbc);

        txtGiayTo = new JTextField(15);
        txtGiayTo.setFont(fieldFont);
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        formPanel.add(txtGiayTo, gbc);

        // Hàng 2: Ngày đi và Đối tượng
        addFormRow(formPanel, gbc, 1, "Ngày đi:", "Đối tượng:", labelFont);

        txtNgayDi = new JTextField(10);
        txtNgayDi.setFont(fieldFont);
        gbc.gridx = 1;
        gbc.gridy = 1;
        formPanel.add(txtNgayDi, gbc);

        String[] doiTuong = {"Người lớn", "Trẻ em", "Người cao tuổi", "Sinh viên"};
        cboDoiTuong = new JComboBox<>(doiTuong);
        cboDoiTuong.setFont(fieldFont);
        cboDoiTuong.addActionListener(e -> capNhatGiaVe());
        gbc.gridx = 3;
        gbc.gridy = 1;
        formPanel.add(cboDoiTuong, gbc);

        // Hàng 3: Lịch trình
        addFormRow(formPanel, gbc, 2, "Lịch trình:", "", labelFont);

        JPanel pnlLichTrinh = new JPanel(new BorderLayout(5, 0));
        pnlLichTrinh.setOpaque(false);

        lblLichTrinh = new JLabel("Chưa chọn");
        lblLichTrinh.setFont(fieldFont);
        pnlLichTrinh.add(lblLichTrinh, BorderLayout.CENTER);


        // Tạo JButton tùy chỉnh cho lịch trình
        btnChonLichTrinh = new JButton("Chọn") {
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

        btnChonLichTrinh.setFont(new Font("Arial", Font.PLAIN, 11));
        btnChonLichTrinh.setForeground(Color.WHITE);
        btnChonLichTrinh.setBorderPainted(false);
        btnChonLichTrinh.setContentAreaFilled(false);
        btnChonLichTrinh.setFocusPainted(false);
        btnChonLichTrinh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnChonLichTrinh.setIcon(createCalendarIcon(12, 12, Color.WHITE));
        btnChonLichTrinh.addActionListener(e -> hienThiDialogChonLichTrinh());

        pnlLichTrinh.add(btnChonLichTrinh, BorderLayout.EAST);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        formPanel.add(pnlLichTrinh, gbc);

        // Hàng 4: Chỗ ngồi
        addFormRow(formPanel, gbc, 3, "Chỗ ngồi:", "", labelFont);

        JPanel pnlChoNgoi = new JPanel(new BorderLayout(5, 0));
        pnlChoNgoi.setOpaque(false);

        lblChoNgoi = new JLabel("Chưa chọn");
        lblChoNgoi.setFont(fieldFont);
        pnlChoNgoi.add(lblChoNgoi, BorderLayout.CENTER);

        // Tạo JButton tùy chỉnh cho chỗ ngồi
        btnChonChoNgoi = new JButton("Chọn") {
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

        btnChonChoNgoi.setFont(new Font("Arial", Font.PLAIN, 11));
        btnChonChoNgoi.setForeground(Color.WHITE);
        btnChonChoNgoi.setBorderPainted(false);
        btnChonChoNgoi.setContentAreaFilled(false);
        btnChonChoNgoi.setFocusPainted(false);
        btnChonChoNgoi.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnChonChoNgoi.setIcon(createSeatIcon(12, 12, Color.WHITE));
        btnChonChoNgoi.addActionListener(e -> hienThiDialogChonChoNgoi());

        pnlChoNgoi.add(btnChonChoNgoi, BorderLayout.EAST);

        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        formPanel.add(pnlChoNgoi, gbc);

        // Hàng 5: Trạng thái và Giá vé
        addFormRow(formPanel, gbc, 4, "Trạng thái:", "Giá vé:", labelFont);

        lblTrangThai = new JLabel("---");
        lblTrangThai.setFont(new Font("Arial", Font.BOLD, 12));
        lblTrangThai.setForeground(warningColor);
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        formPanel.add(lblTrangThai, gbc);

        lblGiaVe = new JLabel("0 VNĐ");
        lblGiaVe.setFont(new Font("Arial", Font.BOLD, 12));
        lblGiaVe.setForeground(successColor);
        gbc.gridx = 3;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        formPanel.add(lblGiaVe, gbc);

        infoPanel.add(formPanel, BorderLayout.CENTER);

        return infoPanel;
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label1, String label2, Font font) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;

        JLabel lbl1 = new JLabel(label1);
        lbl1.setFont(font);
        panel.add(lbl1, gbc);

        if (!label2.isEmpty()) {
            gbc.gridx = 2;
            JLabel lbl2 = new JLabel(label2);
            lbl2.setFont(font);
            panel.add(lbl2, gbc);
        }
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(Color.WHITE);

        btnDoiVe = new JButton("Đổi Vé");
        styleButton(btnDoiVe, primaryColor, Color.WHITE, createExchangeIcon(16, 16, Color.WHITE));
        btnDoiVe.addActionListener(e -> doiVe());

        btnLamMoi = new JButton("Làm Mới");
        styleButton(btnLamMoi, grayColor, Color.WHITE, createRefreshIcon(16, 16, Color.WHITE));
        btnLamMoi.addActionListener(e -> lamMoi());

        JButton btnThoat = new JButton("Thoát");
        styleButton(btnThoat, dangerColor, Color.WHITE, createExitIcon(16, 16, Color.WHITE));
        btnThoat.addActionListener(e -> {
            int response = JOptionPane.showConfirmDialog(
                    this, "Bạn có chắc chắn muốn thoát khỏi chức năng này?",
                    "Xác nhận thoát", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE
            );

            if (response == JOptionPane.YES_OPTION) {
                // Quay lại màn hình chính
                Container parent = this.getParent();
                if (parent != null) {
                    ((CardLayout) parent.getLayout()).show(parent, "Trang chủ");
                }
            }
        });

        // Vô hiệu hóa các trường thông tin và nút ban đầu
        setInputFieldsEnabled(false);
        btnDoiVe.setEnabled(false);
        btnChonLichTrinh.setEnabled(false);
        btnChonChoNgoi.setEnabled(false);

        buttonPanel.add(btnDoiVe);
        buttonPanel.add(btnLamMoi);
        buttonPanel.add(btnThoat);

        return buttonPanel;
    }

    private void styleButton(JButton button, Color bgColor, Color fgColor, Icon icon) {
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setOpaque(true);     // Đảm bảo màu nền được hiển thị
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setIcon(icon);

        // Hiệu ứng hover
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(bgColor.darker());
                }
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(bgColor);
                }
            }
        });
    }

    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(primaryColor, 1),
                        "Lịch Sử Đổi Vé",
                        TitledBorder.LEFT, TitledBorder.TOP,
                        new Font("Arial", Font.BOLD, 14),
                        primaryColor
                ),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        // Tạo model cho bảng lịch sử - FIXED: Added Trạng Thái Cũ and Trạng Thái Mới columns
        String[] columnNames = {"Mã Vé", "Ngày Đổi", "Trạng Thái Cũ", "Trạng Thái Mới"};
        modelLichSu = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Không cho phép chỉnh sửa ô
            }
        };

        tblLichSu = new JTable(modelLichSu);
        customizeTable(tblLichSu);

        // Tùy chỉnh renderer cho cột trạng thái thanh toán
        // FIXED: Changed index from 4 to 3 (Trạng Thái Mới column)
        tblLichSu.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                JLabel label = (JLabel) c;

                if (value != null) {
                    TrangThaiVeTau trangThai = (TrangThaiVeTau) value;
                    switch (trangThai) {
                        case DA_THANH_TOAN:
                            label.setForeground(successColor);
                            label.setIcon(createPaymentIcon(14, 14, successColor));
                            break;
                        case CHO_XAC_NHAN:
                            label.setForeground(warningColor);
                            label.setIcon(createPendingIcon(14, 14, warningColor));
                            break;
                        default:
                            label.setForeground(darkTextColor);
                            label.setIcon(null);
                    }
                }

                label.setHorizontalAlignment(JLabel.CENTER);
                return label;
            }
        });

        // Also apply custom renderer for the old status column (index 2)
        tblLichSu.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                JLabel label = (JLabel) c;

                if (value != null) {
                    TrangThaiVeTau trangThai = (TrangThaiVeTau) value;
                    switch (trangThai) {
                        case DA_THANH_TOAN:
                            label.setForeground(successColor);
                            label.setIcon(createPaymentIcon(14, 14, successColor));
                            break;
                        case CHO_XAC_NHAN:
                            label.setForeground(warningColor);
                            label.setIcon(createPendingIcon(14, 14, warningColor));
                            break;
                        default:
                            label.setForeground(darkTextColor);
                            label.setIcon(null);
                    }
                }

                label.setHorizontalAlignment(JLabel.CENTER);
                return label;
            }
        });

        JScrollPane scrollPane = new JScrollPane(tblLichSu);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        rightPanel.add(scrollPane, BorderLayout.CENTER);

        // Thêm panel tìm kiếm lịch sử
        JPanel searchHistoryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchHistoryPanel.setBackground(Color.WHITE);

        JLabel lblSearch = new JLabel("Tìm kiếm:");
        lblSearch.setFont(new Font("Arial", Font.BOLD, 12));
        lblSearch.setIcon(createSearchIcon(14, 14, primaryColor));
        searchHistoryPanel.add(lblSearch);

        JTextField txtSearch = new JTextField(15);
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            private void search() {
                String searchText = txtSearch.getText().toLowerCase();
                TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelLichSu);
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchText));
                tblLichSu.setRowSorter(sorter);
            }

            @Override
            public void insertUpdate(DocumentEvent e) { search(); }

            @Override
            public void removeUpdate(DocumentEvent e) { search(); }

            @Override
            public void changedUpdate(DocumentEvent e) { search(); }
        });
        searchHistoryPanel.add(txtSearch);

        // Tạo JButton tùy chỉnh cho tìm kiếm lịch sử
        JButton btnSearch = new JButton("Tìm");
        btnSearch.setFont(new Font("Arial", Font.PLAIN, 12));
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setBackground(primaryColor);
        btnSearch.setBorderPainted(false);
        btnSearch.setFocusPainted(false);
        btnSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSearch.setIcon(createSearchIcon(12, 12, Color.WHITE));

        // Thêm hiệu ứng hover
        btnSearch.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnSearch.setBackground(primaryColor.darker());
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnSearch.setBackground(primaryColor);
            }
        });

        searchHistoryPanel.add(btnSearch);
        rightPanel.add(searchHistoryPanel, BorderLayout.NORTH);

        // Đặt độ rộng cho các cột
        TableColumnModel columnModel = tblLichSu.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(80);  // Mã Vé
        columnModel.getColumn(1).setPreferredWidth(120); // Ngày Đổi
        columnModel.getColumn(2).setPreferredWidth(100); // Trạng Thái Cũ
        columnModel.getColumn(3).setPreferredWidth(100); // Trạng Thái Mới

        return rightPanel;
    }

    private void customizeTable(JTable table) {
        // Thiết lập font và màu sắc cho bảng
        table.setFont(new Font("Arial", Font.PLAIN, 12));
        table.setRowHeight(25);
        table.setGridColor(new Color(240, 240, 240));
        table.setShowVerticalLines(true);
        table.setShowHorizontalLines(true);

        // Tùy chỉnh header của bảng - sửa lỗi header không hiển thị
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Arial", Font.BOLD, 12));
        header.setBackground(primaryColor);
        header.setForeground(Color.WHITE);
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setBackground(primaryColor);
                label.setForeground(Color.WHITE);
                label.setHorizontalAlignment(JLabel.CENTER);
                label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
                label.setFont(new Font("Arial", Font.BOLD, 12));
                return label;
            }
        });

        // Đảm bảo JTable sử dụng header đã được tùy chỉnh
        table.setTableHeader(header);

        // Căn giữa nội dung các cột
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Đặt độ rộng cho các cột
        if (table.getColumnCount() >= 4) {
            table.getColumnModel().getColumn(0).setPreferredWidth(80);
            table.getColumnModel().getColumn(1).setPreferredWidth(120);
            table.getColumnModel().getColumn(2).setPreferredWidth(100);
            table.getColumnModel().getColumn(3).setPreferredWidth(100);
        }
    }

    private JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusPanel.setBackground(lightBackground);

        lblStatus = new JLabel(READY_TEXT);
        lblStatus.setFont(new Font("Arial", Font.PLAIN, 12));
        lblStatus.setIcon(createInfoIcon(16, 16, primaryColor));

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(150, 20));

        statusPanel.add(lblStatus, BorderLayout.WEST);
        statusPanel.add(progressBar, BorderLayout.EAST);

        return statusPanel;
    }

    private void updateStatus(String message, boolean isLoading) {
        SwingUtilities.invokeLater(() -> {
            if (lblStatus != null) {
                lblStatus.setText(message);

                // Cập nhật màu sắc và icon cho message
                if (message.equals(LOADING_TEXT)) {
                    lblStatus.setForeground(primaryColor); // Blue
                    lblStatus.setIcon(createLoadingIcon(16, 16));
                } else if (message.equals(ERROR_TEXT)) {
                    lblStatus.setForeground(dangerColor); // Red
                    lblStatus.setIcon(createErrorIcon(16, 16));
                } else if (message.equals(SUCCESS_TEXT)) {
                    lblStatus.setForeground(successColor); // Green
                    lblStatus.setIcon(createSuccessIcon(16, 16));
                } else {
                    lblStatus.setForeground(darkTextColor); // Default
                    lblStatus.setIcon(createInfoIcon(16, 16, primaryColor));
                }
            }

            // Cập nhật progress bar
            if (progressBar != null) {
                progressBar.setVisible(isLoading);
                progressBar.setIndeterminate(isLoading);
            }
        });
    }

    private void timVe() {
        String maVe = txtMaVe.getText().trim();
        if (maVe.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập mã vé!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            updateStatus(LOADING_TEXT, true);

            SwingWorker<VeTau, Void> worker = new SwingWorker<>() {
                @Override
                protected VeTau doInBackground() throws Exception {
                    return doiVeDAO.getVeTau(maVe);
                }

                @Override
                protected void done() {
                    try {
                        veTauHienTai = get();
                        if (veTauHienTai == null) {
                            JOptionPane.showMessageDialog(DoiVePanel.this,
                                    "Không tìm thấy vé với mã: " + maVe,
                                    "Thông báo", JOptionPane.WARNING_MESSAGE);
                            lamMoi();
                        } else {
                            giaVeBanDau = veTauHienTai.getGiaVe();
                            hienThiThongTinVe();

                            // Kiểm tra xem có thể đổi vé không
                            boolean coTheDoiVe = (veTauHienTai.getTrangThai() == TrangThaiVeTau.DA_THANH_TOAN);
                            setInputFieldsEnabled(coTheDoiVe);
                            btnDoiVe.setEnabled(coTheDoiVe);
                            btnChonLichTrinh.setEnabled(coTheDoiVe);
                            btnChonChoNgoi.setEnabled(coTheDoiVe);

                            if (!coTheDoiVe) {
                                JOptionPane.showMessageDialog(DoiVePanel.this,
                                        "Vé này có trạng thái '" + veTauHienTai.getTrangThai() +
                                                "'. Chỉ vé ở trạng thái 'ĐÃ THANH TOÁN' mới có thể đổi.",
                                        "Không thể đổi vé", JOptionPane.WARNING_MESSAGE);
                            }

                            updateStatus(READY_TEXT, false);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(DoiVePanel.this,
                                "Lỗi khi truy vấn dữ liệu: " + e.getMessage(),
                                "Lỗi", JOptionPane.ERROR_MESSAGE);
                        updateStatus(ERROR_TEXT, false);
                    }
                }
            };

            worker.execute();
        } catch (Exception e) {
            updateStatus(ERROR_TEXT, false);
            JOptionPane.showMessageDialog(this, "Lỗi khi truy vấn dữ liệu: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void hienThiThongTinVe() {
        if (veTauHienTai == null) return;

        txtTenKhachHang.setText(veTauHienTai.getTenKhachHang());
        txtGiayTo.setText(veTauHienTai.getGiayTo());
        txtNgayDi.setText(veTauHienTai.getNgayDi().format(formatter));

        // Đặt đối tượng
        String doiTuong = veTauHienTai.getDoiTuong();
        for (int i = 0; i < cboDoiTuong.getItemCount(); i++) {
            if (cboDoiTuong.getItemAt(i).equals(doiTuong)) {
                cboDoiTuong.setSelectedIndex(i);
                break;
            }
        }

        // Hiển thị lịch trình
        if (veTauHienTai.getLichTrinhTau() != null) {
            lichTrinhDaChon = veTauHienTai.getLichTrinhTau();
            lblLichTrinh.setText(lichTrinhDaChon.getMaLich() + " - " +
                    lichTrinhDaChon.getTau().getTuyenTau().getGaDi() +
                    " → " +
                    lichTrinhDaChon.getTau().getTuyenTau().getGaDen() +
                    " (" + lichTrinhDaChon.getGioDi() + ")");
        }

        // Hiển thị chỗ ngồi
        if (veTauHienTai.getChoNgoi() != null) {
            choNgoiDaChon = veTauHienTai.getChoNgoi();
            lblChoNgoi.setText(choNgoiDaChon.getTenCho() + " - " +
                    (choNgoiDaChon.getLoaiCho() != null ? choNgoiDaChon.getLoaiCho().getTenLoai() : ""));
        }

        // Hiển thị khuyến mãi
        if (veTauHienTai.getKhuyenMai() != null) {
            khuyenMaiDaChon = veTauHienTai.getKhuyenMai();
        }

        // Hiển thị trạng thái và giá vé với màu sắc khác nhau
        lblTrangThai.setText(veTauHienTai.getTrangThai().toString());
        setTrangThaiColor(lblTrangThai, veTauHienTai.getTrangThai());

        lblGiaVe.setText(currencyFormatter.format(veTauHienTai.getGiaVe()));
    }

    private void setTrangThaiColor(JLabel label, TrangThaiVeTau trangThai) {
        switch (trangThai) {
            case CHO_XAC_NHAN:
                label.setForeground(warningColor); // Cam
                label.setIcon(createPendingIcon(14, 14, warningColor));
                break;
            case DA_THANH_TOAN:
                label.setForeground(successColor); // Xanh lá
                label.setIcon(createCheckIcon(14, 14, successColor));
                break;
            case DA_TRA:
                label.setForeground(dangerColor); // Đỏ
                label.setIcon(createCancelIcon(14, 14, dangerColor));
                break;
            case DA_DOI:
                label.setForeground(grayColor); // Xám
                label.setIcon(createExchangeIcon(14, 14, grayColor));
                break;
            default:
                label.setForeground(darkTextColor);
                label.setIcon(null);
        }
    }

    private void doiVe() {
        if (veTauHienTai == null) return;

        // Kiểm tra dữ liệu đầu vào
        String tenKhachHang = txtTenKhachHang.getText().trim();
        if (tenKhachHang.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên khách hàng!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            txtTenKhachHang.requestFocus();
            return;
        }

        String giayTo = txtGiayTo.getText().trim();
        if (giayTo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập giấy tờ!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            txtGiayTo.requestFocus();
            return;
        }

        String ngayDiStr = txtNgayDi.getText().trim();
        LocalDate ngayDi;
        try {
            ngayDi = LocalDate.parse(ngayDiStr, formatter);
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this,
                    "Ngày đi không hợp lệ. Vui lòng nhập theo định dạng dd/MM/yyyy!",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            txtNgayDi.requestFocus();
            return;
        }

        // Kiểm tra xem đã chọn lịch trình và chỗ ngồi chưa
        if (lichTrinhDaChon == null) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn lịch trình tàu!",
                    "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (choNgoiDaChon == null) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn chỗ ngồi!",
                    "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            updateStatus(LOADING_TEXT, true);

            // Lưu trữ trạng thái cũ để hiển thị trong lịch sử
            final TrangThaiVeTau trangThaiCu = veTauHienTai.getTrangThai();

            // Cập nhật thông tin vé
            veTauHienTai.setTenKhachHang(tenKhachHang);
            veTauHienTai.setGiayTo(giayTo);
            veTauHienTai.setNgayDi(ngayDi);
            veTauHienTai.setDoiTuong(Objects.requireNonNull(cboDoiTuong.getSelectedItem()).toString());
            veTauHienTai.setLichTrinhTau(lichTrinhDaChon);
            veTauHienTai.setChoNgoi(choNgoiDaChon);
            veTauHienTai.setKhuyenMai(khuyenMaiDaChon);

            // Tính lại giá vé
            double giaVe = tinhGiaVe(choNgoiDaChon, khuyenMaiDaChon, Objects.requireNonNull(cboDoiTuong.getSelectedItem()).toString());
            veTauHienTai.setGiaVe(giaVe);

            // Đổi trạng thái vé thành CHO_XAC_NHAN
            veTauHienTai.setTrangThai(TrangThaiVeTau.CHO_XAC_NHAN);

            // Gọi API để cập nhật vé
            boolean success = doiVeDAO.doiVe(veTauHienTai);

            if (success) {
                updateLichSuAndShowSuccess(trangThaiCu);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Đổi vé không thành công!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                updateStatus(ERROR_TEXT, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi thực hiện đổi vé: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            updateStatus(ERROR_TEXT, false);
        }
    }

    private void hienThiDialogChonLichTrinh() {
        try {
            // Hiển thị dialog chọn lịch trình
            LichTrinhSelectorDialog dialog = new LichTrinhSelectorDialog(
                    (Frame) SwingUtilities.getWindowAncestor(this),
                    lichTrinhTauDAO,
                    toaTauDAO,
                    choNgoiDAO,
                    this::xuLyLichTrinhDaChon
            );

            // Hiển thị dialog
            dialog.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Không thể hiển thị giao diện chọn lịch trình: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void xuLyLichTrinhDaChon(LichTrinhTau lichTrinh) {
        if (lichTrinh != null) {
            lichTrinhDaChon = lichTrinh;
            lblLichTrinh.setText(lichTrinh.getMaLich() + " - " +
                    lichTrinh.getTau().getTuyenTau().getGaDi() +
                    " → " +
                    lichTrinh.getTau().getTuyenTau().getGaDen() +
                    " (" + lichTrinh.getGioDi() + ")");

            // Reset chỗ ngồi vì đã chọn lịch trình mới
            choNgoiDaChon = null;
            lblChoNgoi.setText("Chưa chọn");

            capNhatGiaVe();
        }
    }

    private void hienThiDialogChonChoNgoi() {
        try {
            if (lichTrinhDaChon == null) {
                JOptionPane.showMessageDialog(this,
                        "Vui lòng chọn lịch trình trước khi chọn chỗ ngồi!",
                        "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Hiển thị dialog chọn chỗ ngồi
            ChoNgoiSelectorDialog dialog = new ChoNgoiSelectorDialog(
                    (Frame) SwingUtilities.getWindowAncestor(this),
                    lichTrinhDaChon,
                    choNgoiDAO,
                    toaTauDAO,
                    this::xuLyChoNgoiDaChon
            );
            dialog.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Không thể hiển thị giao diện chọn chỗ ngồi: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void xuLyChoNgoiDaChon(ChoNgoi choNgoi) {
        if (choNgoi != null) {
            choNgoiDaChon = choNgoi;
            lblChoNgoi.setText(choNgoi.getTenCho() + " - " +
                    (choNgoi.getLoaiCho() != null ? choNgoi.getLoaiCho().getTenLoai() : ""));

            capNhatGiaVe();
        }
    }

    private void capNhatGiaVe() {
        if (choNgoiDaChon != null) {
            String doiTuong = Objects.requireNonNull(cboDoiTuong.getSelectedItem()).toString();
            double giaVe = tinhGiaVe(choNgoiDaChon, khuyenMaiDaChon, doiTuong);
            lblGiaVe.setText(currencyFormatter.format(giaVe));
        }
    }

    /**
     * Tính lại giá vé khi đổi vé
     *
     * @param choNgoi   Chỗ ngồi được chọn
     * @param khuyenMai Khuyến mãi áp dụng (có thể null)
     * @param doiTuong  Đối tượng khách hàng (để tính giảm giá)
     * @return Giá vé sau khi tính toán
     */
    private double tinhGiaVe(ChoNgoi choNgoi, KhuyenMai khuyenMai, String doiTuong) {
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

        return giaVe;
    }

    private void updateLichSuAndShowSuccess(TrangThaiVeTau trangThaiCu) {

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String ngayGio = sdf.format(new Date());

        TrangThaiVeTau trangThaiThanhToan = TrangThaiVeTau.CHO_XAC_NHAN;
        if (veTauHienTai.getTrangThai() == TrangThaiVeTau.DA_THANH_TOAN) {
            trangThaiThanhToan = TrangThaiVeTau.DA_THANH_TOAN;
        }

        modelLichSu.addRow(new Object[]{
                veTauHienTai.getMaVe(),
                ngayGio,
                trangThaiCu,
                trangThaiThanhToan
        });

        // Create payment dialog
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Thanh toán đổi vé", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(500, 650); // Tăng kích thước chiều cao để chứa thêm các thành phần mới
        dialog.setLocationRelativeTo(this);

        JPanel pnlContent = new JPanel(new BorderLayout(10, 10));
        pnlContent.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Icon panel
        JLabel lblIcon = new JLabel(createSuccessTickIcon(64, 64, successColor));
        lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
        pnlContent.add(lblIcon, BorderLayout.NORTH);

        // Information panel
        JPanel pnlInfo = new JPanel(new BorderLayout(10, 10));

        // Ticket details
        String thongTinVe = "Thông tin vé đã đổi:\n\n" +
                "- Mã vé: " + veTauHienTai.getMaVe() + "\n" +
                "- Tên khách hàng: " + veTauHienTai.getTenKhachHang() + "\n" +
                "- Giấy tờ: " + veTauHienTai.getGiayTo() + "\n\n" +
                "- Lịch trình: " + veTauHienTai.getLichTrinhTau().getMaLich() + "\n" +
                "- Ngày đi: " + veTauHienTai.getNgayDi() + "\n" +
                "- Giờ đi: " + veTauHienTai.getLichTrinhTau().getGioDi() + "\n" +
                "- Tuyến: " + veTauHienTai.getLichTrinhTau().getTau().getTuyenTau().getGaDi() +
                " → " + veTauHienTai.getLichTrinhTau().getTau().getTuyenTau().getGaDen() + "\n\n" +
                "- Chỗ ngồi: " + veTauHienTai.getChoNgoi().getTenCho() + "\n" +
                "- Loại chỗ: " + veTauHienTai.getChoNgoi().getLoaiCho().getTenLoai() + "\n" +
                "- Đối tượng: " + veTauHienTai.getDoiTuong() + "\n" +
                "- Giá vé: " + currencyFormatter.format(veTauHienTai.getGiaVe()) + "\n\n" +
                "- Trạng thái: " + veTauHienTai.getTrangThai().getValue();

        JTextArea txtThongTin = new JTextArea(thongTinVe);
        txtThongTin.setEditable(false);
        txtThongTin.setFont(new Font("Arial", Font.PLAIN, 14));
        txtThongTin.setBackground(new Color(250, 250, 250));
        txtThongTin.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JScrollPane scrollPane = new JScrollPane(txtThongTin);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        pnlInfo.add(scrollPane, BorderLayout.CENTER);

        // Payment panel
        JPanel pnlPayment = new JPanel();
        pnlPayment.setLayout(new BoxLayout(pnlPayment, BoxLayout.Y_AXIS));
        pnlPayment.setBorder(BorderFactory.createTitledBorder("Thông tin thanh toán"));

        // Thêm panel chọn phương thức thanh toán
        JPanel pnlPaymentMethod = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel lblPaymentMethod = new JLabel("Phương thức thanh toán:");
        pnlPaymentMethod.add(lblPaymentMethod);

        // Tạo radio button cho các phương thức thanh toán
        JRadioButton radCash = new JRadioButton("Tiền mặt", true);
        JRadioButton radTransfer = new JRadioButton("Chuyển khoản");

        ButtonGroup paymentMethodGroup = new ButtonGroup();
        paymentMethodGroup.add(radCash);
        paymentMethodGroup.add(radTransfer);

        // Thêm các radio button vào panel
        pnlPaymentMethod.add(radCash);
        pnlPaymentMethod.add(radTransfer);
        pnlPayment.add(pnlPaymentMethod);

        // Panel cho thanh toán tiền mặt
        JPanel pnlCashPayment = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Total amount
        gbc.gridx = 0; gbc.gridy = 0;
        pnlCashPayment.add(new JLabel("Tổng tiền:"), gbc);

        gbc.gridx = 1;
        JLabel lblTotalAmount = new JLabel(currencyFormatter.format(veTauHienTai.getGiaVe()));
        lblTotalAmount.setFont(new Font("Arial", Font.BOLD, 14));
        pnlCashPayment.add(lblTotalAmount, gbc);

        // Customer payment
        gbc.gridx = 0; gbc.gridy = 1;
        pnlCashPayment.add(new JLabel("Tiền khách đưa:"), gbc);

        gbc.gridx = 1;
        JTextField txtCustomerPayment = new JTextField(15);
        txtCustomerPayment.setFont(new Font("Arial", Font.PLAIN, 14));
        pnlCashPayment.add(txtCustomerPayment, gbc);

        // Change amount
        gbc.gridx = 0; gbc.gridy = 2;
        pnlCashPayment.add(new JLabel("Tiền thối lại:"), gbc);

        gbc.gridx = 1;
        JLabel lblChange = new JLabel("0 VNĐ");
        lblChange.setFont(new Font("Arial", Font.BOLD, 14));
        pnlCashPayment.add(lblChange, gbc);

        // Panel cho thanh toán chuyển khoản
        JPanel pnlTransferPayment = new JPanel();
        pnlTransferPayment.setLayout(new BorderLayout(10, 10));

        // Panel cho thông tin chuyển khoản
        JPanel pnlTransferInfo = new JPanel(new GridBagLayout());
        GridBagConstraints gbcTransfer = new GridBagConstraints();
        gbcTransfer.insets = new Insets(5, 5, 5, 5);
        gbcTransfer.fill = GridBagConstraints.HORIZONTAL;

        // Số tiền cần chuyển
        gbcTransfer.gridx = 0; gbcTransfer.gridy = 0;
        pnlTransferInfo.add(new JLabel("Số tiền cần chuyển:"), gbcTransfer);

        gbcTransfer.gridx = 1;
        JLabel lblTransferAmount = new JLabel(currencyFormatter.format(veTauHienTai.getGiaVe()));
        lblTransferAmount.setFont(new Font("Arial", Font.BOLD, 14));
        pnlTransferInfo.add(lblTransferAmount, gbcTransfer);

        // Phương thức thanh toán
        gbcTransfer.gridx = 0; gbcTransfer.gridy = 1;
        pnlTransferInfo.add(new JLabel("Chọn phương thức:"), gbcTransfer);

        gbcTransfer.gridx = 1;
        String[] paymentOptions = {"Chuyển khoản ngân hàng", "VNPay QR"};
        JComboBox<String> cmbPaymentType = new JComboBox<>(paymentOptions);
        pnlTransferInfo.add(cmbPaymentType, gbcTransfer);

        // Tab panel cho các phương thức thanh toán
        JPanel pnlPaymentTabs = new JPanel(new CardLayout());

        // Tab 1: Chuyển khoản ngân hàng truyền thống
        JPanel pnlBankTransfer = new JPanel();
        pnlBankTransfer.setLayout(new BoxLayout(pnlBankTransfer, BoxLayout.Y_AXIS));

        JPanel pnlAccountInfo = new JPanel();
        pnlAccountInfo.setLayout(new BoxLayout(pnlAccountInfo, BoxLayout.Y_AXIS));
        pnlAccountInfo.setBorder(BorderFactory.createTitledBorder("Thông tin chuyển khoản"));

        JLabel lblBankName = new JLabel("• Ngân hàng: BIDV - Ngân hàng Đầu tư và Phát triển Việt Nam");
        JLabel lblAccountName = new JLabel("• Chủ tài khoản: CÔNG TY CỔ PHẦN VẬN TẢI ĐƯỜNG SẮT LẠC HỒNG");
        JLabel lblAccountNumber = new JLabel("• Số tài khoản: 21410000123456");
        JLabel lblTransferContent = new JLabel("• Nội dung chuyển khoản: " + veTauHienTai.getMaVe());

        pnlAccountInfo.add(lblBankName);
        pnlAccountInfo.add(Box.createVerticalStrut(5));
        pnlAccountInfo.add(lblAccountName);
        pnlAccountInfo.add(Box.createVerticalStrut(5));
        pnlAccountInfo.add(lblAccountNumber);
        pnlAccountInfo.add(Box.createVerticalStrut(5));
        pnlAccountInfo.add(lblTransferContent);

        pnlBankTransfer.add(pnlAccountInfo);

        // Mã giao dịch
        JPanel pnlTransactionId = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlTransactionId.add(new JLabel("Mã giao dịch:"));
        JTextField txtTransactionId = new JTextField(20);
        txtTransactionId.setFont(new Font("Arial", Font.PLAIN, 14));
        pnlTransactionId.add(txtTransactionId);
        pnlBankTransfer.add(pnlTransactionId);

        // Tab 2: VNPay QR
        JPanel pnlVnpayQR = new JPanel(new BorderLayout(10, 10));
        pnlVnpayQR.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel hiển thị mã QR
        JPanel pnlQRDisplay = new JPanel(new BorderLayout());
        pnlQRDisplay.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
        pnlQRDisplay.setBackground(Color.WHITE);
        pnlQRDisplay.setPreferredSize(new Dimension(240, 240));

        // Label để hiển thị mã QR (ban đầu chỉ hiển thị icon tải)
        JLabel lblQRCode = new JLabel();
        lblQRCode.setHorizontalAlignment(SwingConstants.CENTER);
        lblQRCode.setIcon(createLoadingIcon(48, 48));
        pnlQRDisplay.add(lblQRCode, BorderLayout.CENTER);

        pnlVnpayQR.add(pnlQRDisplay, BorderLayout.CENTER);

        // Panel hướng dẫn thanh toán QR
        JPanel pnlQRGuide = new JPanel();
        pnlQRGuide.setLayout(new BoxLayout(pnlQRGuide, BoxLayout.Y_AXIS));

        JLabel lblQRGuide1 = new JLabel("1. Quét mã QR bằng ứng dụng ngân hàng hoặc VNPay");
        JLabel lblQRGuide2 = new JLabel("2. Kiểm tra thông tin giao dịch và xác nhận thanh toán");
        JLabel lblQRGuide3 = new JLabel("3. Hệ thống sẽ tự động cập nhật sau khi thanh toán thành công");

        pnlQRGuide.add(lblQRGuide1);
        pnlQRGuide.add(Box.createVerticalStrut(5));
        pnlQRGuide.add(lblQRGuide2);
        pnlQRGuide.add(Box.createVerticalStrut(5));
        pnlQRGuide.add(lblQRGuide3);

        // Thêm trạng thái thanh toán
        JPanel pnlPaymentStatus = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel lblPaymentStatus = new JLabel("Đang chờ thanh toán...", createLoadingIcon(16, 16), JLabel.LEFT);
        lblPaymentStatus.setForeground(new Color(255, 153, 0)); // Màu cam
        pnlPaymentStatus.add(lblPaymentStatus);

        // Panel cho nút làm mới trạng thái
        JPanel pnlRefresh = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnRefreshStatus = new JButton("Kiểm tra trạng thái");
        btnRefreshStatus.setIcon(createRefreshIcon(16, 16, Color.BLACK));
        pnlRefresh.add(btnRefreshStatus);

        JPanel pnlQRBottom = new JPanel(new BorderLayout());
        pnlQRBottom.add(pnlQRGuide, BorderLayout.NORTH);
        pnlQRBottom.add(pnlPaymentStatus, BorderLayout.CENTER);
        pnlQRBottom.add(pnlRefresh, BorderLayout.SOUTH);

        pnlVnpayQR.add(pnlQRBottom, BorderLayout.SOUTH);

        // Thêm các tab vào panel chính
        pnlPaymentTabs.add(pnlBankTransfer, "BANK_TRANSFER");
        pnlPaymentTabs.add(pnlVnpayQR, "VNPAY_QR");

        // Listener cho combobox để chuyển tab
        cmbPaymentType.addActionListener(e -> {
            CardLayout cl = (CardLayout) pnlPaymentTabs.getLayout();
            int selectedIndex = cmbPaymentType.getSelectedIndex();
            if (selectedIndex == 0) {
                cl.show(pnlPaymentTabs, "BANK_TRANSFER");
            } else {
                cl.show(pnlPaymentTabs, "VNPAY_QR");
                // Tạo QR code khi chọn tab VNPay
                generateVnpayQRCode(lblQRCode, veTauHienTai.getMaVe(), veTauHienTai.getGiaVe(), lblPaymentStatus);
            }
        });

        pnlTransferInfo.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        pnlTransferPayment.add(pnlTransferInfo, BorderLayout.NORTH);
        pnlTransferPayment.add(pnlPaymentTabs, BorderLayout.CENTER);

        // Nút làm mới trạng thái thanh toán
        btnRefreshStatus.addActionListener(e -> {
            checkVnpayPaymentStatus(veTauHienTai.getMaVe(), lblPaymentStatus, dialog);
        });

        // Hiển thị panel phương thức thanh toán ban đầu (mặc định là tiền mặt)
        pnlPayment.add(pnlCashPayment);
        pnlTransferPayment.setVisible(false);
        pnlPayment.add(pnlTransferPayment);

        // Thêm listener cho radio button để chuyển đổi giữa các phương thức thanh toán
        radCash.addActionListener(e -> {
            pnlCashPayment.setVisible(true);
            pnlTransferPayment.setVisible(false);
            dialog.revalidate();
            dialog.repaint();
        });

        radTransfer.addActionListener(e -> {
            pnlCashPayment.setVisible(false);
            pnlTransferPayment.setVisible(true);
            dialog.revalidate();
            dialog.repaint();
        });

        // Add document listener for automatic change calculation
        txtCustomerPayment.getDocument().addDocumentListener(new DocumentListener() {
            private void updateChange() {
                try {
                    String input = txtCustomerPayment.getText().replaceAll("[^\\d]", "");
                    if (!input.isEmpty()) {
                        double customerPayment = Double.parseDouble(input);
                        double change = customerPayment - veTauHienTai.getGiaVe();
                        lblChange.setText(currencyFormatter.format(Math.max(0, change)));
                    } else {
                        lblChange.setText("0 VNĐ");
                    }
                } catch (NumberFormatException e) {
                    lblChange.setText("0 VNĐ");
                }
            }

            @Override
            public void insertUpdate(DocumentEvent e) { updateChange(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updateChange(); }
            @Override
            public void changedUpdate(DocumentEvent e) { updateChange(); }
        });

        pnlInfo.add(pnlPayment, BorderLayout.SOUTH);
        pnlContent.add(pnlInfo, BorderLayout.CENTER);

        // Button panel
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlButtons.setBackground(Color.WHITE);

        // Payment button
        JButton btnThanhToan = new JButton("Thanh toán") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? primaryColor.darker().darker() :
                        getModel().isRollover() ? primaryColor.darker() : primaryColor);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };

        btnThanhToan.setForeground(Color.WHITE);
        btnThanhToan.setFont(new Font("Arial", Font.BOLD, 12));
        btnThanhToan.setBorderPainted(false);
        btnThanhToan.setContentAreaFilled(false);
        btnThanhToan.setFocusPainted(false);
        btnThanhToan.setIcon(createPaymentIcon(16, 16, Color.WHITE));
        btnThanhToan.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnThanhToan.setPreferredSize(new Dimension(120, 30));

        btnThanhToan.addActionListener(e -> {
            try {
                if (radCash.isSelected()) {
                    // Xử lý thanh toán tiền mặt
                    String input = txtCustomerPayment.getText().replaceAll("[^\\d]", "");
                    if (input.isEmpty()) {
                        JOptionPane.showMessageDialog(dialog,
                                "Vui lòng nhập số tiền khách đưa",
                                "Thông báo", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    double customerPayment = Double.parseDouble(input);
                    if (customerPayment < veTauHienTai.getGiaVe()) {
                        JOptionPane.showMessageDialog(dialog,
                                "Số tiền khách đưa không đủ",
                                "Thông báo", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    if (xuLyThanhToan("TIEN_MAT", "")) {
                        double change = customerPayment - veTauHienTai.getGiaVe();
                        showPaymentSuccessDialog(change);
                        processAfterSuccessfulPayment(dialog);
                    }
                } else if (radTransfer.isSelected()) {
                    // Xử lý thanh toán chuyển khoản
                    int selectedPaymentType = cmbPaymentType.getSelectedIndex();

                    if (selectedPaymentType == 0) { // Chuyển khoản ngân hàng
                        String transactionId = txtTransactionId.getText().trim();
                        if (transactionId.isEmpty()) {
                            JOptionPane.showMessageDialog(dialog,
                                    "Vui lòng nhập mã giao dịch",
                                    "Thông báo", JOptionPane.WARNING_MESSAGE);
                            return;
                        }

                        if (xuLyThanhToan("CHUYEN_KHOAN_NGAN_HANG", transactionId)) {
                            showTransferSuccessDialog();
                            processAfterSuccessfulPayment(dialog);
                        } else {
                            JOptionPane.showMessageDialog(dialog,
                                    "Không thể xác thực giao dịch. Vui lòng kiểm tra mã giao dịch.",
                                    "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                    } else { // VNPay QR
                        // Xác thực lại trạng thái thanh toán một lần nữa
                        boolean paymentSuccess = checkVnpayPaymentStatus(veTauHienTai.getMaVe(), lblPaymentStatus, null);

                        if (paymentSuccess) {
                            showVnpaySuccessDialog();
                            processAfterSuccessfulPayment(dialog);
                        } else {
                            JOptionPane.showMessageDialog(dialog,
                                    "Chưa nhận được thông tin thanh toán. Vui lòng thanh toán hoặc kiểm tra lại.",
                                    "Chưa thanh toán", JOptionPane.WARNING_MESSAGE);
                        }
                    }
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Số tiền không hợp lệ",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog,
                        "Lỗi khi thanh toán: " + ex.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        pnlButtons.add(btnThanhToan);
        pnlContent.add(pnlButtons, BorderLayout.SOUTH);

        dialog.add(pnlContent);
        dialog.setVisible(true);
    }

    // Phương thức hiển thị dialog thanh toán VNPay thành công
    private void showVnpaySuccessDialog() {
        JDialog successDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Thanh toán thành công", true);
        successDialog.setSize(350, 200);
        successDialog.setLocationRelativeTo(this);
        successDialog.setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Thêm icon thành công
        JLabel iconLabel = new JLabel(createSuccessTickIcon(64, 64, successColor));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(iconLabel);
        panel.add(Box.createVerticalStrut(15));

        // Thêm thông báo
        JLabel messageLabel = new JLabel("Thanh toán VNPay thành công!");
        messageLabel.setFont(new Font("Arial", Font.BOLD, 14));
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(messageLabel);
        panel.add(Box.createVerticalStrut(20));

        // Thêm nút OK
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> successDialog.dispose());
        okButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(okButton);
        panel.add(buttonPanel);

        successDialog.add(panel);
        successDialog.setVisible(true);
    }

    // Phương thức kiểm tra trạng thái thanh toán VNPay
    private boolean checkVnpayPaymentStatus(String maVe, JLabel lblStatus, JDialog parentDialog) {
        // Tạo SwingWorker để không làm đơ giao diện
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    // Trong môi trường thực tế, bạn sẽ gọi API VNPay để kiểm tra trạng thái giao dịch
                    // Ở đây chúng ta sẽ mô phỏng việc kiểm tra, trả về ngẫu nhiên để demo
                    // Trong thực tế, sẽ kiểm tra dựa trên mã giao dịch đã lưu

                    // Mô phỏng gọi API kiểm tra trạng thái (50% cơ hội thành công)
                    Thread.sleep(1500); // Mô phỏng thời gian gọi API
                    return new Random().nextBoolean();
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        lblStatus.setText("Thanh toán thành công!");
                        lblStatus.setIcon(createSuccessIcon(16, 16));
                        lblStatus.setForeground(new Color(0, 153, 0)); // Màu xanh lá

                        // Hiển thị thông báo nếu được gọi từ nút Kiểm tra
                        if (parentDialog != null) {
                            JOptionPane.showMessageDialog(parentDialog,
                                    "Thanh toán đã được xác nhận thành công!",
                                    "Thanh toán thành công", JOptionPane.INFORMATION_MESSAGE,
                                    createSuccessIcon(32, 32));
                        }
                    } else {
                        lblStatus.setText("Chưa nhận được thanh toán");
                        lblStatus.setIcon(createWarningIcon(16, 16));
                        lblStatus.setForeground(new Color(255, 153, 0)); // Màu cam

                        // Hiển thị thông báo nếu được gọi từ nút Kiểm tra
                        if (parentDialog != null) {
                            JOptionPane.showMessageDialog(parentDialog,
                                    "Chưa nhận được thông tin thanh toán. Vui lòng thử lại sau.",
                                    "Chờ thanh toán", JOptionPane.WARNING_MESSAGE);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    lblStatus.setText("Lỗi kiểm tra thanh toán");
                    lblStatus.setForeground(Color.RED);

                    if (parentDialog != null) {
                        JOptionPane.showMessageDialog(parentDialog,
                                "Lỗi khi kiểm tra trạng thái thanh toán: " + e.getMessage(),
                                "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        };

        worker.execute();

        // Trong môi trường thực tế, bạn cần đợi kết quả từ worker hoặc sử dụng callback
        // Ở đây chúng ta sẽ trả về giá trị giả định để demo
        try {
            return worker.get(3, TimeUnit.SECONDS); // Đợi tối đa 3 giây
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private ImageIcon createWarningIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Vẽ hình tam giác cảnh báo
        g2d.setColor(new Color(255, 153, 0));
        int[] xPoints = {width / 2, width, 0};
        int[] yPoints = {0, height, height};
        g2d.fillPolygon(xPoints, yPoints, 3);

        // Vẽ dấu chấm than
        g2d.setColor(Color.WHITE);
        g2d.fillRect(width / 2 - width / 10, height / 4, width / 5, height / 2);
        g2d.fillOval(width / 2 - width / 10, height * 3 / 4, width / 5, width / 5);

        g2d.dispose();
        return new ImageIcon(image);
    }

    private void generateVnpayQRCode(JLabel lblQRCode, String maVe, double amount, JLabel lblStatus) {
        // Tạo SwingWorker để không làm đơ giao diện
        SwingWorker<ImageIcon, Void> worker = new SwingWorker<>() {
            @Override
            protected ImageIcon doInBackground() {
                try {
                    // Tạo tham số cho API VNPay
                    String vnp_TxnRef = maVe + System.currentTimeMillis(); // Mã tham chiếu giao dịch
                    String vnp_Amount = String.valueOf((long)(amount * 100)); // Số tiền * 100 (đơn vị xu)
                    String vnp_OrderInfo = "Thanh toan hoa don ve tau " + maVe;

                    // URL API tạo mã QR của VNPay (đây là URL giả định, bạn cần thay thế bằng URL thực)
                    String apiUrl = "https://sandbox.vnpayment.vn/paymentv2/create_qr_code.html";

                    // Tạo các tham số API
                    Map<String, String> params = new HashMap<>();
                    params.put("vnp_Version", "2.1.0");
                    params.put("vnp_Command", "pay");
                    params.put("vnp_TmnCode", "YOUR_TMN_CODE"); // Mã website tại VNPay
                    params.put("vnp_Amount", vnp_Amount);
                    params.put("vnp_CurrCode", "VND");
                    params.put("vnp_TxnRef", vnp_TxnRef);
                    params.put("vnp_OrderInfo", vnp_OrderInfo);
                    params.put("vnp_OrderType", "250000"); // Mã danh mục hàng hóa
                    params.put("vnp_Locale", "vn");
                    params.put("vnp_ReturnUrl", "https://yourdomain.com/vnpay_return");

                    // Thêm các tham số khác nếu cần

                    // Thêm thời gian tạo giao dịch
                    Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
                    String vnp_CreateDate = formatter.format(cld.getTime());
                    params.put("vnp_CreateDate", vnp_CreateDate);

                    // Tạo chuỗi hash để xác thực
                    List<String> fieldNames = new ArrayList<>(params.keySet());
                    Collections.sort(fieldNames);

                    StringBuilder hashData = new StringBuilder();
                    StringBuilder query = new StringBuilder();

                    for (String field : fieldNames) {
                        hashData.append(field).append('=').append(URLEncoder.encode(params.get(field), StandardCharsets.US_ASCII.toString()));
                        query.append(URLEncoder.encode(field, StandardCharsets.US_ASCII.toString())).append('=')
                                .append(URLEncoder.encode(params.get(field), StandardCharsets.US_ASCII.toString()));

                        if (fieldNames.indexOf(field) < fieldNames.size() - 1) {
                            hashData.append('&');
                            query.append('&');
                        }
                    }

                    String vnp_SecureHash = hmacSHA512("YOUR_SECRET_KEY", hashData.toString());
                    query.append("&vnp_SecureHash=").append(vnp_SecureHash);

                    String paymentUrl = apiUrl + "?" + query.toString();

                    // Trong môi trường thực tế, bạn sẽ gọi API VNPay để lấy URL hoặc dữ liệu QR
                    // Ở đây, chúng ta sẽ tạo mã QR từ URL thanh toán
                    // Lưu thông tin này để tra cứu trạng thái thanh toán sau này
                    saveVnpayTransaction(vnp_TxnRef, maVe, amount);

                    // Tạo QR code từ URL thanh toán
                    return generateQRCodeImage(paymentUrl, 200, 200);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    ImageIcon qrIcon = get();
                    if (qrIcon != null) {
                        lblQRCode.setIcon(qrIcon);
                        lblStatus.setText("Đang chờ thanh toán...");
                        lblStatus.setForeground(new Color(255, 153, 0)); // Màu cam
                    } else {
                        lblQRCode.setIcon(createErrorIcon(48, 48));
                        lblStatus.setText("Lỗi tạo mã QR");
                        lblStatus.setForeground(Color.RED);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    lblQRCode.setIcon(createErrorIcon(48, 48));
                    lblStatus.setText("Lỗi tạo mã QR");
                    lblStatus.setForeground(Color.RED);
                }
            }
        };

        worker.execute();
    }

    // Phương thức mã hóa chuỗi bằng HMAC SHA512
    private String hmacSHA512(String key, String data) {
        try {
            Mac sha512Hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "HmacSHA512");
            sha512Hmac.init(secretKeySpec);
            byte[] hmacData = sha512Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hmacData);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private ImageIcon generateQRCodeImage(String text, int width, int height) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

        BufferedImage qrImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                qrImage.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }

        // Tạo logo VNPay ở giữa mã QR (tùy chọn)
        BufferedImage logo = createVnpayLogo(width / 5, height / 5);
        if (logo != null) {
            Graphics2D g2d = qrImage.createGraphics();
            int logoWidth = logo.getWidth();
            int logoHeight = logo.getHeight();
            int logoX = (width - logoWidth) / 2;
            int logoY = (height - logoHeight) / 2;

            g2d.drawImage(logo, logoX, logoY, null);
            g2d.dispose();
        }

        return new ImageIcon(qrImage);
    }
    // Phương thức tạo logo VNPay (mô phỏng)
    private BufferedImage createVnpayLogo(int width, int height) {
        BufferedImage logo = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = logo.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Vẽ nền tròn
        g2d.setColor(Color.WHITE);
        g2d.fillOval(0, 0, width, height);

        // Vẽ viền
        g2d.setColor(new Color(29, 118, 186));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(0, 0, width - 1, height - 1);

        // Vẽ chữ "VNPay" đơn giản
        g2d.setFont(new Font("Arial", Font.BOLD, width / 4));
        g2d.setColor(new Color(29, 118, 186));
        FontMetrics fm = g2d.getFontMetrics();
        String text = "VN";
        int textX = (width - fm.stringWidth(text)) / 2;
        int textY = ((height - fm.getHeight()) / 2) + fm.getAscent();
        g2d.drawString(text, textX, textY);

        g2d.dispose();
        return logo;
    }
    // Phương thức lưu thông tin giao dịch VNPay
    private void saveVnpayTransaction(String txnRef, String maVe, double amount) {
        // Lưu thông tin giao dịch vào database hoặc bộ nhớ tạm
        // Ở đây chỉ là mô phỏng, bạn cần thay thế bằng code thực tế
        System.out.println("Đã lưu thông tin giao dịch VNPay: " + txnRef + ", Mã vé: " + maVe + ", Số tiền: " + amount);
    }

    // Phương thức xử lý sau khi thanh toán thành công
    private void processAfterSuccessfulPayment(JDialog dialog) throws RemoteException {
        // Cập nhật trạng thái vé thành ĐÃ_THANH_TOAN
        veTauHienTai.setTrangThai(TrangThaiVeTau.DA_THANH_TOAN);

        // Gọi API để cập nhật trạng thái vé
        boolean success = doiVeDAO.capNhatTrangThaiVe(veTauHienTai.getMaVe(), TrangThaiVeTau.DA_THANH_TOAN);

        if (success) {
            dialog.dispose();
            updateStatus(SUCCESS_TEXT, false);

            // Cập nhật lại trạng thái trên giao diện
            lblTrangThai.setText(veTauHienTai.getTrangThai().toString());
            setTrangThaiColor(lblTrangThai, veTauHienTai.getTrangThai());

            // Cập nhật lại bảng lịch sử
            DefaultTableModel model = (DefaultTableModel) tblLichSu.getModel();
            int rowCount = model.getRowCount();
            if (rowCount > 0) {
                // Cập nhật dòng cuối cùng (vừa thêm)
                model.setValueAt(TrangThaiVeTau.DA_THANH_TOAN, rowCount - 1, 3);
            }

            lamMoi();
        } else {
            JOptionPane.showMessageDialog(dialog,
                    "Không thể cập nhật trạng thái vé",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Phương thức hiển thị thông báo thanh toán chuyển khoản thành công
    private void showTransferSuccessDialog() {
        JDialog successDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Thanh toán thành công", true);
        successDialog.setSize(350, 200);
        successDialog.setLocationRelativeTo(this);
        successDialog.setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Thêm icon thành công
        JLabel iconLabel = new JLabel(createSuccessTickIcon(64, 64, successColor));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(iconLabel);
        panel.add(Box.createVerticalStrut(15));

        // Thêm thông báo
        JLabel messageLabel = new JLabel("Thanh toán chuyển khoản thành công!");
        messageLabel.setFont(new Font("Arial", Font.BOLD, 14));
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(messageLabel);
        panel.add(Box.createVerticalStrut(20));

        // Thêm nút OK
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> successDialog.dispose());
        okButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(okButton);
        panel.add(buttonPanel);

        successDialog.add(panel);
        successDialog.setVisible(true);
    }

    // Phương thức xử lý thanh toán với thông tin về phương thức thanh toán và mã giao dịch (nếu có)
    private boolean xuLyThanhToan(String phuongThucThanhToan, String maGiaoDich) {
        try {
            // Code xử lý thanh toán ở đây
            // Lưu thông tin phương thức thanh toán và mã giao dịch vào cơ sở dữ liệu

            // Giả sử phương thức này luôn trả về true nếu không có ngoại lệ
            System.out.println("Xử lý thanh toán: " + phuongThucThanhToan + ", Mã giao dịch: " +
                    (maGiaoDich.isEmpty() ? "Không có" : maGiaoDich));

            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    // Phương thức hiển thị thông báo thanh toán tiền mặt thành công
    private void showPaymentSuccessDialog(double change) {
        JDialog successDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Thanh toán thành công", true);
        successDialog.setSize(350, 240);
        successDialog.setLocationRelativeTo(this);
        successDialog.setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Thêm icon thành công
        JLabel iconLabel = new JLabel(createSuccessTickIcon(64, 64, successColor));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(iconLabel);
        panel.add(Box.createVerticalStrut(15));

        // Thêm thông báo
        JLabel messageLabel = new JLabel("Thanh toán thành công!");
        messageLabel.setFont(new Font("Arial", Font.BOLD, 14));
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(messageLabel);
        panel.add(Box.createVerticalStrut(10));

        // Thêm thông tin tiền thối
        JLabel changeLabel = new JLabel("Tiền thối: " + currencyFormatter.format(change));
        changeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(changeLabel);
        panel.add(Box.createVerticalStrut(20));

        // Thêm nút OK
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> successDialog.dispose());
        okButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(okButton);
        panel.add(buttonPanel);

        successDialog.add(panel);
        successDialog.setVisible(true);
    }

//    private void showPaymentSuccessDialog(double change) {
//        JDialog successDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
//                "Thanh toán thành công", true);
//        successDialog.setLayout(new BorderLayout(10, 10));
//        successDialog.setSize(300, 200);
//        successDialog.setLocationRelativeTo(this);
//
//        JPanel pnlContent = new JPanel(new BorderLayout(10, 10));
//        pnlContent.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
//
//        // Success message
//        JLabel lblMessage = new JLabel("Thanh toán thành công!");
//        lblMessage.setHorizontalAlignment(SwingConstants.CENTER);
//        lblMessage.setFont(new Font("Arial", Font.BOLD, 16));
//        pnlContent.add(lblMessage, BorderLayout.NORTH);
//
//        // Change amount
//        JLabel lblChange = new JLabel("Tiền thối lại: " + currencyFormatter.format(change));
//        lblChange.setHorizontalAlignment(SwingConstants.CENTER);
//        lblChange.setFont(new Font("Arial", Font.PLAIN, 14));
//        pnlContent.add(lblChange, BorderLayout.CENTER);
//
//        // OK button
//        JButton btnOK = new JButton("Đóng");
//        btnOK.addActionListener(e -> successDialog.dispose());
//        JPanel pnlButton = new JPanel(new FlowLayout(FlowLayout.CENTER));
//        pnlButton.add(btnOK);
//        pnlContent.add(pnlButton, BorderLayout.SOUTH);
//
//        successDialog.add(pnlContent);
//        successDialog.setVisible(true);
//    }

    private Icon createPaymentIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);

        // Vẽ biểu tượng tiền
        g2.setStroke(new BasicStroke(1.5f));

        // Vẽ đồng xu
        g2.drawOval(2, 2, width - 4, height - 4);

        // Vẽ ký hiệu đồng (₫)
        Font font = new Font("SansSerif", Font.BOLD, height - 6);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int x = (width - fm.stringWidth("₫")) / 2;
        int y = ((height - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString("₫", x, y);

        g2.dispose();
        return new ImageIcon(image);
    }

    private void lamMoi() {
        txtMaVe.setText("");
        txtTenKhachHang.setText("");
        txtGiayTo.setText("");
        txtNgayDi.setText("");
        cboDoiTuong.setSelectedIndex(0);

        lblLichTrinh.setText("Chưa chọn");
        lblChoNgoi.setText("Chưa chọn");

        lblTrangThai.setText("---");
        lblTrangThai.setForeground(Color.BLACK);
        lblGiaVe.setText("0 VNĐ");

        veTauHienTai = null;
        lichTrinhDaChon = null;
        choNgoiDaChon = null;
        khuyenMaiDaChon = null;

        setInputFieldsEnabled(false);
        btnDoiVe.setEnabled(false);
        btnChonLichTrinh.setEnabled(false);
        btnChonChoNgoi.setEnabled(false);
        updateStatus(READY_TEXT, false);
    }

    private void setInputFieldsEnabled(boolean enabled) {
        txtTenKhachHang.setEnabled(enabled);
        txtGiayTo.setEnabled(enabled);
        txtNgayDi.setEnabled(enabled);
        cboDoiTuong.setEnabled(enabled);
        btnChonLichTrinh.setEnabled(enabled);
        btnChonChoNgoi.setEnabled(enabled);
    }

    // ===== ICON CREATION METHODS =====

    /**
     * Tạo icon vé
     */
    private ImageIcon createTicketIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);

        // Vẽ hình chữ nhật cho vé
        g2.fillRoundRect(2, 4, width - 4, height - 8, 5, 5);

        // Vẽ đường đứt quãng cho vé
        g2.setColor(new Color(255, 255, 255, 180));
        float[] dash = {2f, 2f};
        g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10, dash, 0));
        g2.drawLine(2, height / 2, width - 2, height / 2);

        g2.dispose();
        return new ImageIcon(image);
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
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(2, 2, width - 8, height - 8);

        // Vẽ cán của kính lúp
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(width - 4, height - 4, width - 7, height - 7);

        g2.dispose();
        return new ImageIcon(image);
    }

    /**
     * Tạo icon đổi
     */
    private ImageIcon createExchangeIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);

        // Vẽ mũi tên đi lên
        int arrowWidth = width / 3;
        g2.setStroke(new BasicStroke(1.5f));

        // Mũi tên lên
        g2.drawLine(arrowWidth, height - 4, arrowWidth, 4);
        g2.drawLine(arrowWidth, 4, arrowWidth - 3, 7);
        g2.drawLine(arrowWidth, 4, arrowWidth + 3, 7);

        // Mũi tên xuống
        g2.drawLine(width - arrowWidth, 4, width - arrowWidth, height - 4);
        g2.drawLine(width - arrowWidth, height - 4, width - arrowWidth - 3, height - 7);
        g2.drawLine(width - arrowWidth, height - 4, width - arrowWidth + 3, height - 7);

        g2.dispose();
        return new ImageIcon(image);
    }

    /**
     * Tạo icon làm mới
     */
    private ImageIcon createRefreshIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(color);

        // Vẽ biểu tượng làm mới
        g2d.setStroke(new BasicStroke(width / 8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int margin = width / 6;
        g2d.drawArc(margin, margin, width - 2 * margin, height - 2 * margin, 0, 300);

        // Vẽ mũi tên
        int arrowSize = width / 4;
        g2d.fillPolygon(
                new int[] {width - margin, width - margin - arrowSize, width - margin},
                new int[] {margin, margin, margin + arrowSize},
                3
        );

        g2d.dispose();
        return new ImageIcon(image);
    }

    /**
     * Tạo icon thoát
     */
    private ImageIcon createExitIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);

        // Vẽ hình cửa
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRect(2, 2, width - 8, height - 4);

        // Vẽ mũi tên ra ngoài
        g2.drawLine(width - 4, height / 2, width - 10, height / 2);
        g2.drawLine(width - 7, height / 2 - 3, width - 4, height / 2);
        g2.drawLine(width - 7, height / 2 + 3, width - 4, height / 2);

        g2.dispose();
        return new ImageIcon(image);
    }

    /**
     * Tạo icon thành công
     */
    private ImageIcon createSuccessIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Vẽ hình tròn xanh
        g2d.setColor(new Color(0, 153, 0));
        g2d.fillOval(0, 0, width, height);

        // Vẽ dấu tick
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(width / 5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int[] xPoints = {width / 4, width / 2, width * 3 / 4};
        int[] yPoints = {height / 2, height * 3 / 4, height / 3};
        g2d.drawPolyline(xPoints, yPoints, 3);

        g2d.dispose();
        return new ImageIcon(image);
    }

    /**
     * Tạo icon thành công với nền tròn
     */
    private ImageIcon createSuccessTickIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Vẽ hình tròn nền
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 50));
        g2.fillOval(0, 0, width, height);

        // Vẽ viền tròn
        g2.setColor(color);
        g2.setStroke(new BasicStroke(width / 16f));
        g2.drawOval(width / 10, height / 10, width - width / 5, height - height / 5);

        // Vẽ dấu tích
        g2.setColor(color);
        g2.setStroke(new BasicStroke(width / 12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int x1 = width / 4;
        int y1 = height / 2;
        int x2 = width / 2 - width / 12;
        int y2 = height - height / 3;
        int x3 = width - width / 3;
        int y3 = height / 3;
        g2.drawLine(x1, y1, x2, y2);
        g2.drawLine(x2, y2, x3, y3);

        g2.dispose();
        return new ImageIcon(image);
    }

    /**
     * Tạo icon loading
     */
    // Các phương thức tạo icon
    private ImageIcon createLoadingIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(200, 200, 200));

        int margin = width / 10;
        g2d.setStroke(new BasicStroke(width / 10f));
        g2d.drawOval(margin, margin, width - 2 * margin, height - 2 * margin);

        g2d.setColor(new Color(0, 153, 204));
        g2d.drawArc(margin, margin, width - 2 * margin, height - 2 * margin, 0, 270);

        g2d.dispose();
        return new ImageIcon(image);
    }

    /**
     * Tạo icon lỗi
     */
    private ImageIcon createErrorIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Vẽ hình tròn đỏ
        g2d.setColor(Color.RED);
        g2d.fillOval(0, 0, width, height);

        // Vẽ dấu X
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(width / 5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(width / 3, height / 3, width * 2 / 3, height * 2 / 3);
        g2d.drawLine(width * 2 / 3, height / 3, width / 3, height * 2 / 3);

        g2d.dispose();
        return new ImageIcon(image);
    }

    /**
     * Tạo icon thông tin
     */
    private ImageIcon createInfoIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);

        // Vẽ hình tròn viền
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(2, 2, width - 4, height - 4);

        // Vẽ chữ i
        g2.setFont(new Font("SansSerif", Font.BOLD, height - 6));
        FontMetrics fm = g2.getFontMetrics();
        int x = (width - fm.charWidth('i')) / 2;
        int y = (height + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString("i", x, y);

        g2.dispose();
        return new ImageIcon(image);
    }

    /**
     * Tạo icon dấu tích
     */
    private ImageIcon createCheckIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Vẽ dấu tích
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int x1 = width / 4;
        int y1 = height / 2;
        int x2 = width / 2 - 1;
        int y2 = height - height / 4;
        int x3 = width - width / 4;
        int y3 = height / 4;
        g2.drawLine(x1, y1, x2, y2);
        g2.drawLine(x2, y2, x3, y3);

        g2.dispose();
        return new ImageIcon(image);
    }

    /**
     * Tạo icon hủy (X)
     */
    private ImageIcon createCancelIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Vẽ dấu X
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(4, 4, width - 4, height - 4);
        g2.drawLine(width - 4, 4, 4, height - 4);

        g2.dispose();
        return new ImageIcon(image);
    }

    /**
     * Tạo icon đang chờ
     */
    private ImageIcon createPendingIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Vẽ hình tròn đồng hồ
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(2, 2, width - 4, height - 4);

        // Vẽ kim đồng hồ
        g2.drawLine(width / 2, height / 2, width / 2, 4);
        g2.drawLine(width / 2, height / 2, width - 4, height / 2);

        g2.dispose();
        return new ImageIcon(image);
    }

    /**
     * Tạo icon lịch (cho nút chọn lịch trình)
     */
    private ImageIcon createCalendarIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Vẽ hình chữ nhật calendar
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRect(2, 4, width - 4, height - 6);

        // Vẽ phần trên của calendar
        g2.drawLine(width / 4, 2, width / 4, 6);
        g2.drawLine(width * 3 / 4, 2, width * 3 / 4, 6);

        // Vẽ các dòng bên trong
        g2.drawLine(2, height / 3 + 2, width - 2, height / 3 + 2);

        // Vẽ dấu X đánh dấu ngày
        int centerX = width / 2;
        int centerY = (height / 3 + 2 + height) / 2;
        g2.drawLine(centerX - 2, centerY - 2, centerX + 2, centerY + 2);
        g2.drawLine(centerX + 2, centerY - 2, centerX - 2, centerY + 2);

        g2.dispose();
        return new ImageIcon(image);
    }

    /**
     * Tạo icon ghế (cho nút chọn chỗ ngồi)
     */
    private ImageIcon createSeatIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Vẽ hình ghế ngồi
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1f));

        // Phần ngồi của ghế
        g2.fillRect(2, height / 2, width - 4, height / 3);

        // Phần lưng ghế
        g2.fillRect(4, 3, width - 8, height / 2);

        // Phần chân ghế
        g2.drawLine(4, height - 2, 6, height / 2 + height / 3);
        g2.drawLine(width - 4, height - 2, width - 6, height / 2 + height / 3);

        g2.dispose();
        return new ImageIcon(image);
    }
    private boolean xuLyThanhToan() throws RemoteException {
        try {
            // 1. Tìm khách hàng từ mã vé
            KhachHang khachHang = doiVeDAO.getKhachHangByMaVe(veTauHienTai.getMaVe());
            if (khachHang == null) {
                throw new Exception("Không tìm thấy thông tin khách hàng!");
            }
            System.out.println("Đã tìm thấy KhachHang: " + khachHang.getMaKhachHang());

            // 2. Tạo hóa đơn mới
            HoaDon hoaDon = new HoaDon();
            String maHD = generateMaHD();
            System.out.println("Generated MaHD: " + maHD);
            hoaDon.setMaHD(maHD);
            hoaDon.setNgayLap(LocalDateTime.now());
            hoaDon.setTienGiam(giaVeBanDau - veTauHienTai.getGiaVe());
            hoaDon.setTongTien(veTauHienTai.getGiaVe());
            hoaDon.setKhachHang(khachHang);

            // Debugging the NhanVien reference
            if (nhanVienPanel == null) {
                System.err.println("ERROR: nhanVienPanel is null");
                throw new Exception("Thiếu thông tin nhân viên!");
            }
            System.out.println("NhanVien info: " + nhanVienPanel.getClass().getName());
            hoaDon.setNv(nhanVienPanel);

            // Get LoaiHoaDon and verify it exists
            LoaiHoaDon loaiHoaDon = loaiHoaDonDAO.findById("LHD001");
            if (loaiHoaDon == null) {
                System.err.println("ERROR: Không tìm thấy loại hóa đơn LHD001");
                throw new Exception("Không tìm thấy loại hóa đơn!");
            }
            System.out.println("Found LoaiHoaDon: " + loaiHoaDon.getMaLoaiHoaDon());
            hoaDon.setLoaiHoaDon(loaiHoaDon);

            // 3. Lưu hóa đơn
            System.out.println("Attempting to save HoaDon...");
            boolean savedHoaDon = hoaDonDAO.saveHoaDon(hoaDon);
            if (!savedHoaDon) {
                System.err.println("Failed to save HoaDon!");
                throw new Exception("Không thể lưu hóa đơn!");
            }
            System.out.println("HoaDon saved successfully!");

            // 4. Tạo chi tiết hóa đơn mới
            ChiTietHoaDon chiTietHoaDon = new ChiTietHoaDon();

            // Tạo ID cho chi tiết hóa đơn
            ChiTietHoaDonId chiTietId = new ChiTietHoaDonId();
            chiTietId.setMaHD(maHD);
            chiTietId.setMaVe(veTauHienTai.getMaVe());
            chiTietHoaDon.setId(chiTietId);

            // Thiết lập các tham chiếu
            chiTietHoaDon.setHoaDon(hoaDon);
            chiTietHoaDon.setVeTau(veTauHienTai);

            // Thiết lập các giá trị tài chính
            double vat = 0.1; // VAT 8% - Điều chỉnh theo quy định của bạn
            chiTietHoaDon.setSoLuong(1); // Mỗi vé được tính là 1 đơn vị
            chiTietHoaDon.setVAT(vat);

            // Tính toán thành tiền và tiền thuế
            double thanhTien = veTauHienTai.getGiaVe(); // Giá vé sau khi đã giảm giá
            double tienThue = thanhTien * vat;

            chiTietHoaDon.setThanhTien(thanhTien);
            chiTietHoaDon.setTienThue(tienThue);

            // 5. Lưu chi tiết hóa đơn
            boolean savedChiTiet = chiTietHoaDonDAO.save(chiTietHoaDon);
            if (!savedChiTiet) {
                // Xóa hóa đơn đã tạo nếu không thể lưu chi tiết
                // hoaDonDAO.delete(maHD); // Giả định có phương thức delete
                throw new Exception("Không thể lưu chi tiết hóa đơn!");
            }

            System.out.println("Đã tìm thấy KhachHang: " + khachHang.getMaKhachHang());
            System.out.println("Generated MaHD: " + maHD);
            System.out.println("NhanVien info: " + nhanVienPanel.getClass().getName());
            System.out.println("Found LoaiHoaDon: " + loaiHoaDon.getMaLoaiHoaDon());
            System.out.println("HoaDon saved successfully!");
            System.out.println("ChiTietHoaDon saved successfully!");

            return true;

        } catch (Exception e) {
            System.err.println("Error in xuLyThanhToan: " + e.getMessage());
            e.printStackTrace();
            throw new RemoteException("Lỗi khi xử lý thanh toán: " + e.getMessage(), e);
        }
    }

    private String generateMaHD() {
        // Format: HD + yyyyMMdd + 4 số random
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String datePart = sdf.format(new Date());
        String randomPart = String.format("%04d", new Random().nextInt(10000));
        return "HD" + datePart + randomPart;
    }
}