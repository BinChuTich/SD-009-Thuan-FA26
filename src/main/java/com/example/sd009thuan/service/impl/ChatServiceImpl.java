package com.example.sd009thuan.service.impl;

import com.example.sd009thuan.dto.chat.*;
import com.example.sd009thuan.entity.HoaDon;
import com.example.sd009thuan.entity.NhanVien;
import com.example.sd009thuan.repository.HoaDonRepository;
import com.example.sd009thuan.repository.NhanVienRepository;
import com.example.sd009thuan.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final HoaDonRepository hoaDonRepository;
    private final NhanVienRepository nhanVienRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // In-memory thread-safe storage (HOÀN TOÀN KHÔNG SỬA VÀO DATABASE GỐC)
    private final Map<Long, CuocHoiThoaiDTO> conversations = new ConcurrentHashMap<>();
    private final Map<Long, List<TinNhanDTO>> messagesByConv = new ConcurrentHashMap<>();
    private final AtomicLong convIdSeq = new AtomicLong(1);
    private final AtomicLong msgIdSeq = new AtomicLong(1);

    @Override
    public CuocHoiThoaiDTO khoiTaoHoacLayCuocHoiThoai(KhoiTaoChatRequest req) {
        if (req != null && req.getCuocHoiThoaiId() != null) {
            CuocHoiThoaiDTO existing = conversations.get(req.getCuocHoiThoaiId());
            if (existing != null) {
                if (req.getTenKhachHang() != null && !req.getTenKhachHang().trim().isEmpty()) {
                    existing.setTenKhachHang(req.getTenKhachHang().trim());
                }
                if (req.getKhachHangId() != null) {
                    existing.setKhachHangId(req.getKhachHangId());
                }
                if (req.getSoDienThoai() != null && !req.getSoDienThoai().trim().isEmpty()) {
                    existing.setSoDienThoai(req.getSoDienThoai().trim());
                }
                return enrichDTO(existing);
            }
        }

        // Tạo cuộc hội thoại mới
        long newId = convIdSeq.getAndIncrement();
        String name = (req != null && req.getTenKhachHang() != null && !req.getTenKhachHang().trim().isEmpty())
                ? req.getTenKhachHang().trim()
                : "Khách trực tuyến #" + (1000 + (newId % 9000));
        String phone = req != null ? req.getSoDienThoai() : null;
        Long khId = req != null ? req.getKhachHangId() : null;

        CuocHoiThoaiDTO conv = CuocHoiThoaiDTO.builder()
                .id(newId)
                .khachHangId(khId)
                .nhanVienId(null)
                .tenKhachHang(name)
                .soDienThoai(phone)
                .trangThai(1) // 1 = Đang hoạt động
                .ngayTao(LocalDateTime.now())
                .ngayCapNhat(LocalDateTime.now())
                .soTinChuaDoc(0L)
                .build();

        conversations.put(newId, conv);
        messagesByConv.put(newId, new CopyOnWriteArrayList<>());

        // Tin nhắn chào mừng ban đầu từ hệ thống AI
        TinNhanDTO welcomeMsg = TinNhanDTO.builder()
                .id(msgIdSeq.getAndIncrement())
                .cuocHoiThoaiId(newId)
                .nguoiGui("AI")
                .noiDung("Xin chào quý khách! Cảm ơn bạn đã ghé thăm FF T-Shirt. Chúng tôi có thể hỗ trợ gì cho bạn về sản phẩm, tư vấn chọn size hay theo dõi đơn hàng hôm nay ạ?")
                .daDoc(true)
                .ngayTao(LocalDateTime.now())
                .build();

        messagesByConv.get(newId).add(welcomeMsg);
        conv.setTinNhanCuoi(welcomeMsg.getNoiDung());
        conv.setThoiGianTinNhanCuoi(welcomeMsg.getNgayTao());

        return enrichDTO(conv);
    }

    @Override
    public List<CuocHoiThoaiDTO> layDanhSachCuocHoiThoai(String keyword, Integer trangThai) {
        return conversations.values().stream()
                .filter(c -> {
                    if (trangThai != null && !Objects.equals(c.getTrangThai(), trangThai)) {
                        return false;
                    }
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String k = keyword.trim().toLowerCase();
                        boolean matchName = c.getTenKhachHang() != null && c.getTenKhachHang().toLowerCase().contains(k);
                        boolean matchPhone = c.getSoDienThoai() != null && c.getSoDienThoai().contains(k);
                        return matchName || matchPhone;
                    }
                    return true;
                })
                .sorted((a, b) -> {
                    LocalDateTime t1 = a.getNgayCapNhat() != null ? a.getNgayCapNhat() : a.getNgayTao();
                    LocalDateTime t2 = b.getNgayCapNhat() != null ? b.getNgayCapNhat() : b.getNgayTao();
                    return t2.compareTo(t1);
                })
                .map(this::enrichDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CuocHoiThoaiDTO layChiTietCuocHoiThoai(Long id) {
        CuocHoiThoaiDTO conv = conversations.get(id);
        if (conv == null) {
            throw new IllegalArgumentException("Không tìm thấy cuộc hội thoại ID: " + id);
        }
        return enrichDTO(conv);
    }

    @Override
    public List<TinNhanDTO> layDanhSachTinNhan(Long cuocHoiThoaiId, Long lastId) {
        List<TinNhanDTO> list = messagesByConv.getOrDefault(cuocHoiThoaiId, Collections.emptyList());
        if (lastId == null || lastId <= 0) {
            return new ArrayList<>(list);
        }
        return list.stream()
                .filter(m -> m.getId() > lastId)
                .collect(Collectors.toList());
    }

    @Override
    public TinNhanDTO guiTinNhan(GuiTinNhanRequest req) {
        if (req == null || req.getNoiDung() == null || req.getNoiDung().trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung tin nhắn không được để trống");
        }

        CuocHoiThoaiDTO conv;
        if (req.getCuocHoiThoaiId() != null && conversations.containsKey(req.getCuocHoiThoaiId())) {
            conv = conversations.get(req.getCuocHoiThoaiId());
            if (req.getTenKhachHang() != null && !req.getTenKhachHang().trim().isEmpty() &&
                    (conv.getTenKhachHang() == null || conv.getTenKhachHang().startsWith("Khách trực tuyến"))) {
                conv.setTenKhachHang(req.getTenKhachHang().trim());
            }
        } else {
            KhoiTaoChatRequest initReq = new KhoiTaoChatRequest();
            initReq.setKhachHangId(req.getKhachHangId());
            initReq.setTenKhachHang(req.getTenKhachHang());
            initReq.setSoDienThoai(req.getSoDienThoai());
            conv = khoiTaoHoacLayCuocHoiThoai(initReq);
        }

        String staffName = null;
        if (req.getNhanVienId() != null) {
            NhanVien nv = nhanVienRepository.findById(req.getNhanVienId()).orElse(null);
            if (nv != null) {
                conv.setNhanVienId(nv.getId());
                staffName = nv.getTenNhanVien();
                conv.setTenNhanVien(staffName);
            }
        }

        String nguoiGui = (req.getNguoiGui() != null && !req.getNguoiGui().trim().isEmpty())
                ? req.getNguoiGui().trim().toUpperCase()
                : "KHACH_HANG";

        if ("NHAN_VIEN".equals(nguoiGui) || "ADMIN".equals(nguoiGui)) {
            if (conv.getTrangThai() == null || conv.getTrangThai() != 1) {
                conv.setTrangThai(1);
            }
        } else if ("KHACH_HANG".equals(nguoiGui)) {
            if (conv.getTrangThai() != null && conv.getTrangThai() == 0) {
                conv.setTrangThai(2); // Mở lại phiên nếu khách nhắn
            }
        }

        TinNhanDTO tn = TinNhanDTO.builder()
                .id(msgIdSeq.getAndIncrement())
                .cuocHoiThoaiId(conv.getId())
                .nhanVienId(req.getNhanVienId())
                .tenNhanVien(staffName)
                .nguoiGui(nguoiGui)
                .noiDung(req.getNoiDung().trim())
                .daDoc(false)
                .ngayTao(LocalDateTime.now())
                .build();

        messagesByConv.computeIfAbsent(conv.getId(), k -> new CopyOnWriteArrayList<>()).add(tn);
        conv.setNgayCapNhat(LocalDateTime.now());
        conv.setTinNhanCuoi(tn.getNoiDung());
        conv.setThoiGianTinNhanCuoi(tn.getNgayTao());

        return tn;
    }

    @Override
    public void danhDauDaDoc(Long cuocHoiThoaiId, String role) {
        if (cuocHoiThoaiId == null) return;
        List<TinNhanDTO> list = messagesByConv.get(cuocHoiThoaiId);
        if (list == null) return;

        boolean isAdminOrStaff = "ADMIN".equalsIgnoreCase(role) || "NHAN_VIEN".equalsIgnoreCase(role);
        for (TinNhanDTO msg : list) {
            if (isAdminOrStaff) {
                if ("KHACH_HANG".equals(msg.getNguoiGui())) {
                    msg.setDaDoc(true);
                }
            } else {
                if (!"KHACH_HANG".equals(msg.getNguoiGui())) {
                    msg.setDaDoc(true);
                }
            }
        }
    }

    @Override
    public ThongBaoAdminDTO layThongBaoAdmin() {
        // Đếm tin chưa đọc từ khách hàng
        long countUnread = messagesByConv.values().stream()
                .flatMap(Collection::stream)
                .filter(m -> "KHACH_HANG".equals(m.getNguoiGui()) && !Boolean.TRUE.equals(m.getDaDoc()))
                .count();

        // Đơn hàng mới từ cơ sở dữ liệu gốc (Trạng thái 1 = Chờ xác nhận)
        long countNewOrders = 0;
        try {
            countNewOrders = hoaDonRepository.countByTrangThai(1);
        } catch (Exception e) {}

        List<HoaDon> recentOrders = new ArrayList<>();
        try {
            recentOrders = hoaDonRepository.findRecentOrders(PageRequest.of(0, 6));
        } catch (Exception e) {}

        List<ThongBaoAdminDTO.ThongBaoItemDTO> items = new ArrayList<>();

        // 1. Thêm các đơn hàng mới
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

        // 2. Thêm các tin nhắn chat mới từ khách hàng
        List<TinNhanDTO> latestClientMsgs = messagesByConv.values().stream()
                .flatMap(Collection::stream)
                .filter(m -> "KHACH_HANG".equals(m.getNguoiGui()))
                .sorted((a, b) -> b.getNgayTao().compareTo(a.getNgayTao()))
                .limit(6)
                .collect(Collectors.toList());

        for (TinNhanDTO t : latestClientMsgs) {
            CuocHoiThoaiDTO c = conversations.get(t.getCuocHoiThoaiId());
            String name = (c != null && c.getTenKhachHang() != null) ? c.getTenKhachHang() : "Khách hàng";
            String timeStr = t.getNgayTao() != null ? t.getNgayTao().format(TIME_FORMATTER) : "";
            items.add(ThongBaoAdminDTO.ThongBaoItemDTO.builder()
                    .id(t.getId())
                    .loai("CHAT")
                    .tieuDe("Tin nhắn mới từ " + name)
                    .noiDung(t.getNoiDung())
                    .thoiGian(timeStr)
                    .daDoc(Boolean.TRUE.equals(t.getDaDoc()))
                    .cuocHoiThoaiId(t.getCuocHoiThoaiId())
                    .lienKet("/tin-nhan?id=" + t.getCuocHoiThoaiId())
                    .build());
        }

        List<ThongBaoAdminDTO.TinNhanThongBaoDTO> dtoList = latestClientMsgs.stream().map(t -> {
            CuocHoiThoaiDTO c = conversations.get(t.getCuocHoiThoaiId());
            String name = (c != null && c.getTenKhachHang() != null) ? c.getTenKhachHang() : "Khách hàng";
            String timeStr = t.getNgayTao() != null ? t.getNgayTao().format(TIME_FORMATTER) : "";
            return ThongBaoAdminDTO.TinNhanThongBaoDTO.builder()
                    .tinNhanId(t.getId())
                    .cuocHoiThoaiId(t.getCuocHoiThoaiId())
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
    public void dongCuocHoiThoai(Long id) {
        CuocHoiThoaiDTO conv = conversations.get(id);
        if (conv == null) return;

        conv.setTrangThai(0); // 0 = Đã đóng
        conv.setNgayCapNhat(LocalDateTime.now());

        TinNhanDTO endMsg = TinNhanDTO.builder()
                .id(msgIdSeq.getAndIncrement())
                .cuocHoiThoaiId(id)
                .nguoiGui("AI")
                .noiDung("Phiên hỗ trợ đã kết thúc. Cảm ơn quý khách đã tin tưởng và đồng hành cùng FF T-Shirt!")
                .daDoc(true)
                .ngayTao(LocalDateTime.now())
                .build();

        messagesByConv.computeIfAbsent(id, k -> new CopyOnWriteArrayList<>()).add(endMsg);
        conv.setTinNhanCuoi(endMsg.getNoiDung());
        conv.setThoiGianTinNhanCuoi(endMsg.getNgayTao());
    }

    @Override
    public CuocHoiThoaiDTO tiepNhanCuocHoiThoai(Long id, Long nhanVienId) {
        CuocHoiThoaiDTO conv = conversations.get(id);
        if (conv == null) {
            throw new IllegalArgumentException("Không tìm thấy cuộc hội thoại ID: " + id);
        }

        String staffName = "chuyên viên tư vấn";
        if (nhanVienId != null) {
            NhanVien nv = nhanVienRepository.findById(nhanVienId).orElse(null);
            if (nv != null) {
                conv.setNhanVienId(nv.getId());
                staffName = nv.getTenNhanVien() != null ? nv.getTenNhanVien() : staffName;
                conv.setTenNhanVien(staffName);
            }
        }

        conv.setTrangThai(1); // 1 = Đang hoạt động
        conv.setNgayCapNhat(LocalDateTime.now());

        TinNhanDTO acceptMsg = TinNhanDTO.builder()
                .id(msgIdSeq.getAndIncrement())
                .cuocHoiThoaiId(id)
                .nhanVienId(nhanVienId)
                .nguoiGui("AI")
                .noiDung("Nhân viên " + staffName + " đã tiếp nhận hỗ trợ bạn.")
                .daDoc(true)
                .ngayTao(LocalDateTime.now())
                .build();

        messagesByConv.computeIfAbsent(id, k -> new CopyOnWriteArrayList<>()).add(acceptMsg);
        conv.setTinNhanCuoi(acceptMsg.getNoiDung());
        conv.setThoiGianTinNhanCuoi(acceptMsg.getNgayTao());

        return enrichDTO(conv);
    }

    @Override
    public CuocHoiThoaiDTO yeuCauHoTro(Long id) {
        CuocHoiThoaiDTO conv = conversations.get(id);
        if (conv == null) {
            throw new IllegalArgumentException("Không tìm thấy cuộc hội thoại ID: " + id);
        }

        conv.setTrangThai(2); // 2 = Chờ nhận
        conv.setNgayCapNhat(LocalDateTime.now());

        // Kiểm tra xem tin nhắn gần nhất có phải thông báo kết nối chưa
        List<TinNhanDTO> list = messagesByConv.getOrDefault(id, Collections.emptyList());
        boolean alreadyNotified = !list.isEmpty() &&
                "AI".equals(list.get(list.size() - 1).getNguoiGui()) &&
                list.get(list.size() - 1).getNoiDung() != null &&
                (list.get(list.size() - 1).getNoiDung().contains("chuyên viên") || list.get(list.size() - 1).getNoiDung().contains("nhân viên"));

        if (!alreadyNotified) {
            TinNhanDTO waitMsg = TinNhanDTO.builder()
                    .id(msgIdSeq.getAndIncrement())
                    .cuocHoiThoaiId(id)
                    .nguoiGui("AI")
                    .noiDung("Chuyên viên tư vấn của FF T-Shirt đã nhận được yêu cầu và sẽ hỗ trợ anh/chị ngay trong giây lát nhé! ❤️")
                    .daDoc(false)
                    .ngayTao(LocalDateTime.now())
                    .build();

            messagesByConv.computeIfAbsent(id, k -> new CopyOnWriteArrayList<>()).add(waitMsg);
            conv.setTinNhanCuoi(waitMsg.getNoiDung());
            conv.setThoiGianTinNhanCuoi(waitMsg.getNgayTao());
        }

        return enrichDTO(conv);
    }

    @Override
    public void xoaCuocHoiThoai(Long id) {
        if (id == null) return;
        conversations.remove(id);
        messagesByConv.remove(id);
    }

    @Override
    public void xoaTatCaCuocHoiThoai() {
        conversations.clear();
        messagesByConv.clear();
    }

    private CuocHoiThoaiDTO enrichDTO(CuocHoiThoaiDTO c) {
        if (c == null) return null;
        List<TinNhanDTO> list = messagesByConv.getOrDefault(c.getId(), Collections.emptyList());
        if (!list.isEmpty()) {
            TinNhanDTO last = list.get(list.size() - 1);
            c.setTinNhanCuoi(last.getNoiDung());
            c.setThoiGianTinNhanCuoi(last.getNgayTao());
        }
        long unread = list.stream()
                .filter(m -> "KHACH_HANG".equals(m.getNguoiGui()) && !Boolean.TRUE.equals(m.getDaDoc()))
                .count();
        c.setSoTinChuaDoc(unread);
        return c;
    }
}
