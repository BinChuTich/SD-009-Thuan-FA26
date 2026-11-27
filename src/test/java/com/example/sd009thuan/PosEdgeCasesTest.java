package com.example.sd009thuan;

import com.example.sd009thuan.dto.HoaDonPosDTO;
import com.example.sd009thuan.entity.*;
import com.example.sd009thuan.repository.*;
import com.example.sd009thuan.service.DotGiamGiaService;
import com.example.sd009thuan.service.HoaDonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

class PosEdgeCasesTest {

    private HoaDonRepository hoaDonRepository;
    private ChiTietHoaDonRepository chiTietHoaDonRepository;
    private LichSuHoaDonRepository lichSuHoaDonRepository;
    private HinhThucThanhToanRepository hinhThucThanhToanRepository;
    private ChiTietSanPhamRepository chiTietSanPhamRepository;
    private KhachHangRepository khachHangRepository;
    private PhuongThucThanhToanRepository phuongThucThanhToanRepository;
    private PhieuGiamGiaRepository phieuGiamGiaRepository;
    private NhanVienRepository nhanVienRepository;
    private HinhAnhRepository hinhAnhRepository;
    private DotGiamGiaService dotGiamGiaService;

    private HoaDonService hoaDonService;

    @BeforeEach
    void setUp() {
        hoaDonRepository = Mockito.mock(HoaDonRepository.class);
        chiTietHoaDonRepository = Mockito.mock(ChiTietHoaDonRepository.class);
        lichSuHoaDonRepository = Mockito.mock(LichSuHoaDonRepository.class);
        hinhThucThanhToanRepository = Mockito.mock(HinhThucThanhToanRepository.class);
        chiTietSanPhamRepository = Mockito.mock(ChiTietSanPhamRepository.class);
        khachHangRepository = Mockito.mock(KhachHangRepository.class);
        phuongThucThanhToanRepository = Mockito.mock(PhuongThucThanhToanRepository.class);
        phieuGiamGiaRepository = Mockito.mock(PhieuGiamGiaRepository.class);
        nhanVienRepository = Mockito.mock(NhanVienRepository.class);
        hinhAnhRepository = Mockito.mock(HinhAnhRepository.class);
        dotGiamGiaService = Mockito.mock(DotGiamGiaService.class);
        when(chiTietSanPhamRepository.deductStockIfAvailable(any(Long.class), anyInt())).thenReturn(1);

        hoaDonService = new HoaDonService(
                hoaDonRepository,
                chiTietHoaDonRepository,
                lichSuHoaDonRepository,
                hinhThucThanhToanRepository,
                chiTietSanPhamRepository,
                khachHangRepository,
                phuongThucThanhToanRepository,
                phieuGiamGiaRepository,
                nhanVienRepository,
                hinhAnhRepository
        );

        // Inject DotGiamGiaService bằng reflection
        try {
            var field = HoaDonService.class.getDeclaredField("dotGiamGiaService");
            field.setAccessible(true);
            field.set(hoaDonService, dotGiamGiaService);
        } catch (Exception e) {
            fail("Không thể inject dotGiamGiaService: " + e.getMessage());
        }
    }

    // =========================================================================
    // CASE 1 & CASE 4: ĐỔI GIÁ SẢN PHẨM & ĐỢT GIẢM GIÁ THAY ĐỔI
    // =========================================================================

    @Test
    @DisplayName("Case 1: Giá niêm yết thay đổi - Hệ thống phát hiện chênh lệch và yêu cầu xác nhận")
    void testCase1_GiaNiemYetThayDoi_PhatHienChenhLech() {
        ChiTietSanPham ctsp = new ChiTietSanPham();
        ctsp.setId(10L);
        ctsp.setGiaBan(BigDecimal.valueOf(150000)); // Giá niêm yết mới trong DB là 150.000đ

        SanPham sp = new SanPham();
        sp.setTenSanPham("Áo Phông Polo Basic");
        ctsp.setIdSanPham(sp);

        when(chiTietSanPhamRepository.findById(10L)).thenReturn(Optional.of(ctsp));
        when(dotGiamGiaService.getActiveVariantMap(null)).thenReturn(new HashMap<>());

        // Dữ liệu giỏ hàng trên máy POS lúc khách chọn có giá cũ 120.000đ
        HoaDonPosDTO.ItemChiTietDTO item = new HoaDonPosDTO.ItemChiTietDTO();
        item.setIdChiTietSanPham(10L);
        item.setDonGia(BigDecimal.valueOf(120000));
        item.setSoLuong(2);

        HoaDonPosDTO dto = new HoaDonPosDTO();
        dto.setChiTietList(List.of(item));

        Map<String, Object> checkResult = hoaDonService.kiemTraHopLeDonHangPos(dto);

        // Kết quả mong đợi: thayDoiGia = true, hiển thị rõ chênh lệch 30.000đ/sp
        assertTrue((Boolean) checkResult.get("thayDoiGia"));
        List<Map<String, Object>> changedItems = (List<Map<String, Object>>) checkResult.get("danhSachThayDoiGia");
        assertEquals(1, changedItems.size());
        assertEquals(BigDecimal.valueOf(120000), changedItems.get(0).get("giaCu"));
        assertEquals(BigDecimal.valueOf(150000), changedItems.get(0).get("giaMoi"));
        assertEquals(BigDecimal.valueOf(30000), changedItems.get(0).get("chenhLech"));
    }

    @Test
    @DisplayName("Case 4: Khuyến mãi đợt giảm giá kết thúc - Tính lại giá gốc và yêu cầu xác nhận")
    void testCase4_KhuyenMaiKetThuc_GiaThayDoi() {
        ChiTietSanPham ctsp = new ChiTietSanPham();
        ctsp.setId(20L);
        ctsp.setGiaBan(BigDecimal.valueOf(200000)); // Giá gốc

        SanPham sp = new SanPham();
        sp.setTenSanPham("Áo Phông Oversize Streetwear");
        ctsp.setIdSanPham(sp);

        when(chiTietSanPhamRepository.findById(20L)).thenReturn(Optional.of(ctsp));

        // Khuyến mãi trước đó giảm 20% -> giá lúc add giỏ là 160.000đ
        // Nhưng hiện tại đợt giảm giá đã hết hạn -> getActiveVariantMap không còn biến thể 20L
        when(dotGiamGiaService.getActiveVariantMap(null)).thenReturn(new HashMap<>());

        HoaDonPosDTO.ItemChiTietDTO item = new HoaDonPosDTO.ItemChiTietDTO();
        item.setIdChiTietSanPham(20L);
        item.setDonGia(BigDecimal.valueOf(160000)); // Giá khuyến mãi cũ
        item.setSoLuong(1);

        HoaDonPosDTO dto = new HoaDonPosDTO();
        dto.setChiTietList(List.of(item));

        Map<String, Object> checkResult = hoaDonService.kiemTraHopLeDonHangPos(dto);

        assertTrue((Boolean) checkResult.get("thayDoiGia"));
        List<Map<String, Object>> changedItems = (List<Map<String, Object>>) checkResult.get("danhSachThayDoiGia");
        assertEquals(1, changedItems.size());
        assertEquals(BigDecimal.valueOf(160000), changedItems.get(0).get("giaCu"));
        assertEquals(BigDecimal.valueOf(200000), changedItems.get(0).get("giaMoi"));
    }

    @Test
    @DisplayName("Case 1 & 4: Khi thanh toán trực tiếp mà giá bị đổi nhưng chưa xác nhận (chapNhanGiaMoi = false) -> Bị chặn 409 CONFLICT")
    void testCase1_ThanhToanGiaDoi_ChuaXacNhan_ThrowConflict() {
        ChiTietSanPham ctsp = new ChiTietSanPham();
        ctsp.setId(10L);
        ctsp.setGiaBan(BigDecimal.valueOf(150000)); // Giá thực tế trong DB
        ctsp.setSoLuong(10);
        when(chiTietSanPhamRepository.findById(10L)).thenReturn(Optional.of(ctsp));
        when(dotGiamGiaService.getActiveVariantMap(null)).thenReturn(new HashMap<>());

        HoaDonPosDTO dto = new HoaDonPosDTO();
        dto.setMaHoaDon("HD_TEST_01");
        dto.setChapNhanGiaMoi(false); // Chưa xác nhận giá mới

        HoaDonPosDTO.ItemChiTietDTO item = new HoaDonPosDTO.ItemChiTietDTO();
        item.setIdChiTietSanPham(10L);
        item.setDonGia(BigDecimal.valueOf(100000)); // Giá cũ trong giỏ
        item.setSoLuong(1);
        dto.setChiTietList(List.of(item));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            hoaDonService.createHoaDonPos(dto);
        });

        assertEquals(409, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("hoặc khuyến mãi đã thay đổi"));
    }

    // =========================================================================
    // CASE 2: SỬA VOUCHER / VOUCHER HẾT HẠN / ĐỔI ĐIỀU KIỆN
    // =========================================================================

    @Test
    @DisplayName("Case 2: Voucher hết hạn trong lúc đang thao tác thanh toán -> Báo không hợp lệ")
    void testCase2_VoucherHetHan_BaoLoi() {
        PhieuGiamGia voucher = new PhieuGiamGia();
        voucher.setId(100L);
        voucher.setMaPhieuGiamGia("VOUCHER_EXPIRED");
        voucher.setTrangThai(1);
        voucher.setSoLuongSuDung(10);
        voucher.setNgayBatDau(Instant.now().minus(5, ChronoUnit.DAYS));
        voucher.setNgayKetThuc(Instant.now().minus(1, ChronoUnit.HOURS)); // Đã hết hạn 1 tiếng trước

        when(phieuGiamGiaRepository.findById(100L)).thenReturn(Optional.of(voucher));

        Map<String, Object> result = hoaDonService.validateAndCalculateVoucher(100L, null, BigDecimal.valueOf(500000));

        assertFalse((Boolean) result.get("hopLe"));
        assertTrue(result.get("lyDo").toString().contains("đã hết hạn sử dụng"));
    }

    @Test
    @DisplayName("Case 2: Voucher bị sửa điều kiện đơn hàng tối thiểu -> Không đủ điều kiện")
    void testCase2_VoucherSuaHoaDonToiThieu_KhongDuDieuKien() {
        PhieuGiamGia voucher = new PhieuGiamGia();
        voucher.setId(101L);
        voucher.setMaPhieuGiamGia("VOUCHER_MIN");
        voucher.setTrangThai(1);
        voucher.setSoLuongSuDung(10);
        voucher.setNgayBatDau(Instant.now().minus(1, ChronoUnit.DAYS));
        voucher.setNgayKetThuc(Instant.now().plus(1, ChronoUnit.DAYS));
        voucher.setHoaDonToiThieu(BigDecimal.valueOf(500000)); // Quản trị viên vừa nâng mức tối thiểu lên 500.000đ

        when(phieuGiamGiaRepository.findById(101L)).thenReturn(Optional.of(voucher));

        // Đơn hàng hiện tại chỉ có 300.000đ
        Map<String, Object> result = hoaDonService.validateAndCalculateVoucher(101L, null, BigDecimal.valueOf(300000));

        assertFalse((Boolean) result.get("hopLe"));
        assertTrue(result.get("lyDo").toString().contains("chưa đạt giá trị tối thiểu"));
    }

    @Test
    @DisplayName("Case 2: Voucher hợp lệ giảm 10% tối đa 50.000đ -> Tính chuẩn xác")
    void testCase2_VoucherHopLe_TinhDungMucGiam() {
        PhieuGiamGia voucher = new PhieuGiamGia();
        voucher.setId(102L);
        voucher.setMaPhieuGiamGia("DISCOUNT10");
        voucher.setTrangThai(1);
        voucher.setSoLuongSuDung(10);
        voucher.setLoaiPhieuGiamGia(1); // Giảm 10%
        voucher.setGiaTriGiamGia(BigDecimal.valueOf(10));
        voucher.setGiamToiDa(BigDecimal.valueOf(50000)); // Giảm tối đa 50.000đ
        voucher.setHoaDonToiThieu(BigDecimal.valueOf(200000));
        voucher.setNgayBatDau(Instant.now().minus(1, ChronoUnit.DAYS));
        voucher.setNgayKetThuc(Instant.now().plus(1, ChronoUnit.DAYS));

        when(phieuGiamGiaRepository.findById(102L)).thenReturn(Optional.of(voucher));

        // Đơn hàng 800.000đ -> 10% = 80.000đ -> chặn tối đa 50.000đ
        Map<String, Object> result = hoaDonService.validateAndCalculateVoucher(102L, null, BigDecimal.valueOf(800000));

        assertTrue((Boolean) result.get("hopLe"));
        assertEquals(BigDecimal.valueOf(50000), result.get("giamGia"));
    }

    // =========================================================================
    // CASE 3: CHUYỂN QUYỀN / KHÓA TÀI KHOẢN NHÂN VIÊN & BÀN GIAO HÓA ĐƠN
    // =========================================================================

    @Test
    @DisplayName("Case 3: Nhân viên bị khóa tài khoản (trangThai = 0) -> Chặn thao tác POS")
    void testCase3_NhanVienBiKhoa_ChanThaoTac() {
        NhanVien nv = new NhanVien();
        nv.setId(99L);
        nv.setTenNhanVien("Nguyễn Văn A");
        nv.setTrangThai(0); // Tài khoản bị khóa hoặc nghỉ việc

        when(nhanVienRepository.findById(99L)).thenReturn(Optional.of(nv));

        Map<String, Object> result = hoaDonService.validateNhanVienPos(99L);

        assertFalse((Boolean) result.get("hopLe"));
        assertTrue(result.get("lyDo").toString().contains("đã bị khóa"));
    }

    @Test
    @DisplayName("Case 3: Bàn giao hóa đơn chờ cho nhân viên đang hoạt động -> Thành công, không mất giỏ hàng")
    void testCase3_BanGiaoHoaDon_ThanhCong() {
        HoaDon hd = new HoaDon();
        hd.setId(500L);
        hd.setMaHoaDon("HD_CHO_01");
        hd.setTrangThai(1);
        hd.setTrangThaiThanhToan(0); // Đang chờ thanh toán

        NhanVien nvMoi = new NhanVien();
        nvMoi.setId(88L);
        nvMoi.setMaNhanVien("NV088");
        nvMoi.setTenNhanVien("Trần Thị B");
        nvMoi.setTrangThai(1); // Đang hoạt động

        when(hoaDonRepository.findByMaHoaDon("HD_CHO_01")).thenReturn(Optional.of(hd));
        when(nhanVienRepository.findById(88L)).thenReturn(Optional.of(nvMoi));
        when(hoaDonRepository.save(any(HoaDon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> body = new HashMap<>();
        body.put("maHoaDon", "HD_CHO_01");
        body.put("idNhanVienMoi", 88L);
        body.put("ghiChu", "Bàn giao ca chiều");

        Map<String, Object> res = hoaDonService.banGiaoHoaDonPos(body);

        assertTrue((Boolean) res.get("success"));
        assertEquals("Trần Thị B", res.get("tenNhanVienMoi"));
        assertEquals(nvMoi, hd.getIdNhanVien());
    }

    @Test
    @DisplayName("Case 3 Idempotency: Ngăn bàn giao hóa đơn đã hoàn tất thanh toán (trangThaiThanhToan = 1) -> Báo lỗi")
    void testCase3_NganBanGiaoHoaDonDaThanhToan() {
        HoaDon hd = new HoaDon();
        hd.setId(501L);
        hd.setMaHoaDon("HD_DA_THANH_TOAN");
        hd.setTrangThai(5);
        hd.setTrangThaiThanhToan(1); // Đã thanh toán xong

        when(hoaDonRepository.findByMaHoaDon("HD_DA_THANH_TOAN")).thenReturn(Optional.of(hd));

        Map<String, Object> body = new HashMap<>();
        body.put("maHoaDon", "HD_DA_THANH_TOAN");
        body.put("idNhanVienMoi", 88L);

        assertThrows(IllegalStateException.class, () -> {
            hoaDonService.banGiaoHoaDonPos(body);
        });
    }
}

