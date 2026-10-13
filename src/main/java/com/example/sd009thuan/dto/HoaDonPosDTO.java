package com.example.sd009thuan.dto;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class HoaDonPosDTO {
    private Integer loaiDon; // 1: Tại quầy, 2: Giao hàng
    private String tenKhachHang;
    private String soDienThoaiKhachHang;
    private String diaChiNhanHang;
    private BigDecimal tongTien;
    private BigDecimal tongTienGiamGia;
    private BigDecimal phiShip;
    private Integer trangThai; // 5: Hoàn thành
    private Integer trangThaiThanhToan; // 1: Đã thanh toán
    private String phuongThucThanhToan;
    private String ghiChu;
    private List<ItemChiTietDTO> chiTietList;

    @Getter
    @Setter
    public static class ItemChiTietDTO {
        private Long sanPhamId; // id của SanPhamChiTiet
        private Integer soLuong;
        private BigDecimal giaDuocTinh;
    }
}