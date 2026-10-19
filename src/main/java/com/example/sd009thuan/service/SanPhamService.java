package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.PageResponse;
import com.example.sd009thuan.dto.SanPhamFilterRequest;
import com.example.sd009thuan.dto.SanPhamRequest;
import com.example.sd009thuan.dto.SanPhamResponse;
import com.example.sd009thuan.entity.*;
import com.example.sd009thuan.repository.*;
import jakarta.persistence.criteria.Predicate;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class SanPhamService {

    private final SanPhamRepository sanPhamRepository;
    private final ChiTietSanPhamRepository chiTietSanPhamRepository;
    private final HinhAnhRepository hinhAnhRepository;
    private final XuatXuRepository xuatXuRepository;
    private final ChatLieuRepository chatLieuRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final DanhMucRepository danhMucRepository;
    private final CoAoRepository coAoRepository;
    private final TayAoRepository tayAoRepository;
    private final HoaTietRepository hoaTietRepository;
    private final ChiTietDotGiamGiaRepository chiTietDotGiamGiaRepository;
    private final DotGiamGiaRepository dotGiamGiaRepository;

    public SanPhamService(
            SanPhamRepository sanPhamRepository,
            ChiTietSanPhamRepository chiTietSanPhamRepository,
            HinhAnhRepository hinhAnhRepository,
            XuatXuRepository xuatXuRepository,
            ChatLieuRepository chatLieuRepository,
            ThuongHieuRepository thuongHieuRepository,
            DanhMucRepository danhMucRepository,
            CoAoRepository coAoRepository,
            TayAoRepository tayAoRepository,
            HoaTietRepository hoaTietRepository,
            ChiTietDotGiamGiaRepository chiTietDotGiamGiaRepository,
            DotGiamGiaRepository dotGiamGiaRepository
    ) {
        this.sanPhamRepository = sanPhamRepository;
        this.chiTietSanPhamRepository = chiTietSanPhamRepository;
        this.hinhAnhRepository = hinhAnhRepository;
        this.xuatXuRepository = xuatXuRepository;
        this.chatLieuRepository = chatLieuRepository;
        this.thuongHieuRepository = thuongHieuRepository;
        this.danhMucRepository = danhMucRepository;
        this.coAoRepository = coAoRepository;
        this.tayAoRepository = tayAoRepository;
        this.hoaTietRepository = hoaTietRepository;
        this.chiTietDotGiamGiaRepository = chiTietDotGiamGiaRepository;
        this.dotGiamGiaRepository = dotGiamGiaRepository;
    }

    private Specification<SanPham> buildSpecification(SanPhamFilterRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (req == null) {
                return cb.conjunction();
            }

            if (req.getKeyword() != null && !req.getKeyword().trim().isEmpty()) {
                String pattern = "%" + req.getKeyword().trim().toLowerCase() + "%";
                Predicate matchMa = cb.like(cb.lower(root.get("maSanPham")), pattern);
                Predicate matchTen = cb.like(cb.lower(root.get("tenSanPham")), pattern);
                predicates.add(cb.or(matchMa, matchTen));
            }

            if (req.getIdThuongHieu() != null) {
                predicates.add(cb.equal(root.get("idThuongHieu").get("id"), req.getIdThuongHieu()));
            }
            if (req.getIdXuatXu() != null) {
                predicates.add(cb.equal(root.get("idXuatXu").get("id"), req.getIdXuatXu()));
            }
            if (req.getIdDanhMuc() != null) {
                predicates.add(cb.equal(root.get("idDanhMuc").get("id"), req.getIdDanhMuc()));
            }
            if (req.getIdChatLieu() != null) {
                predicates.add(cb.equal(root.get("idChatLieu").get("id"), req.getIdChatLieu()));
            }
            if (req.getIdCoAo() != null) {
                predicates.add(cb.equal(root.get("idCoAo").get("id"), req.getIdCoAo()));
            }
            if (req.getIdTayAo() != null) {
                predicates.add(cb.equal(root.get("idTayAo").get("id"), req.getIdTayAo()));
            }
            if (req.getIdHoaTiet() != null) {
                predicates.add(cb.equal(root.get("idHoaTiet").get("id"), req.getIdHoaTiet()));
            }
            if (req.getTrangThai() != null) {
                predicates.add(cb.equal(root.get("trangThai"), req.getTrangThai()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public PageResponse<SanPhamResponse> getAll(SanPhamFilterRequest req) {
        String sortDir = (req != null && req.getSortDir() != null && !req.getSortDir().trim().isEmpty())
                ? req.getSortDir().trim() : "desc";
        String sortBy = (req != null && req.getSortBy() != null && !req.getSortBy().trim().isEmpty())
                ? req.getSortBy().trim() : "id";
        int page = (req != null && req.getPage() >= 0) ? req.getPage() : 0;
        int size = (req != null && req.getSize() > 0) ? req.getSize() : 10;

        Sort sort = Sort.by(
                sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC,
                sortBy
        );
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<SanPham> spec = buildSpecification(req);

        Page<SanPham> pageResult = sanPhamRepository.findAll(spec, pageable);
        List<SanPhamResponse> dtoList = pageResult.getContent().stream()
                .map(this::convertToResponse)
                .toList();

        return PageResponse.<SanPhamResponse>builder()
                .content(dtoList)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    public SanPhamResponse getById(Long id) {
        SanPham sp = sanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm có ID: " + id));
        return convertToResponse(sp);
    }

    public String nextCode() {
        List<SanPham> all = sanPhamRepository.findAll();
        int maxIndex = 0;
        for (SanPham sp : all) {
            String code = sp.getMaSanPham();
            if (code != null) {
                String trimmed = code.trim().toUpperCase();
                if (trimmed.startsWith("SP")) {
                    String numPart = trimmed.substring(2).trim();
                    if (numPart.matches("\\d{1,4}")) {
                        try {
                            int val = Integer.parseInt(numPart);
                            if (val > maxIndex) {
                                maxIndex = val;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }
        int next = maxIndex + 1;
        String candidate = String.format("SP%03d", next);
        while (sanPhamRepository.existsByMaSanPhamIgnoreCase(candidate)) {
            next++;
            candidate = String.format("SP%03d", next);
        }
        return candidate;
    }

    private boolean isSameAttributes(
            SanPham sp,
            Long idDanhMuc,
            Long idThuongHieu,
            Long idChatLieu,
            Long idCoAo,
            Long idTayAo,
            Long idXuatXu,
            Long idHoaTiet
    ) {
        Long spDanhMuc = sp.getIdDanhMuc() != null ? sp.getIdDanhMuc().getId() : null;
        Long spThuongHieu = sp.getIdThuongHieu() != null ? sp.getIdThuongHieu().getId() : null;
        Long spChatLieu = sp.getIdChatLieu() != null ? sp.getIdChatLieu().getId() : null;
        Long spCoAo = sp.getIdCoAo() != null ? sp.getIdCoAo().getId() : null;
        Long spTayAo = sp.getIdTayAo() != null ? sp.getIdTayAo().getId() : null;
        Long spXuatXu = sp.getIdXuatXu() != null ? sp.getIdXuatXu().getId() : null;
        Long spHoaTiet = sp.getIdHoaTiet() != null ? sp.getIdHoaTiet().getId() : null;

        return java.util.Objects.equals(spDanhMuc, idDanhMuc)
                && java.util.Objects.equals(spThuongHieu, idThuongHieu)
                && java.util.Objects.equals(spChatLieu, idChatLieu)
                && java.util.Objects.equals(spCoAo, idCoAo)
                && java.util.Objects.equals(spTayAo, idTayAo)
                && java.util.Objects.equals(spXuatXu, idXuatXu)
                && java.util.Objects.equals(spHoaTiet, idHoaTiet);
    }

    public Optional<SanPham> findIdenticalProduct(
            String tenSanPham,
            Long idDanhMuc,
            Long idThuongHieu,
            Long idChatLieu,
            Long idCoAo,
            Long idTayAo,
            Long idXuatXu,
            Long idHoaTiet
    ) {
        if (tenSanPham == null || tenSanPham.trim().isEmpty()) {
            return Optional.empty();
        }
        List<SanPham> sameNameProducts = sanPhamRepository.findAllByTenSanPhamIgnoreCase(tenSanPham.trim());
        for (SanPham sp : sameNameProducts) {
            if (isSameAttributes(sp, idDanhMuc, idThuongHieu, idChatLieu, idCoAo, idTayAo, idXuatXu, idHoaTiet)) {
                return Optional.of(sp);
            }
        }
        return Optional.empty();
    }

    public java.util.Map<String, Object> checkDuplicate(String maSanPham, String tenSanPham) {
        return checkDuplicate(maSanPham, tenSanPham, null, null, null, null, null, null, null);
    }

    public java.util.Map<String, Object> checkDuplicate(
            String maSanPham,
            String tenSanPham,
            Long idDanhMuc,
            Long idThuongHieu,
            Long idChatLieu,
            Long idCoAo,
            Long idTayAo,
            Long idXuatXu,
            Long idHoaTiet
    ) {
        String trimmedMa = maSanPham != null ? maSanPham.trim() : "";
        String trimmedTen = tenSanPham != null ? tenSanPham.trim() : "";

        java.util.Map<String, Object> response = new java.util.HashMap<>();

        // Kiểm tra theo mã sản phẩm nếu người dùng có nhập mã
        Optional<SanPham> existingByCode = !trimmedMa.isEmpty()
                ? sanPhamRepository.findByMaSanPhamIgnoreCase(trimmedMa)
                : Optional.empty();

        // Tìm sản phẩm trùng CẢ TÊN VÀ TẤT CẢ THUỘC TÍNH
        Optional<SanPham> existingIdentical = !trimmedTen.isEmpty()
                ? findIdenticalProduct(trimmedTen, idDanhMuc, idThuongHieu, idChatLieu, idCoAo, idTayAo, idXuatXu, idHoaTiet)
                : Optional.empty();

        // Xung đột mã: Mã thuộc về SP khác nhưng bộ tên + thuộc tính lại trùng SP khác
        if (existingByCode.isPresent() && existingIdentical.isPresent()
                && !existingByCode.get().getId().equals(existingIdentical.get().getId())) {
            response.put("conflict", true);
            response.put("exists", true);
            response.put("message", "Mã sản phẩm '" + trimmedMa + "' thuộc về sản phẩm '"
                    + existingByCode.get().getTenSanPham() + "', nhưng bộ thông tin (tên + thuộc tính) lại trùng với sản phẩm '"
                    + existingIdentical.get().getMaSanPham() + "'. Vui lòng kiểm tra lại mã hoặc để trống mã để hệ thống tự sinh!");
            return response;
        }

        // Mã đã tồn tại ở SP khác nhưng thuộc tính không trùng hoàn toàn
        if (existingByCode.isPresent() && existingIdentical.isEmpty()) {
            response.put("conflict", true);
            response.put("exists", true);
            response.put("message", "Mã sản phẩm '" + trimmedMa + "' đã tồn tại trong hệ thống (thuộc sản phẩm '"
                    + existingByCode.get().getTenSanPham() + "'). Vui lòng nhập mã khác hoặc để trống mã để hệ thống tự sinh!");
            return response;
        }

        // Trùng cả tên và tất cả thuộc tính
        Optional<SanPham> existingSpOpt = existingIdentical.isPresent() ? existingIdentical : (existingByCode.isPresent() ? existingByCode : Optional.empty());

        if (existingSpOpt.isPresent()) {
            SanPham sp = existingSpOpt.get();
            response.put("exists", true);
            response.put("conflict", false);
            response.put("id", sp.getId());
            response.put("maSanPham", sp.getMaSanPham());
            response.put("tenSanPham", sp.getTenSanPham());
            response.put("matchedBy", "cả tên và toàn bộ các thuộc tính");

            List<ChiTietSanPham> existingVariants = chiTietSanPhamRepository.findByIdSanPham_Id(sp.getId());
            response.put("existingVariantCount", existingVariants != null ? existingVariants.size() : 0);
            return response;
        }

        // Có ít nhất 1 thuộc tính khác (hoặc tên khác) -> Thêm mới hoàn toàn
        response.put("exists", false);
        response.put("conflict", false);
        return response;
    }

    @Transactional
    public SanPhamResponse create(SanPhamRequest req) {
        String trimmedName = req.getTenSanPham() != null ? req.getTenSanPham().trim() : "";
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống!");
        }

        String trimmedCode = req.getMaSanPham() != null ? req.getMaSanPham().trim() : "";

        // Kiểm tra xem có sản phẩm nào trùng CẢ TÊN VÀ TẤT CẢ THUỘC TÍNH không
        Optional<SanPham> existingIdentical = findIdenticalProduct(
                trimmedName,
                req.getIdDanhMuc(),
                req.getIdThuongHieu(),
                req.getIdChatLieu(),
                req.getIdCoAo(),
                req.getIdTayAo(),
                req.getIdXuatXu(),
                req.getIdHoaTiet()
        );

        Optional<SanPham> existingByCode = !trimmedCode.isEmpty()
                ? sanPhamRepository.findByMaSanPhamIgnoreCase(trimmedCode)
                : Optional.empty();

        // Xung đột: Mã SP thuộc SP A nhưng bộ tên + thuộc tính lại thuộc SP B
        if (existingByCode.isPresent() && existingIdentical.isPresent()
                && !existingByCode.get().getId().equals(existingIdentical.get().getId())) {
            throw new IllegalArgumentException("Xung đột: Mã sản phẩm '" + trimmedCode + "' thuộc về sản phẩm '"
                    + existingByCode.get().getTenSanPham() + "', nhưng bộ thông tin tên và thuộc tính lại trùng với sản phẩm '"
                    + existingIdentical.get().getMaSanPham() + "'! Vui lòng kiểm tra lại mã.");
        }

        // Mã đã tồn tại ở SP khác nhưng thuộc tính không trùng
        if (existingByCode.isPresent() && existingIdentical.isEmpty()) {
            throw new IllegalArgumentException("Mã sản phẩm '" + trimmedCode + "' đã tồn tại trong hệ thống (thuộc sản phẩm '"
                    + existingByCode.get().getTenSanPham() + "'). Vui lòng nhập mã khác hoặc để trống để hệ thống tự sinh mã!");
        }

        // TRƯỜNG HỢP 1: Trùng CẢ TÊN VÀ TẤT CẢ CÁC THUỘC TÍNH -> GỘP VÀ CỘNG DỒN BIẾN THỂ
        if (existingIdentical.isPresent()) {
            SanPham existingSp = existingIdentical.get();

            if (!Boolean.TRUE.equals(req.getXacNhanGop())) {
                throw new IllegalArgumentException("Sản phẩm '" + trimmedName + "' với cùng toàn bộ các thuộc tính đã tồn tại trong hệ thống (Mã: "
                        + existingSp.getMaSanPham() + ")! Vui lòng xác nhận gộp để cộng dồn các biến thể.");
            }

            // Người dùng ĐÃ XÁC NHẬN GỘP: Cập nhật thông tin mô tả, trạng thái
            if (req.getMoTa() != null && !req.getMoTa().trim().isEmpty()) {
                existingSp.setMoTa(req.getMoTa().trim());
            }

            existingSp.setTrangThai(1);
            existingSp.setNguoiCapNhat(req.getNguoiThaoTac() != null ? req.getNguoiThaoTac() : "Admin");

            SanPham saved = sanPhamRepository.save(existingSp);

            if (req.getHinhAnhs() != null && !req.getHinhAnhs().isEmpty()) {
                saveProductImages(saved, req.getHinhAnhs());
            }

            return convertToResponse(saved);
        }

        // TRƯỜNG HỢP 2: CÓ ÍT NHẤT 1 THUỘC TÍNH KHÔNG TRÙNG (HOẶC TÊN MỚI) -> TẠO MỚI HOÀN TOÀN, KHÔNG CỘNG DỒN
        if (trimmedCode.isEmpty()) {
            req.setMaSanPham(nextCode());
        } else {
            if (sanPhamRepository.existsByMaSanPhamIgnoreCase(trimmedCode)) {
                throw new IllegalArgumentException("Mã sản phẩm '" + trimmedCode + "' đã tồn tại trong hệ thống! Vui lòng nhập mã khác hoặc để trống để tự sinh mã.");
            }
            req.setMaSanPham(trimmedCode);
        }

        SanPham sp = new SanPham();
        sp.setMaSanPham(req.getMaSanPham());
        sp.setTenSanPham(trimmedName);
        sp.setMoTa(req.getMoTa());
        sp.setTrangThai(req.getTrangThai() != null ? req.getTrangThai() : 1);
        sp.setNguoiTao(req.getNguoiThaoTac() != null ? req.getNguoiThaoTac() : "Admin");

        if (req.getIdXuatXu() != null) {
            xuatXuRepository.findById(req.getIdXuatXu()).ifPresent(sp::setIdXuatXu);
        }
        if (req.getIdChatLieu() != null) {
            chatLieuRepository.findById(req.getIdChatLieu()).ifPresent(sp::setIdChatLieu);
        }
        if (req.getIdThuongHieu() != null) {
            thuongHieuRepository.findById(req.getIdThuongHieu()).ifPresent(sp::setIdThuongHieu);
        }
        if (req.getIdDanhMuc() != null) {
            danhMucRepository.findById(req.getIdDanhMuc()).ifPresent(sp::setIdDanhMuc);
        }
        if (req.getIdCoAo() != null) {
            coAoRepository.findById(req.getIdCoAo()).ifPresent(sp::setIdCoAo);
        }
        if (req.getIdTayAo() != null) {
            tayAoRepository.findById(req.getIdTayAo()).ifPresent(sp::setIdTayAo);
        }
        if (req.getIdHoaTiet() != null) {
            hoaTietRepository.findById(req.getIdHoaTiet()).ifPresent(sp::setIdHoaTiet);
        }

        SanPham saved = sanPhamRepository.save(sp);

        if (req.getHinhAnhs() != null && !req.getHinhAnhs().isEmpty()) {
            saveProductImages(saved, req.getHinhAnhs());
        }

        return convertToResponse(saved);
    }


    private void saveProductImages(SanPham saved, List<String> hinhAnhs) {
        for (String rawItem : hinhAnhs) {
            if (rawItem == null || rawItem.trim().isEmpty()) continue;
            String item = rawItem.trim();
            Long colorId = null;
            if (item.contains("|")) {
                String[] parts = item.split("\\|", 2);
                try {
                    colorId = Long.parseLong(parts[0].trim());
                } catch (Exception ignored) {}
                item = parts[1].trim();
            }
            String processedUrl = processImage(item);
            if (processedUrl != null && !processedUrl.trim().isEmpty()) {
                HinhAnh ha = new HinhAnh();
                ha.setIdSanPham(saved);
                ha.setTenAnh(colorId != null ? "COLOR_" + colorId : saved.getTenSanPham());
                ha.setDuongDan(processedUrl.trim());
                ha.setTrangThai(1);
                hinhAnhRepository.save(ha);
            }
        }
    }

    @Transactional
    public SanPhamResponse update(Long id, SanPhamRequest req) {
        SanPham sp = sanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm có ID: " + id));

        String trimmedName = req.getTenSanPham() != null ? req.getTenSanPham().trim() : "";
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống!");
        }
        List<SanPham> sameNameOthers = sanPhamRepository.findAllByTenSanPhamIgnoreCaseAndIdNot(trimmedName, id);
        for (SanPham other : sameNameOthers) {
            if (isSameAttributes(other, req.getIdDanhMuc(), req.getIdThuongHieu(), req.getIdChatLieu(),
                    req.getIdCoAo(), req.getIdTayAo(), req.getIdXuatXu(), req.getIdHoaTiet())) {
                throw new IllegalArgumentException("Đã tồn tại một sản phẩm khác có cùng tên '" + trimmedName + "' và toàn bộ các thuộc tính này (Mã: " + other.getMaSanPham() + ")!");
            }
        }

        if (req.getMaSanPham() != null && !req.getMaSanPham().trim().isEmpty()) {
            String trimmedCode = req.getMaSanPham().trim();
            if (sanPhamRepository.existsByMaSanPhamIgnoreCaseAndIdNot(trimmedCode, id)) {
                throw new IllegalArgumentException("Mã sản phẩm '" + trimmedCode + "' đã tồn tại ở sản phẩm khác!");
            }
            sp.setMaSanPham(trimmedCode);
        }

        sp.setTenSanPham(trimmedName);
        sp.setMoTa(req.getMoTa());
        if (req.getTrangThai() != null) {
            sp.setTrangThai(req.getTrangThai());
        }
        sp.setNguoiCapNhat(req.getNguoiThaoTac() != null ? req.getNguoiThaoTac() : "Admin");

        if (req.getIdXuatXu() != null) {
            xuatXuRepository.findById(req.getIdXuatXu()).ifPresent(sp::setIdXuatXu);
        } else {
            sp.setIdXuatXu(null);
        }

        if (req.getIdChatLieu() != null) {
            chatLieuRepository.findById(req.getIdChatLieu()).ifPresent(sp::setIdChatLieu);
        } else {
            sp.setIdChatLieu(null);
        }

        if (req.getIdThuongHieu() != null) {
            thuongHieuRepository.findById(req.getIdThuongHieu()).ifPresent(sp::setIdThuongHieu);
        } else {
            sp.setIdThuongHieu(null);
        }

        if (req.getIdDanhMuc() != null) {
            danhMucRepository.findById(req.getIdDanhMuc()).ifPresent(sp::setIdDanhMuc);
        } else {
            sp.setIdDanhMuc(null);
        }

        if (req.getIdCoAo() != null) {
            coAoRepository.findById(req.getIdCoAo()).ifPresent(sp::setIdCoAo);
        } else {
            sp.setIdCoAo(null);
        }

        if (req.getIdTayAo() != null) {
            tayAoRepository.findById(req.getIdTayAo()).ifPresent(sp::setIdTayAo);
        } else {
            sp.setIdTayAo(null);
        }

        if (req.getIdHoaTiet() != null) {
            hoaTietRepository.findById(req.getIdHoaTiet()).ifPresent(sp::setIdHoaTiet);
        } else {
            sp.setIdHoaTiet(null);
        }

        SanPham updated = sanPhamRepository.save(sp);

        if (req.getHinhAnhs() != null) {
            List<HinhAnh> oldImages = hinhAnhRepository.findByIdSanPham_Id(id);
            hinhAnhRepository.deleteAll(oldImages);
            for (String rawItem : req.getHinhAnhs()) {
                if (rawItem == null || rawItem.trim().isEmpty()) continue;
                String item = rawItem.trim();
                Long colorId = null;
                if (item.contains("|")) {
                    String[] parts = item.split("\\|", 2);
                    try {
                        colorId = Long.parseLong(parts[0].trim());
                    } catch (Exception ignored) {}
                    item = parts[1].trim();
                }
                String processedUrl = processImage(item);
                if (processedUrl != null && !processedUrl.trim().isEmpty()) {
                    HinhAnh ha = new HinhAnh();
                    ha.setIdSanPham(updated);
                    ha.setTenAnh(colorId != null ? "COLOR_" + colorId : updated.getTenSanPham());
                    ha.setDuongDan(processedUrl.trim());
                    ha.setTrangThai(1);
                    hinhAnhRepository.save(ha);
                }
            }
        }

        return convertToResponse(updated);
    }

    private String processImage(String imageInput) {
        if (imageInput == null || imageInput.trim().isEmpty()) {
            return null;
        }
        String trimmed = imageInput.trim();
        if (trimmed.startsWith("data:image/")) {
            try {
                int commaIdx = trimmed.indexOf(",");
                if (commaIdx != -1) {
                    String metadata = trimmed.substring(0, commaIdx);
                    String base64Data = trimmed.substring(commaIdx + 1);

                    String extension = ".png";
                    if (metadata.contains("image/jpeg") || metadata.contains("image/jpg")) {
                        extension = ".jpg";
                    } else if (metadata.contains("image/webp")) {
                        extension = ".webp";
                    } else if (metadata.contains("image/gif")) {
                        extension = ".gif";
                    }

                    byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Data);

                    java.nio.file.Path uploadDir = java.nio.file.Paths.get("uploads");
                    if (!java.nio.file.Files.exists(uploadDir)) {
                        java.nio.file.Files.createDirectories(uploadDir);
                    }

                    String filename = "sp_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8) + extension;
                    java.nio.file.Path filePath = uploadDir.resolve(filename);
                    java.nio.file.Files.write(filePath, imageBytes);

                    return "/uploads/" + filename;
                }
            } catch (Exception e) {
                return null;
            }
        }

        if (trimmed.length() > 500) {
            return trimmed.substring(0, 500);
        }
        return trimmed;
    }

    @Transactional
    public void changeStatus(Long id, Integer trangThai) {
        SanPham sp = sanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm có ID: " + id));
        sp.setTrangThai(trangThai);
        sanPhamRepository.save(sp);
    }

    @Transactional
    public void delete(Long id) {
        SanPham sp = sanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm có ID: " + id));

        try {
            List<ChiTietSanPham> variants = chiTietSanPhamRepository.findByIdSanPham_Id(id);
            if (variants != null && !variants.isEmpty()) {
                chiTietSanPhamRepository.deleteAll(variants);
                chiTietSanPhamRepository.flush();
            }

            List<HinhAnh> images = hinhAnhRepository.findByIdSanPham_Id(id);
            if (images != null && !images.isEmpty()) {
                hinhAnhRepository.deleteAll(images);
                hinhAnhRepository.flush();
            }

            sanPhamRepository.delete(sp);
            sanPhamRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Không thể xóa sản phẩm vì đã có biến thể phát sinh giao dịch/hóa đơn trong hệ thống! Vui lòng chuyển trạng thái sang 'Ngừng kinh doanh'.");
        }
    }

    public byte[] exportExcel(SanPhamFilterRequest req) {
        Specification<SanPham> spec = buildSpecification(req);
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        List<SanPham> list = sanPhamRepository.findAll(spec, sort);
        List<SanPhamResponse> dtoList = list.stream().map(this::convertToResponse).toList();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Danh Sách Sản Phẩm");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setAlignment(HorizontalAlignment.RIGHT);
            numberStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);
            centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            String[] headers = {
                    "STT", "Mã sản phẩm", "Tên sản phẩm", "Danh mục", "Thương hiệu",
                    "Xuất xứ", "Chất liệu", "Cổ áo", "Tay áo", "Họa tiết",
                    "Số màu", "Số kích thước",
                    "Giá bán (VNĐ)", "Giá sau giảm (VNĐ)", "Đợt giảm giá",
                    "Số lượng tồn", "Số biến thể",
                    "Trạng thái", "Ngày tạo"
            };

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(24);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

            int rowIdx = 1;
            for (SanPhamResponse sp : dtoList) {
                Row row = sheet.createRow(rowIdx++);

                Cell c0 = row.createCell(0);
                c0.setCellValue(rowIdx - 1);
                c0.setCellStyle(centerStyle);

                Cell c1 = row.createCell(1);
                c1.setCellValue(sp.getMaSanPham() != null ? sp.getMaSanPham() : "");
                c1.setCellStyle(dataStyle);

                Cell c2 = row.createCell(2);
                c2.setCellValue(sp.getTenSanPham() != null ? sp.getTenSanPham() : "");
                c2.setCellStyle(dataStyle);

                Cell c3 = row.createCell(3);
                c3.setCellValue(sp.getTenDanhMuc() != null ? sp.getTenDanhMuc() : "");
                c3.setCellStyle(dataStyle);

                Cell c4 = row.createCell(4);
                c4.setCellValue(sp.getTenThuongHieu() != null ? sp.getTenThuongHieu() : "");
                c4.setCellStyle(dataStyle);

                Cell c5 = row.createCell(5);
                c5.setCellValue(sp.getTenXuatXu() != null ? sp.getTenXuatXu() : "");
                c5.setCellStyle(dataStyle);

                Cell c6 = row.createCell(6);
                c6.setCellValue(sp.getTenChatLieu() != null ? sp.getTenChatLieu() : "");
                c6.setCellStyle(dataStyle);

                Cell c7 = row.createCell(7);
                c7.setCellValue(sp.getTenCoAo() != null ? sp.getTenCoAo() : "");
                c7.setCellStyle(dataStyle);

                Cell c8 = row.createCell(8);
                c8.setCellValue(sp.getTenTayAo() != null ? sp.getTenTayAo() : "");
                c8.setCellStyle(dataStyle);

                Cell c9 = row.createCell(9);
                c9.setCellValue(sp.getTenHoaTiet() != null ? sp.getTenHoaTiet() : "");
                c9.setCellStyle(dataStyle);

                Cell c10 = row.createCell(10);
                c10.setCellValue(sp.getSoMau() != null ? sp.getSoMau() : 0);
                c10.setCellStyle(centerStyle);

                Cell c11 = row.createCell(11);
                c11.setCellValue(sp.getSoKichCo() != null ? sp.getSoKichCo() : 0);
                c11.setCellStyle(centerStyle);

                Cell c12 = row.createCell(12);
                c12.setCellValue(sp.getMinGia() != null ? sp.getMinGia().doubleValue() : 0);
                c12.setCellStyle(numberStyle);

                Cell c13 = row.createCell(13);
                if (Boolean.TRUE.equals(sp.getCoGiamGia()) && sp.getMinGiaSauGiam() != null) {
                    c13.setCellValue(sp.getMinGiaSauGiam().doubleValue());
                } else {
                    c13.setCellValue(sp.getMinGia() != null ? sp.getMinGia().doubleValue() : 0);
                }
                c13.setCellStyle(numberStyle);

                Cell c14 = row.createCell(14);
                c14.setCellValue(Boolean.TRUE.equals(sp.getCoGiamGia()) && sp.getTenDotGiamGia() != null ? sp.getTenDotGiamGia() : "Không áp dụng");
                c14.setCellStyle(centerStyle);

                Cell c15 = row.createCell(15);
                c15.setCellValue(sp.getTongSoLuong() != null ? sp.getTongSoLuong() : 0);
                c15.setCellStyle(centerStyle);

                Cell c16 = row.createCell(16);
                c16.setCellValue(sp.getSoLuongBienThe() != null ? sp.getSoLuongBienThe() : 0);
                c16.setCellStyle(centerStyle);

                Cell c17 = row.createCell(17);
                c17.setCellValue(sp.getTrangThai() != null && sp.getTrangThai() == 1 ? "Đang kinh doanh" : "Ngừng kinh doanh");
                c17.setCellStyle(centerStyle);

                Cell c18 = row.createCell(18);
                c18.setCellValue(sp.getNgayTao() != null ? formatter.format(sp.getNgayTao()) : "");
                c18.setCellStyle(centerStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi xuất file Excel: " + e.getMessage());
        }
    }

    private SanPhamResponse convertToResponse(SanPham sp) {
        BigDecimal minGia = chiTietSanPhamRepository.findMinPriceBySanPhamId(sp.getId());
        BigDecimal maxGia = chiTietSanPhamRepository.findMaxPriceBySanPhamId(sp.getId());
        Long sum = chiTietSanPhamRepository.sumSoLuongBySanPhamId(sp.getId());
        Integer tongSoLuong = sum != null ? sum.intValue() : 0;
        List<ChiTietSanPham> variants = chiTietSanPhamRepository.findByIdSanPham_Id(sp.getId());
        int soMau = 0;
        int soKichCo = 0;
        if (variants != null && !variants.isEmpty()) {
            soMau = (int) variants.stream()
                    .map(v -> v.getIdMauSac() != null ? v.getIdMauSac().getId() : null)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .count();
            soKichCo = (int) variants.stream()
                    .map(v -> v.getIdKichCo() != null ? v.getIdKichCo().getId() : null)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .count();
        }

        // Kiểm tra và liên kết với đợt giảm giá đang hoạt động
        Instant now = Instant.now();
        List<ChiTietDotGiamGia> activeDiscounts = chiTietDotGiamGiaRepository.findActiveDiscountsBySanPhamId(sp.getId(), now);

        boolean coGiamGia = false;
        BigDecimal minGiaSauGiam = null;
        BigDecimal maxGiaSauGiam = null;
        BigDecimal phanTramGiam = null;
        String tenDotGiamGia = null;

        if (activeDiscounts != null && !activeDiscounts.isEmpty()) {
            coGiamGia = true;
            ChiTietDotGiamGia bestDgg = activeDiscounts.stream()
                .max(java.util.Comparator.comparing(c -> {
                    BigDecimal pt = c.getPhanTramGiamBienThe() != null 
                            ? c.getPhanTramGiamBienThe() 
                            : (c.getIdDotGiamGia() != null ? c.getIdDotGiamGia().getPhanTramGiam() : BigDecimal.ZERO);
                    return pt != null ? pt : BigDecimal.ZERO;
                }))
                .orElse(activeDiscounts.get(0));

            if (bestDgg.getIdDotGiamGia() != null) {
                tenDotGiamGia = bestDgg.getIdDotGiamGia().getTenDotGiamGia();
                phanTramGiam = bestDgg.getPhanTramGiamBienThe() != null 
                        ? bestDgg.getPhanTramGiamBienThe() 
                        : bestDgg.getIdDotGiamGia().getPhanTramGiam();
            }

            java.util.Map<Long, BigDecimal> discountMap = new java.util.HashMap<>();
            for (ChiTietDotGiamGia c : activeDiscounts) {
                if (c.getIdChiTietSanPham() != null) {
                    BigDecimal pt = c.getPhanTramGiamBienThe() != null 
                            ? c.getPhanTramGiamBienThe() 
                            : (c.getIdDotGiamGia() != null ? c.getIdDotGiamGia().getPhanTramGiam() : BigDecimal.ZERO);
                    discountMap.merge(c.getIdChiTietSanPham().getId(), pt, BigDecimal::max);
                }
            }

            if (variants != null && !variants.isEmpty()) {
                for (ChiTietSanPham v : variants) {
                    if (v.getGiaBan() == null) continue;
                    BigDecimal pt = discountMap.get(v.getId());
                    if (pt == null) {
                        pt = BigDecimal.ZERO;
                    }
                    BigDecimal priceAfter = v.getGiaBan();
                    if (pt != null && pt.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal factor = BigDecimal.valueOf(100).subtract(pt).max(BigDecimal.ZERO);
                        priceAfter = v.getGiaBan().multiply(factor).divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
                    }
                    if (minGiaSauGiam == null || priceAfter.compareTo(minGiaSauGiam) < 0) {
                        minGiaSauGiam = priceAfter;
                    }
                    if (maxGiaSauGiam == null || priceAfter.compareTo(maxGiaSauGiam) > 0) {
                        maxGiaSauGiam = priceAfter;
                    }
                }
            }

            if (minGiaSauGiam != null && minGia != null && minGiaSauGiam.compareTo(minGia) >= 0) {
                coGiamGia = false;
                minGiaSauGiam = null;
                maxGiaSauGiam = null;
            }
        }

        List<HinhAnh> images = hinhAnhRepository.findByIdSanPham_Id(sp.getId());
        String anhDaiDien = (images != null && !images.isEmpty()) ? images.get(0).getDuongDan() : null;
        List<String> hinhAnhs = (images != null) ? images.stream().map(ha -> {
            if (ha.getTenAnh() != null && ha.getTenAnh().startsWith("COLOR_")) {
                return ha.getTenAnh().substring(6) + "|" + ha.getDuongDan();
            }
            return ha.getDuongDan();
        }).toList() : List.of();

        return SanPhamResponse.builder()
                .id(sp.getId())
                .maSanPham(sp.getMaSanPham())
                .tenSanPham(sp.getTenSanPham())
                .moTa(sp.getMoTa())
                .trangThai(sp.getTrangThai())
                .ngayTao(sp.getNgayTao())
                .ngayCapNhat(sp.getNgayCapNhat())
                .nguoiTao(sp.getNguoiTao())
                .idXuatXu(sp.getIdXuatXu() != null ? sp.getIdXuatXu().getId() : null)
                .tenXuatXu(sp.getIdXuatXu() != null ? sp.getIdXuatXu().getTenXuatXu() : null)
                .idChatLieu(sp.getIdChatLieu() != null ? sp.getIdChatLieu().getId() : null)
                .tenChatLieu(sp.getIdChatLieu() != null ? sp.getIdChatLieu().getTenChatLieu() : null)
                .idThuongHieu(sp.getIdThuongHieu() != null ? sp.getIdThuongHieu().getId() : null)
                .tenThuongHieu(sp.getIdThuongHieu() != null ? sp.getIdThuongHieu().getTenThuongHieu() : null)
                .idDanhMuc(sp.getIdDanhMuc() != null ? sp.getIdDanhMuc().getId() : null)
                .tenDanhMuc(sp.getIdDanhMuc() != null ? sp.getIdDanhMuc().getTenDanhMuc() : null)
                .idCoAo(sp.getIdCoAo() != null ? sp.getIdCoAo().getId() : null)
                .tenCoAo(sp.getIdCoAo() != null ? sp.getIdCoAo().getTenCoAo() : null)
                .idTayAo(sp.getIdTayAo() != null ? sp.getIdTayAo().getId() : null)
                .tenTayAo(sp.getIdTayAo() != null ? sp.getIdTayAo().getTenTayAo() : null)
                .idHoaTiet(sp.getIdHoaTiet() != null ? sp.getIdHoaTiet().getId() : null)
                .tenHoaTiet(sp.getIdHoaTiet() != null ? sp.getIdHoaTiet().getTenHoaTiet() : null)
                .minGia(minGia)
                .maxGia(maxGia)
                .coGiamGia(coGiamGia)
                .minGiaSauGiam(minGiaSauGiam)
                .maxGiaSauGiam(maxGiaSauGiam)
                .phanTramGiam(phanTramGiam)
                .tenDotGiamGia(tenDotGiamGia)
                .tongSoLuong(tongSoLuong)
                .soLuongBienThe(variants != null ? variants.size() : 0)
                .soMau(soMau)
                .soKichCo(soKichCo)
                .anhDaiDien(anhDaiDien)
                .hinhAnhs(hinhAnhs)
                .build();
    }
}
