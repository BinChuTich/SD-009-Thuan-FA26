package com.example.sd009thuan.service.impl;

import com.example.sd009thuan.dto.chat.*;
import com.example.sd009thuan.entity.CuocHoiThoai;
import com.example.sd009thuan.entity.KhachHang;
import com.example.sd009thuan.entity.NhanVien;
import com.example.sd009thuan.entity.TinNhan;
import com.example.sd009thuan.repository.CuocHoiThoaiRepository;
import com.example.sd009thuan.repository.KhachHangRepository;
import com.example.sd009thuan.repository.NhanVienRepository;
import com.example.sd009thuan.repository.TinNhanRepository;
import com.example.sd009thuan.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.example.sd009thuan.entity.HoaDon;
import com.example.sd009thuan.repository.HoaDonRepository;
import java.time.ZoneId;
import java.util.ArrayList;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final CuocHoiThoaiRepository cuocHoiThoaiRepository;
    private final TinNhanRepository tinNhanRepository;
    private final KhachHangRepository khachHangRepository;
    private final NhanVienRepository nhanVienRepository;
    private final HoaDonRepository hoaDonRepository;


    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    @Override
    @Transactional
    public CuocHoiThoaiDTO khoiTaoHoacLayCuocHoiThoai(KhoiTaoChatRequest req) {
        CuocHoiThoai cht = null;

        // 1. Nếu client đã lưu ID cuộc hội thoại trước đó và chưa đóng, tái sử dụng
        if (req != null && req.getCuocHoiThoaiId() != null) {
            Optional<CuocHoiThoai> byId = cuocHoiThoaiRepository.findById(req.getCuocHoiThoaiId());
            if (byId.isPresent() && byId.get().getTrangThai() != null && byId.get().getTrangThai() != 0) {
                cht = byId.get();
                if (req.getTenKhachHang() != null && !req.getTenKhachHang().trim().isEmpty()) {
                    cht.setTenKhachHang(req.getTenKhachHang().trim());
                    cht = cuocHoiThoaiRepository.save(cht);
                }
                return toCuocHoiThoaiDTO(cht);
            }
        }

        // 2. Tìm theo khách hàng đã đăng nhập đang mở (trạng thái 1 hoặc 2)
        if (req != null && req.getKhachHangId() != null) {
            Optional<CuocHoiThoai> opt = cuocHoiThoaiRepository.findTopByKhachHang_IdAndTrangThaiInOrderByIdDesc(req.getKhachHangId(), List.of(1, 2));
            if (opt.isPresent()) {
                cht = opt.get();
            } else {
                KhachHang kh = khachHangRepository.findById(req.getKhachHangId()).orElse(null);
                cht = new CuocHoiThoai();
                cht.setKhachHang(kh);
                cht.setTenKhachHang(kh != null ? kh.getTenKhachHang() : (req.getTenKhachHang() != null ? req.getTenKhachHang() : "Khách hàng"));
                cht.setSoDienThoai(kh != null ? kh.getSoDienThoai() : req.getSoDienThoai());
                cht.setTrangThai(1);
                cht = cuocHoiThoaiRepository.save(cht);
                guiTinNhanTuDongChaoMung(cht);
            }
        } else if (req != null && req.getSoDienThoai() != null && !req.getSoDienThoai().trim().isEmpty()) {
            String sdt = req.getSoDienThoai().trim();
            Optional<CuocHoiThoai> opt = cuocHoiThoaiRepository.findTopBySoDienThoaiAndTrangThaiInOrderByIdDesc(sdt, List.of(1, 2));
            if (opt.isPresent()) {
                cht = opt.get();
                if (req.getTenKhachHang() != null && !req.getTenKhachHang().trim().isEmpty()) {
                    cht.setTenKhachHang(req.getTenKhachHang().trim());
                    cht = cuocHoiThoaiRepository.save(cht);
                }
            } else {
                cht = new CuocHoiThoai();
                cht.setTenKhachHang(req.getTenKhachHang() != null && !req.getTenKhachHang().trim().isEmpty() ? req.getTenKhachHang().trim() : "Khách " + sdt);
                cht.setSoDienThoai(sdt);
                cht.setTrangThai(1);
                cht = cuocHoiThoaiRepository.save(cht);
                guiTinNhanTuDongChaoMung(cht);
            }
        } else {
            cht = new CuocHoiThoai();
            String ten = (req != null && req.getTenKhachHang() != null && !req.getTenKhachHang().trim().isEmpty())
                    ? req.getTenKhachHang().trim()
                    : "Khách trực tuyến #" + (System.currentTimeMillis() % 10000);
            cht.setTenKhachHang(ten);
            cht.setTrangThai(1);
            cht = cuocHoiThoaiRepository.save(cht);
            guiTinNhanTuDongChaoMung(cht);
        }

        return toCuocHoiThoaiDTO(cht);
    }

    private void guiTinNhanTuDongChaoMung(CuocHoiThoai cht) {
        TinNhan tn = new TinNhan();
        tn.setCuocHoiThoai(cht);
        tn.setNguoiGui("AI");
        tn.setNoiDung("Xin chào quý khách! Cảm ơn bạn đã ghé thăm FF T-Shirt. Chúng tôi có thể hỗ trợ gì cho bạn về sản phẩm, tư vấn chọn size hay theo dõi đơn hàng hôm nay ạ?");
        tn.setDaDoc(true);
        tinNhanRepository.save(tn);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuocHoiThoaiDTO> layDanhSachCuocHoiThoai(String keyword, Integer trangThai) {
        List<CuocHoiThoai> list = cuocHoiThoaiRepository.searchCuocHoiThoai(keyword, trangThai);
        return list.stream().map(this::toCuocHoiThoaiDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CuocHoiThoaiDTO layChiTietCuocHoiThoai(Long id) {
        CuocHoiThoai cht = cuocHoiThoaiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc hội thoại ID: " + id));
        return toCuocHoiThoaiDTO(cht);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TinNhanDTO> layDanhSachTinNhan(Long cuocHoiThoaiId, Long lastId) {
        List<TinNhan> list;
        if (lastId != null && lastId > 0) {
            list = tinNhanRepository.findByCuocHoiThoai_IdAndIdGreaterThanOrderByNgayTaoAsc(cuocHoiThoaiId, lastId);
        } else {
            list = tinNhanRepository.findByCuocHoiThoai_IdOrderByNgayTaoAsc(cuocHoiThoaiId);
        }
        return list.stream().map(this::toTinNhanDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TinNhanDTO guiTinNhan(GuiTinNhanRequest req) {
        if (req == null || req.getNoiDung() == null || req.getNoiDung().trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung tin nhắn không được để trống");
        }

        CuocHoiThoai cht;
        if (req.getCuocHoiThoaiId() != null) {
            cht = cuocHoiThoaiRepository.findById(req.getCuocHoiThoaiId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc hội thoại ID: " + req.getCuocHoiThoaiId()));
        } else {
            KhoiTaoChatRequest initReq = new KhoiTaoChatRequest();
            initReq.setKhachHangId(req.getKhachHangId());
            initReq.setTenKhachHang(req.getTenKhachHang());
            initReq.setSoDienThoai(req.getSoDienThoai());
            CuocHoiThoaiDTO dto = khoiTaoHoacLayCuocHoiThoai(initReq);
            cht = cuocHoiThoaiRepository.findById(dto.getId()).orElseThrow();
        }

        NhanVien nv = null;
        if (req.getNhanVienId() != null) {
            nv = nhanVienRepository.findById(req.getNhanVienId()).orElse(null);
            if (nv != null && cht.getNhanVien() == null) {
                cht.setNhanVien(nv);
            }
        }

        String nguoiGui = (req.getNguoiGui() != null && !req.getNguoiGui().trim().isEmpty())
                ? req.getNguoiGui().trim().toUpperCase()
                : "KHACH_HANG";

        if ("NHAN_VIEN".equals(nguoiGui) || "ADMIN".equals(nguoiGui)) {
            // Khi nhân viên trả lời, nếu phiên đang chờ nhận hoặc đã đóng, tự động kích hoạt Đang hoạt động (1)
            if (cht.getTrangThai() == null || cht.getTrangThai() != 1) {
                cht.setTrangThai(1);
            }
        } else if ("KHACH_HANG".equals(nguoiGui)) {
            // Nếu phiên đã đóng mà khách nhắn lại, chuyển sang Chờ nhận (2)
            if (cht.getTrangThai() != null && cht.getTrangThai() == 0) {
                cht.setTrangThai(2);
            }
        }

        TinNhan tn = new TinNhan();
        tn.setCuocHoiThoai(cht);
        tn.setNhanVien(nv);
        tn.setNguoiGui(nguoiGui);
        tn.setNoiDung(req.getNoiDung().trim());
        tn.setDaDoc(false);

        TinNhan saved = tinNhanRepository.save(tn);

        cht.setNgayCapNhat(LocalDateTime.now());
        cuocHoiThoaiRepository.save(cht);

        return toTinNhanDTO(saved);
    }

    @Override
    @Transactional
    public void danhDauDaDoc(Long cuocHoiThoaiId, String role) {
        if (cuocHoiThoaiId == null) return;
        if ("ADMIN".equalsIgnoreCase(role) || "NHAN_VIEN".equalsIgnoreCase(role)) {
            tinNhanRepository.danhDauAdminDaDoc(cuocHoiThoaiId);
        } else {
            tinNhanRepository.danhDauKhachHangDaDoc(cuocHoiThoaiId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ThongBaoAdminDTO layThongBaoAdmin() {
        long countUnread = tinNhanRepository.countByDaDocFalseAndNguoiGui("KHACH_HANG");
        long countNewOrders = 0;
        try {
            countNewOrders = hoaDonRepository.countByTrangThai(1);
        } catch (Exception e) {}

        List<TinNhan> latest = tinNhanRepository.findLatestMessages(PageRequest.of(0, 6));
        List<HoaDon> recentOrders = new ArrayList<>();
        try {
            recentOrders = hoaDonRepository.findRecentOrders(PageRequest.of(0, 6));
        } catch (Exception e) {}

        List<ThongBaoAdminDTO.ThongBaoItemDTO> items = new ArrayList<>();

        // 1. Thêm các đơn hàng mới (Trạng thái 1 = Chờ xác nhận)
        if (recentOrders != null) {
            for (HoaDon hd : recentOrders) {
                if (hd.getTrangThai() != null && hd.getTrangThai() == 1) {
                    String name = (hd.getTenKhachHang() != null && !hd.getTenKhachHang().trim().isEmpty())
                            ? hd.getTenKhachHang()
                            : (hd.getIdKhachHang() != null ? hd.getIdKhachHang().getTenKhachHang() : "Khách hàng");
                    String timeStr = hd.getNgayTao() != null
                            ? DateTimeFormatter.ofPattern("HH:mm dd/MM").format(hd.getNgayTao().atZone(ZoneId.systemDefault()))
                            : "";
                    String moneyStr = hd.getTienSauGiamGia() != null
                            ? String.format("%,d đ", hd.getTienSauGiamGia().longValue()).replace(',', '.')
                            : "0 đ";

                    items.add(ThongBaoAdminDTO.ThongBaoItemDTO.builder()
                            .id(hd.getId())
                            .loai("DON_HANG")
                            .tieuDe("Đơn hàng mới #" + hd.getMaHoaDon())
                            .noiDung("Khách: " + name + " • " + moneyStr)
                            .thoiGian(timeStr)
                            .daDoc(false)
                            .maDonHang(hd.getMaHoaDon())
                            .lienKet("/hoa-don?search=" + hd.getMaHoaDon())
                            .build());
                }
            }
        }

        // 2. Thêm các tin nhắn chat mới
        if (latest != null) {
            for (TinNhan t : latest) {
                CuocHoiThoai c = t.getCuocHoiThoai();
                String name = (c != null && c.getTenKhachHang() != null) ? c.getTenKhachHang() : "Khách hàng";
                String timeStr = t.getNgayTao() != null ? t.getNgayTao().format(DateTimeFormatter.ofPattern("HH:mm dd/MM")) : "";

                items.add(ThongBaoAdminDTO.ThongBaoItemDTO.builder()
                        .id(t.getId())
                        .loai("CHAT")
                        .tieuDe("Tin nhắn từ " + name)
                        .noiDung(t.getNoiDung())
                        .thoiGian(timeStr)
                        .daDoc(Boolean.TRUE.equals(t.getDaDoc()))
                        .cuocHoiThoaiId(c != null ? c.getId() : null)
                        .lienKet("/tin-nhan?id=" + (c != null ? c.getId() : ""))
                        .build());
            }
        }

        List<ThongBaoAdminDTO.TinNhanThongBaoDTO> dtoList = latest.stream().map(t -> {
            CuocHoiThoai c = t.getCuocHoiThoai();
            String name = (c != null && c.getTenKhachHang() != null) ? c.getTenKhachHang() : "Khách hàng";
            String timeStr = t.getNgayTao() != null ? t.getNgayTao().format(TIME_FORMATTER) : "";
            return ThongBaoAdminDTO.TinNhanThongBaoDTO.builder()
                    .tinNhanId(t.getId())
                    .cuocHoiThoaiId(c != null ? c.getId() : null)
                    .tenKhachHang(name)
                    .noiDung(t.getNoiDung())
                    .thoiGian(timeStr)
                    .daDoc(Boolean.TRUE.equals(t.getDaDoc()))
                    .build();
        }).collect(Collectors.toList());

        long totalCount = countUnread + countNewOrders;

        return ThongBaoAdminDTO.builder()
                .tongSoThongBao(totalCount)
                .soTinChuaDoc(countUnread)
                .soDonHangMoi(countNewOrders)
                .danhSachThongBao(items)
                .danhSachMoi(dtoList)
                .build();
    }

    @Override
    @Transactional
    public void dongCuocHoiThoai(Long id) {
        CuocHoiThoai cht = cuocHoiThoaiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc hội thoại ID: " + id));
        cht.setTrangThai(0);
        cht.setNgayCapNhat(LocalDateTime.now());
        CuocHoiThoai saved = cuocHoiThoaiRepository.save(cht);

        TinNhan tn = new TinNhan();
        tn.setCuocHoiThoai(saved);
        tn.setNguoiGui("AI");
        tn.setNoiDung("Phiên hỗ trợ đã kết thúc. Cảm ơn quý khách đã tin tưởng và đồng hành cùng FF T-Shirt!");
        tn.setDaDoc(true);
        tinNhanRepository.save(tn);
    }

    @Override
    @Transactional
    public CuocHoiThoaiDTO tiepNhanCuocHoiThoai(Long id, Long nhanVienId) {
        CuocHoiThoai cht = cuocHoiThoaiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc hội thoại ID: " + id));

        NhanVien nv = null;
        if (nhanVienId != null) {
            nv = nhanVienRepository.findById(nhanVienId).orElse(null);
            if (nv != null) {
                cht.setNhanVien(nv);
            }
        }

        cht.setTrangThai(1); // 1 = Đang hoạt động
        cht.setNgayCapNhat(LocalDateTime.now());
        CuocHoiThoai saved = cuocHoiThoaiRepository.save(cht);

        // Tạo tin nhắn thông báo từ hệ thống AI
        TinNhan tn = new TinNhan();
        tn.setCuocHoiThoai(saved);
        tn.setNhanVien(nv);
        tn.setNguoiGui("AI");
        String name = (nv != null && nv.getTenNhanVien() != null) ? nv.getTenNhanVien() : "tư vấn";
        tn.setNoiDung("Nhân viên " + name + " đã tiếp nhận hỗ trợ bạn.");
        tn.setDaDoc(true);
        tinNhanRepository.save(tn);

        return toCuocHoiThoaiDTO(saved);
    }

    @Override
    @Transactional
    public CuocHoiThoaiDTO yeuCauHoTro(Long id) {
        CuocHoiThoai cht = cuocHoiThoaiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc hội thoại ID: " + id));

        cht.setTrangThai(2); // 2 = Chờ nhận
        cht.setNgayCapNhat(LocalDateTime.now());
        CuocHoiThoai saved = cuocHoiThoaiRepository.save(cht);

        Optional<TinNhan> lastMsg = tinNhanRepository.findTopByCuocHoiThoai_IdOrderByNgayTaoDesc(saved.getId());
        boolean alreadyNotified = lastMsg.isPresent() &&
                "AI".equals(lastMsg.get().getNguoiGui()) &&
                lastMsg.get().getNoiDung() != null &&
                (lastMsg.get().getNoiDung().contains("chuyên viên") || lastMsg.get().getNoiDung().contains("nhân viên"));

        if (!alreadyNotified) {
            TinNhan tn = new TinNhan();
            tn.setCuocHoiThoai(saved);
            tn.setNguoiGui("AI");
            tn.setNoiDung("Chuyên viên tư vấn của FF T-Shirt đã nhận được yêu cầu và sẽ hỗ trợ anh/chị ngay trong giây lát nhé! ❤️");
            tn.setDaDoc(false);
            tinNhanRepository.save(tn);
        }

        return toCuocHoiThoaiDTO(saved);
    }

    private CuocHoiThoaiDTO toCuocHoiThoaiDTO(CuocHoiThoai c) {
        if (c == null) return null;

        Optional<TinNhan> lastMsg = tinNhanRepository.findTopByCuocHoiThoai_IdOrderByNgayTaoDesc(c.getId());
        long unread = tinNhanRepository.countByCuocHoiThoai_IdAndDaDocFalseAndNguoiGui(c.getId(), "KHACH_HANG");

        return CuocHoiThoaiDTO.builder()
                .id(c.getId())
                .khachHangId(c.getKhachHang() != null ? c.getKhachHang().getId() : null)
                .tenKhachHang(c.getTenKhachHang() != null ? c.getTenKhachHang() : "Khách hàng")
                .soDienThoai(c.getSoDienThoai())
                .nhanVienId(c.getNhanVien() != null ? c.getNhanVien().getId() : null)
                .tenNhanVien(c.getNhanVien() != null ? c.getNhanVien().getTenNhanVien() : null)
                .trangThai(c.getTrangThai())
                .ngayTao(c.getNgayTao())
                .ngayCapNhat(c.getNgayCapNhat())
                .tinNhanCuoi(lastMsg.map(TinNhan::getNoiDung).orElse(""))
                .thoiGianTinNhanCuoi(lastMsg.map(TinNhan::getNgayTao).orElse(c.getNgayCapNhat()))
                .soTinChuaDoc(unread)
                .build();
    }

    private TinNhanDTO toTinNhanDTO(TinNhan t) {
        if (t == null) return null;
        return TinNhanDTO.builder()
                .id(t.getId())
                .cuocHoiThoaiId(t.getCuocHoiThoai() != null ? t.getCuocHoiThoai().getId() : null)
                .nhanVienId(t.getNhanVien() != null ? t.getNhanVien().getId() : null)
                .tenNhanVien(t.getNhanVien() != null ? t.getNhanVien().getTenNhanVien() : null)
                .nguoiGui(t.getNguoiGui())
                .noiDung(t.getNoiDung())
                .daDoc(t.getDaDoc())
                .ngayTao(t.getNgayTao())
                .build();
    }
}

