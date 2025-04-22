package dao;

import model.ToaTau;

import java.rmi.RemoteException;
import java.util.List;

public interface ToaTauDAO {

    ToaTau getToaTauById(String id);

    List<ToaTau> getToaByTau(String maTau) throws RemoteException;
}
