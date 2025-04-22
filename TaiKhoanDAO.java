package dao;

import model.NhanVien;
import model.TaiKhoan;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface TaiKhoanDAO  extends Remote {
    TaiKhoan getTaiKhoanById(String id) throws RemoteException;

    boolean save(TaiKhoan tk) throws RemoteException;

    // update password với maNV
    boolean updatePassword(String maNV, String passWord) throws RemoteException;

    boolean delete(String id) throws RemoteException;
    String getPasswordByEmail(String email) throws RemoteException;
    NhanVien checkLogin(String maNhanVien, String password) throws RemoteException;
    boolean insert(TaiKhoan nd) throws RemoteException;
}
