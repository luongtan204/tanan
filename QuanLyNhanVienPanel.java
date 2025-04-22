package guiClient;

import com.toedter.calendar.JDateChooser;
import dao.*;
import dao.impl.NhanVienDAOImpl;
import lombok.SneakyThrows;
import model.NhanVien;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static guiClient.IconFactory.createExchangeIcon;

public class QuanLyNhanVienPanel extends JPanel implements ActionListener {

    private ArrayList<NhanVien> danhSachNhanVien = new ArrayList<>();
    private JTextField txtMaNV, txtTenNV, txtSoDT, txtCCCD, txtDiaChi;
    private JButton btnTaiAnh;
    private String tenFileAnhDuocChon = "";

    private JComboBox<String> cmbChucVu;
    private JLabel lblAnh;
    private static final int AVATAR_WIDTH = 200;
    private static final int AVATAR_HEIGHT = 200;
    private JLabel lblNhanVienDangChon = null;
    private JDateChooser dateChooserNgayVaoLam;
    private NhanVienDAO nhanVienDAO = new NhanVienDAOImpl();
    private JButton btnThem;
    private JButton btnSua;
    private JButton btnLamMoi;
    private JButton btnLuu;
    private JPanel danhSachPanel;
    private JButton btnXoaNV;
    private boolean isEditMode = false;
    private NhanVien nhanVienDangSua; // thêm biến này ở lớp chứa actionPerformed

    // Màu sắc chính
    private Color primaryColor = new Color(41, 128, 185); // Màu xanh dương
    private Color successColor = new Color(46, 204, 113); // Màu xanh lá
    private Color warningColor = new Color(243, 156, 18); // Màu vàng cam
    private Color dangerColor = new Color(231, 76, 60);   // Màu đỏ
    private Color grayColor = new Color(108, 117, 125);   // Màu xám
    private Color darkTextColor = new Color(52, 73, 94);  // Màu chữ tối
    private Color lightBackground = new Color(240, 240, 240); // Màu nền nhạt
    private static final String RMI_SERVER_IP = "127.0.0.1";
    private static final int RMI_SERVER_PORT = 9090;
    public QuanLyNhanVienPanel() throws RemoteException {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        initUI();
        connectToServer(); // Gọi connectToServer để thiết lập kết nối và load dữ liệu
    }

    private void connectToServer() {
        try {
            Registry registry = LocateRegistry.getRegistry(RMI_SERVER_IP, RMI_SERVER_PORT);
            nhanVienDAO = (NhanVienDAO) registry.lookup("nhanVienDAO");
            // Thông báo kết nối thành công sau khi lookup thành công
//            JOptionPane.showMessageDialog(this,
//                    "Kết nối đến server RMI thành công!",
//                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            // Lấy dữ liệu sau khi lookup thành công (coi như kết nối RMI thành công)
            try {
                List<NhanVien> dataFromServer = nhanVienDAO.getAllNhanVien();
                if (dataFromServer != null) {
                    danhSachNhanVien.addAll(dataFromServer);
                }
                // Thông báo kết nối và tải dữ liệu thành công (tùy chọn)
                // JOptionPane.showMessageDialog(this, "Đã tải dữ liệu nhân viên.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (RemoteException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi khi tải danh sách nhân viên từ server!", "Lỗi Dữ Liệu", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
                danhSachNhanVien.clear(); // Đảm bảo danh sách rỗng nếu có lỗi tải
            }
            taiLaiDanhSachNhanVien(); // Cập nhật giao diện sau khi cố gắng lấy dữ liệu

        } catch (RemoteException | NotBoundException e) {
            // Không hiển thị thông báo lỗi kết nối ở đây
            JOptionPane.showMessageDialog(this,
                    "Không thể kết nối đến server RMI: " + e.getMessage(),
                    "Lỗi Kết Nối", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            danhSachNhanVien.clear(); // Đảm bảo danh sách rỗng nếu không kết nối được
            taiLaiDanhSachNhanVien(); // Cập nhật giao diện với danh sách rỗng
        }
    }

    private void initUI() {

        Font labelFont = new Font("Arial", Font.BOLD, 16);

        Font textFont = new Font("Arial", Font.PLAIN, 12);

        Font btnFont = new Font("Arial", Font.PLAIN, 16);



// DANH SÁCH BÊN TRÁI

        danhSachPanel = new JPanel();

        danhSachPanel.setLayout(new BoxLayout(danhSachPanel, BoxLayout.Y_AXIS));

        danhSachPanel.setBackground(Color.WHITE);



        for (NhanVien nv : danhSachNhanVien) {

            JPanel panelNhanVien = new JPanel(new BorderLayout());

            panelNhanVien.setBackground(Color.WHITE);

            panelNhanVien.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));

            panelNhanVien.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));



            JLabel lblNV = new JLabel(nv.getMaNV());

            lblNV.setFont(textFont);

            lblNV.setOpaque(true);

            lblNV.setBackground(Color.WHITE);

            lblNV.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

            lblNV.setCursor(new Cursor(Cursor.HAND_CURSOR));



            lblNV.addMouseListener(new MouseAdapter() {

                Color originalBg = lblNV.getBackground();



                @Override

                public void mouseEntered(MouseEvent e) {

                    if (lblNV != lblNhanVienDangChon) {

                        lblNV.setBackground(new Color(220, 220, 220));

                    }

                }



                @Override

                public void mouseExited(MouseEvent e) {

                    if (lblNV != lblNhanVienDangChon) {

                        lblNV.setBackground(Color.WHITE);

                    }

                }



                @Override

                public void mouseClicked(MouseEvent e) {

                    hienThiThongTinNhanVien(nv);

                    if (lblNhanVienDangChon != null) {

                        lblNhanVienDangChon.setBackground(Color.WHITE);

                    }

                    lblNV.setBackground(new Color(173, 216, 230));

                    lblNhanVienDangChon = lblNV;

                }

            });



            panelNhanVien.add(lblNV, BorderLayout.CENTER);



            JButton btnXoaNV = new JButton("Xóa");

            styleButton(btnXoaNV, dangerColor, Color.WHITE, createDeleteIcon(10, 10, Color.WHITE));



            btnXoaNV.setActionCommand(nv.getMaNV());

            btnXoaNV.addActionListener(e -> {

                String maNVCanXoa = e.getActionCommand();

                int option = JOptionPane.showConfirmDialog(QuanLyNhanVienPanel.this, "Bạn có chắc chắn muốn xóa nhân viên có mã " + maNVCanXoa + "?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION);

                if (option == JOptionPane.YES_OPTION) {

                    boolean isDeleted = false;

                    try {

                        isDeleted = nhanVienDAO.delete(maNVCanXoa);

                    } catch (RemoteException ex) {

                        throw new RuntimeException(ex);

                    }

                    if (isDeleted) {

                        danhSachNhanVien.removeIf(nhanVien -> nhanVien.getMaNV().equals(maNVCanXoa));

                        for (Component comp : danhSachPanel.getComponents()) {

                            if (comp instanceof JPanel) {

                                JPanel panel = (JPanel) comp;

                                if (panel.getComponentCount() > 1) {

                                    JLabel lbl = (JLabel) panel.getComponent(0);

                                    if (lbl.getText().equals(maNVCanXoa)) {

                                        danhSachPanel.remove(panel);

                                        danhSachPanel.revalidate();

                                        danhSachPanel.repaint();

                                        break;

                                    }

                                }

                            }

                        }

                        JOptionPane.showMessageDialog(QuanLyNhanVienPanel.this, "Nhân viên có mã " + maNVCanXoa + " đã được xóa.");

                        if (lblNhanVienDangChon != null && lblNhanVienDangChon.getText().equals(maNVCanXoa)) {

                            clearThongTinNhanVien();

                            lblNhanVienDangChon = null;

                        }

                    } else {

                        JOptionPane.showMessageDialog(QuanLyNhanVienPanel.this, "Xóa nhân viên không thành công.");

                    }

                }

            });



            panelNhanVien.add(btnXoaNV, BorderLayout.EAST);

            danhSachPanel.add(panelNhanVien);

        }



        JScrollPane scrollPane = new JScrollPane(danhSachPanel);

        scrollPane.setPreferredSize(new Dimension(300, 0));

        add(scrollPane, BorderLayout.WEST);



// CHI TIẾT BÊN PHẢI

        JPanel chiTietPanel = new JPanel(new GridBagLayout());

        chiTietPanel.setBackground(new Color(245, 248, 255));

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(5, 10, 5, 10);

        gbc.anchor = GridBagConstraints.WEST;

        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.weightx = 1.0;



// TITLE

        gbc.gridx = 0;

        gbc.gridy = 0;

        gbc.gridwidth = 2;

        gbc.anchor = GridBagConstraints.CENTER;

        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.weighty = 0;

        gbc.insets = new Insets(0, 0, 5, 0);



        JPanel titlePanel = new JPanel(new BorderLayout());

        titlePanel.setBackground(new Color(41, 128, 185));

        JLabel lblTitle = new JLabel("Quản lý nhân viên");

        lblTitle.setFont(new Font("Arial", Font.BOLD, 20));

        lblTitle.setForeground(Color.WHITE);

        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);

        titlePanel.add(lblTitle,BorderLayout.CENTER);

        chiTietPanel.add(titlePanel, gbc);

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

// AVATAR (Cập nhật để đảm bảo JLabel không thay đổi kích thước khi ảnh được tải lên)

        gbc.gridy = 1;

        gbc.fill = GridBagConstraints.NONE;

        gbc.insets = new Insets(5, 0, 10, 0);

        lblAnh = new JLabel();

        lblAnh.setPreferredSize(new Dimension(AVATAR_WIDTH, AVATAR_HEIGHT)); // Kích thước cố định cho JLabel

        lblAnh.setHorizontalAlignment(SwingConstants.CENTER);

        lblAnh.setVerticalAlignment(SwingConstants.TOP);

        lblAnh.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        chiTietPanel.add(lblAnh, gbc);



// CÁC TRƯỜNG THÔNG TIN

        gbc.gridwidth = 1;

        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.weighty = 0;

        gbc.insets = new Insets(5, 10, 5, 10);



        String[] labels = {"Mã NV:", "Tên NV:", "SĐT:", "CCCD:", "Địa chỉ:", "Ngày vào:", "Chức vụ:", "Avatar:"};

        JComponent[] fields = {

                taoTextField(textFont, 200),

                taoTextField(textFont, 200),

                taoTextField(textFont, 200),

                taoTextField(textFont, 200),

                taoTextField(textFont, 200),

                taoDateChooser(),

                taoComboBox(textFont),

                taoButtonTaiAnh(textFont)

        };



        for (int i = 0; i < labels.length; i++) {

            gbc.gridx = 0;

            gbc.gridy = i + 2;

            JLabel label = new JLabel(labels[i]);

            label.setFont(labelFont);

            label.setForeground(Color.DARK_GRAY);

            chiTietPanel.add(label, gbc);



            gbc.gridx = 1;

            gbc.insets = new Insets(5, 5, 5, 10);

            chiTietPanel.add(fields[i], gbc);

        }



        txtMaNV = (JTextField) fields[0];

        txtTenNV = (JTextField) fields[1];

        txtSoDT = (JTextField) fields[2];

        txtCCCD = (JTextField) fields[3];

        txtDiaChi = (JTextField) fields[4];

        dateChooserNgayVaoLam = (JDateChooser) fields[5];

        cmbChucVu = (JComboBox<String>) fields[6];

        btnTaiAnh = (JButton) fields[7];



        txtMaNV.addActionListener(e -> {

            String maTim = txtMaNV.getText().trim();

            if (maTim.isEmpty()) return;



            NhanVien nvTim = null;

            for (NhanVien nv : danhSachNhanVien) {

                if (nv.getMaNV().equalsIgnoreCase(maTim)) {

                    nvTim = nv;

                    break;

                }

            }



            if (nvTim != null) {

                hienThiThongTinNhanVien(nvTim);

                if (lblNhanVienDangChon != null) {

                    lblNhanVienDangChon.setBackground(Color.WHITE);

                }

                for (Component comp : danhSachPanel.getComponents()) {

                    if (comp instanceof JPanel) {

                        JPanel panelNhanVien = (JPanel) comp;

                        if (panelNhanVien.getComponentCount() > 0) {

                            JLabel lbl = (JLabel) panelNhanVien.getComponent(0);

                            if (lbl.getText().equalsIgnoreCase(maTim)) {

                                lbl.dispatchEvent(new MouseEvent(lbl, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 0, 0, 1, false));

                                break;

                            }

                        }

                    }

                }

            } else {

                JOptionPane.showMessageDialog(this, "Không tìm thấy mã nhân viên: " + maTim);

            }

        });



// BUTTONS

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 50));

        btnThem = new JButton("Thêm");

        styleButton(btnThem, successColor, Color.WHITE, createAddIcon(16, 16, Color.WHITE));

        btnSua = new JButton("Sửa");

        styleButton(btnSua, warningColor, Color.WHITE, createEditIcon(16, 16, Color.WHITE));

        btnLamMoi = new JButton("Làm mới");

        styleButton(btnLamMoi, grayColor, Color.WHITE, createRefreshIcon(16, 16, Color.WHITE));

        btnLuu = new JButton("Lưu");

        styleButton(btnLuu, primaryColor, Color.WHITE, createSaveIcon(16, 16, Color.WHITE));





        btnSua.addActionListener(e -> {

            String ma = txtMaNV.getText().trim();

            if (ma.isEmpty()) {

                JOptionPane.showMessageDialog(this, "Vui lòng chọn nhân viên hợp lệ!");

                return;

            }



            for (NhanVien nv : danhSachNhanVien) {

                if (nv.getMaNV().equals(ma)) {

                    nhanVienDangSua = nv;

                    isEditMode = true;

                    setEditableFields(true);

                    txtMaNV.setEditable(false);

                    break;

                }

            }



            if (nhanVienDangSua == null) {

                JOptionPane.showMessageDialog(this, "Không tìm thấy nhân viên cần sửa!");

            }

        });



        btnLuu.addActionListener(this);

        btnThem.addActionListener(this);

        btnLamMoi.addActionListener(this);



        buttonPanel.add(btnThem);

        buttonPanel.add(btnSua);

        buttonPanel.add(btnLamMoi);

        buttonPanel.add(btnLuu);



        gbc.gridx = 0;

        gbc.gridy = labels.length + 2;

        gbc.gridwidth = 2;

        gbc.fill = GridBagConstraints.NONE;

        gbc.anchor = GridBagConstraints.CENTER;

        gbc.insets = new Insets(20, 0, 10, 0);

        chiTietPanel.add(buttonPanel, gbc);



        add(chiTietPanel, BorderLayout.CENTER);

    }


    private ImageIcon createRefreshIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);

        // Vẽ hình tròn cho nút refresh
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawArc(3, 3, width - 6, height - 6, 45, 270);

        // Vẽ mũi tên
        g2.fillPolygon(
                new int[]{width - 3, width - 7, width},
                new int[]{3, 7, 7},
                3
        );

        g2.dispose();
        return new ImageIcon(image);
    }
    public  ImageIcon createAddIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(3f));

        // Vẽ dấu cộng
        int centerX = width / 2;
        int centerY = height / 2;
        int size = Math.min(width, height) / 4;
        g2.drawLine(centerX - size, centerY, centerX + size, centerY); // Horizontal line
        g2.drawLine(centerX, centerY - size, centerX, centerY + size); // Vertical line

        g2.dispose();
        return new ImageIcon(image);
    }
    public  ImageIcon createEditIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2f));

        // Vẽ cây viết
        int[] xPoints = {width / 3, width / 2, width - 5};
        int[] yPoints = {height / 2, height / 3, height / 2};
        g2.fillPolygon(xPoints, yPoints, 3); // Vẽ ngòi bút (hình tam giác)

        // Vẽ thân bút
        g2.setStroke(new BasicStroke(3f));
        g2.drawLine(width / 2, height / 3, width / 2, height); // Vẽ thân bút

        g2.dispose();
        return new ImageIcon(image);
    }
    public  ImageIcon createSaveIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2f));

        // Vẽ hình giống như import
        int[] xPoints1 = {5, width - 5, width - 5};
        int[] yPoints1 = {height / 2, height / 2, height - 5};
        g2.fillPolygon(xPoints1, yPoints1, 3); // Vẽ mũi tên dưới

        g2.setStroke(new BasicStroke(3f));
        g2.drawLine(width / 2, 5, width / 2, height / 2); // Vẽ đường lên trên

        g2.dispose();
        return new ImageIcon(image);
    }
    public  ImageIcon createDeleteIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(4f)); // Tăng độ dày của đường vẽ

        // Vẽ dấu X
        g2.drawLine(5, 5, width - 5, height - 5);  // Vẽ đường chéo trái sang phải
        g2.drawLine(width - 5, 5, 5, height - 5);  // Vẽ đường chéo phải sang trái

        g2.dispose();
        return new ImageIcon(image);
    }
    public static ImageIcon createExportIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(3f));

        // Vẽ mũi tên hướng lên
        int arrowWidth = width / 3;
        int arrowHeight = height / 2;

        // Vẽ đường ngang
        g2.drawLine(5, height - 5, width - 5, height - 5);

        // Vẽ mũi tên hướng lên
        g2.drawLine(width / 2, 5, width / 2, height - 5); // Đoạn thẳng đứng
        g2.drawLine(width / 2 - 5, 15, width / 2 + 5, 15); // Đoạn mũi tên

        g2.dispose();
        return new ImageIcon(image);
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



    @Override
    public void actionPerformed(ActionEvent e) {
        Object o = e.getSource();

        if (o == btnThem) {
            String maNV = generateMaNV();
            LocalDate ngayVaoLam = LocalDate.now();
            txtMaNV.setText(maNV);
            txtTenNV.setText("");
            txtSoDT.setText("");
            txtCCCD.setText("");
            txtDiaChi.setText("");
            dateChooserNgayVaoLam.setDate(java.sql.Date.valueOf(ngayVaoLam));
            cmbChucVu.setSelectedIndex(0);
            lblAnh.setIcon(null);
            setEditableFields(true);
            txtMaNV.setEditable(false);
        } else if (o == btnLamMoi) {
            clearThongTinNhanVien();
            taiLaiDanhSachNhanVien();
            txtMaNV.setEnabled(true);
            setEditableFields(true);
            isEditMode = false;
        }
        else if (o == btnLuu) {
            if (isValidInput()) {
                NhanVien nv;
                if (isEditMode && nhanVienDangSua != null) {
                    // Nếu đang trong chế độ sửa, dùng lại đối tượng cũ
                    nv = nhanVienDangSua;
                } else {
                    // Nếu đang thêm mới
                    nv = new NhanVien();
                    nv.setMaNV(txtMaNV.getText());
                }

                nv.setTenNV(txtTenNV.getText());
                nv.setSoDT(txtSoDT.getText());
                nv.setCccd(txtCCCD.getText());
                nv.setDiaChi(txtDiaChi.getText());

                java.util.Date selectedDate = dateChooserNgayVaoLam.getDate();
                if (selectedDate != null) {
                    nv.setNgayVaoLam(selectedDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
                } else {
                    JOptionPane.showMessageDialog(this, "Vui lòng chọn ngày vào làm.");
                    return;
                }

                nv.setChucVu(cmbChucVu.getSelectedItem().toString());
                nv.setAvata(tenFileAnhDuocChon);
                nv.setTrangThai("Hoạt động");

                boolean result;

                if (isEditMode) {
                    try {
                        result = nhanVienDAO.update(nv);
                    } catch (RemoteException ex) {
                        throw new RuntimeException(ex);
                    }
                    if (result) {
                        JOptionPane.showMessageDialog(this, "Cập nhật nhân viên thành công!");
                        taiLaiDanhSachNhanVien();
                        isEditMode = false;
                        nhanVienDangSua = null;
                        clearThongTinNhanVien();
                    } else {
                        JOptionPane.showMessageDialog(this, "Cập nhật nhân viên thất bại!");
                    }
                } else {
                    try {
                        result = nhanVienDAO.save(nv);
                    } catch (RemoteException ex) {
                        throw new RuntimeException(ex);
                    }
                    if (result) {
                        JOptionPane.showMessageDialog(this, "Lưu nhân viên thành công!");
                        danhSachNhanVien.add(nv);
                        taiLaiDanhSachNhanVien();
                        clearThongTinNhanVien();
                    } else {
                        JOptionPane.showMessageDialog(this, "Lưu nhân viên không thành công!");
                    }
                }

            } else {
                JOptionPane.showMessageDialog(this, "Vui lòng điền đầy đủ thông tin!");
            }


        }

        else if (o == btnTaiAnh) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Chọn ảnh đại diện");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                java.io.File file = fileChooser.getSelectedFile();
                tenFileAnhDuocChon = file.getName();
                try {
                    java.nio.file.Path source = file.toPath();
                    java.nio.file.Path destination = java.nio.file.Paths.get("src/main/resources/Anh_HeThong/" + tenFileAnhDuocChon);
                    java.nio.file.Files.copy(source, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    ImageIcon icon = new ImageIcon(destination.toString());
                    Image scaledImage = icon.getImage().getScaledInstance(AVATAR_WIDTH, AVATAR_HEIGHT, Image.SCALE_SMOOTH);
                    lblAnh.setIcon(new ImageIcon(scaledImage));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Lỗi khi tải ảnh: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }
    }

    private void clearThongTinNhanVien() {
        txtMaNV.setText("");
        txtTenNV.setText("");
        txtSoDT.setText("");
        txtCCCD.setText("");
        txtDiaChi.setText("");
        dateChooserNgayVaoLam.setDate(null);
        cmbChucVu.setSelectedIndex(0);
        lblAnh.setIcon(null);
        txtMaNV.setEnabled(true);
        setEditableFields(true);
    }

    private JTextField taoTextField(Font font, int width) {
        JTextField tf = new JTextField();
        tf.setPreferredSize(new Dimension(width, 30));
        tf.setFont(font);
        return tf;
    }

    private JComboBox<String> taoComboBox(Font font) {
        JComboBox<String> cb = new JComboBox<>(new String[]{"Nhân viên", "Quản lý", "Trưởng phòng"});
        cb.setFont(font);
        cb.setPreferredSize(new Dimension(200, 30));
        return cb;
    }

    private JDateChooser taoDateChooser() {
        JDateChooser dateChooser = new JDateChooser();
        dateChooser.setFont(new Font("Arial", Font.PLAIN, 16));
        dateChooser.setPreferredSize(new Dimension(200, 30));
        return dateChooser;
    }



    private void hienThiThongTinNhanVien(NhanVien nv) {
        txtMaNV.setText(nv.getMaNV());
        txtTenNV.setText(nv.getTenNV());
        txtSoDT.setText(nv.getSoDT());
        txtCCCD.setText(nv.getCccd());
        txtDiaChi.setText(nv.getDiaChi());

        // Chuyển đổi LocalDate sang java.util.Date cho JDateChooser
        LocalDate ngayVaoLam = nv.getNgayVaoLam();
        if (ngayVaoLam != null) {
            dateChooserNgayVaoLam.setDate(java.sql.Date.valueOf(ngayVaoLam));
        } else {
            dateChooserNgayVaoLam.setDate(null); // Hoặc xử lý trường hợp null theo nhu cầu
        }

        cmbChucVu.setSelectedItem(nv.getChucVu());
        tenFileAnhDuocChon = nv.getAvata();

        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/Anh_HeThong/" + nv.getAvata()));
            Image scaledImage = icon.getImage().getScaledInstance(AVATAR_WIDTH, AVATAR_HEIGHT, Image.SCALE_SMOOTH);
            lblAnh.setIcon(new ImageIcon(scaledImage));
        } catch (Exception e) {
            lblAnh.setIcon(null);
            System.err.println("Không load được ảnh: " + e.getMessage());
        }

        setEditableFields(false); // Ngăn chỉnh sửa sau khi hiển thị
    }

    private void setEditableFields(boolean isEditable) {
        txtTenNV.setEditable(isEditable);
        txtSoDT.setEditable(isEditable);
        txtCCCD.setEditable(isEditable);
        txtDiaChi.setEditable(isEditable);
        dateChooserNgayVaoLam.setEnabled(false); // Ngày vào làm không chỉnh sửa
//        txtAvata.setEditable(isEditable);
        cmbChucVu.setEnabled(isEditable);

        // Mã nhân viên sẽ chỉ không cho sửa khi đã hiển thị thông tin
        txtMaNV.setEditable(isEditable);  // Đảm bảo rằng khi làm mới, mã có thể sửa
    }

    private JButton taoButtonTaiAnh(Font font) {
        JButton button = new JButton("Tải ảnh lên");
        styleButton(button, darkTextColor, Color.WHITE, createExportIcon(20, 10, Color.WHITE));
        button.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Chọn ảnh đại diện");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                java.io.File file = fileChooser.getSelectedFile();
                tenFileAnhDuocChon = file.getName();

                // Copy ảnh vào thư mục "Anh_HeThong"
                try {
                    java.nio.file.Path source = file.toPath();
                    java.nio.file.Path destination = java.nio.file.Paths.get("src/main/resources/Anh_HeThong/" + tenFileAnhDuocChon);
                    java.nio.file.Files.copy(source, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                    // Hiển thị ảnh vừa tải lên
                    ImageIcon icon = new ImageIcon(destination.toString());
                    Image scaledImage = icon.getImage().getScaledInstance(AVATAR_WIDTH, AVATAR_HEIGHT, Image.SCALE_SMOOTH);
                    lblAnh.setIcon(new ImageIcon(scaledImage));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Lỗi khi tải ảnh: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });
        return button;
    }

    private String generateMaNV() {
        // Tạo mã nhân viên tự động theo cú pháp NVXXXX
        int nextId = danhSachNhanVien.size() + 1;
        return String.format("NV%04d", nextId);
    }

    private boolean isValidInput() {
        // Kiểm tra tính hợp lệ của các trường thông tin
        return !txtTenNV.getText().isEmpty() && !txtSoDT.getText().isEmpty() && !txtCCCD.getText().isEmpty() && !txtDiaChi.getText().isEmpty();
    }

//    @SneakyThrows
    public void taiLaiDanhSachNhanVien() {
        danhSachPanel.removeAll();

        Font textFont = new Font("Arial", Font.PLAIN, 16);

        for (NhanVien nv : danhSachNhanVien) {
            JPanel panelNhanVien = new JPanel(new BorderLayout());
            panelNhanVien.setBackground(Color.WHITE);
            panelNhanVien.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
            panelNhanVien.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

            JLabel lblNV = new JLabel(nv.getMaNV());
            lblNV.setFont(textFont);
            lblNV.setOpaque(true);
            lblNV.setBackground(Color.WHITE);
            lblNV.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
            lblNV.setCursor(new Cursor(Cursor.HAND_CURSOR));

            lblNV.addMouseListener(new MouseAdapter() {
                Color originalBg = lblNV.getBackground();

                @Override
                public void mouseEntered(MouseEvent e) {
                    if (lblNV != lblNhanVienDangChon) {
                        lblNV.setBackground(new Color(220, 220, 220));
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (lblNV != lblNhanVienDangChon) {
                        lblNV.setBackground(Color.WHITE);
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    hienThiThongTinNhanVien(nv);
                    if (lblNhanVienDangChon != null) {
                        lblNhanVienDangChon.setBackground(Color.WHITE);
                    }
                    lblNV.setBackground(new Color(173, 216, 230));
                    lblNhanVienDangChon = lblNV;
                }
            });

            panelNhanVien.add(lblNV, BorderLayout.CENTER);

            JButton btnXoaNV = new JButton("Xóa");
            styleButton(btnXoaNV, dangerColor, Color.WHITE, createDeleteIcon(10, 10, Color.WHITE));
            btnXoaNV.setActionCommand(nv.getMaNV());
            btnXoaNV.addActionListener(e -> {
                // ... (ActionListener cho nút xóa giữ nguyên)
            });

            panelNhanVien.add(btnXoaNV, BorderLayout.EAST);
            danhSachPanel.add(panelNhanVien);
        }

        danhSachPanel.revalidate();
        danhSachPanel.repaint();
    }




}
