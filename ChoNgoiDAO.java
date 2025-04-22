package dao;

import model.ChoNgoi;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface ChoNgoiDAO {
    Map<String, String> getAvailableSeatsMapByScheduleAndToa(String maLich, String maToa) throws RemoteException;

    ChoNgoi getById(String id);

    public List<ChoNgoi> getListByToa(String maToa) throws RemoteException;
}
