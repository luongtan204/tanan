package dao;

import model.ChoNgoi;
import model.KhachHang;
import model.TrangThaiVeTau;
import model.VeTau;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface DoiVeDAO extends Remote {
    VeTau getVeTau(String id) throws RemoteException;
    boolean doiVe(VeTau veTau) throws RemoteException;
    List<VeTau> getVeTauByTrangThai(TrangThaiVeTau trangThai) throws RemoteException;
    boolean testConnection() throws RemoteException;
    List<TrangThaiVeTau> getAllTrangThaiVe() throws RemoteException;

    // Thêm chức năng mới
    boolean datVe(VeTau veTau, String choNgoiId) throws RemoteException;
    boolean huyVe(String maVe) throws RemoteException;
    boolean thanhToanVe(String maVe) throws RemoteException;
    boolean capNhatTrangThaiVe(String maVe, TrangThaiVeTau trangThai) throws RemoteException;
    KhachHang getKhachHangByMaVe(String maVe) throws RemoteException;
}