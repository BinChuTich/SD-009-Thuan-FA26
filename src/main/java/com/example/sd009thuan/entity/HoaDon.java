package com.example.sd009thuan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "hoa_don")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class HoaDon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    // Nhân viên tạo hóa đơn
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_nhan_vien")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private NhanVien idNhanVien;

    // Khách hàng
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_khach_hang")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private KhachHang idKhachHang;

    // Phiếu giảm giá
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_phieu_giam_gia")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private PhieuGiamGia idPhieuGiamGia;

    // Mã hóa đơn
    @Size(max = 50)
    @Column(name = "ma_hoa_don", length = 50, unique = true)
    private String maHoaDon;

    /*
     * 1 = Bán tại quầy
     * 2 = Bán online
     */
    @NotNull
    @Column(name = "loai_don", nullable = false)
    private Integer loaiDon;

    // Phí vận chuyển
    @NotNull
    @ColumnDefault("0")
    @Column(
            name = "phi_van_chuyen",
            nullable = false,
            precision = 18,
            scale = 2
    )
    private BigDecimal phiVanChuyen;

    // Tổng tiền trước giảm giá
    @NotNull
    @ColumnDefault("0")
    @Column(
            name = "tong_tien",
            nullable = false,
            precision = 18,
            scale = 2
    )
    private BigDecimal tongTien;

    // Tiền sau khi giảm giá
    @NotNull
    @ColumnDefault("0")
    @Column(
            name = "tien_sau_giam_gia",
            nullable = false,
            precision = 18,
            scale = 2
    )
    private BigDecimal tienSauGiamGia;

    // Tên khách hàng
    @Size(max = 255)
    @Nationalized
    @Column(name = "ten_khach_hang")
    private String tenKhachHang;

    // Số điện thoại khách hàng
    @Size(max = 20)
    @Column(
            name = "so_dien_thoai_khach_hang",
            length = 20
    )
    private String soDienThoaiKhachHang;

    // Địa chỉ nhận hàng
    @Nationalized
    @Lob
    @Column(name = "dia_chi_nhan_hang")
    private String diaChiNhanHang;

    // Ngày tạo
    @NotNull
    @ColumnDefault("getdate()")
    @Column(name = "ngay_tao", nullable = false)
    private Instant ngayTao;

    // Người tạo
    @Size(max = 100)
    @Nationalized
    @Column(name = "nguoi_tao", length = 100)
    private String nguoiTao;

    // Ngày cập nhật
    @Column(name = "ngay_cap_nhat")
    private Instant ngayCapNhat;

    // Người cập nhật
    @Size(max = 100)
    @Nationalized
    @Column(name = "nguoi_cap_nhat", length = 100)
    private String nguoiCapNhat;

    /*
     * Trạng thái đơn hàng
     * 1 = Chờ xử lý
     * 2 = Đã xác nhận
     * 3 = Đang chuẩn bị
     * 4 = Đang giao
     * 5 = Hoàn thành
     * 6 = Đã hủy
     */
    @NotNull
    @ColumnDefault("1")
    @Column(name = "trang_thai", nullable = false)
    private Integer trangThai;

    /*
     * Trạng thái thanh toán
     * 0 = Chưa thanh toán
     * 1 = Đã thanh toán
     * 2 = Thanh toán một phần
     */
    @NotNull
    @ColumnDefault("0")
    @Column(name = "trang_thai_thanh_toan", nullable = false)
    private Integer trangThaiThanhToan;

    // Ghi chú
    @Nationalized
    @Lob
    @Column(name = "ghi_chu")
    private String ghiChu;
    // Chi tiết hóa đơn
    @JsonIgnore
    @OneToMany(mappedBy = "idHoaDon")
    private Set<ChiTietHoaDon> chiTietHoaDons = new LinkedHashSet<>();

    // Hình thức thanh toán
    @JsonIgnore
    @OneToMany(mappedBy = "idHoaDon")
    private Set<HinhThucThanhToan> hinhThucThanhToan = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "idHoaDon")
    private Set<LichSuHoaDon> lichSuHoaDons = new LinkedHashSet<>();

    // Các trường tiện ích lấy trực tiếp từ bảng khóa ngoại
    @JsonProperty("tenNhanVien")
    public String getTenNhanVien() {
        if (idNhanVien != null) {
            if (idNhanVien.getTenNhanVien() != null && !idNhanVien.getTenNhanVien().trim().isEmpty()) {
                return idNhanVien.getTenNhanVien();
            }
            if (idNhanVien.getTenTaiKhoan() != null && !idNhanVien.getTenTaiKhoan().trim().isEmpty()) {
                return idNhanVien.getTenTaiKhoan();
            }
        }
        return null;
    }

    @JsonProperty("maNhanVien")
    public String getMaNhanVien() {
        return idNhanVien != null ? idNhanVien.getMaNhanVien() : null;
    }

    @JsonProperty("tenKhachHangHienThi")
    public String getTenKhachHangHienThi() {
        if (idKhachHang != null && idKhachHang.getTenKhachHang() != null && !idKhachHang.getTenKhachHang().trim().isEmpty()) {
            return idKhachHang.getTenKhachHang();
        }
        if (tenKhachHang != null && !tenKhachHang.trim().isEmpty()) {
            return tenKhachHang;
        }
        return "Khách lẻ";
    }

    @JsonProperty("soDienThoaiHienThi")
    public String getSoDienThoaiHienThi() {
        if (idKhachHang != null && idKhachHang.getSoDienThoai() != null && !idKhachHang.getSoDienThoai().trim().isEmpty()) {
            return idKhachHang.getSoDienThoai();
        }
        return soDienThoaiKhachHang;
    }

    @JsonProperty("tenPhieuGiamGia")
    public String getTenPhieuGiamGia() {
        return idPhieuGiamGia != null ? idPhieuGiamGia.getTenPhieuGiamGia() : null;
    }
}