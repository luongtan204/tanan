package dao;

import model.LoaiHoaDon;

import java.rmi.Remote;

public interface LoaiHoaDonDAO extends Remote {
    LoaiHoaDon findById(String id) throws Exception;
}
