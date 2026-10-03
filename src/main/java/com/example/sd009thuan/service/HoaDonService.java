package com.example.sd009thuan.service;

import com.example.sd009thuan.entity.HoaDon;
import com.example.sd009thuan.repository.HoaDonRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class HoaDonService {

    private final HoaDonRepository hoaDonRepository;

    public HoaDonService(HoaDonRepository hoaDonRepository) {
        this.hoaDonRepository = hoaDonRepository;
    }
    public Optional<HoaDon> getByMaHoaDon(String maHoaDon) {
        return hoaDonRepository.findByMaHoaDon(maHoaDon);
    }
    // Lấy tất cả hóa đơn
    public List<HoaDon> getAll() {
        return hoaDonRepository.findAll();
    }

    // Lấy hóa đơn theo ID
    public Optional<HoaDon> getById(Long id) {
        return hoaDonRepository.findById(id);
    }

    // Thêm hóa đơn
    public HoaDon create(HoaDon hoaDon) {
        return hoaDonRepository.save(hoaDon);
    }

//    // Sửa hóa đơn
//    public HoaDon update(Long id, HoaDon hoaDonMoi) {
//
//        HoaDon hoaDonCu = hoaDonRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));
//
////        hoaDonCu.setMaHoaDon(hoaDonMoi.getMaHoaDon());
////        hoaDonCu.setLoaiDon(hoaDonMoi.getLoaiDon());
//        hoaDonCu.setPhiShip(hoaDonMoi.getPhiShip());
//        hoaDonCu.setTongTien(hoaDonMoi.getTongTien());
//        hoaDonCu.setTongTienGiamGia(hoaDonMoi.getTongTienGiamGia());
//        hoaDonCu.setTenKhachHang(hoaDonMoi.getTenKhachHang());
//        hoaDonCu.setSoDienThoaiKhachHang(hoaDonMoi.getSoDienThoaiKhachHang());
//        hoaDonCu.setDiaChiNhanHang(hoaDonMoi.getDiaChiNhanHang());
//        hoaDonCu.setNgayTao(hoaDonMoi.getNgayTao());
//        hoaDonCu.setNguoiTao(hoaDonMoi.getNguoiTao());
//        hoaDonCu.setNgayCapNhat(hoaDonMoi.getNgayCapNhat());
//        hoaDonCu.setNguoiCapNhat(hoaDonMoi.getNguoiCapNhat());
//        hoaDonCu.setTrangThai(hoaDonMoi.getTrangThai());
//        hoaDonCu.setGhiChu(hoaDonMoi.getGhiChu());
//
//        return hoaDonRepository.save(hoaDonCu);
//    }

    // Xóa hóa đơn
    public void delete(Long id) {
        hoaDonRepository.deleteById(id);
    }
}