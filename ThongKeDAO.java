package dao;

import model.KetQuaThongKeVe;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.util.List;

/**
 * Interface cho việc thống kê dữ liệu vé tàu qua RMI
 */
public interface ThongKeDAO extends Remote {

    /**
     * Thống kê số lượng vé theo thời gian
     *
     * @param tuNgay ngày bắt đầu thống kê
     * @param denNgay ngày kết thúc thống kê
     * @param loaiThoiGian loại thời gian (Ngày, Tuần, Tháng, Quý, Năm)
     * @return danh sách kết quả thống kê
     * @throws RemoteException lỗi RMI
     */
    List<KetQuaThongKeVe> thongKeVeTheoThoiGian(
            LocalDate tuNgay,
            LocalDate denNgay,
            String loaiThoiGian) throws RemoteException;

    /**
     * Kiểm tra kết nối database
     *
     * @return true nếu kết nối thành công
     * @throws RemoteException lỗi RMI
     */
    boolean testConnection() throws RemoteException;
}