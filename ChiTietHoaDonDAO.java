package dao;

import model.ChiTietHoaDon;
import model.ChiTietHoaDonId;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ChiTietHoaDonDAO extends Remote {

     // Read: Lấy tất cả chi tiết hóa đơn
     public List<ChiTietHoaDon> getAllList() throws RemoteException;

     // Read: Tìm chi tiết hóa đơn theo ID
     public ChiTietHoaDon getById(ChiTietHoaDonId id) throws RemoteException;

     // Create: Thêm chi tiết hóa đơn
     public boolean save(ChiTietHoaDon chiTietHoaDon) throws RemoteException;

     // Update: Cập nhật thông tin chi tiết hóa đơn
     public boolean update(ChiTietHoaDon chiTietHoaDon) throws RemoteException;

     // Delete: Xóa chi tiết hóa đơn theo ID
     public boolean delete(ChiTietHoaDonId id) throws RemoteException;

     // Lấy danh sách chi tiết hóa đơn theo mã hóa đơn
     public List<ChiTietHoaDon> getByHoaDonId(String hoaDonId) throws RemoteException;

     // Lấy danh sách chi tiết hóa đơn theo mã vé
     public List<ChiTietHoaDon> getByVeTauId(String veTauId) throws RemoteException;
}