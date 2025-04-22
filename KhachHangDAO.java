package dao;

import model.KhachHang;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * @Dự án: PhanTanJavaNhomGPT
 * @Class: KhachHangDAO
 * @Tạo vào ngày: 18/04/2025
 * @Tác giả: Nguyen Huu Sang
 */
public interface KhachHangDAO extends Remote {
    List<KhachHang> getAll() throws RemoteException;

//    List<KhachHang> getAllList();

    KhachHang findById(String id) throws RemoteException;
    boolean save(KhachHang khachHang) throws RemoteException;

    boolean update(KhachHang khachHang) throws RemoteException;

    boolean delete(String id) throws RemoteException;
    List<KhachHang> listKhachHangsByName(String name) throws RemoteException;
    List<KhachHang> listKhachHangsByPoints(double from, double to) throws RemoteException;

    // Add method to search by phone number
    List<KhachHang> searchByPhone(String phone) throws RemoteException;

    List<String> getTenKhachHang() throws RemoteException;

    // Add method to filter customers by type
    List<KhachHang> filterByType(String typeName) throws RemoteException;

    boolean testConnection() throws RemoteException;

    boolean add(KhachHang newCustomer) throws RemoteException;

    // Find customer by ID card and phone number
    KhachHang findByIdCardAndPhone(String idCard, String phone) throws RemoteException;


//    KhachHang getById(String id);
}
