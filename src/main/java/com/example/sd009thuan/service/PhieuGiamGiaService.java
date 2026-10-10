package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.KhachHangDTO;
import com.example.sd009thuan.dto.PhieuGiamGiaDTO;
import com.example.sd009thuan.entity.DotGiamGia;
import com.example.sd009thuan.entity.KhachHang;
import com.example.sd009thuan.entity.KhachHangPhieuGiamGia;
import com.example.sd009thuan.entity.PhieuGiamGia;
import com.example.sd009thuan.repository.DotGiamGiaRepository;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PhieuGiamGiaService {

    @Autowired
    private PhieuGiamGiaRepository phieuGiamGiaRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private KhachHangPhieuGiamGiaRepository khachHangPhieuGiamGiaRepository;

    @Autowired
    private DotGiamGiaRepository dotGiamGiaRepository;

    @Autowired
    private EmailService emailService;

    /**
     * Quy tắc 3 trạng thái tự động theo vòng đời của phiếu & đợt giảm giá:
     * - 2: Sắp diễn ra (Thời gian hiện tại < Ngày bắt đầu)
     * - 1: Đang diễn ra (Ngày bắt đầu <= Thời gian hiện tại <= Ngày kết thúc)
     * - 0: Đã kết thúc / Hết hạn (Thời gian hiện tại > Ngày kết thúc)
     */
    public static int tinhTrangThaiTheoThoiGian(Instant ngayBatDau, Instant ngayKetThuc) {
        Instant now = Instant.now();
        if (ngayKetThuc != null && now.isAfter(ngayKetThuc)) {
            return 0; // Đã kết thúc / Hết hạn
        }
        if (ngayBatDau != null && now.isBefore(ngayBatDau)) {
            return 2; // Sắp diễn ra
        }
        return 1; // Đang diễn ra
    }

    public static boolean isHinhThucCaNhan(String hinhThuc, List<Long> idKhachHangList) {
        if (idKhachHangList != null && !idKhachHangList.isEmpty()) {
            return true;
        }
        if (hinhThuc == null) return false;
        String s = hinhThuc.trim().toLowerCase();
        return s.contains("nhân") || s.contains("nhan") || s.contains("ca") || s.contains("cá");
    }

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

        // 4. Giảm tối đa (Bắt buộc khi là %)
        if (dto.getLoaiPhieuGiamGia() == 1) {
            if (dto.getGiamToiDa() == null) {
                throw new IllegalArgumentException("Mức giảm tối đa không được để trống khi giảm theo %!");
            }
            if (dto.getGiamToiDa().compareTo(new BigDecimal("1000")) < 0) {
                throw new IllegalArgumentException("Mức giảm tối đa phải tối thiểu từ 1.000 VNĐ!");
            }
            if (dto.getHoaDonToiThieu() != null && dto.getGiamToiDa().compareTo(dto.getHoaDonToiThieu()) >= 0) {
                throw new IllegalArgumentException("Mức giảm tối đa phải nhỏ hơn giá trị đơn hàng tối thiểu!");
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

        // 6. Số lượng sử dụng (Đối với Công khai)
        boolean isPersonal = isHinhThucCaNhan(dto.getHinhThuc(), dto.getIdKhachHangList());
        if (!isPersonal) {
            if (dto.getSoLuongSuDung() == null || dto.getSoLuongSuDung() <= 0) {
                throw new IllegalArgumentException("Hình thức Công khai yêu cầu nhập số lượng phát hành lớn hơn 0!");
            }
        }

        // 7. Ngày bắt đầu và Ngày kết thúc
        if (dto.getNgayBatDau() == null) {
            throw new IllegalArgumentException("Ngày bắt đầu không được để trống!");
        }
        if (dto.getNgayKetThuc() == null) {
            throw new IllegalArgumentException("Ngày kết thúc không được để trống!");
        }
        if (dto.getNgayKetThuc().isBefore(dto.getNgayBatDau())) {
            throw new IllegalArgumentException("Thời gian kết thúc phải diễn ra sau hoặc cùng thời điểm với thời gian bắt đầu!");
        }

        // 8. Khi tạo mới: Ngày bắt đầu không được là ngày trong quá khứ
        if (isCreate) {
            ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
            LocalDate today = LocalDate.now(zoneId);
            LocalDate startDate = dto.getNgayBatDau().atZone(zoneId).toLocalDate();
            if (startDate.isBefore(today)) {
                throw new IllegalArgumentException("Thời gian bắt đầu không được là ngày trong quá khứ (phải từ ngày hiện tại " + today + " trở đi)!");
            }
        }

        // 9. Khách hàng đối với phiếu cá nhân
        if (isPersonal) {
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

        List<Long> idKhachList = khachHangPhieuGiamGiaRepository.findIdKhachHangByIdPhieu(phieu.getId());
        List<String> listTenKhach = phieuGiamGiaRepository.findTenKhachHangByIdPhieu(phieu.getId());

        if ((idKhachList != null && !idKhachList.isEmpty()) || (listTenKhach != null && !listTenKhach.isEmpty())) {
            dto.setHinhThuc("Cá nhân");
            int count = (idKhachList != null && !idKhachList.isEmpty()) ? idKhachList.size() : listTenKhach.size();
            dto.setSoKhachHang(count);
            dto.setDanhSachKhachHang(listTenKhach != null ? listTenKhach : Collections.emptyList());
            dto.setIdKhachHangList(idKhachList != null ? idKhachList : Collections.emptyList());
        } else {
            dto.setHinhThuc("Công khai");
            dto.setSoKhachHang(0);
            dto.setDanhSachKhachHang(Collections.emptyList());
            dto.setIdKhachHangList(Collections.emptyList());
        }

        return dto;
    }

    @Transactional
    public List<PhieuGiamGiaDTO> getAllPhieuGiamGia() {
        List<PhieuGiamGia> list = phieuGiamGiaRepository.findAllByOrderByIdDesc();
        List<PhieuGiamGia> canCapNhat = new ArrayList<>();
        Instant now = Instant.now();

        for (PhieuGiamGia phieu : list) {
            // 1. Nếu phiếu đã quá ngày kết thúc -> Bắt buộc hết hạn (0)
            if (phieu.getNgayKetThuc() != null && now.isAfter(phieu.getNgayKetThuc())) {
                if (phieu.getTrangThai() == null || phieu.getTrangThai() != 0) {
                    phieu.setTrangThai(0);
                    canCapNhat.add(phieu);
                }
            } else if (phieu.getTrangThai() != null && phieu.getTrangThai() == 0) {
                // 2. Nếu người dùng đã chủ động bấm "Ngừng hoạt động" (0) và chưa hết hạn -> GIỮ NGUYÊN trạng thái 0
                continue;
            } else {
                // 3. Nếu đang hoạt động (1 hoặc 2) -> Tự động tính toán theo thời gian thực (1: Đang diễn ra, 2: Sắp diễn ra)
                int expected = tinhTrangThaiTheoThoiGian(phieu.getNgayBatDau(), phieu.getNgayKetThuc());
                if (phieu.getTrangThai() == null || phieu.getTrangThai() != expected) {
                    phieu.setTrangThai(expected);
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

    @Transactional
    public Page<PhieuGiamGiaDTO> getPhanTrangPhieuGiamGia(Pageable pageable) {
        Page<PhieuGiamGia> pageEntity = phieuGiamGiaRepository.findAllByOrderByIdDesc(pageable);
        return pageEntity.map(this::convertToDTO);
    }

    /**
     * Cron Job / Background Scheduler: Tự động quét và cập nhật trạng thái vòng đời
     * cho toàn bộ Phiếu giảm giá & Đợt giảm giá mỗi 30 giây.
     */
    @Scheduled(fixedRate = 30000)
    public void tuDongCapNhatTrangThaiVongDoi() {
        Instant now = Instant.now();

        // 1. Quét và cập nhật Phiếu giảm giá
        List<PhieuGiamGia> listPhieu = phieuGiamGiaRepository.findAll();
        List<PhieuGiamGia> canLuuPhieu = new ArrayList<>();
        for (PhieuGiamGia p : listPhieu) {
            if (p.getNgayKetThuc() != null && now.isAfter(p.getNgayKetThuc())) {
                if (p.getTrangThai() == null || p.getTrangThai() != 0) {
                    p.setTrangThai(0);
                    canLuuPhieu.add(p);
                }
            } else if (p.getTrangThai() != null && p.getTrangThai() == 0) {
                // Bị ngừng hoạt động thủ công -> Không tự động bật lại
                continue;
            } else {
                int trangThaiChuan = tinhTrangThaiTheoThoiGian(p.getNgayBatDau(), p.getNgayKetThuc());
                if (p.getTrangThai() == null || p.getTrangThai() != trangThaiChuan) {
                    p.setTrangThai(trangThaiChuan);
                    canLuuPhieu.add(p);
                }
            }
        }
        if (!canLuuPhieu.isEmpty()) {
            phieuGiamGiaRepository.saveAll(canLuuPhieu);
            System.out.println("==> [Scheduler] Tự động cập nhật trạng thái vòng đời cho " + canLuuPhieu.size() + " phiếu giảm giá.");
        }

        // 2. Quét và cập nhật Đợt giảm giá
        List<DotGiamGia> listDot = dotGiamGiaRepository.findAll();
        List<DotGiamGia> canLuuDot = new ArrayList<>();
        for (DotGiamGia d : listDot) {
            if (d.getNgayKetThuc() != null && now.isAfter(d.getNgayKetThuc())) {
                if (d.getTrangThai() == null || d.getTrangThai() != 0) {
                    d.setTrangThai(0);
                    canLuuDot.add(d);
                }
            } else if (d.getTrangThai() != null && d.getTrangThai() == 0) {
                continue;
            } else {
                int trangThaiChuan = tinhTrangThaiTheoThoiGian(d.getNgayBatDau(), d.getNgayKetThuc());
                if (d.getTrangThai() == null || d.getTrangThai() != trangThaiChuan) {
                    d.setTrangThai(trangThaiChuan);
                    canLuuDot.add(d);
                }
            }
        }
        if (!canLuuDot.isEmpty()) {
            dotGiamGiaRepository.saveAll(canLuuDot);
            System.out.println("==> [Scheduler] Tự động cập nhật trạng thái vòng đời cho " + canLuuDot.size() + " đợt giảm giá.");
        }
    }

    @Transactional
    public Optional<PhieuGiamGiaDTO> getById(Long id) {
        return phieuGiamGiaRepository.findById(id).map(phieu -> {
            Instant now = Instant.now();
            if (phieu.getNgayKetThuc() != null && now.isAfter(phieu.getNgayKetThuc())) {
                if (phieu.getTrangThai() == null || phieu.getTrangThai() != 0) {
                    phieu.setTrangThai(0);
                    phieuGiamGiaRepository.save(phieu);
                }
            } else if (phieu.getTrangThai() != null && phieu.getTrangThai() == 0) {
                // Người dùng tắt thủ công -> Giữ nguyên 0
            } else {
                int expected = tinhTrangThaiTheoThoiGian(phieu.getNgayBatDau(), phieu.getNgayKetThuc());
                if (phieu.getTrangThai() == null || phieu.getTrangThai() != expected) {
                    phieu.setTrangThai(expected);
                    phieuGiamGiaRepository.save(phieu);
                }
            }
            return convertToDTO(phieu);
        });
    }

    @Transactional
    public PhieuGiamGiaDTO toggleTrangThai(Long id) {
        Optional<PhieuGiamGia> optional = phieuGiamGiaRepository.findById(id);
        if (optional.isPresent()) {
            PhieuGiamGia phieu = optional.get();
            // Nếu đã quá hạn thì không bật lại được
            Instant now = Instant.now();
            boolean isDeactivating = false;
            if (phieu.getNgayKetThuc() != null && now.isAfter(phieu.getNgayKetThuc())) {
                phieu.setTrangThai(0);
            } else {
                // Nếu đang hoạt động (1 hoặc 2) -> Ngừng hoạt động (0)
                // Nếu đang ngừng hoạt động (0) -> Kích hoạt lại theo thời gian thực (1: Đang diễn ra, hoặc 2: Sắp diễn ra)
                if (phieu.getTrangThai() != null && phieu.getTrangThai() != 0) {
                    phieu.setTrangThai(0);
                    isDeactivating = true;
                } else {
                    int newStatus = tinhTrangThaiTheoThoiGian(phieu.getNgayBatDau(), phieu.getNgayKetThuc());
                    phieu.setTrangThai(newStatus);
                }
            }
            PhieuGiamGia saved = phieuGiamGiaRepository.save(phieu);

            // Nếu vừa chuyển sang ngừng hoạt động -> gửi email thông báo cho khách hàng sở hữu voucher
            if (isDeactivating) {
                List<KhachHangPhieuGiamGia> currentLinks = khachHangPhieuGiamGiaRepository.findByIdPhieuGiamGia_Id(id);
                for (KhachHangPhieuGiamGia link : currentLinks) {
                    KhachHang kh = link.getIdKhachHang();
                    if (kh != null && kh.getEmail() != null && !kh.getEmail().trim().isEmpty()) {
                        emailService.sendVoucherDeactivatedEmail(kh, saved);
                    }
                }
            }

            return convertToDTO(saved);
        }
        return null;
    }

    @Transactional
    public PhieuGiamGiaDTO updatePhieuGiamGia(Long id, PhieuGiamGiaDTO dto) {
        validatePhieuGiamGiaDTO(dto, false);

        Optional<PhieuGiamGia> optional = phieuGiamGiaRepository.findById(id);
        if (optional.isPresent()) {
            PhieuGiamGia phieu = optional.get();

            // Kiểm tra xem thông tin cốt lõi của voucher có thay đổi không (để quyết định gửi email cập nhật)
            boolean hasVoucherChanged = !Objects.equals(phieu.getTenPhieuGiamGia(), dto.getTenPhieuGiamGia().trim())
                    || !Objects.equals(phieu.getLoaiPhieuGiamGia(), dto.getLoaiPhieuGiamGia())
                    || phieu.getGiaTriGiamGia().compareTo(dto.getGiaTriGiamGia()) != 0
                    || !Objects.equals(phieu.getNgayBatDau(), dto.getNgayBatDau())
                    || !Objects.equals(phieu.getNgayKetThuc(), dto.getNgayKetThuc())
                    || (dto.getGiamToiDa() != null && (phieu.getGiamToiDa() == null || phieu.getGiamToiDa().compareTo(dto.getGiamToiDa()) != 0))
                    || phieu.getHoaDonToiThieu().compareTo(dto.getHoaDonToiThieu()) != 0;

            phieu.setTenPhieuGiamGia(dto.getTenPhieuGiamGia().trim());
            phieu.setLoaiPhieuGiamGia(dto.getLoaiPhieuGiamGia());
            phieu.setGiaTriGiamGia(dto.getGiaTriGiamGia());
            phieu.setGiamToiDa(dto.getLoaiPhieuGiamGia() == 1 ? dto.getGiamToiDa() : null);
            phieu.setHoaDonToiThieu(dto.getHoaDonToiThieu());
            phieu.setNgayBatDau(dto.getNgayBatDau());
            phieu.setNgayKetThuc(dto.getNgayKetThuc());
            // Trạng thái tự động theo ngày giờ (nếu trước đó đã tắt thủ công và chưa hết hạn thì giữ nguyên 0)
            if (phieu.getTrangThai() != null && phieu.getTrangThai() == 0 && dto.getNgayKetThuc() != null && !Instant.now().isAfter(dto.getNgayKetThuc())) {
                phieu.setTrangThai(0);
            } else {
                phieu.setTrangThai(tinhTrangThaiTheoThoiGian(dto.getNgayBatDau(), dto.getNgayKetThuc()));
            }

            boolean isPersonal = isHinhThucCaNhan(dto.getHinhThuc(), dto.getIdKhachHangList());
            List<KhachHangPhieuGiamGia> currentLinks = khachHangPhieuGiamGiaRepository.findByIdPhieuGiamGia_Id(id);
            Map<Long, KhachHangPhieuGiamGia> oldMap = new HashMap<>();
            for (KhachHangPhieuGiamGia l : currentLinks) {
                oldMap.put(l.getIdKhachHang().getId(), l);
            }
            Set<Long> oldCustomerIds = oldMap.keySet();
            Set<Long> newCustomerIds = (isPersonal && dto.getIdKhachHangList() != null)
                    ? new HashSet<>(dto.getIdKhachHangList()) : new HashSet<>();

            if (isPersonal) {
                // Cá nhân: Số lượng tự động khóa và tính theo số khách hàng được chọn
                phieu.setSoLuongSuDung(newCustomerIds.size());

                // Khách hàng mới được thêm: Gửi email nhận voucher mới
                Set<Long> addedIds = new HashSet<>(newCustomerIds);
                addedIds.removeAll(oldCustomerIds);
                for (Long khId : addedIds) {
                    Optional<KhachHang> khOpt = khachHangRepository.findById(khId);
                    if (khOpt.isPresent()) {
                        KhachHang kh = khOpt.get();
                        KhachHangPhieuGiamGia link = new KhachHangPhieuGiamGia();
                        link.setIdPhieuGiamGia(phieu);
                        link.setIdKhachHang(kh);
                        link.setTrangThai(1);
                        khachHangPhieuGiamGiaRepository.save(link);
                        emailService.sendVoucherIssuedEmail(kh, phieu);
                    }
                }

                // Khách hàng bị gỡ bỏ: Gửi email thông báo hủy/thu hồi voucher
                Set<Long> removedIds = new HashSet<>(oldCustomerIds);
                removedIds.removeAll(newCustomerIds);
                for (Long khId : removedIds) {
                    KhachHangPhieuGiamGia link = oldMap.get(khId);
                    if (link != null) {
                        KhachHang kh = link.getIdKhachHang();
                        khachHangPhieuGiamGiaRepository.delete(link);
                        emailService.sendVoucherRevokedEmail(kh, phieu);
                    }
                }

                // Khách hàng giữ nguyên: Nếu voucher có thay đổi thông tin thì gửi email cập nhật
                if (hasVoucherChanged) {
                    Set<Long> keptIds = new HashSet<>(oldCustomerIds);
                    keptIds.retainAll(newCustomerIds);
                    for (Long khId : keptIds) {
                        KhachHang kh = oldMap.get(khId).getIdKhachHang();
                        emailService.sendVoucherUpdatedEmail(kh, phieu);
                    }
                }
            } else {
                // Công khai: Số lượng nhập tay tự do
                int soLuong = (dto.getSoLuongSuDung() != null && dto.getSoLuongSuDung() > 0) ? dto.getSoLuongSuDung() : 0;
                phieu.setSoLuongSuDung(soLuong);

                // Nếu chuyển từ Cá nhân sang Công khai: Thu hồi tất cả liên kết cũ
                if (!oldCustomerIds.isEmpty()) {
                    for (KhachHangPhieuGiamGia link : currentLinks) {
                        KhachHang kh = link.getIdKhachHang();
                        khachHangPhieuGiamGiaRepository.delete(link);
                        emailService.sendVoucherRevokedEmail(kh, phieu);
                    }
                }
            }

            PhieuGiamGia saved = phieuGiamGiaRepository.save(phieu);
            return convertToDTO(saved);
        }
        return null;
    }

    public List<KhachHangDTO> getAllKhachHang() {
        List<Object[]> rows = khachHangRepository.findKhachHangChiTietMuaHang();
        List<KhachHangDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            KhachHangDTO dto = new KhachHangDTO();
            dto.setId(row[0] != null ? ((Number) row[0]).longValue() : null);
            dto.setMaKhachHang(row[1] != null ? row[1].toString() : null);
            dto.setTenKhachHang(row[2] != null ? row[2].toString() : "");
            dto.setSoDienThoai(row[3] != null ? row[3].toString() : null);
            dto.setEmail(row[4] != null ? row[4].toString() : null);
            if (row[5] != null) {
                if (row[5] instanceof java.sql.Date sqlDate) {
                    dto.setNgaySinh(sqlDate.toLocalDate());
                } else if (row[5] instanceof LocalDate ld) {
                    dto.setNgaySinh(ld);
                }
            }
            dto.setGioiTinh(row[6] != null ? (Boolean) row[6] : true);
            dto.setTrangThai(row[7] != null ? ((Number) row[7]).intValue() : 1);
            dto.setTongDonMua(row[8] != null ? ((Number) row[8]).longValue() : 0L);
            if (row[9] != null) {
                if (row[9] instanceof java.sql.Timestamp ts) {
                    dto.setNgayMuaGanNhat(ts.toInstant());
                } else if (row[9] instanceof Instant inst) {
                    dto.setNgayMuaGanNhat(inst);
                }
            }
            result.add(dto);
        }
        return result;
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
        phieu.setNgayBatDau(dto.getNgayBatDau());
        phieu.setNgayKetThuc(dto.getNgayKetThuc());
        // Trạng thái tự động theo ngày giờ
        phieu.setTrangThai(tinhTrangThaiTheoThoiGian(dto.getNgayBatDau(), dto.getNgayKetThuc()));

        boolean isPersonal = isHinhThucCaNhan(dto.getHinhThuc(), dto.getIdKhachHangList());
        if (isPersonal) {
            // Cá nhân: Số lượng tự động tính theo số khách hàng được chọn
            int soKhach = (dto.getIdKhachHangList() != null) ? dto.getIdKhachHangList().size() : 0;
            phieu.setSoLuongSuDung(soKhach);
        } else {
            int soLuongMoi = (dto.getSoLuongSuDung() != null && dto.getSoLuongSuDung() > 0) ? dto.getSoLuongSuDung() : 0;
            phieu.setSoLuongSuDung(soLuongMoi);
        }

        PhieuGiamGia saved = phieuGiamGiaRepository.save(phieu);

        // Nếu là phiếu cá nhân: gán khách hàng và gửi email thông báo tự động
        if (isPersonal && dto.getIdKhachHangList() != null && !dto.getIdKhachHangList().isEmpty()) {
            for (Long idKhach : dto.getIdKhachHangList()) {
                Optional<KhachHang> khOpt = khachHangRepository.findById(idKhach);
                if (khOpt.isPresent()) {
                    KhachHang kh = khOpt.get();
                    KhachHangPhieuGiamGia link = new KhachHangPhieuGiamGia();
                    link.setIdPhieuGiamGia(saved);
                    link.setIdKhachHang(kh);
                    link.setTrangThai(1);
                    khachHangPhieuGiamGiaRepository.save(link);

                    // Tự động gửi email tặng voucher
                    emailService.sendVoucherIssuedEmail(kh, saved);
                }
            }
        }

        return convertToDTO(saved);
    }
}
