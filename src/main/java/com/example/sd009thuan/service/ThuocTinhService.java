package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.ThuocTinhRequest;
import com.example.sd009thuan.entity.*;
import com.example.sd009thuan.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ThuocTinhService {

    private final ChatLieuRepository chatLieuRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final XuatXuRepository xuatXuRepository;
    private final DanhMucRepository danhMucRepository;
    private final CoAoRepository coAoRepository;
    private final TayAoRepository tayAoRepository;
    private final HoaTietRepository hoaTietRepository;
    private final MauSacRepository mauSacRepository;
    private final KichCoRepository kichCoRepository;

    public ThuocTinhService(
            ChatLieuRepository chatLieuRepository,
            ThuongHieuRepository thuongHieuRepository,
            XuatXuRepository xuatXuRepository,
            DanhMucRepository danhMucRepository,
            CoAoRepository coAoRepository,
            TayAoRepository tayAoRepository,
            HoaTietRepository hoaTietRepository,
            MauSacRepository mauSacRepository,
            KichCoRepository kichCoRepository
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
        if (req.getTen() == null || req.getTen().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên thuộc tính không được để trống!");
        }
        String t = normalizeType(type);
        long suffix = System.currentTimeMillis() % 1000000;
        int status = (req.getTrangThai() != null) ? req.getTrangThai() : 1;

        return switch (t) {
            case "chat-lieu" -> {
                ChatLieu cl = new ChatLieu();
                cl.setMaChatLieu((req.getMa() != null && !req.getMa().trim().isEmpty()) ? req.getMa().trim() : "CL" + suffix);
                cl.setTenChatLieu(req.getTen().trim());
                cl.setTrangThai(status);
                yield chatLieuRepository.save(cl);
            }
            case "thuong-hieu" -> {
                ThuongHieu th = new ThuongHieu();
                th.setMaThuongHieu((req.getMa() != null && !req.getMa().trim().isEmpty()) ? req.getMa().trim() : "TH" + suffix);
                th.setTenThuongHieu(req.getTen().trim());
                th.setTrangThai(status);
                yield thuongHieuRepository.save(th);
            }
            case "xuat-xu" -> {
                XuatXu xx = new XuatXu();
                xx.setMaXuatXu((req.getMa() != null && !req.getMa().trim().isEmpty()) ? req.getMa().trim() : "XX" + suffix);
                xx.setTenXuatXu(req.getTen().trim());
                xx.setTrangThai(status);
                yield xuatXuRepository.save(xx);
            }
            case "danh-muc" -> {
                DanhMuc dm = new DanhMuc();
                dm.setMaDanhMuc((req.getMa() != null && !req.getMa().trim().isEmpty()) ? req.getMa().trim() : "DM" + suffix);
                dm.setTenDanhMuc(req.getTen().trim());
                dm.setTrangThai(status);
                yield danhMucRepository.save(dm);
            }
            case "co-ao" -> {
                CoAo ca = new CoAo();
                ca.setMaCoAo((req.getMa() != null && !req.getMa().trim().isEmpty()) ? req.getMa().trim() : "CA" + suffix);
                ca.setTenCoAo(req.getTen().trim());
                ca.setTrangThai(status);
                yield coAoRepository.save(ca);
            }
            case "tay-ao" -> {
                TayAo ta = new TayAo();
                ta.setMaTayAo((req.getMa() != null && !req.getMa().trim().isEmpty()) ? req.getMa().trim() : "TA" + suffix);
                ta.setTenTayAo(req.getTen().trim());
                ta.setTrangThai(status);
                yield tayAoRepository.save(ta);
            }
            case "hoa-tiet" -> {
                HoaTiet ht = new HoaTiet();
                ht.setMaHoaTiet((req.getMa() != null && !req.getMa().trim().isEmpty()) ? req.getMa().trim() : "HT" + suffix);
                ht.setTenHoaTiet(req.getTen().trim());
                ht.setTrangThai(status);
                yield hoaTietRepository.save(ht);
            }
            case "mau-sac" -> {
                MauSac ms = new MauSac();
                ms.setMaMauSac((req.getMa() != null && !req.getMa().trim().isEmpty()) ? req.getMa().trim() : "MS" + suffix);
                ms.setTenMauSac(req.getTen().trim());
                ms.setMaHex((req.getMaHex() != null && !req.getMaHex().trim().isEmpty()) ? req.getMaHex().trim() : "#000000");
                ms.setTrangThai(status);
                yield mauSacRepository.save(ms);
            }
            case "kich-co" -> {
                KichCo kc = new KichCo();
                kc.setMaKichCo((req.getMa() != null && !req.getMa().trim().isEmpty()) ? req.getMa().trim() : "KC" + suffix);
                kc.setTenKichCo(req.getTen().trim());
                kc.setTrangThai(status);
                yield kichCoRepository.save(kc);
            }
            default -> throw new IllegalArgumentException("Loại thuộc tính không hợp lệ: " + type);
        };
    }

    public Object update(String type, Long id, ThuocTinhRequest req) {
        if (req.getTen() == null || req.getTen().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên thuộc tính không được để trống!");
        }
        String t = normalizeType(type);
        int status = (req.getTrangThai() != null) ? req.getTrangThai() : 1;

        return switch (t) {
            case "chat-lieu" -> {
                ChatLieu cl = chatLieuRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy chất liệu!"));
                if (req.getMa() != null && !req.getMa().trim().isEmpty()) cl.setMaChatLieu(req.getMa().trim());
                cl.setTenChatLieu(req.getTen().trim());
                cl.setTrangThai(status);
                yield chatLieuRepository.save(cl);
            }
            case "thuong-hieu" -> {
                ThuongHieu th = thuongHieuRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy thương hiệu!"));
                if (req.getMa() != null && !req.getMa().trim().isEmpty()) th.setMaThuongHieu(req.getMa().trim());
                th.setTenThuongHieu(req.getTen().trim());
                th.setTrangThai(status);
                yield thuongHieuRepository.save(th);
            }
            case "xuat-xu" -> {
                XuatXu xx = xuatXuRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy xuất xứ!"));
                if (req.getMa() != null && !req.getMa().trim().isEmpty()) xx.setMaXuatXu(req.getMa().trim());
                xx.setTenXuatXu(req.getTen().trim());
                xx.setTrangThai(status);
                yield xuatXuRepository.save(xx);
            }
            case "danh-muc" -> {
                DanhMuc dm = danhMucRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục!"));
                if (req.getMa() != null && !req.getMa().trim().isEmpty()) dm.setMaDanhMuc(req.getMa().trim());
                dm.setTenDanhMuc(req.getTen().trim());
                dm.setTrangThai(status);
                yield danhMucRepository.save(dm);
            }
            case "co-ao" -> {
                CoAo ca = coAoRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy cổ áo!"));
                if (req.getMa() != null && !req.getMa().trim().isEmpty()) ca.setMaCoAo(req.getMa().trim());
                ca.setTenCoAo(req.getTen().trim());
                ca.setTrangThai(status);
                yield coAoRepository.save(ca);
            }
            case "tay-ao" -> {
                TayAo ta = tayAoRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy tay áo!"));
                if (req.getMa() != null && !req.getMa().trim().isEmpty()) ta.setMaTayAo(req.getMa().trim());
                ta.setTenTayAo(req.getTen().trim());
                ta.setTrangThai(status);
                yield tayAoRepository.save(ta);
            }
            case "hoa-tiet" -> {
                HoaTiet ht = hoaTietRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy họa tiết!"));
                if (req.getMa() != null && !req.getMa().trim().isEmpty()) ht.setMaHoaTiet(req.getMa().trim());
                ht.setTenHoaTiet(req.getTen().trim());
                ht.setTrangThai(status);
                yield hoaTietRepository.save(ht);
            }
            case "mau-sac" -> {
                MauSac ms = mauSacRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy màu sắc!"));
                if (req.getMa() != null && !req.getMa().trim().isEmpty()) ms.setMaMauSac(req.getMa().trim());
                ms.setTenMauSac(req.getTen().trim());
                if (req.getMaHex() != null && !req.getMaHex().trim().isEmpty()) ms.setMaHex(req.getMaHex().trim());
                ms.setTrangThai(status);
                yield mauSacRepository.save(ms);
            }
            case "kich-co" -> {
                KichCo kc = kichCoRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy kích cỡ!"));
                if (req.getMa() != null && !req.getMa().trim().isEmpty()) kc.setMaKichCo(req.getMa().trim());
                kc.setTenKichCo(req.getTen().trim());
                kc.setTrangThai(status);
                yield kichCoRepository.save(kc);
            }
            default -> throw new IllegalArgumentException("Loại thuộc tính không hợp lệ: " + type);
        };
    }

    public void changeStatus(String type, Long id, Integer status) {
        String t = normalizeType(type);
        int newStatus = (status != null) ? status : 1;
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
            throw new RuntimeException("Thuộc tính này đang được sử dụng trong sản phẩm, không thể xóa! Bạn có thể chuyển sang trạng thái Ngừng sử dụng.");
        }
    }
}
