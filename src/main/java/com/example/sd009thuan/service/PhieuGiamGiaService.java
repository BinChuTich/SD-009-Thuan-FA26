package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.PhieuGiamGiaDTO;
import com.example.sd009thuan.entity.KhachHang;
import com.example.sd009thuan.entity.KhachHangPhieuGiamGia;
import com.example.sd009thuan.entity.PhieuGiamGia;
import com.example.sd009thuan.repository.KhachHangPhieuGiamGiaRepository;
import com.example.sd009thuan.repository.KhachHangRepository;
import com.example.sd009thuan.repository.PhieuGiamGiaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
@Service
public class PhieuGiamGiaService {
    @Autowired
    private PhieuGiamGiaRepository phieuGiamGiaRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private KhachHangPhieuGiamGiaRepository khachHangPhieuGiamGiaRepository;

    private PhieuGiamGiaDTO convertToDTO(PhieuGiamGia phieu) {
        PhieuGiamGiaDTO dto = new PhieuGiamGiaDTO();
        dto.setId(phieu.getId());
        dto.setMaPhieuGiamGia(phieu.getMaPhieuGiamGia());
        dto.setTenPhieuGiamGia(phieu.getTenPhieuGiamGia());
        dto.setLoaiPhieuGiamGia(phieu.getLoaiPhieuGiamGia());
        dto.setGiaTriGiamGia(phieu.getGiaTriGiamGia());
        dto.setGiamToiDa(phieu.getGiamToiDa());
        dto.setHoaDonToiThieu(phieu.getHoaDonToiThieu());
        dto.setSoLuongSuDung(phieu.getSoLuongSuDung());
        dto.setNgayBatDau(phieu.getNgayBatDau());
        dto.setNgayKetThuc(phieu.getNgayKetThuc());
        dto.setTrangThai(phieu.getTrangThai());

        List<String> listTenKhach = phieuGiamGiaRepository.findTenKhachHangByIdPhieu(phieu.getId());
        if (listTenKhach != null && !listTenKhach.isEmpty()) {
            dto.setHinhThuc("Cá nhân");
            dto.setSoKhachHang(listTenKhach.size());
            dto.setDanhSachKhachHang(listTenKhach);
        } else {
            dto.setHinhThuc("Công khai");
            dto.setSoKhachHang(0);
            dto.setDanhSachKhachHang(Collections.emptyList());
        }

        return dto;
    }

    public List<PhieuGiamGiaDTO> getAllPhieuGiamGia() {
        List<PhieuGiamGia> list = phieuGiamGiaRepository.findAllByOrderByIdDesc();
        Instant now = Instant.now();
        List<PhieuGiamGia> canCapNhat = new ArrayList<>();

        for (PhieuGiamGia phieu : list) {
            if (phieu.getNgayKetThuc() != null && phieu.getNgayKetThuc().isBefore(now)) {
                if (phieu.getTrangThai() != null && phieu.getTrangThai() == 1) {
                    phieu.setTrangThai(0);
                    canCapNhat.add(phieu);
                }
            }
        }
        if (!canCapNhat.isEmpty()) {
            phieuGiamGiaRepository.saveAll(canCapNhat);
        }

        List<PhieuGiamGiaDTO> dtoList = new ArrayList<>();
        for (PhieuGiamGia phieu : list) {
            dtoList.add(convertToDTO(phieu));
        }

        return dtoList;
    }

    public Page<PhieuGiamGiaDTO> getPhanTrangPhieuGiamGia(Pageable pageable) {
        Page<PhieuGiamGia> pageEntity = phieuGiamGiaRepository.findAllByOrderByIdDesc(pageable);
        return pageEntity.map(this::convertToDTO);
    }


    @Scheduled(cron = "0 * * * * ?")
    public void tuDongKiemTraVaCapNhatHetHan() {
        List<PhieuGiamGia> list = phieuGiamGiaRepository.findAll();
        Instant now = Instant.now();
        List<PhieuGiamGia> hetHanList = new ArrayList<>();

        for (PhieuGiamGia phieu : list) {
            if (phieu.getNgayKetThuc() != null && phieu.getNgayKetThuc().isBefore(now)) {
                if (phieu.getTrangThai() != null && phieu.getTrangThai() == 1) {
                    phieu.setTrangThai(0);
                    hetHanList.add(phieu);
                }
            }
        }

        if (!hetHanList.isEmpty()) {
            phieuGiamGiaRepository.saveAll(hetHanList);
            System.out.println("==> [Hệ thống] Đã tự động cập nhật " + hetHanList.size() + " phiếu giảm giá sang trạng thái HẾT HẠN.");
        }
    }


    public Optional<PhieuGiamGiaDTO> getById(Long id) {
        return phieuGiamGiaRepository.findById(id).map(this::convertToDTO);
    }


    public PhieuGiamGiaDTO toggleTrangThai(Long id) {
        Optional<PhieuGiamGia> optional = phieuGiamGiaRepository.findById(id);
        if (optional.isPresent()) {
            PhieuGiamGia phieu = optional.get();
            int newStatus = (phieu.getTrangThai() != null && phieu.getTrangThai() == 1) ? 0 : 1;
            phieu.setTrangThai(newStatus);
            PhieuGiamGia saved = phieuGiamGiaRepository.save(phieu);
            return convertToDTO(saved);
        }
        return null;
    }


    public PhieuGiamGiaDTO updatePhieuGiamGia(Long id, PhieuGiamGiaDTO dto) {
        Optional<PhieuGiamGia> optional = phieuGiamGiaRepository.findById(id);
        if (optional.isPresent()) {
            PhieuGiamGia phieu = optional.get();

            if (dto.getTenPhieuGiamGia() != null && !dto.getTenPhieuGiamGia().trim().isEmpty()) {
                phieu.setTenPhieuGiamGia(dto.getTenPhieuGiamGia().trim());
            }
            if (dto.getLoaiPhieuGiamGia() != null) {
                phieu.setLoaiPhieuGiamGia(dto.getLoaiPhieuGiamGia());
            }
            if (dto.getGiaTriGiamGia() != null) {
                phieu.setGiaTriGiamGia(dto.getGiaTriGiamGia());
            }
            phieu.setGiamToiDa(dto.getGiamToiDa());

            if (dto.getHoaDonToiThieu() != null) {
                phieu.setHoaDonToiThieu(dto.getHoaDonToiThieu());
            }
            if (dto.getSoLuongSuDung() != null) {
                phieu.setSoLuongSuDung(dto.getSoLuongSuDung());
            }
            if (dto.getNgayBatDau() != null) {
                phieu.setNgayBatDau(dto.getNgayBatDau());
            }
            if (dto.getNgayKetThuc() != null) {
                phieu.setNgayKetThuc(dto.getNgayKetThuc());
            }
            if (dto.getTrangThai() != null) {
                phieu.setTrangThai(dto.getTrangThai());
            }

            if (phieu.getNgayKetThuc() != null && phieu.getNgayKetThuc().isBefore(Instant.now())) {
                phieu.setTrangThai(0);
            }

            PhieuGiamGia saved = phieuGiamGiaRepository.save(phieu);
            return convertToDTO(saved);
        }
        return null;
    }

    public List<KhachHang> getAllKhachHang() {
        return khachHangRepository.findAllByOrderByTenKhachHangAsc();
    }

    @Transactional
    public PhieuGiamGiaDTO createPhieuGiamGia(PhieuGiamGiaDTO dto) {
        PhieuGiamGia phieu = new PhieuGiamGia();

        String ma = dto.getMaPhieuGiamGia();
        if (ma == null || ma.trim().isEmpty()) {
            ma = "PGG" + (System.currentTimeMillis() % 1000000);
        }
        phieu.setMaPhieuGiamGia(ma.trim().toUpperCase());


        String ten = dto.getTenPhieuGiamGia();
        phieu.setTenPhieuGiamGia(ten != null ? ten.trim() : "Phiếu giảm giá mới");

        phieu.setLoaiPhieuGiamGia(dto.getLoaiPhieuGiamGia() != null ? dto.getLoaiPhieuGiamGia() : 1);


        phieu.setGiaTriGiamGia(dto.getGiaTriGiamGia() != null ? dto.getGiaTriGiamGia() : BigDecimal.ZERO);

        phieu.setGiamToiDa(dto.getGiamToiDa());

        phieu.setHoaDonToiThieu(dto.getHoaDonToiThieu() != null ? dto.getHoaDonToiThieu() : BigDecimal.ZERO);

        phieu.setSoLuongSuDung(dto.getSoLuongSuDung() != null ? dto.getSoLuongSuDung() : 100);

        Instant ngayBatDau = dto.getNgayBatDau() != null ? dto.getNgayBatDau() : Instant.now();
        phieu.setNgayBatDau(ngayBatDau);
        phieu.setNgayKetThuc(dto.getNgayKetThuc());

        int status = dto.getTrangThai() != null ? dto.getTrangThai() : 1;
        if (phieu.getNgayKetThuc() != null && phieu.getNgayKetThuc().isBefore(Instant.now())) {
            status = 0;
        }
        phieu.setTrangThai(status);

        PhieuGiamGia saved = phieuGiamGiaRepository.save(phieu);

        if ("Cá nhân".equalsIgnoreCase(dto.getHinhThuc()) && dto.getIdKhachHangList() != null && !dto.getIdKhachHangList().isEmpty()) {
            for (Long idKhach : dto.getIdKhachHangList()) {
                Optional<KhachHang> khOpt = khachHangRepository.findById(idKhach);
                if (khOpt.isPresent()) {
                    KhachHangPhieuGiamGia link = new KhachHangPhieuGiamGia();
                    link.setIdPhieuGiamGia(saved);
                    link.setIdKhachHang(khOpt.get());
                    link.setTrangThai(1);
                    khachHangPhieuGiamGiaRepository.save(link);
                }
            }
        }

        return convertToDTO(saved);
    }
}
