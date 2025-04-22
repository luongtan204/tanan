package dao;

import model.KhuyenMai;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.util.List;

public interface KhuyenMaiDAO extends Remote {
    // Lấy danh sách tất cả các khuyến mãi
    List<KhuyenMai> findAll() throws RemoteException;

    // Lấy danh sách khuyến mãi theo tên
    List<KhuyenMai> findByName(String name) throws RemoteException;

    // Lấy khuyến mãi theo mã
    KhuyenMai findById(String id) throws RemoteException;

    // Thêm hoặc cập nhật khuyến mãi
    boolean save(KhuyenMai khuyenMai) throws RemoteException;

    // Xóa khuyến mãi theo mã
    boolean delete(String id) throws RemoteException;

    // Tìm các khuyến mãi đang áp dụng
    List<KhuyenMai> findOngoingPromotions() throws RemoteException;

    // Tìm các khuyến mãi áp dụng cho tất cả đối tượng dựa trên ngày lịch trình tàu
    List<KhuyenMai> findPromotionsForAllByScheduleDate(LocalDate scheduleDate) throws RemoteException;

    // Kiểm tra kết nối
    boolean testConnection() throws RemoteException;
}
