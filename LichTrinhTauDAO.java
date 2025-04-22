package dao;

import model.LichTrinhTau;
import model.TrangThai;
import model.TrangThaiVeTau;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.util.List;

public interface LichTrinhTauDAO extends Remote {
    List<LichTrinhTau> getAllList() throws RemoteException;
    LichTrinhTau getById(String id) throws RemoteException;
    boolean save(LichTrinhTau lichTrinhTau) throws RemoteException;
    boolean update(LichTrinhTau lichTrinhTau) throws RemoteException;
    boolean delete(LichTrinhTau lichTrinhTau) throws RemoteException;
    boolean delete(String id) throws RemoteException;
    List<LichTrinhTau> getListLichTrinhTauByDate(LocalDate date) throws RemoteException;
    List<LichTrinhTau> getListLichTrinhTauByDateAndGaDi(LocalDate date, String gaDi) throws RemoteException;
    List<LichTrinhTau> getListLichTrinhTauByDateAndGaDiGaDen(LocalDate date, String gaDi, String gaDen) throws RemoteException;
    List<LichTrinhTau> getListLichTrinhTauByDateAndGaDiGaDenAndGioDi(LocalDate date, String gaDi, String gaDen, String gioDi) throws RemoteException;
    // Phương thức kiểm tra kết nối
    boolean testConnection() throws RemoteException;
    List<TrangThai> getTrangThai() throws RemoteException;

    List<LichTrinhTau> getListLichTrinhTauByDateRange(LocalDate startDate, LocalDate endDate) throws RemoteException;

    List<String> getAllStations() throws RemoteException;
    List<LichTrinhTau> getListLichTrinhTauByTrangThai(TrangThai... trangThai) throws RemoteException;
    List<LichTrinhTau> getListLichTrinhTauByMaTauAndNgayDi(String maTau, LocalDate ngayDi) throws RemoteException;
    long getAvailableSeatsBySchedule(String maLich) throws RemoteException;
    long getReservedSeatsBySchedule(String maLich) throws RemoteException;
    long getReservedSeatsByScheduleAndCar(String maLich, String maToa) throws RemoteException;
    long getTotalSeatsBySchedule(String maLich) throws RemoteException;
    long getTotalSeatsByScheduleAndCar(String maLich, String maToa) throws RemoteException;
    double getReservationPercentageBySchedule(String maLich) throws RemoteException;
    double getReservationPercentageByScheduleAndCar(String maLich, String maToa) throws RemoteException;
    boolean updateTicketStatusBySchedule(String maLich, TrangThaiVeTau trangThai) throws RemoteException;
    boolean updateTicketStatusByScheduleAndCar(String maLich, String maToa, TrangThaiVeTau trangThai) throws RemoteException;
}
