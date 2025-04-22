package dao;

import model.HoaDon;
import model.LoaiHoaDon;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.util.List;

/**
 * @Dự án: PhanTanJavaNhomGPT
 * @Interface: HoaDonDAO
 * @Tạo vào ngày: 18/04/2025
 * @Tác giả: Nguyen Huu Sang
 */
public interface HoaDonDAO extends Remote {
    // Create: Thêm hóa đơn mới
    boolean saveHoaDon(HoaDon hoaDon) throws RemoteException;

    // Read: Lấy danh sách hóa đơn
    List<HoaDon> getAllHoaDons() throws RemoteException;

    // Read: Tìm hóa đơn theo mã hóa đơn
    HoaDon getHoaDonById(String maHD) throws RemoteException;

    // Update: Cập nhật thông tin hóa đơn
    boolean updateHoaDon(HoaDon hoaDon) throws RemoteException;

    // Delete: Xóa hóa đơn theo mã hóa đơn
    boolean deleteHoaDon(String maHD) throws RemoteException;

    // Retrieve invoices by customer ID
    List<HoaDon> getByCustomerId(String customerId) throws RemoteException;

    String generateMaHoaDon(LocalDate ngay) throws RemoteException;
    LoaiHoaDon getLoaiHoaDonById(String maLoaiHD) throws RemoteException;
}