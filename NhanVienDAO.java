package dao;

import model.NhanVien;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface NhanVienDAO extends Remote {
    NhanVien getnhanvienById(String id) throws RemoteException;

    boolean save(NhanVien nv) throws RemoteException;

    boolean update(NhanVien nv) throws RemoteException;

    boolean delete(String id) throws RemoteException;

    List<NhanVien> getAllNhanVien() throws RemoteException;
    boolean testConnection() throws RemoteException;

}
