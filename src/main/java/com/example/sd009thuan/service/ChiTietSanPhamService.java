package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.ChiTietSanPhamRequest;
import com.example.sd009thuan.dto.ChiTietSanPhamResponse;
import com.example.sd009thuan.dto.PageResponse;
import com.example.sd009thuan.entity.ChiTietSanPham;
import com.example.sd009thuan.entity.KichCo;
import com.example.sd009thuan.entity.MauSac;
import com.example.sd009thuan.entity.SanPham;
import com.example.sd009thuan.repository.ChiTietSanPhamRepository;
import com.example.sd009thuan.repository.KichCoRepository;
import com.example.sd009thuan.repository.MauSacRepository;
import com.example.sd009thuan.repository.SanPhamRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ChiTietSanPhamService {

    private final ChiTietSanPhamRepository chiTietSanPhamRepository;
    private final SanPhamRepository sanPhamRepository;
    private final KichCoRepository kichCoRepository;
    private final MauSacRepository mauSacRepository;

    public ChiTietSanPhamService(
            ChiTietSanPhamRepository chiTietSanPhamRepository,
            SanPhamRepository sanPhamRepository,
            KichCoRepository kichCoRepository,
            MauSacRepository mauSacRepository
    ) {
        this.chiTietSanPhamRepository = chiTietSanPhamRepository;
        this.sanPhamRepository = sanPhamRepository;
        this.kichCoRepository = kichCoRepository;
        this.mauSacRepository = mauSacRepository;
    }

    public List<ChiTietSanPhamResponse> getBySanPhamId(Long idSanPham) {
        return chiTietSanPhamRepository.findByIdSanPham_Id(idSanPham).stream()
                .map(this::convertToResponse)
                .toList();
    }

    public PageResponse<ChiTietSanPhamResponse> filterVariants(
            Long idSanPham,
            Long idKichCo,
            Long idMauSac,
            BigDecimal minGia,
            BigDecimal maxGia,
            Integer trangThai,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<ChiTietSanPham> result = chiTietSanPhamRepository.filterVariants(
                idSanPham, idKichCo, idMauSac, minGia, maxGia, trangThai, pageable
        );

        List<ChiTietSanPhamResponse> dtoList = result.getContent().stream()
                .map(this::convertToResponse)
                .toList();

        return PageResponse.<ChiTietSanPhamResponse>builder()
                .content(dtoList)
                .pageNumber(result.getNumber())
                .pageSize(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    public ChiTietSanPhamResponse getById(Long id) {
        ChiTietSanPham ct = chiTietSanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể có ID: " + id));
        return convertToResponse(ct);
    }

    @Transactional
    public ChiTietSanPhamResponse create(ChiTietSanPhamRequest req) {
        SanPham sp = sanPhamRepository.findById(req.getIdSanPham())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm có ID: " + req.getIdSanPham()));
        KichCo kc = kichCoRepository.findById(req.getIdKichCo())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kích cỡ có ID: " + req.getIdKichCo()));
        MauSac ms = mauSacRepository.findById(req.getIdMauSac())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy màu sắc có ID: " + req.getIdMauSac()));

        Optional<ChiTietSanPham> existing = chiTietSanPhamRepository.findByIdSanPham_IdAndIdKichCo_IdAndIdMauSac_Id(
                req.getIdSanPham(), req.getIdKichCo(), req.getIdMauSac()
        );
        if (existing.isPresent()) {
            throw new RuntimeException("Biến thể với kích cỡ " + kc.getTenKichCo() + " và màu sắc " + ms.getTenMauSac() + " đã tồn tại cho sản phẩm này!");
        }

        if (req.getMaChiTietSanPham() == null || req.getMaChiTietSanPham().trim().isEmpty()) {
            req.setMaChiTietSanPham("CTSP" + System.currentTimeMillis());
        } else if (chiTietSanPhamRepository.existsByMaChiTietSanPham(req.getMaChiTietSanPham().trim())) {
            throw new RuntimeException("Mã chi tiết sản phẩm đã tồn tại: " + req.getMaChiTietSanPham());
        }

        ChiTietSanPham ct = new ChiTietSanPham();
        ct.setIdSanPham(sp);
        ct.setIdKichCo(kc);
        ct.setIdMauSac(ms);
        ct.setMaChiTietSanPham(req.getMaChiTietSanPham().trim());
        ct.setSoLuong(req.getSoLuong());
        ct.setGiaBan(req.getGiaBan());
        ct.setTrangThai(req.getTrangThai() != null ? req.getTrangThai() : 1);
        ct.setNguoiTao(req.getNguoiThaoTac() != null ? req.getNguoiThaoTac() : "Admin");

        ChiTietSanPham saved = chiTietSanPhamRepository.save(ct);
        return convertToResponse(saved);
    }

    @Transactional
    public ChiTietSanPhamResponse update(Long id, ChiTietSanPhamRequest req) {
        ChiTietSanPham ct = chiTietSanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể có ID: " + id));

        KichCo kc = kichCoRepository.findById(req.getIdKichCo())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kích cỡ có ID: " + req.getIdKichCo()));
        MauSac ms = mauSacRepository.findById(req.getIdMauSac())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy màu sắc có ID: " + req.getIdMauSac()));

        if (req.getMaChiTietSanPham() != null && !req.getMaChiTietSanPham().trim().isEmpty()) {
            if (chiTietSanPhamRepository.existsByMaChiTietSanPhamAndIdNot(req.getMaChiTietSanPham().trim(), id)) {
                throw new RuntimeException("Mã chi tiết sản phẩm đã được sử dụng: " + req.getMaChiTietSanPham());
            }
            ct.setMaChiTietSanPham(req.getMaChiTietSanPham().trim());
        }

        ct.setIdKichCo(kc);
        ct.setIdMauSac(ms);
        ct.setSoLuong(req.getSoLuong());
        ct.setGiaBan(req.getGiaBan());
        if (req.getTrangThai() != null) {
            ct.setTrangThai(req.getTrangThai());
        }
        ct.setNguoiCapNhat(req.getNguoiThaoTac() != null ? req.getNguoiThaoTac() : "Admin");

        ChiTietSanPham updated = chiTietSanPhamRepository.save(ct);
        return convertToResponse(updated);
    }

    @Transactional
    public void changeStatus(Long id, Integer trangThai) {
        ChiTietSanPham ct = chiTietSanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể có ID: " + id));
        ct.setTrangThai(trangThai);
        chiTietSanPhamRepository.save(ct);
    }

    @Transactional
    public void delete(Long id) {
        if (!chiTietSanPhamRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy biến thể có ID: " + id);
        }
        chiTietSanPhamRepository.deleteById(id);
    }

    private ChiTietSanPhamResponse convertToResponse(ChiTietSanPham ct) {
        return ChiTietSanPhamResponse.builder()
                .id(ct.getId())
                .idSanPham(ct.getIdSanPham() != null ? ct.getIdSanPham().getId() : null)
                .maSanPham(ct.getIdSanPham() != null ? ct.getIdSanPham().getMaSanPham() : null)
                .tenSanPham(ct.getIdSanPham() != null ? ct.getIdSanPham().getTenSanPham() : null)
                .idKichCo(ct.getIdKichCo() != null ? ct.getIdKichCo().getId() : null)
                .tenKichCo(ct.getIdKichCo() != null ? ct.getIdKichCo().getTenKichCo() : null)
                .idMauSac(ct.getIdMauSac() != null ? ct.getIdMauSac().getId() : null)
                .tenMauSac(ct.getIdMauSac() != null ? ct.getIdMauSac().getTenMauSac() : null)
                .maHex(ct.getIdMauSac() != null ? ct.getIdMauSac().getMaHoa() : null)
                .maChiTietSanPham(ct.getMaChiTietSanPham())
                .soLuong(ct.getSoLuong())
                .giaBan(ct.getGiaBan())
                .trangThai(ct.getTrangThai())
                .ngayTao(ct.getNgayTao())
                .build();
    }
}
