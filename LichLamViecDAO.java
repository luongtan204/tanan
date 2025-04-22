package dao;

import jakarta.transaction.Transactional;
import model.LichLamViec;

import java.rmi.RemoteException;
import java.time.LocalDate;
import java.util.List;

public interface LichLamViecDAO {
    LichLamViec getLichLamViecById(String id) throws RemoteException;

    boolean save(LichLamViec llv) throws RemoteException;

    boolean update(LichLamViec llv) throws RemoteException;

    boolean delete(String id) throws RemoteException;

    List<LichLamViec> getCaLamViecForDate(String maNhanVien, LocalDate today) throws RemoteException;

    @Transactional
    void updateTrangThai(String maLichLamViec, String trangThai) throws RemoteException;
}
