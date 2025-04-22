package guiClient;

import dao.LichLamViecDAO;
import dao.NhanVienDAO;
import dao.TaiKhoanDAO;
import dao.impl.LichLamViecDAOImpl;
import dao.impl.NhanVienDAOImpl;
import dao.impl.TaiKhoanDAOImpl;
//import model.EmailSender;
import model.LichLamViec;
import model.NhanVien;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import javax.imageio.ImageIO;

public class FrmDangNhap extends JFrame implements ActionListener {
    private JPanel mainPanel;
    private JLabel backgroundLabel;
    private JTextField txtMaNhanVien;
    private JPasswordField txtMatKhau;
    private JTextField txtMatKhauVisible; // Mã mật khẩu hiển thị dưới dạng text
    private JButton btnQuenMatKhau;
    private JButton btnDangNhap;
    private JCheckBox showPasswordCheckBox;
    private TaiKhoanDAO taiKhoanDAO = new TaiKhoanDAOImpl();
    private NhanVien nv;
    private LichLamViecDAO llv_dao = new LichLamViecDAOImpl();
    private static final String RMI_SERVER_IP = "127.0.0.1";
    private static final int RMI_SERVER_PORT = 9090;
    private NhanVienDAO nhanVienDAO = new NhanVienDAOImpl();
    private boolean isConnected = false;

    public FrmDangNhap() throws RemoteException {
        setTitle("Đăng nhập");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);

        mainPanel = new JPanel(new BorderLayout());
        setContentPane(mainPanel);

        // Panel hình ảnh (70%)
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setPreferredSize(new Dimension((int) (getWidth() * 0.7), getHeight()));
        try {
            BufferedImage originalImage = ImageIO.read(getClass().getResource("/Anh_HeThong/banner_1.jpg"));
            int targetHeight = getHeight();
            int targetWidth = (int) (((double) targetHeight / originalImage.getHeight()) * originalImage.getWidth());

            backgroundLabel = new JLabel(new ImageIcon(originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH)));
            imagePanel.add(backgroundLabel, BorderLayout.CENTER);
        } catch (IOException | NullPointerException e) {
            System.err.println("Không tìm thấy ảnh nền: " + e.getMessage());
            imagePanel.setBackground(new Color(240, 248, 255));
        }

        // Panel đăng nhập (30%) - container
        JPanel loginContainer = new JPanel(new GridBagLayout());
        loginContainer.setPreferredSize(new Dimension((int) (getWidth() * 0.3), getHeight()));
        loginContainer.setBackground(new Color(240, 240, 240));

        // Panel chính đăng nhập với bo góc và padding
        JPanel loginPanel = new JPanel(new GridBagLayout());
        loginPanel.setBackground(Color.WHITE);
        loginPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                BorderFactory.createEmptyBorder(30, 30, 30, 30)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        JLabel titleLabel = new JLabel("Ga Lạc Hồng", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(new Color(30, 144, 255));
        loginPanel.add(titleLabel, gbc);

        // Mã nhân viên
        gbc.gridy++;
        gbc.insets = new Insets(30, 10, 5, 10);
        JLabel maNhanVienLabel = new JLabel("Mã nhân viên:");
        maNhanVienLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        loginPanel.add(maNhanVienLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 10, 15, 10);
        txtMaNhanVien = new JTextField(15);
        txtMaNhanVien.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        loginPanel.add(txtMaNhanVien, gbc);

        // Mật khẩu
        gbc.gridy++;
        gbc.insets = new Insets(20, 10, 5, 10);
        JLabel matKhauLabel = new JLabel("Mật khẩu:");
        matKhauLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        loginPanel.add(matKhauLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 10, 20, 10);

        // Mật khẩu hiển thị
        txtMatKhau = new JPasswordField(15);
        txtMatKhau.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        loginPanel.add(txtMatKhau, gbc);

        // Checkbox hiển thị mật khẩu
        showPasswordCheckBox = new JCheckBox("Hiển thị mật khẩu");
        showPasswordCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        showPasswordCheckBox.setOpaque(false);
        showPasswordCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (showPasswordCheckBox.isSelected()) {
                    txtMatKhau.setEchoChar((char) 0); // Hiển thị mật khẩu
                } else {
                    txtMatKhau.setEchoChar('*'); // Ẩn mật khẩu
                }
            }
        });

        gbc.gridy++;
        loginPanel.add(showPasswordCheckBox, gbc);

        // Nút
        gbc.gridy++;
        gbc.insets = new Insets(30, 10, 10, 10);
        btnDangNhap = new JButton("Đăng nhập");
        btnDangNhap.setBackground(new Color(30, 144, 255));
        btnDangNhap.setForeground(Color.WHITE);
        btnDangNhap.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnDangNhap.setFocusPainted(false);
        btnDangNhap.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDangNhap.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));

        // Hover hiệu ứng
        btnDangNhap.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnDangNhap.setBackground(new Color(0, 120, 215));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnDangNhap.setBackground(new Color(30, 144, 255));
            }
        });

        btnQuenMatKhau = new JButton("Quên mật khẩu?");
        btnQuenMatKhau.setContentAreaFilled(false);
        btnQuenMatKhau.setBorderPainted(false);
        btnQuenMatKhau.setForeground(Color.GRAY);
        btnQuenMatKhau.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnQuenMatKhau.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setOpaque(false);
        buttonPanel.add(btnDangNhap, BorderLayout.NORTH);
        buttonPanel.add(btnQuenMatKhau, BorderLayout.SOUTH);
        loginPanel.add(buttonPanel, gbc);

        loginContainer.add(loginPanel);

        // Add vào main panel
        mainPanel.add(imagePanel, BorderLayout.WEST);
        mainPanel.add(loginContainer, BorderLayout.EAST);

        // Sự kiện
        btnDangNhap.addActionListener(this);
        btnQuenMatKhau.addActionListener(this);
    }
    private void connectToServer() {
        try {
            Registry registry = LocateRegistry.getRegistry(RMI_SERVER_IP, RMI_SERVER_PORT);
            nhanVienDAO = (NhanVienDAO) registry.lookup("nhanVienDAO");
            // Thông báo kết nối thành công sau khi lookup thành công
//            JOptionPane.showMessageDialog(this,
//                    "Kết nối đến server RMI thành công!",
//                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            isConnected = true;

        } catch (RemoteException | NotBoundException e) {
            // Không hiển thị thông báo lỗi kết nối ở đây
            JOptionPane.showMessageDialog(this,
                    "Không thể kết nối đến server RMI: " + e.getMessage(),
                    "Lỗi Kết Nối", JOptionPane.ERROR_MESSAGE);

            e.printStackTrace();
        }
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnDangNhap) {
            // Kiểm tra tên đăng nhập
            String user = txtMaNhanVien.getText();
            if (user == null || user.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tên đăng nhập không được để trống");
                txtMaNhanVien.requestFocus();
                return;
            }

            // Kiểm tra mật khẩu
            String password = txtMatKhau.getText();
            if (password == null || password.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Mật khẩu không được để trống");
                txtMatKhau.requestFocus();
                return;
            }
            connectToServer();
            try {
                // Kiểm tra đăng nhập
                nv = taiKhoanDAO.checkLogin(user, password);
                if (nv == null) {
                    JOptionPane.showMessageDialog(this, "Tên đăng nhập sai hoặc mật khẩu sai", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // ----- PHẦN NÀY ĐÃ ĐƯỢC COMMENT LẠI, KHÔNG ẢNH HƯỞNG LOGIN -----
            /*
            LocalDateTime now = LocalDateTime.now();
            LocalDate today = now.toLocalDate();
            System.out.println("today:" + today);

            List<LichLamViec> lichLamViecs = llv_dao.getCaLamViecForDate(nv.getMaNV(), today);

            if (lichLamViecs == null || lichLamViecs.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Không có ca làm việc nào cho ngày hôm nay", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            } else {
                for (LichLamViec llv : lichLamViecs) {
                    LocalDateTime gioBatDau = llv.getGioBatDau();
                    if (now.isAfter(gioBatDau)) {
                        llv.setTrangThai("Tre");
                    } else {
                        llv.setTrangThai("Dung gio");
                    }
                    llv_dao.updateTrangThai(llv.getMaLichLamViec(), llv.getTrangThai());
                }
            }
            */
                // ----- KẾT THÚC PHẦN COMMENT -----

                // Tiến hành mở form và thông báo đăng nhập thành công
                if(isConnected){
                    JOptionPane.showMessageDialog(this,
                            "Đăng nhập thành công!",
                            "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    if (nv.getChucVu().trim().equalsIgnoreCase("Nhân viên")) {
                        MainGUI a = new MainGUI(nv);
                        a.setVisible(true);
                        this.dispose();
                        JOptionPane.showMessageDialog(this, "Đăng nhập thành công với vai trò Nhân viên");
                    } else if (nv.getChucVu().trim().equalsIgnoreCase("Quan ly")) {
                        MainGUI b = new MainGUI(nv);
                        b.setVisible(true);
                        this.dispose();
                        JOptionPane.showMessageDialog(this, "Đăng nhập thành công với vai trò quản lý");
                    }
                }


            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Đã xảy ra lỗi khi đăng nhập: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }

        } else if (e.getSource() == btnQuenMatKhau) {
            // Phần quên mật khẩu giữ nguyên như cũ
            String user = txtMaNhanVien.getText();
            if (user == null || user.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập tên đăng nhập trước khi thực hiện quên mật khẩu.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                txtMaNhanVien.requestFocus();
                return;
            }

            String email = JOptionPane.showInputDialog(this, "Vui lòng nhập email để lấy lại mật khẩu:", "Quên mật khẩu", JOptionPane.INFORMATION_MESSAGE);

            if (email == null || email.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Email không được để trống", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

//            try {
//                JOptionPane.showMessageDialog(this, "Đang gửi email. Vui lòng chờ trong giây lát...", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
//
//                String password = taiKhoanDAO.getPasswordByEmail(email);
//                if (password == null) {
//                    JOptionPane.showMessageDialog(this, "Email không tồn tại trong hệ thống", "Lỗi", JOptionPane.ERROR_MESSAGE);
//                } else {
//                    boolean emailSent = EmailSender.sendPasswordEmail(email, password);
//                    if (emailSent) {
//                        JOptionPane.showMessageDialog(this, "Mật khẩu đã được gửi đến email của bạn.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
//                    } else {
//                        JOptionPane.showMessageDialog(this, "Gửi email thất bại. Vui lòng thử lại sau.", "Lỗi", JOptionPane.ERROR_MESSAGE);
//                    }
//                }
//            } catch (Exception ex) {
//                JOptionPane.showMessageDialog(this, "Đã xảy ra lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
//                ex.printStackTrace();
//            }
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FrmDangNhap frame = null;
            try {
                frame = new FrmDangNhap();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
            frame.setVisible(true);
        });
    }
}
