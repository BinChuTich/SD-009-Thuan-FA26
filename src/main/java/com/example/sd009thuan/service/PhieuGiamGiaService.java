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
import java.time.LocalDate;
import java.time.ZoneId;
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

    private void validatePhieuGiamGiaDTO(PhieuGiamGiaDTO dto, boolean isCreate) {
        if (dto == null) {
            throw new IllegalArgumentException("Dữ liệu phiếu giảm giá không hợp lệ!");
        }

        // 1. Tên phiếu giảm giá
        if (dto.getTenPhieuGiamGia() == null || dto.getTenPhieuGiamGia().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên phiếu giảm giá không được để trống!");
        }
        if (dto.getTenPhieuGiamGia().trim().length() > 255) {
            throw new IllegalArgumentException("Tên phiếu giảm giá không được vượt quá 255 ký tự!");
        }

        // 2. Loại phiếu giảm giá
        if (dto.getLoaiPhieuGiamGia() == null || (dto.getLoaiPhieuGiamGia() != 1 && dto.getLoaiPhieuGiamGia() != 2)) {
            throw new IllegalArgumentException("Loại phiếu giảm giá không hợp lệ (1: %, 2: Tiền mặt)!");
        }

        // 3. Mức giảm giá
        if (dto.getGiaTriGiamGia() == null || dto.getGiaTriGiamGia().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Mức giảm giá phải lớn hơn 0!");
        }
        if (dto.getLoaiPhieuGiamGia() == 1) {
            if (dto.getGiaTriGiamGia().compareTo(BigDecimal.ONE) < 0 || dto.getGiaTriGiamGia().compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Mức giảm theo phần trăm phải từ 1% đến 100%!");
            }
        } else if (dto.getLoaiPhieuGiamGia() == 2) {
            if (dto.getGiaTriGiamGia().compareTo(new BigDecimal("1000")) < 0) {
                throw new IllegalArgumentException("Mức giảm tiền mặt phải tối thiểu từ 1.000 VNĐ!");
            }
        }

        // 4. Giảm tối đa (khi là %)
        if (dto.getLoaiPhieuGiamGia() == 1 && dto.getGiamToiDa() != null) {
            if (dto.getGiamToiDa().compareTo(new BigDecimal("1000")) < 0) {
                throw new IllegalArgumentException("Mức giảm tối đa phải tối thiểu từ 1.000 VNĐ!");
            }
        }

        // 5. Hóa đơn tối thiểu
        if (dto.getHoaDonToiThieu() == null || dto.getHoaDonToiThieu().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Hóa đơn tối thiểu không được để trống và không được âm!");
        }
        if (dto.getHoaDonToiThieu().compareTo(BigDecimal.ZERO) > 0 && dto.getHoaDonToiThieu().compareTo(new BigDecimal("1000")) < 0) {
            throw new IllegalArgumentException("Hóa đơn tối thiểu phải từ 1.000 VNĐ trở lên (hoặc bằng 0)!");
        }
        if (dto.getLoaiPhieuGiamGia() == 2 && dto.getHoaDonToiThieu() != null
                && dto.getGiaTriGiamGia().compareTo(dto.getHoaDonToiThieu()) > 0) {
            throw new IllegalArgumentException("Mức giảm tiền mặt không được lớn hơn giá trị hóa đơn tối thiểu!");
        }

        // 6. Số lượng sử dụng (Nếu có nhập thì phải > 0, nếu để trống hoặc 0 thì hiểu là không giới hạn)
        if (dto.getSoLuongSuDung() != null && dto.getSoLuongSuDung() < 0) {
            throw new IllegalArgumentException("Số lượt sử dụng không được là số âm!");
        }

        // 7. Ngày bắt đầu và Ngày kết thúc
        if (dto.getNgayBatDau() == null) {
            throw new IllegalArgumentException("Ngày bắt đầu không được để trống!");
        }
        if (dto.getNgayKetThuc() != null && dto.getNgayKetThuc().isBefore(dto.getNgayBatDau())) {
            throw new IllegalArgumentException("Thời gian kết thúc phải diễn ra sau hoặc cùng thời điểm với thời gian bắt đầu!");
        }

        // 8. Khi tạo mới: Ngày bắt đầu không được là ngày trong quá khứ (phải từ hôm nay trở đi)
        if (isCreate) {
            ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
            LocalDate today = LocalDate.now(zoneId);
            LocalDate startDate = dto.getNgayBatDau().atZone(zoneId).toLocalDate();
            if (startDate.isBefore(today)) {
                throw new IllegalArgumentException("Thời gian bắt đầu không được là ngày trong quá khứ (phải từ ngày hiện tại " + today + " trở đi)!");
            }
        }

        // 9. Khách hàng đối với phiếu cá nhân
        if ("Cá nhân".equalsIgnoreCase(dto.getHinhThuc())) {
            if (dto.getIdKhachHangList() == null || dto.getIdKhachHangList().isEmpty()) {
                throw new IllegalArgumentException("Hình thức Cá nhân yêu cầu chọn ít nhất 1 khách hàng áp dụng!");
            }
        }
    }

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
        validatePhieuGiamGiaDTO(dto, false);

        Optional<PhieuGiamGia> optional = phieuGiamGiaRepository.findById(id);
        if (optional.isPresent()) {
            PhieuGiamGia phieu = optional.get();

            phieu.setTenPhieuGiamGia(dto.getTenPhieuGiamGia().trim());
            phieu.setLoaiPhieuGiamGia(dto.getLoaiPhieuGiamGia());
            phieu.setGiaTriGiamGia(dto.getGiaTriGiamGia());
            phieu.setGiamToiDa(dto.getLoaiPhieuGiamGia() == 1 ? dto.getGiamToiDa() : null);
            phieu.setHoaDonToiThieu(dto.getHoaDonToiThieu());
            int soLuong = (dto.getSoLuongSuDung() != null && dto.getSoLuongSuDung() > 0) ? dto.getSoLuongSuDung() : 0;
            phieu.setSoLuongSuDung(soLuong);
            phieu.setNgayBatDau(dto.getNgayBatDau());
            phieu.setNgayKetThuc(dto.getNgayKetThuc());

            int status = dto.getTrangThai() != null ? dto.getTrangThai() : 1;
            if (phieu.getNgayKetThuc() != null && phieu.getNgayKetThuc().isBefore(Instant.now())) {
                status = 0;
            }
            phieu.setTrangThai(status);

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
        validatePhieuGiamGiaDTO(dto, true);

        PhieuGiamGia phieu = new PhieuGiamGia();

        String ma = dto.getMaPhieuGiamGia();
        if (ma == null || ma.trim().isEmpty()) {
            ma = "PGG" + (System.currentTimeMillis() % 1000000);
        }
        phieu.setMaPhieuGiamGia(ma.trim().toUpperCase());

        phieu.setTenPhieuGiamGia(dto.getTenPhieuGiamGia().trim());
        phieu.setLoaiPhieuGiamGia(dto.getLoaiPhieuGiamGia());
        phieu.setGiaTriGiamGia(dto.getGiaTriGiamGia());
        phieu.setGiamToiDa(dto.getLoaiPhieuGiamGia() == 1 ? dto.getGiamToiDa() : null);
        phieu.setHoaDonToiThieu(dto.getHoaDonToiThieu());
        int soLuongMoi = (dto.getSoLuongSuDung() != null && dto.getSoLuongSuDung() > 0) ? dto.getSoLuongSuDung() : 0;
        phieu.setSoLuongSuDung(soLuongMoi);
        phieu.setNgayBatDau(dto.getNgayBatDau());
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
