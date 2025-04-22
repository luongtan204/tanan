package dao;

import model.ChiTietHoaDon;
import model.VeTau;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.util.List;

public interface TraCuuVeDAO extends Remote {

    VeTau timVeTauTheoMa(String maVe) throws RemoteException;

    List<VeTau> timDanhSachVeTauTheoMa(String maVe) throws RemoteException;

    ChiTietHoaDon timChiTietHoaDonTheoMaVe(String maVe) throws RemoteException;

    List<VeTau> timVeTauTheoGiayTo(String giayTo) throws RemoteException;

    List<VeTau> timVeTauTheoTenKH(String tenKhachHang) throws RemoteException;

    List<VeTau> timVeTauTheoChitiet(String tenKhachHang, String giayTo, LocalDate ngayDi, String maChoNgoi, String doiTuong) throws RemoteException;

    List<VeTau> timVeTauTheoTenKHVaThoiGian(String hoTen, LocalDate ngayDiFrom, LocalDate ngayDiTo) throws RemoteException;

    boolean testConnection() throws RemoteException;
}