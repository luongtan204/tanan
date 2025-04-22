package dao;

import model.ChoNgoi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ChoNgoiCallback extends Remote {
    // Thông báo cập nhật trạng thái chỗ ngồi (đã đặt hoặc chưa đặt trong một lịch trình)
    void capNhatTrangThaiDatChoNgoi(String maCho, String maLichTrinh, boolean daDat, String sessionId) throws RemoteException;

    // Thông báo cập nhật khả năng sử dụng chỗ ngồi (đang sửa chữa hay không)
    void capNhatKhaNangSuDungChoNgoi(String maCho, boolean khaDung) throws RemoteException;

    // Thông báo cập nhật toàn bộ danh sách chỗ ngồi
    void capNhatDanhSachChoNgoi() throws RemoteException;
}