package dao;

import model.ChoNgoi;
import model.ToaTau;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ChoNgoiDoiVeDAO extends Remote {
    // Lấy danh sách chỗ ngồi theo toa tàu
    List<ChoNgoi> getChoNgoiByToaTau(String maToaTau) throws RemoteException;

    // Kiểm tra chỗ ngồi có khả dụng không (tinh_trang = true)
    boolean kiemTraChoNgoiKhaDung(String maCho) throws RemoteException;

    // Kiểm tra chỗ ngồi đã được đặt trong một lịch trình cụ thể chưa
    boolean kiemTraChoNgoiDaDat(String maCho, String maLichTrinh) throws RemoteException;

    // Khóa chỗ ngồi tạm thời (khi khách hàng chọn)
    boolean khoaChoNgoi(String maCho, String maLichTrinh, String sessionId, long thoiGianKhoaMillis) throws RemoteException;

    // Hủy khóa chỗ ngồi
    boolean huyKhoaChoNgoi(String maCho, String maLichTrinh, String sessionId) throws RemoteException;

    // Cập nhật trạng thái khả dụng của chỗ ngồi (đang sửa chữa hay không)
    boolean capNhatKhaNangSuDungChoNgoi(String maCho, boolean khaDung) throws RemoteException;

    // Đặt lại trạng thái đặt chỗ ngồi dựa trên vé
    int dongBoTrangThaiDatCho() throws RemoteException;

    // Đăng ký client để nhận thông báo khi có thay đổi trạng thái chỗ ngồi
    void dangKyClientChoThongBao(ChoNgoiCallback callback) throws RemoteException;

    // Hủy đăng ký client
    void huyDangKyClientChoThongBao(ChoNgoiCallback callback) throws RemoteException;
}