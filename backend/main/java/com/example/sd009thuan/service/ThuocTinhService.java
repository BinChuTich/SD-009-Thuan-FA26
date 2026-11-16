package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.ThuocTinhRequest;
import com.example.sd009thuan.entity.*;
import com.example.sd009thuan.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class ThuocTinhService {

    private static final Pattern HEX_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");

    private final ChatLieuRepository chatLieuRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final XuatXuRepository xuatXuRepository;
    private final DanhMucRepository danhMucRepository;
    private final CoAoRepository coAoRepository;
    private final TayAoRepository tayAoRepository;
    private final HoaTietRepository hoaTietRepository;
    private final MauSacRepository mauSacRepository;
    private final KichCoRepository kichCoRepository;
    private final SanPhamRepository sanPhamRepository;
    private final ChiTietSanPhamRepository chiTietSanPhamRepository;

    public ThuocTinhService(
            ChatLieuRepository chatLieuRepository,
            ThuongHieuRepository thuongHieuRepository,
            XuatXuRepository xuatXuRepository,
            DanhMucRepository danhMucRepository,
            CoAoRepository coAoRepository,
            TayAoRepository tayAoRepository,
            HoaTietRepository hoaTietRepository,
            MauSacRepository mauSacRepository,
            KichCoRepository kichCoRepository,
            SanPhamRepository sanPhamRepository,
            ChiTietSanPhamRepository chiTietSanPhamRepository
    ) {
        this.chatLieuRepository = chatLieuRepository;
        this.thuongHieuRepository = thuongHieuRepository;
        this.xuatXuRepository = xuatXuRepository;
        this.danhMucRepository = danhMucRepository;
        this.coAoRepository = coAoRepository;
        this.tayAoRepository = tayAoRepository;
        this.hoaTietRepository = hoaTietRepository;
        this.mauSacRepository = mauSacRepository;
        this.kichCoRepository = kichCoRepository;
        this.sanPhamRepository = sanPhamRepository;
        this.chiTietSanPhamRepository = chiTietSanPhamRepository;
    }

    public Map<String, Object> getAllThuocTinh() {
        Map<String, Object> map = new HashMap<>();
        map.put("chatLieu", chatLieuRepository.findAll());
        map.put("thuongHieu", thuongHieuRepository.findAll());
        map.put("xuatXu", xuatXuRepository.findAll());
        map.put("danhMuc", danhMucRepository.findAll());
        map.put("coAo", coAoRepository.findAll());
        map.put("tayAo", tayAoRepository.findAll());
        map.put("hoaTiet", hoaTietRepository.findAll());
        map.put("mauSac", mauSacRepository.findAll());
        map.put("kichCo", kichCoRepository.findAll());
        return map;
    }

    public List<ChatLieu> getChatLieu() { return chatLieuRepository.findAll(); }
    public List<ThuongHieu> getThuongHieu() { return thuongHieuRepository.findAll(); }
    public List<XuatXu> getXuatXu() { return xuatXuRepository.findAll(); }
    public List<DanhMuc> getDanhMuc() { return danhMucRepository.findAll(); }
    public List<CoAo> getCoAo() { return coAoRepository.findAll(); }
    public List<TayAo> getTayAo() { return tayAoRepository.findAll(); }
    public List<HoaTiet> getHoaTiet() { return hoaTietRepository.findAll(); }
    public List<MauSac> getMauSac() { return mauSacRepository.findAll(); }
    public List<KichCo> getKichCo() { return kichCoRepository.findAll(); }

    private String normalizeType(String type) {
        if (type == null) return "";
        return type.trim().toLowerCase().replace("_", "-");
    }

    private String getDisplayName(String type) {
        return switch (normalizeType(type)) {
            case "chat-lieu" -> "chất liệu";
            case "thuong-hieu" -> "thương hiệu";
            case "xuat-xu" -> "xuất xứ";
            case "danh-muc" -> "danh mục";
            case "co-ao" -> "cổ áo";
            case "tay-ao" -> "tay áo";
            case "hoa-tiet" -> "họa tiết";
            case "mau-sac" -> "màu sắc";
            case "kich-co" -> "kích cỡ";
            default -> "thuộc tính";
        };
    }

    public List<?> getByType(String type) {
        String t = normalizeType(type);
        return switch (t) {
            case "chat-lieu" -> chatLieuRepository.findAll();
            case "thuong-hieu" -> thuongHieuRepository.findAll();
            case "xuat-xu" -> xuatXuRepository.findAll();
            case "danh-muc" -> danhMucRepository.findAll();
            case "co-ao" -> coAoRepository.findAll();
            case "tay-ao" -> tayAoRepository.findAll();
            case "hoa-tiet" -> hoaTietRepository.findAll();
            case "mau-sac" -> mauSacRepository.findAll();
            case "kich-co" -> kichCoRepository.findAll();
            default -> throw new IllegalArgumentException("Loại thuộc tính không hợp lệ: " + type);
        };
    }

    public Object getById(String type, Long id) {
        String t = normalizeType(type);
        return switch (t) {
            case "chat-lieu" -> chatLieuRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy chất liệu!"));
            case "thuong-hieu" -> thuongHieuRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy thương hiệu!"));
            case "xuat-xu" -> xuatXuRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy xuất xứ!"));
            case "danh-muc" -> danhMucRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục!"));
            case "co-ao" -> coAoRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy cổ áo!"));
            case "tay-ao" -> tayAoRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy tay áo!"));
            case "hoa-tiet" -> hoaTietRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy họa tiết!"));
            case "mau-sac" -> mauSacRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy màu sắc!"));
            case "kich-co" -> kichCoRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy kích cỡ!"));
            default -> throw new IllegalArgumentException("Loại thuộc tính không hợp lệ: " + type);
        };
    }

    public Object create(String type, ThuocTinhRequest req) {
        String t = normalizeType(type);
        String displayName = getDisplayName(t);

        // 1. Validate Tên
        if (req.getTen() == null || req.getTen().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên " + displayName + " không được để trống!");
        }
        String ten = req.getTen().trim();
        if (ten.length() > 255) {
            throw new IllegalArgumentException("Tên " + displayName + " không được vượt quá 255 ký tự!");
        }

        // 2. Validate Mã
        String ma = (req.getMa() != null) ? req.getMa().trim() : "";
        if (ma.length() > 50) {
            throw new IllegalArgumentException("Mã " + displayName + " không được vượt quá 50 ký tự!");
        }

        // 3. Validate Trạng thái
        int status = (req.getTrangThai() != null && req.getTrangThai() == 0) ? 0 : 1;

        // 4. Validate & kiểm tra trùng lặp theo từng loại
        long suffix = System.currentTimeMillis() % 1000000;
        return switch (t) {
            case "chat-lieu" -> {
                if (chatLieuRepository.existsByTenChatLieuIgnoreCase(ten)) {
                    throw new IllegalArgumentException("Tên chất liệu \"" + ten + "\" đã tồn tại trong hệ thống!");
                }
                String finalMa = !ma.isEmpty() ? ma : "CL" + suffix;
                if (chatLieuRepository.existsByMaChatLieu(finalMa)) {
                    throw new IllegalArgumentException("Mã chất liệu \"" + finalMa + "\" đã tồn tại trong hệ thống!");
                }
                ChatLieu cl = new ChatLieu();
                cl.setMaChatLieu(finalMa);
                cl.setTenChatLieu(ten);
                cl.setTrangThai(status);
                yield chatLieuRepository.save(cl);
            }
            case "thuong-hieu" -> {
                if (thuongHieuRepository.existsByTenThuongHieuIgnoreCase(ten)) {
                    throw new IllegalArgumentException("Tên thương hiệu \"" + ten + "\" đã tồn tại trong hệ thống!");
                }
                String finalMa = !ma.isEmpty() ? ma : "TH" + suffix;
                if (thuongHieuRepository.existsByMaThuongHieu(finalMa)) {
                    throw new IllegalArgumentException("Mã thương hiệu \"" + finalMa + "\" đã tồn tại trong hệ thống!");
                }
                ThuongHieu th = new ThuongHieu();
                th.setMaThuongHieu(finalMa);
                th.setTenThuongHieu(ten);
                th.setTrangThai(status);
                yield thuongHieuRepository.save(th);
            }
            case "xuat-xu" -> {
                if (xuatXuRepository.existsByTenXuatXuIgnoreCase(ten)) {
                    throw new IllegalArgumentException("Tên xuất xứ \"" + ten + "\" đã tồn tại trong hệ thống!");
                }
                String finalMa = !ma.isEmpty() ? ma : "XX" + suffix;
                if (xuatXuRepository.existsByMaXuatXu(finalMa)) {
                    throw new IllegalArgumentException("Mã xuất xứ \"" + finalMa + "\" đã tồn tại trong hệ thống!");
                }
                XuatXu xx = new XuatXu();
                xx.setMaXuatXu(finalMa);
                xx.setTenXuatXu(ten);
                xx.setTrangThai(status);
                yield xuatXuRepository.save(xx);
            }
            case "danh-muc" -> {
                if (danhMucRepository.existsByTenDanhMucIgnoreCase(ten)) {
                    throw new IllegalArgumentException("Tên danh mục \"" + ten + "\" đã tồn tại trong hệ thống!");
                }
                String finalMa = !ma.isEmpty() ? ma : "DM" + suffix;
                if (danhMucRepository.existsByMaDanhMuc(finalMa)) {
                    throw new IllegalArgumentException("Mã danh mục \"" + finalMa + "\" đã tồn tại trong hệ thống!");
                }
                DanhMuc dm = new DanhMuc();
                dm.setMaDanhMuc(finalMa);
                dm.setTenDanhMuc(ten);
                dm.setTrangThai(status);
                yield danhMucRepository.save(dm);
            }
            case "co-ao" -> {
                if (coAoRepository.existsByTenCoAoIgnoreCase(ten)) {
                    throw new IllegalArgumentException("Tên cổ áo \"" + ten + "\" đã tồn tại trong hệ thống!");
                }
                String finalMa = !ma.isEmpty() ? ma : "CA" + suffix;
                if (coAoRepository.existsByMaCoAo(finalMa)) {
                    throw new IllegalArgumentException("Mã cổ áo \"" + finalMa + "\" đã tồn tại trong hệ thống!");
                }
                CoAo ca = new CoAo();
                ca.setMaCoAo(finalMa);
                ca.setTenCoAo(ten);
                ca.setTrangThai(status);
                yield coAoRepository.save(ca);
            }
            case "tay-ao" -> {
                if (tayAoRepository.existsByTenTayAoIgnoreCase(ten)) {
                    throw new IllegalArgumentException("Tên tay áo \"" + ten + "\" đã tồn tại trong hệ thống!");
                }
                String finalMa = !ma.isEmpty() ? ma : "TA" + suffix;
                if (tayAoRepository.existsByMaTayAo(finalMa)) {
                    throw new IllegalArgumentException("Mã tay áo \"" + finalMa + "\" đã tồn tại trong hệ thống!");
                }
                TayAo ta = new TayAo();
                ta.setMaTayAo(finalMa);
                ta.setTenTayAo(ten);
                ta.setTrangThai(status);
                yield tayAoRepository.save(ta);
            }
            case "hoa-tiet" -> {
                if (hoaTietRepository.existsByTenHoaTietIgnoreCase(ten)) {
                    throw new IllegalArgumentException("Tên họa tiết \"" + ten + "\" đã tồn tại trong hệ thống!");
                }
                String finalMa = !ma.isEmpty() ? ma : "HT" + suffix;
                if (hoaTietRepository.existsByMaHoaTiet(finalMa)) {
                    throw new IllegalArgumentException("Mã họa tiết \"" + finalMa + "\" đã tồn tại trong hệ thống!");
                }
                HoaTiet ht = new HoaTiet();
                ht.setMaHoaTiet(finalMa);
                ht.setTenHoaTiet(ten);
                ht.setTrangThai(status);
                yield hoaTietRepository.save(ht);
            }
            case "mau-sac" -> {
                if (mauSacRepository.existsByTenMauSacIgnoreCase(ten)) {
                    throw new IllegalArgumentException("Tên màu sắc \"" + ten + "\" đã tồn tại trong hệ thống!");
                }
                String finalMa = !ma.isEmpty() ? ma : "MS" + suffix;
                if (mauSacRepository.existsByMaMauSac(finalMa)) {
                    throw new IllegalArgumentException("Mã màu sắc \"" + finalMa + "\" đã tồn tại trong hệ thống!");
                }
                String hex = (req.getMaHex() != null && !req.getMaHex().trim().isEmpty()) ? req.getMaHex().trim() : "#496883";
                if (!HEX_PATTERN.matcher(hex).matches()) {
                    throw new IllegalArgumentException("Mã màu HEX không hợp lệ (Ví dụ: #FF0000 hoặc #FFF)!");
                }
                MauSac ms = new MauSac();
                ms.setMaMauSac(finalMa);
                ms.setTenMauSac(ten);
                ms.setMaHex(hex);
                ms.setTrangThai(status);
                yield mauSacRepository.save(ms);
            }
            case "kich-co" -> {
                if (kichCoRepository.existsByTenKichCoIgnoreCase(ten)) {
                    throw new IllegalArgumentException("Tên kích cỡ \"" + ten + "\" đã tồn tại trong hệ thống!");
                }
                String finalMa = !ma.isEmpty() ? ma : "KC" + suffix;
                if (kichCoRepository.existsByMaKichCo(finalMa)) {
                    throw new IllegalArgumentException("Mã kích cỡ \"" + finalMa + "\" đã tồn tại trong hệ thống!");
                }
                KichCo kc = new KichCo();
                kc.setMaKichCo(finalMa);
                kc.setTenKichCo(ten);
                kc.setTrangThai(status);
                yield kichCoRepository.save(kc);
            }
            default -> throw new IllegalArgumentException("Loại thuộc tính không hợp lệ: " + type);
        };
    }

    public Object update(String type, Long id, ThuocTinhRequest req) {
        String t = normalizeType(type);
        String displayName = getDisplayName(t);

        // 1. Validate Tên
        if (req.getTen() == null || req.getTen().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên " + displayName + " không được để trống!");
        }
        String ten = req.getTen().trim();
        if (ten.length() > 255) {
            throw new IllegalArgumentException("Tên " + displayName + " không được vượt quá 255 ký tự!");
        }

        // 2. Validate Mã
        String ma = (req.getMa() != null) ? req.getMa().trim() : "";
        if (ma.length() > 50) {
            throw new IllegalArgumentException("Mã " + displayName + " không được vượt quá 50 ký tự!");
        }

        // 3. Validate Trạng thái
        int status = (req.getTrangThai() != null && req.getTrangThai() == 0) ? 0 : 1;

        // 4. Update & Kiểm tra trùng lặp cho ID khác
        return switch (t) {
            case "chat-lieu" -> {
                ChatLieu cl = chatLieuRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy chất liệu!"));
                if (chatLieuRepository.existsByTenChatLieuIgnoreCaseAndIdNot(ten, id)) {
                    throw new IllegalArgumentException("Tên chất liệu \"" + ten + "\" đã được sử dụng cho một chất liệu khác!");
                }
                if (!ma.isEmpty() && chatLieuRepository.existsByMaChatLieuAndIdNot(ma, id)) {
                    throw new IllegalArgumentException("Mã chất liệu \"" + ma + "\" đã tồn tại trong hệ thống!");
                }
                if (!ma.isEmpty()) cl.setMaChatLieu(ma);
                cl.setTenChatLieu(ten);
                cl.setTrangThai(status);
                yield chatLieuRepository.save(cl);
            }
            case "thuong-hieu" -> {
                ThuongHieu th = thuongHieuRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy thương hiệu!"));
                if (thuongHieuRepository.existsByTenThuongHieuIgnoreCaseAndIdNot(ten, id)) {
                    throw new IllegalArgumentException("Tên thương hiệu \"" + ten + "\" đã được sử dụng cho một thương hiệu khác!");
                }
                if (!ma.isEmpty() && thuongHieuRepository.existsByMaThuongHieuAndIdNot(ma, id)) {
                    throw new IllegalArgumentException("Mã thương hiệu \"" + ma + "\" đã tồn tại trong hệ thống!");
                }
                if (!ma.isEmpty()) th.setMaThuongHieu(ma);
                th.setTenThuongHieu(ten);
                th.setTrangThai(status);
                yield thuongHieuRepository.save(th);
            }
            case "xuat-xu" -> {
                XuatXu xx = xuatXuRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy xuất xứ!"));
                if (xuatXuRepository.existsByTenXuatXuIgnoreCaseAndIdNot(ten, id)) {
                    throw new IllegalArgumentException("Tên xuất xứ \"" + ten + "\" đã được sử dụng cho một xuất xứ khác!");
                }
                if (!ma.isEmpty() && xuatXuRepository.existsByMaXuatXuAndIdNot(ma, id)) {
                    throw new IllegalArgumentException("Mã xuất xứ \"" + ma + "\" đã tồn tại trong hệ thống!");
                }
                if (!ma.isEmpty()) xx.setMaXuatXu(ma);
                xx.setTenXuatXu(ten);
                xx.setTrangThai(status);
                yield xuatXuRepository.save(xx);
            }
            case "danh-muc" -> {
                DanhMuc dm = danhMucRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục!"));
                if (danhMucRepository.existsByTenDanhMucIgnoreCaseAndIdNot(ten, id)) {
                    throw new IllegalArgumentException("Tên danh mục \"" + ten + "\" đã được sử dụng cho một danh mục khác!");
                }
                if (!ma.isEmpty() && danhMucRepository.existsByMaDanhMucAndIdNot(ma, id)) {
                    throw new IllegalArgumentException("Mã danh mục \"" + ma + "\" đã tồn tại trong hệ thống!");
                }
                if (!ma.isEmpty()) dm.setMaDanhMuc(ma);
                dm.setTenDanhMuc(ten);
                dm.setTrangThai(status);
                yield danhMucRepository.save(dm);
            }
            case "co-ao" -> {
                CoAo ca = coAoRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy cổ áo!"));
                if (coAoRepository.existsByTenCoAoIgnoreCaseAndIdNot(ten, id)) {
                    throw new IllegalArgumentException("Tên cổ áo \"" + ten + "\" đã được sử dụng cho một cổ áo khác!");
                }
                if (!ma.isEmpty() && coAoRepository.existsByMaCoAoAndIdNot(ma, id)) {
                    throw new IllegalArgumentException("Mã cổ áo \"" + ma + "\" đã tồn tại trong hệ thống!");
                }
                if (!ma.isEmpty()) ca.setMaCoAo(ma);
                ca.setTenCoAo(ten);
                ca.setTrangThai(status);
                yield coAoRepository.save(ca);
            }
            case "tay-ao" -> {
                TayAo ta = tayAoRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy tay áo!"));
                if (tayAoRepository.existsByTenTayAoIgnoreCaseAndIdNot(ten, id)) {
                    throw new IllegalArgumentException("Tên tay áo \"" + ten + "\" đã được sử dụng cho một tay áo khác!");
                }
                if (!ma.isEmpty() && tayAoRepository.existsByMaTayAoAndIdNot(ma, id)) {
                    throw new IllegalArgumentException("Mã tay áo \"" + ma + "\" đã tồn tại trong hệ thống!");
                }
                if (!ma.isEmpty()) ta.setMaTayAo(ma);
                ta.setTenTayAo(ten);
                ta.setTrangThai(status);
                yield tayAoRepository.save(ta);
            }
            case "hoa-tiet" -> {
                HoaTiet ht = hoaTietRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy họa tiết!"));
                if (hoaTietRepository.existsByTenHoaTietIgnoreCaseAndIdNot(ten, id)) {
                    throw new IllegalArgumentException("Tên họa tiết \"" + ten + "\" đã được sử dụng cho một họa tiết khác!");
                }
                if (!ma.isEmpty() && hoaTietRepository.existsByMaHoaTietAndIdNot(ma, id)) {
                    throw new IllegalArgumentException("Mã họa tiết \"" + ma + "\" đã tồn tại trong hệ thống!");
                }
                if (!ma.isEmpty()) ht.setMaHoaTiet(ma);
                ht.setTenHoaTiet(ten);
                ht.setTrangThai(status);
                yield hoaTietRepository.save(ht);
            }
            case "mau-sac" -> {
                MauSac ms = mauSacRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy màu sắc!"));
                if (mauSacRepository.existsByTenMauSacIgnoreCaseAndIdNot(ten, id)) {
                    throw new IllegalArgumentException("Tên màu sắc \"" + ten + "\" đã được sử dụng cho một màu sắc khác!");
                }
                if (!ma.isEmpty() && mauSacRepository.existsByMaMauSacAndIdNot(ma, id)) {
                    throw new IllegalArgumentException("Mã màu sắc \"" + ma + "\" đã tồn tại trong hệ thống!");
                }
                if (req.getMaHex() != null && !req.getMaHex().trim().isEmpty()) {
                    String hex = req.getMaHex().trim();
                    if (!HEX_PATTERN.matcher(hex).matches()) {
                        throw new IllegalArgumentException("Mã màu HEX không hợp lệ (Ví dụ: #FF0000 hoặc #FFF)!");
                    }
                    ms.setMaHex(hex);
                }
                if (!ma.isEmpty()) ms.setMaMauSac(ma);
                ms.setTenMauSac(ten);
                ms.setTrangThai(status);
                yield mauSacRepository.save(ms);
            }
            case "kich-co" -> {
                KichCo kc = kichCoRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy kích cỡ!"));
                if (kichCoRepository.existsByTenKichCoIgnoreCaseAndIdNot(ten, id)) {
                    throw new IllegalArgumentException("Tên kích cỡ \"" + ten + "\" đã được sử dụng cho một kích cỡ khác!");
                }
                if (!ma.isEmpty() && kichCoRepository.existsByMaKichCoAndIdNot(ma, id)) {
                    throw new IllegalArgumentException("Mã kích cỡ \"" + ma + "\" đã tồn tại trong hệ thống!");
                }
                if (!ma.isEmpty()) kc.setMaKichCo(ma);
                kc.setTenKichCo(ten);
                kc.setTrangThai(status);
                yield kichCoRepository.save(kc);
            }
            default -> throw new IllegalArgumentException("Loại thuộc tính không hợp lệ: " + type);
        };
    }

    public void changeStatus(String type, Long id, Integer status) {
        String t = normalizeType(type);
        int newStatus = (status != null && status == 0) ? 0 : 1;
        switch (t) {
            case "chat-lieu" -> {
                ChatLieu cl = chatLieuRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy chất liệu!"));
                cl.setTrangThai(newStatus);
                chatLieuRepository.save(cl);
            }
            case "thuong-hieu" -> {
                ThuongHieu th = thuongHieuRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy thương hiệu!"));
                th.setTrangThai(newStatus);
                thuongHieuRepository.save(th);
            }
            case "xuat-xu" -> {
                XuatXu xx = xuatXuRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy xuất xứ!"));
                xx.setTrangThai(newStatus);
                xuatXuRepository.save(xx);
            }
            case "danh-muc" -> {
                DanhMuc dm = danhMucRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục!"));
                dm.setTrangThai(newStatus);
                danhMucRepository.save(dm);
            }
            case "co-ao" -> {
                CoAo ca = coAoRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy cổ áo!"));
                ca.setTrangThai(newStatus);
                coAoRepository.save(ca);
            }
            case "tay-ao" -> {
                TayAo ta = tayAoRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy tay áo!"));
                ta.setTrangThai(newStatus);
                tayAoRepository.save(ta);
            }
            case "hoa-tiet" -> {
                HoaTiet ht = hoaTietRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy họa tiết!"));
                ht.setTrangThai(newStatus);
                hoaTietRepository.save(ht);
            }
            case "mau-sac" -> {
                MauSac ms = mauSacRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy màu sắc!"));
                ms.setTrangThai(newStatus);
                mauSacRepository.save(ms);
            }
            case "kich-co" -> {
                KichCo kc = kichCoRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy kích cỡ!"));
                kc.setTrangThai(newStatus);
                kichCoRepository.save(kc);
            }
            default -> throw new IllegalArgumentException("Loại thuộc tính không hợp lệ: " + type);
        }
    }

    public void delete(String type, Long id) {
        String t = normalizeType(type);
        String displayName = getDisplayName(t);

        // Kiểm tra ràng buộc khóa ngoại trước khi xóa
        switch (t) {
            case "chat-lieu" -> {
                if (sanPhamRepository.existsByIdChatLieu_Id(id)) {
                    throw new IllegalArgumentException("Chất liệu này đang được liên kết với sản phẩm, không thể xóa vĩnh viễn! Bạn hãy chuyển sang trạng thái Ngừng sử dụng.");
                }
            }
            case "thuong-hieu" -> {
                if (sanPhamRepository.existsByIdThuongHieu_Id(id)) {
                    throw new IllegalArgumentException("Thương hiệu này đang được liên kết với sản phẩm, không thể xóa vĩnh viễn! Bạn hãy chuyển sang trạng thái Ngừng sử dụng.");
                }
            }
            case "xuat-xu" -> {
                if (sanPhamRepository.existsByIdXuatXu_Id(id)) {
                    throw new IllegalArgumentException("Xuất xứ này đang được liên kết với sản phẩm, không thể xóa vĩnh viễn! Bạn hãy chuyển sang trạng thái Ngừng sử dụng.");
                }
            }
            case "danh-muc" -> {
                if (sanPhamRepository.existsByIdDanhMuc_Id(id)) {
                    throw new IllegalArgumentException("Danh mục này đang được liên kết với sản phẩm, không thể xóa vĩnh viễn! Bạn hãy chuyển sang trạng thái Ngừng sử dụng.");
                }
            }
            case "co-ao" -> {
                if (sanPhamRepository.existsByIdCoAo_Id(id)) {
                    throw new IllegalArgumentException("Cổ áo này đang được liên kết với sản phẩm, không thể xóa vĩnh viễn! Bạn hãy chuyển sang trạng thái Ngừng sử dụng.");
                }
            }
            case "tay-ao" -> {
                if (sanPhamRepository.existsByIdTayAo_Id(id)) {
                    throw new IllegalArgumentException("Tay áo này đang được liên kết với sản phẩm, không thể xóa vĩnh viễn! Bạn hãy chuyển sang trạng thái Ngừng sử dụng.");
                }
            }
            case "hoa-tiet" -> {
                if (sanPhamRepository.existsByIdHoaTiet_Id(id)) {
                    throw new IllegalArgumentException("Họa tiết này đang được liên kết với sản phẩm, không thể xóa vĩnh viễn! Bạn hãy chuyển sang trạng thái Ngừng sử dụng.");
                }
            }
            case "mau-sac" -> {
                if (chiTietSanPhamRepository.existsByIdMauSac_Id(id)) {
                    throw new IllegalArgumentException("Màu sắc này đang được sử dụng trong biến thể sản phẩm, không thể xóa vĩnh viễn! Bạn hãy chuyển sang trạng thái Ngừng sử dụng.");
                }
            }
            case "kich-co" -> {
                if (chiTietSanPhamRepository.existsByIdKichCo_Id(id)) {
                    throw new IllegalArgumentException("Kích cỡ này đang được sử dụng trong biến thể sản phẩm, không thể xóa vĩnh viễn! Bạn hãy chuyển sang trạng thái Ngừng sử dụng.");
                }
            }
            default -> throw new IllegalArgumentException("Loại thuộc tính không hợp lệ: " + type);
        }

        try {
            switch (t) {
                case "chat-lieu" -> chatLieuRepository.deleteById(id);
                case "thuong-hieu" -> thuongHieuRepository.deleteById(id);
                case "xuat-xu" -> xuatXuRepository.deleteById(id);
                case "danh-muc" -> danhMucRepository.deleteById(id);
                case "co-ao" -> coAoRepository.deleteById(id);
                case "tay-ao" -> tayAoRepository.deleteById(id);
                case "hoa-tiet" -> hoaTietRepository.deleteById(id);
                case "mau-sac" -> mauSacRepository.deleteById(id);
                case "kich-co" -> kichCoRepository.deleteById(id);
                default -> throw new IllegalArgumentException("Loại thuộc tính không hợp lệ: " + type);
            }
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Thuộc tính " + displayName + " này đang được sử dụng trong hệ thống, không thể xóa! Hãy chuyển sang trạng thái Ngừng sử dụng.");
        }
    }
}
