package dao;

import model.LichTrinhTau;
import model.Tau;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface TauDAO extends Remote {
    List<Tau> getAllListT() throws RemoteException;
    List<Tau> getAllWithRoutes() throws RemoteException;
    public Tau getTauByLichTrinhTau(LichTrinhTau lichTrinh) throws RemoteException;
}
