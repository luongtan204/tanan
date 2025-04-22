package dao;

import model.LoaiKhachHang;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * @Dự án: PhanTanJavaNhomGPT
 * @Class: LoaiKhachHangDAO
 * @Tạo vào ngày: 18/04/2025
 * @Tác giả: Nguyen Huu Sang
 */
public interface LoaiKhachHangDAO extends Remote {
    List<LoaiKhachHang> getAll() throws RemoteException;
    LoaiKhachHang findById(String id) throws RemoteException;
    boolean save(LoaiKhachHang loaiKhachHang) throws RemoteException;
    boolean update(LoaiKhachHang loaiKhachHang) throws RemoteException;
    boolean delete(String id) throws RemoteException;

//     List<LoaiKhachHang> getAllList();
}