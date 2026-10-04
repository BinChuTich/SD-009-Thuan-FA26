package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.PageResponse;
import com.example.sd009thuan.dto.SanPhamFilterRequest;
import com.example.sd009thuan.dto.SanPhamRequest;
import com.example.sd009thuan.dto.SanPhamResponse;
import com.example.sd009thuan.entity.*;
import com.example.sd009thuan.repository.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
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
            HoaTietRepository hoaTietRepository
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
    }

    public PageResponse<SanPhamResponse> getAll(SanPhamFilterRequest req) {
        Sort sort = Sort.by(
                req.getSortDir().equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC,
                req.getSortBy()
        );
        Pageable pageable = PageRequest.of(req.getPage(), req.getSize(), sort);

        Specification<SanPham> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

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

    @Transactional
    public SanPhamResponse create(SanPhamRequest req) {
        if (req.getMaSanPham() == null || req.getMaSanPham().trim().isEmpty()) {
            req.setMaSanPham("SP" + System.currentTimeMillis());
        } else if (sanPhamRepository.existsByMaSanPham(req.getMaSanPham().trim())) {
            throw new RuntimeException("Mã sản phẩm đã tồn tại: " + req.getMaSanPham());
        }

        SanPham sp = new SanPham();
        sp.setMaSanPham(req.getMaSanPham().trim());
        sp.setTenSanPham(req.getTenSanPham().trim());
        sp.setMoTa(req.getMoTa());
        sp.setTrangThai(req.getTrangThai() != null ? req.getTrangThai() : 1);
        sp.setNguoiTao(req.getNguoiThaoTac() != null ? req.getNguoiThaoTac() : "Admin");

        setAttributes(sp, req);

        SanPham saved = sanPhamRepository.save(sp);
        return convertToResponse(saved);
    }

    @Transactional
    public SanPhamResponse update(Long id, SanPhamRequest req) {
        SanPham sp = sanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm có ID: " + id));

        if (req.getMaSanPham() != null && !req.getMaSanPham().trim().isEmpty()) {
            if (sanPhamRepository.existsByMaSanPhamAndIdNot(req.getMaSanPham().trim(), id)) {
                throw new RuntimeException("Mã sản phẩm đã được sử dụng: " + req.getMaSanPham());
            }
            sp.setMaSanPham(req.getMaSanPham().trim());
        }

        sp.setTenSanPham(req.getTenSanPham().trim());
        sp.setMoTa(req.getMoTa());
        if (req.getTrangThai() != null) {
            sp.setTrangThai(req.getTrangThai());
        }
        sp.setNguoiCapNhat(req.getNguoiThaoTac() != null ? req.getNguoiThaoTac() : "Admin");

        setAttributes(sp, req);

        SanPham updated = sanPhamRepository.save(sp);
        return convertToResponse(updated);
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
        if (!sanPhamRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy sản phẩm có ID: " + id);
        }
        sanPhamRepository.deleteById(id);
    }

    private void setAttributes(SanPham sp, SanPhamRequest req) {
        if (req.getIdXuatXu() != null) {
            sp.setIdXuatXu(xuatXuRepository.findById(req.getIdXuatXu()).orElse(null));
        }
        if (req.getIdChatLieu() != null) {
            sp.setIdChatLieu(chatLieuRepository.findById(req.getIdChatLieu()).orElse(null));
        }
        if (req.getIdThuongHieu() != null) {
            sp.setIdThuongHieu(thuongHieuRepository.findById(req.getIdThuongHieu()).orElse(null));
        }
        if (req.getIdDanhMuc() != null) {
            sp.setIdDanhMuc(danhMucRepository.findById(req.getIdDanhMuc()).orElse(null));
        }
        if (req.getIdCoAo() != null) {
            sp.setIdCoAo(coAoRepository.findById(req.getIdCoAo()).orElse(null));
        }
        if (req.getIdTayAo() != null) {
            sp.setIdTayAo(tayAoRepository.findById(req.getIdTayAo()).orElse(null));
        }
        if (req.getIdHoaTiet() != null) {
            sp.setIdHoaTiet(hoaTietRepository.findById(req.getIdHoaTiet()).orElse(null));
        }
    }

    private SanPhamResponse convertToResponse(SanPham sp) {
        BigDecimal minGia = chiTietSanPhamRepository.findMinPriceBySanPhamId(sp.getId());
        BigDecimal maxGia = chiTietSanPhamRepository.findMaxPriceBySanPhamId(sp.getId());
        Integer tongSoLuong = chiTietSanPhamRepository.sumSoLuongBySanPhamId(sp.getId());
        List<ChiTietSanPham> variants = chiTietSanPhamRepository.findByIdSanPham_Id(sp.getId());

        List<HinhAnh> images = hinhAnhRepository.findByIdSanPham_Id(sp.getId());
        String anhDaiDien = !images.isEmpty() ? images.get(0).getDuongDan() : null;

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
                .tongSoLuong(tongSoLuong)
                .soLuongBienThe(variants != null ? variants.size() : 0)
                .anhDaiDien(anhDaiDien)
                .build();
    }
}
