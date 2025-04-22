package dao;

import model.ToaTau;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ToaTauDoiVeDAO extends Remote {
    // Lấy danh sách toa tàu theo mã tàu
    List<ToaTau> getToaTauByMaTau(String maTau) throws RemoteException;
}