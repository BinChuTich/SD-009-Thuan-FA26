package com.example.sd009thuan.service.impl;

import com.example.sd009thuan.dto.chat.*;
import com.example.sd009thuan.entity.HoaDon;
import com.example.sd009thuan.entity.NhanVien;
import com.example.sd009thuan.repository.HoaDonRepository;
import com.example.sd009thuan.repository.NhanVienRepository;
import com.example.sd009thuan.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final HoaDonRepository hoaDonRepository;
    private final NhanVienRepository nhanVienRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // In-memory thread-safe storage (HOÀN TOÀN KHÔNG SỬA VÀO DATABASE GỐC)
    private final Map<Long, CuocHoiThoaiDTO> conversations = new ConcurrentHashMap<>();
    private final Map<Long, List<TinNhanDTO>> messagesByConv = new ConcurrentHashMap<>();
    private final Map<Long, DanhGiaCuocHoiThoaiDTO> ratingsByConv = new ConcurrentHashMap<>();
    private final Map<Long, LocalDateTime> khachTypingTimes = new ConcurrentHashMap<>();
    private final Map<Long, LocalDateTime> nhanVienTypingTimes = new ConcurrentHashMap<>();
    private final AtomicLong convIdSeq = new AtomicLong(1);
    private final AtomicLong msgIdSeq = new AtomicLong(1);
    private final AtomicLong ratingIdSeq = new AtomicLong(1);
    private final AtomicBoolean historyDirty = new AtomicBoolean(false);

    @PostConstruct
    public void restoreChatHistory() {
        Path path = getHistoryPath();
        if (!Files.exists(path)) return;
        try {
            ChatHistorySnapshot snapshot = objectMapper.readValue(path.toFile(), ChatHistorySnapshot.class);
            if (snapshot.conversations() != null) {
                snapshot.conversations().forEach(c -> conversations.put(c.getId(), c));
            }
            if (snapshot.messagesByConversation() != null) {
                snapshot.messagesByConversation().forEach((id, list) ->
                        messagesByConv.put(id, new CopyOnWriteArrayList<>(list != null ? list : List.of())));
            }
            if (snapshot.ratingsByConversation() != null) ratingsByConv.putAll(snapshot.ratingsByConversation());
            ratingsByConv.values().forEach(this::classifyRating);

            convIdSeq.set(conversations.keySet().stream().mapToLong(Long::longValue).max().orElse(0L) + 1);
            msgIdSeq.set(messagesByConv.values().stream().flatMap(Collection::stream)
                    .map(TinNhanDTO::getId).filter(Objects::nonNull).mapToLong(Long::longValue).max().orElse(0L) + 1);
            ratingIdSeq.set(ratingsByConv.values().stream().map(DanhGiaCuocHoiThoaiDTO::getId)
                    .filter(Objects::nonNull).mapToLong(Long::longValue).max().orElse(0L) + 1);
            log.info("Đã khôi phục {} cuộc hội thoại và {} nhóm tin nhắn từ {}",
                    conversations.size(), messagesByConv.size(), path);
        } catch (Exception ex) {
            log.error("Không thể khôi phục lịch sử chat từ {}. Giữ file để kiểm tra, không ghi đè: {}", path, ex.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${chat.history.persist-interval-ms:2000}")
    public void persistChatHistoryIfChanged() {
        if (!historyDirty.compareAndSet(true, false)) return;
        try {
            persistChatHistory();
        } catch (Exception ex) {
            historyDirty.set(true);
            log.error("Không thể lưu lịch sử chat: {}", ex.getMessage());
        }
    }

    @PreDestroy
    public void persistChatHistoryOnShutdown() {
        if (!conversations.isEmpty() || historyDirty.get()) {
            try {
                persistChatHistory();
            } catch (Exception ex) {
                log.error("Không thể lưu lịch sử chat khi dừng ứng dụng: {}", ex.getMessage());
            }
        }
    }

    private synchronized void persistChatHistory() throws Exception {
        Path path = getHistoryPath();
        Files.createDirectories(path.getParent());
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        Map<Long, List<TinNhanDTO>> messageCopy = new LinkedHashMap<>();
        messagesByConv.forEach((id, list) -> messageCopy.put(id, new ArrayList<>(list)));
        ChatHistorySnapshot snapshot = new ChatHistorySnapshot(
                new ArrayList<>(conversations.values()), messageCopy, new LinkedHashMap<>(ratingsByConv));
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), snapshot);
        try {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path getHistoryPath() {
        String dir = uploadDir != null && !uploadDir.isBlank() ? uploadDir : "uploads";
        return Paths.get(dir, "chat-history.json").toAbsolutePath().normalize();
    }

    private void markHistoryDirty() {
        historyDirty.set(true);
    }

    private record ChatHistorySnapshot(
            List<CuocHoiThoaiDTO> conversations,
            Map<Long, List<TinNhanDTO>> messagesByConversation,
            Map<Long, DanhGiaCuocHoiThoaiDTO> ratingsByConversation) {}

    @Override
    public CuocHoiThoaiDTO khoiTaoHoacLayCuocHoiThoai(KhoiTaoChatRequest req) {
        // 1. Kiểm tra theo ID cuộc hội thoại cụ thể
        if (req != null && req.getCuocHoiThoaiId() != null) {
            CuocHoiThoaiDTO existing = conversations.get(req.getCuocHoiThoaiId());
            // CHỈ SỬ DỤNG LẠI NẾU HỘI THOẠI ĐANG MỞ (trangThai != 0)
            // Nếu hội thoại đã đóng (trangThai == 0), giữ nguyên ở tab Đã đóng, không dùng lại ID này
            if (existing != null && existing.getTrangThai() != null && existing.getTrangThai() != 0) {
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

        // 2. Nếu khách hàng đã đăng nhập (có ID hoặc Tên), tìm cuộc hội thoại CHƯA ĐÓNG (trangThai != 0) để dùng lại
        if (req != null && req.getKhachHangId() != null) {
            CuocHoiThoaiDTO existingByUser = conversations.values().stream()
                    .filter(c -> req.getKhachHangId().equals(c.getKhachHangId()) && c.getTrangThai() != null && c.getTrangThai() != 0)
                    .sorted((a, b) -> (b.getNgayCapNhat() != null ? b.getNgayCapNhat() : b.getNgayTao())
                            .compareTo(a.getNgayCapNhat() != null ? a.getNgayCapNhat() : a.getNgayTao()))
                    .findFirst()
                    .orElse(null);
            if (existingByUser != null) {
                if (req.getTenKhachHang() != null && !req.getTenKhachHang().trim().isEmpty()) {
                    existingByUser.setTenKhachHang(req.getTenKhachHang().trim());
                }
                return enrichDTO(existingByUser);
            }
        } else if (req != null && req.getTenKhachHang() != null && !req.getTenKhachHang().trim().isEmpty() && !req.getTenKhachHang().startsWith("Khách trực tuyến")) {
            CuocHoiThoaiDTO existingByName = conversations.values().stream()
                    .filter(c -> req.getTenKhachHang().trim().equalsIgnoreCase(c.getTenKhachHang()) && c.getTrangThai() != null && c.getTrangThai() != 0)
                    .sorted((a, b) -> (b.getNgayCapNhat() != null ? b.getNgayCapNhat() : b.getNgayTao())
                            .compareTo(a.getNgayCapNhat() != null ? a.getNgayCapNhat() : a.getNgayTao()))
                    .findFirst()
                    .orElse(null);
            if (existingByName != null) {
                return enrichDTO(existingByName);
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
                .trangThai(2) // 2 = Chờ nhận (chờ nhân viên tiếp nhận)
                .ngayTao(LocalDateTime.now())
                .ngayCapNhat(LocalDateTime.now())
                .thoiGianKhachPhanHoiCuoi(LocalDateTime.now())
                .daGuiCanhBao2Phut(false)
                .soTinChuaDoc(0L)
                .build();

        conversations.put(newId, conv);
        messagesByConv.put(newId, new CopyOnWriteArrayList<>());
        markHistoryDirty();

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
            CuocHoiThoaiDTO existing = conversations.get(req.getCuocHoiThoaiId());
            // NẾU PHIÊN NÀY ĐÃ ĐÓNG (trangThai == 0):
            // GIỮ NGUYÊN PHIÊN CŨ Ở TAB "ĐÃ ĐÓNG" CÙNG TOÀN BỘ TIN NHẮN CŨ!
            // TẠO PHIÊN MỚI TOANH VÀ ĐẨY VÀO PHẦN "CHỜ NHẬN" (trangThai = 2) ĐỂ NHÂN VIÊN TIẾP NHẬN
            if (existing.getTrangThai() != null && existing.getTrangThai() == 0) {
                KhoiTaoChatRequest newReq = new KhoiTaoChatRequest();
                newReq.setKhachHangId(req.getKhachHangId() != null ? req.getKhachHangId() : existing.getKhachHangId());
                newReq.setTenKhachHang(req.getTenKhachHang() != null ? req.getTenKhachHang() : existing.getTenKhachHang());
                newReq.setSoDienThoai(req.getSoDienThoai() != null ? req.getSoDienThoai() : existing.getSoDienThoai());
                conv = khoiTaoHoacLayCuocHoiThoai(newReq);
                conv.setTrangThai(2); // 2 = Chờ nhận (Tiếp nhận mới)
            } else {
                conv = existing;
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
        if (staffName == null && req.getTenNhanVien() != null && !req.getTenNhanVien().trim().isEmpty()) {
            staffName = req.getTenNhanVien().trim();
            if (req.getNhanVienId() != null) {
                conv.setNhanVienId(req.getNhanVienId());
            }
            conv.setTenNhanVien(staffName);
        }

        String nguoiGui = (req.getNguoiGui() != null && !req.getNguoiGui().trim().isEmpty())
                ? req.getNguoiGui().trim().toUpperCase()
                : "KHACH_HANG";

        if ("NHAN_VIEN".equals(nguoiGui) || "ADMIN".equals(nguoiGui)) {
            if (conv.getTrangThai() == null || conv.getTrangThai() != 1) {
                conv.setTrangThai(1);
            }
        } else if ("KHACH_HANG".equals(nguoiGui)) {
            // Khi khách hàng gửi tin nhắn: cập nhật thời gian phản hồi cuối và reset các cảnh báo 5p, 10p, 2p
            conv.setThoiGianKhachPhanHoiCuoi(LocalDateTime.now());
            conv.setDaGuiCanhBao5Phut(false);
            conv.setDaGuiCanhBao10Phut(false);
            conv.setDaGuiCanhBao2Phut(false);

            // Khi khách phản hồi: tự động xóa các thông báo nhắc nhở timeout cũ
            List<TinNhanDTO> list = messagesByConv.get(conv.getId());
            if (list != null) {
                list.removeIf(m -> "HE_THONG".equals(m.getNguoiGui()) && m.getNoiDung() != null && m.getNoiDung().contains("Bạn vẫn còn ở đó chứ"));
            }

            if (conv.getTrangThai() == null || (conv.getTrangThai() != 1 && conv.getNhanVienId() == null)) {
                conv.setTrangThai(2); // Đẩy vào Chờ nhận để nhân viên tiếp nhận
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

        if ("KHACH_HANG".equals(nguoiGui)) {
            khachTypingTimes.remove(conv.getId());
        } else {
            nhanVienTypingTimes.remove(conv.getId());
        }

        markHistoryDirty();

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
        markHistoryDirty();
    }

    @Override
    public ThongBaoAdminDTO layThongBaoAdmin() {
        // Cuộc hội thoại đang chờ nhân viên tiếp nhận (trangThai = 2)
        List<CuocHoiThoaiDTO> waitingConvs = conversations.values().stream()
                .filter(c -> c.getTrangThai() != null && c.getTrangThai() == 2)
                .sorted((a, b) -> {
                    LocalDateTime t1 = a.getNgayCapNhat() != null ? a.getNgayCapNhat() : a.getNgayTao();
                    LocalDateTime t2 = b.getNgayCapNhat() != null ? b.getNgayCapNhat() : b.getNgayTao();
                    return t2.compareTo(t1);
                })
                .collect(Collectors.toList());

        // Đếm tin chưa đọc từ khách hàng
        long totalChatNotifications = waitingConvs.size();

        // Đơn hàng mới từ cơ sở dữ liệu gốc (Trạng thái 1 = Chờ xác nhận)
        long countNewOrders = 0;
        try {
            countNewOrders = hoaDonRepository.countByTrangThaiAndLoaiDon(1, 2);
        } catch (Exception e) {}

        List<HoaDon> recentOrders = new ArrayList<>();
        try {
            recentOrders = hoaDonRepository.findRecentOrdersByLoaiDonAndTrangThai(2, 1, PageRequest.of(0, 10));
        } catch (Exception e) {}

        List<ThongBaoAdminDTO.ThongBaoItemDTO> items = new ArrayList<>();

        // 1. Thêm các yêu cầu chat chờ nhân viên tiếp nhận
        for (CuocHoiThoaiDTO wc : waitingConvs) {
            String name = wc.getTenKhachHang() != null ? wc.getTenKhachHang() : "Khách hàng";
            String timeStr = wc.getNgayCapNhat() != null ? wc.getNgayCapNhat().format(TIME_FORMATTER) : "";
            items.add(ThongBaoAdminDTO.ThongBaoItemDTO.builder()
                    .id(wc.getId())
                    .loai("CHAT")
                    .tieuDe("Yêu cầu tư vấn mới từ " + name)
                    .noiDung(wc.getTinNhanCuoi() != null && !wc.getTinNhanCuoi().isEmpty() ? wc.getTinNhanCuoi() : "Khách hàng đang chờ nhân viên tiếp nhận cuộc trò chuyện.")
                    .thoiGian(timeStr)
                    .daDoc(false)
                    .cuocHoiThoaiId(wc.getId())
                    .lienKet("/tin-nhan?id=" + wc.getId())
                    .build());
        }

        // 2. Thêm các đơn hàng mới
        if (recentOrders != null) {
            for (HoaDon hd : recentOrders) {
                if (hd.getTrangThai() != null && hd.getTrangThai() == 1) {
                    BigDecimal tong = hd.getTienSauGiamGia() != null ? hd.getTienSauGiamGia() : (hd.getTongTien() != null ? hd.getTongTien() : BigDecimal.ZERO);
                    boolean isPosDraft = "Bán tại quầy".equalsIgnoreCase(hd.getDiaChiNhanHang())
                                         || (hd.getGhiChu() != null && (hd.getGhiChu().toLowerCase().contains("quầy") || hd.getGhiChu().toLowerCase().contains("pos")))
                                         || (tong.compareTo(BigDecimal.ZERO) <= 0 && "Khách lẻ".equalsIgnoreCase(hd.getTenKhachHang()));
                    if (isPosDraft) continue;
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

        long actualOrderNotifs = items.stream().filter(i -> "DON_HANG".equals(i.getLoai())).count();
        long totalCount = totalChatNotifications + actualOrderNotifs;

        return ThongBaoAdminDTO.builder()
                .tongSoThongBao(totalCount)
                .soTinChuaDoc(totalChatNotifications)
                .soDonHangMoi(actualOrderNotifs)
                .danhSachThongBao(items)
                .danhSachMoi(Collections.emptyList())
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
                .nguoiGui("HE_THONG")
                .noiDung("Phiên hỗ trợ đã kết thúc. Cảm ơn quý khách đã tin tưởng và đồng hành cùng FF T-Shirt!")
                .daDoc(true)
                .ngayTao(LocalDateTime.now())
                .build();

        messagesByConv.computeIfAbsent(id, k -> new CopyOnWriteArrayList<>()).add(endMsg);
        conv.setTinNhanCuoi(endMsg.getNoiDung());
        conv.setThoiGianTinNhanCuoi(endMsg.getNgayTao());
        markHistoryDirty();
    }

    @Override
    public CuocHoiThoaiDTO tiepNhanCuocHoiThoai(Long id, Long nhanVienId) {
        return tiepNhanCuocHoiThoai(id, nhanVienId, null);
    }

    @Override
    public CuocHoiThoaiDTO tiepNhanCuocHoiThoai(Long id, Long nhanVienId, String tenNhanVien) {
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
        if ((conv.getTenNhanVien() == null || conv.getTenNhanVien().equals("chuyên viên tư vấn"))
            && tenNhanVien != null && !tenNhanVien.trim().isEmpty()) {
            staffName = tenNhanVien.trim();
            if (nhanVienId != null) {
                conv.setNhanVienId(nhanVienId);
            }
            conv.setTenNhanVien(staffName);
        }

        conv.setTrangThai(1); // 1 = Đang hoạt động
        conv.setNgayCapNhat(LocalDateTime.now());
        conv.setThoiGianKhachPhanHoiCuoi(LocalDateTime.now());
        conv.setDaGuiCanhBao5Phut(false);
        conv.setDaGuiCanhBao10Phut(false);
        conv.setDaGuiCanhBao2Phut(false);

        // Xóa thông báo nhắc nhở cũ nếu có
        List<TinNhanDTO> existingMsgs = messagesByConv.get(id);
        if (existingMsgs != null) {
            existingMsgs.removeIf(m -> "HE_THONG".equals(m.getNguoiGui()) && m.getNoiDung() != null && m.getNoiDung().contains("Bạn vẫn còn ở đó chứ"));
        }

        TinNhanDTO acceptMsg = TinNhanDTO.builder()
                .id(msgIdSeq.getAndIncrement())
                .cuocHoiThoaiId(id)
                .nhanVienId(nhanVienId)
                .tenNhanVien(staffName)
                .nguoiGui("HE_THONG")
                .noiDung(staffName + " đã tiếp nhận cuộc trò chuyện.")
                .daDoc(true)
                .ngayTao(LocalDateTime.now())
                .build();

        messagesByConv.computeIfAbsent(id, k -> new CopyOnWriteArrayList<>()).add(acceptMsg);
        conv.setTinNhanCuoi(acceptMsg.getNoiDung());
        conv.setThoiGianTinNhanCuoi(acceptMsg.getNgayTao());
        markHistoryDirty();

        return enrichDTO(conv);
    }

    @Override
    public CuocHoiThoaiDTO yeuCauHoTro(Long id) {
        CuocHoiThoaiDTO conv = conversations.get(id);
        if (conv == null) {
            throw new IllegalArgumentException("Không tìm thấy cuộc hội thoại ID: " + id);
        }

        // Nếu cuộc hội thoại này ĐÃ ĐÓNG (trangThai == 0):
        if (conv.getTrangThai() != null && conv.getTrangThai() == 0) {
            KhoiTaoChatRequest newReq = new KhoiTaoChatRequest();
            newReq.setKhachHangId(conv.getKhachHangId());
            newReq.setTenKhachHang(conv.getTenKhachHang());
            newReq.setSoDienThoai(conv.getSoDienThoai());
            CuocHoiThoaiDTO newConv = khoiTaoHoacLayCuocHoiThoai(newReq);
            newConv.setTrangThai(2); // 2 = Chờ nhận
            newConv.setNgayCapNhat(LocalDateTime.now());
            return enrichDTO(newConv);
        }

        // Nếu cuộc hội thoại ĐÃ ĐƯỢC TIẾP NHẬN (trangThai == 1 và có nhân viên):
        if (conv.getTrangThai() != null && conv.getTrangThai() == 1 && conv.getNhanVienId() != null) {
            return enrichDTO(conv);
        }

        // Nếu đang ở hàng chờ sẵn rồi (trangThai == 2), giữ nguyên
        if (conv.getTrangThai() != null && conv.getTrangThai() == 2) {
            return enrichDTO(conv);
        }

        conv.setTrangThai(2); // 2 = Chờ nhận
        conv.setNgayCapNhat(LocalDateTime.now());
        markHistoryDirty();
        return enrichDTO(conv);
    }

    @Override
    public void xoaCuocHoiThoai(Long id) {
        if (id == null) return;
        conversations.remove(id);
        messagesByConv.remove(id);
        ratingsByConv.remove(id);
        markHistoryDirty();
    }

    @Override
    public void xoaTatCaCuocHoiThoai() {
        conversations.clear();
        messagesByConv.clear();
        ratingsByConv.clear();
        markHistoryDirty();
    }

    @Override
    public DanhGiaCuocHoiThoaiDTO guiDanhGia(Long cuocHoiThoaiId, DanhGiaCuocHoiThoaiDTO dto) {
        if (cuocHoiThoaiId == null) {
            throw new IllegalArgumentException("ID cuộc hội thoại không hợp lệ");
        }
        CuocHoiThoaiDTO conv = conversations.get(cuocHoiThoaiId);
        if (conv == null) {
            throw new IllegalArgumentException("Không tìm thấy cuộc hội thoại ID: " + cuocHoiThoaiId);
        }

        int score = 5;
        if (dto != null && dto.getDiemDanhGia() != null) {
            score = Math.max(1, Math.min(5, dto.getDiemDanhGia()));
        }

        String comment = (dto != null && dto.getNoiDungDanhGia() != null) ? dto.getNoiDungDanhGia().trim() : "";
        long newRatingId = ratingIdSeq.getAndIncrement();
        String code = String.format("DG%03d", newRatingId);

        Long khId = (dto != null && dto.getKhachHangId() != null) ? dto.getKhachHangId() : conv.getKhachHangId();
        String khName = (dto != null && dto.getTenKhachHang() != null && !dto.getTenKhachHang().trim().isEmpty())
                ? dto.getTenKhachHang().trim()
                : conv.getTenKhachHang();

        Long nvId = (dto != null && dto.getNhanVienId() != null) ? dto.getNhanVienId() : conv.getNhanVienId();
        String nvName = (dto != null && dto.getTenNhanVien() != null && !dto.getTenNhanVien().trim().isEmpty())
                ? dto.getTenNhanVien().trim()
                : conv.getTenNhanVien();

        DanhGiaCuocHoiThoaiDTO savedRating = DanhGiaCuocHoiThoaiDTO.builder()
                .id(newRatingId)
                .maDanhGia(code)
                .cuocHoiThoaiId(cuocHoiThoaiId)
                .khachHangId(khId)
                .tenKhachHang(khName)
                .nhanVienId(nvId)
                .tenNhanVien(nvName)
                .diemDanhGia(score)
                .noiDungDanhGia(comment)
                .trangThai(1)
                .ngayTao(LocalDateTime.now())
                .build();
        classifyRating(savedRating);

        ratingsByConv.put(cuocHoiThoaiId, savedRating);
        conv.setDanhGia(savedRating);

        // Lưu vào lịch sử chat dưới dạng tin nhắn đặc biệt
        StringBuilder sb = new StringBuilder();
        sb.append("⭐ Đánh giá: ").append(score).append(" sao");
        if (!comment.isEmpty()) {
            sb.append(" • \"").append(comment).append("\"");
        }

        TinNhanDTO ratingMsg = TinNhanDTO.builder()
                .id(msgIdSeq.getAndIncrement())
                .cuocHoiThoaiId(cuocHoiThoaiId)
                .nhanVienId(nvId)
                .tenNhanVien(nvName)
                .nguoiGui("DANH_GIA")
                .noiDung(sb.toString())
                .daDoc(true)
                .ngayTao(LocalDateTime.now())
                .danhGia(savedRating)
                .build();

        messagesByConv.computeIfAbsent(cuocHoiThoaiId, k -> new CopyOnWriteArrayList<>()).add(ratingMsg);
        conv.setTinNhanCuoi(ratingMsg.getNoiDung());
        conv.setThoiGianTinNhanCuoi(ratingMsg.getNgayTao());
        markHistoryDirty();

        return savedRating;
    }

    @Override
    public DanhGiaCuocHoiThoaiDTO layDanhGia(Long cuocHoiThoaiId) {
        if (cuocHoiThoaiId == null) return null;
        DanhGiaCuocHoiThoaiDTO rating = ratingsByConv.get(cuocHoiThoaiId);
        if (rating != null) classifyRating(rating);
        return rating;
    }

    @Override
    public DanhGiaCuocHoiThoaiDTO danhDauDanhGiaDaXuLy(Long cuocHoiThoaiId, String nguoiXuLy) {
        DanhGiaCuocHoiThoaiDTO rating = ratingsByConv.get(cuocHoiThoaiId);
        if (rating == null) throw new IllegalArgumentException("Cuộc hội thoại chưa có đánh giá");
        classifyRating(rating);
        rating.setDaXuLy(true);
        rating.setNguoiXuLy(nguoiXuLy == null || nguoiXuLy.isBlank() ? "Quản trị viên" : nguoiXuLy.trim());
        rating.setNgayXuLy(LocalDateTime.now());
        CuocHoiThoaiDTO conversation = conversations.get(cuocHoiThoaiId);
        if (conversation != null) conversation.setDanhGia(rating);
        List<TinNhanDTO> messages = messagesByConv.get(cuocHoiThoaiId);
        if (messages != null) messages.stream().filter(m -> m.getDanhGia() != null).forEach(m -> m.setDanhGia(rating));
        markHistoryDirty();
        return rating;
    }

    private void classifyRating(DanhGiaCuocHoiThoaiDTO rating) {
        if (rating == null) return;
        int score = rating.getDiemDanhGia() == null ? 5 : rating.getDiemDanhGia();
        String starClass = score >= 4 ? "TICH_CUC" : (score == 3 ? "TRUNG_LAP" : "TIEU_CUC");
        String text = normalizeRatingText(rating.getNoiDungDanhGia());

        List<String> positiveWords = List.of("tot", "nhiet tinh", "hai long", "nhanh", "de thuong", "tuyet voi", "chu dao", "chinh xac", "cam on");
        List<String> negativeWords = List.of("cham", "lau", "te", "khong hai long", "that vong", "khong tot", "sai", "kho chiu", "khong giai quyet", "tu van sai", "thai do kem");
        boolean positive = positiveWords.stream().anyMatch(text::contains);
        boolean negative = negativeWords.stream().anyMatch(text::contains);
        String sentiment = text.isBlank() ? "KHONG_XAC_DINH"
                : positive && negative ? "HON_HOP"
                : negative ? "TIEU_CUC"
                : positive ? "TICH_CUC" : "TRUNG_LAP";

        List<String> topics = new ArrayList<>();
        if (containsAny(text, "cham", "lau", "phan hoi", "doi")) topics.add("TOC_DO_PHAN_HOI");
        if (containsAny(text, "nhan vien", "thai do", "nhiet tinh", "de thuong", "kho chiu")) topics.add("THAI_DO_NHAN_VIEN");
        if (containsAny(text, "size", "kich co", "rong", "chat", "vua")) topics.add("TU_VAN_SIZE");
        if (containsAny(text, "san pham", "mau", "chat lieu", "ao")) topics.add("THONG_TIN_SAN_PHAM");
        if (containsAny(text, "gia", "sale", "giam gia", "khuyen mai", "voucher")) topics.add("GIA_KHUYEN_MAI");
        if (containsAny(text, "ship", "giao hang", "van chuyen")) topics.add("VAN_CHUYEN");
        if (containsAny(text, "bot", "ai", "tu dong")) topics.add("CHATBOT_AI");
        if (topics.isEmpty() && !text.isBlank()) topics.add("KHAC");

        boolean conflict = ("TICH_CUC".equals(starClass) && "TIEU_CUC".equals(sentiment))
                || ("TIEU_CUC".equals(starClass) && "TICH_CUC".equals(sentiment));
        rating.setPhanLoai(starClass);
        rating.setCamXucNoiDung(sentiment);
        rating.setChuDe(topics);
        rating.setMauThuanSaoNoiDung(conflict);
        rating.setCanXuLy(score <= 2 || negative || "HON_HOP".equals(sentiment) || conflict);
        if (rating.getDaXuLy() == null) rating.setDaXuLy(false);
    }

    private boolean containsAny(String text, String... terms) {
        return Arrays.stream(terms).anyMatch(text::contains);
    }

    private String normalizeRatingText(String value) {
        if (value == null) return "";
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT).trim();
    }

    /**
     * TỰ ĐỘNG ĐÓNG PHIÊN VÀ CẢNH BÁO KHI KHÁCH HÀNG KHÔNG PHẢN HỒI:
     * - Sau 13 phút (780 giây) không phản hồi: Hiện cảnh báo:
     *   "Bạn vẫn còn ở đó chứ? Nếu bạn không phản hồi trong 2 phút nữa, phiên chat này sẽ tự động đóng."
     * - Sau 15 phút (900 giây) không phản hồi: Tự động đóng phiên và hiện thông báo:
     *   "Vì bạn không phản hồi, phiên chat này đã được tự động đóng. Nếu cần hỗ trợ thêm, xin vui lòng bắt đầu một cuộc hội thoại mới. Xin cảm ơn."
     */
    @Scheduled(fixedDelay = 5000)
    public void kiemTraTuDongDongPhienKhongPhanHoi() {
        LocalDateTime now = LocalDateTime.now();
        boolean changed = false;
        for (CuocHoiThoaiDTO conv : conversations.values()) {
            // THEO YÊU CẦU: Chỉ áp dụng đếm thời gian khi ĐANG CHAT VỚI NHÂN VIÊN (trangThai == 1)
            // Nếu chưa tiếp nhận (trangThai == 2 hoặc khác) thì CHƯA TÍNH!
            if (conv.getTrangThai() != null && conv.getTrangThai() == 1) {
                LocalDateTime lastActive = conv.getThoiGianKhachPhanHoiCuoi();
                if (lastActive == null) {
                    lastActive = conv.getNgayCapNhat() != null ? conv.getNgayCapNhat() : conv.getNgayTao();
                }
                if (lastActive == null) continue;

                long secondsInactive = Duration.between(lastActive, now).getSeconds();

                // 1. Sau 5 phút (300s) không phản hồi: Báo còn 10 phút
                if (secondsInactive >= 5 * 60 && !Boolean.TRUE.equals(conv.getDaGuiCanhBao5Phut())) {
                    conv.setDaGuiCanhBao5Phut(true);

                    TinNhanDTO warnMsg = TinNhanDTO.builder()
                            .id(msgIdSeq.getAndIncrement())
                            .cuocHoiThoaiId(conv.getId())
                            .nguoiGui("HE_THONG")
                            .noiDung("Bạn vẫn còn ở đó chứ? Phiên chat sẽ tự động đóng sau 10 phút nữa nếu bạn không phản hồi.")
                            .daDoc(false)
                            .ngayTao(now)
                            .build();

                    messagesByConv.computeIfAbsent(conv.getId(), k -> new CopyOnWriteArrayList<>()).add(warnMsg);
                    conv.setTinNhanCuoi(warnMsg.getNoiDung());
                    conv.setThoiGianTinNhanCuoi(warnMsg.getNgayTao());
                    changed = true;
                }

                // 2. Sau 10 phút (600s) không phản hồi: Báo còn 5 phút
                if (secondsInactive >= 10 * 60 && !Boolean.TRUE.equals(conv.getDaGuiCanhBao10Phut())) {
                    conv.setDaGuiCanhBao10Phut(true);

                    TinNhanDTO warnMsg = TinNhanDTO.builder()
                            .id(msgIdSeq.getAndIncrement())
                            .cuocHoiThoaiId(conv.getId())
                            .nguoiGui("HE_THONG")
                            .noiDung("Bạn vẫn còn ở đó chứ? Phiên chat sẽ tự động đóng sau 5 phút nữa nếu bạn không phản hồi.")
                            .daDoc(false)
                            .ngayTao(now)
                            .build();

                    messagesByConv.computeIfAbsent(conv.getId(), k -> new CopyOnWriteArrayList<>()).add(warnMsg);
                    conv.setTinNhanCuoi(warnMsg.getNoiDung());
                    conv.setThoiGianTinNhanCuoi(warnMsg.getNgayTao());
                    changed = true;
                }

                // 3. Sau 13 phút (780s - còn khoảng 2 phút): Cảnh báo 2 phút
                if (secondsInactive >= 13 * 60 && !Boolean.TRUE.equals(conv.getDaGuiCanhBao2Phut())) {
                    conv.setDaGuiCanhBao2Phut(true);

                    TinNhanDTO warnMsg = TinNhanDTO.builder()
                            .id(msgIdSeq.getAndIncrement())
                            .cuocHoiThoaiId(conv.getId())
                            .nguoiGui("HE_THONG")
                            .noiDung("Bạn vẫn còn ở đó chứ? Nếu bạn không phản hồi trong 2 phút nữa, phiên chat này sẽ tự động đóng.")
                            .daDoc(false)
                            .ngayTao(now)
                            .build();

                    messagesByConv.computeIfAbsent(conv.getId(), k -> new CopyOnWriteArrayList<>()).add(warnMsg);
                    conv.setTinNhanCuoi(warnMsg.getNoiDung());
                    conv.setThoiGianTinNhanCuoi(warnMsg.getNgayTao());
                    changed = true;
                }

                // 4. Tự động đóng phiên sau 15 phút (900s) không phản hồi
                if (secondsInactive >= 15 * 60) {
                    conv.setTrangThai(0); // 0 = Đã đóng
                    conv.setNgayCapNhat(now);

                    TinNhanDTO autoCloseMsg = TinNhanDTO.builder()
                            .id(msgIdSeq.getAndIncrement())
                            .cuocHoiThoaiId(conv.getId())
                            .nguoiGui("HE_THONG")
                            .noiDung("Vì bạn không phản hồi, phiên chat này đã được tự động đóng. Nếu cần hỗ trợ thêm, xin vui lòng bắt đầu một cuộc hội thoại mới. Xin cảm ơn.")
                            .daDoc(true)
                            .ngayTao(now)
                            .build();

                    messagesByConv.computeIfAbsent(conv.getId(), k -> new CopyOnWriteArrayList<>()).add(autoCloseMsg);
                    conv.setTinNhanCuoi(autoCloseMsg.getNoiDung());
                    conv.setThoiGianTinNhanCuoi(autoCloseMsg.getNgayTao());
                    changed = true;
                }
            }
        }
        if (changed) markHistoryDirty();
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
        c.setDanhGia(ratingsByConv.get(c.getId()));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime khachTime = khachTypingTimes.get(c.getId());
        boolean khachTyping = khachTime != null && Math.abs(Duration.between(khachTime, now).getSeconds()) <= 6;
        c.setKhachDangNhap(khachTyping);

        LocalDateTime nvTime = nhanVienTypingTimes.get(c.getId());
        boolean nvTyping = nvTime != null && Math.abs(Duration.between(nvTime, now).getSeconds()) <= 6;
        c.setNhanVienDangNhap(nvTyping);

        return c;
    }

    @Override
    public void capNhatDangNhap(Long cuocHoiThoaiId, String nguoiGui, Boolean isTyping) {
        if (cuocHoiThoaiId == null) return;
        if ("KHACH_HANG".equalsIgnoreCase(nguoiGui)) {
            if (Boolean.TRUE.equals(isTyping)) {
                khachTypingTimes.put(cuocHoiThoaiId, LocalDateTime.now());
                CuocHoiThoaiDTO conv = conversations.get(cuocHoiThoaiId);
                if (conv != null) {
                    conv.setThoiGianKhachPhanHoiCuoi(LocalDateTime.now());
                    conv.setDaGuiCanhBao5Phut(false);
                    conv.setDaGuiCanhBao10Phut(false);
                    conv.setDaGuiCanhBao2Phut(false);

                    // Khi khách bắt đầu soạn tin phản hồi: tự động xóa các thông báo nhắc nhở timeout cũ
                    List<TinNhanDTO> list = messagesByConv.get(cuocHoiThoaiId);
                    if (list != null) {
                        list.removeIf(m -> "HE_THONG".equals(m.getNguoiGui()) && m.getNoiDung() != null && m.getNoiDung().contains("Bạn vẫn còn ở đó chứ"));
                    }
                    markHistoryDirty();
                }
            } else {
                khachTypingTimes.remove(cuocHoiThoaiId);
            }
        } else {
            if (Boolean.TRUE.equals(isTyping)) {
                nhanVienTypingTimes.put(cuocHoiThoaiId, LocalDateTime.now());
            } else {
                nhanVienTypingTimes.remove(cuocHoiThoaiId);
            }
        }
    }
}
