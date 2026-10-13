package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.KhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, Long>, JpaSpecificationExecutor<KhachHang> {
    List<KhachHang> findAllByOrderByTenKhachHangAsc();

    boolean existsByMaKhachHangIgnoreCase(String maKhachHang);
    boolean existsByMaKhachHangIgnoreCaseAndIdNot(String maKhachHang, Long id);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    Optional<KhachHang> findTopByOrderByIdDesc();
    Optional<KhachHang> findByMaKhachHang(String maKhachHang);
    Optional<KhachHang> findByTaiKhoan(String taiKhoan);
    Optional<KhachHang> findByEmail(String email);

    @Query(value = "SELECT kh.id, kh.ma_khach_hang, kh.ten_khach_hang, kh.so_dien_thoai, kh.email, " +
            "kh.ngay_sinh, kh.gioi_tinh, kh.trang_thai, " +
            "COUNT(hd.id) AS tong_don_mua, MAX(hd.ngay_tao) AS ngay_mua_gan_nhat " +
            "FROM khach_hang kh " +
            "LEFT JOIN hoa_don hd ON hd.id_khach_hang = kh.id " +
            "GROUP BY kh.id, kh.ma_khach_hang, kh.ten_khach_hang, kh.so_dien_thoai, kh.email, kh.ngay_sinh, kh.gioi_tinh, kh.trang_thai " +
            "ORDER BY kh.ten_khach_hang ASC", nativeQuery = true)
    List<Object[]> findKhachHangChiTietMuaHang();
}

