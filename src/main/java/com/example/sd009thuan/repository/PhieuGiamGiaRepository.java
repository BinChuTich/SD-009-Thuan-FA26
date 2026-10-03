package com.example.sd009thuan.repository;


import com.example.sd009thuan.entity.PhieuGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import java.util.List;


@Service
public interface PhieuGiamGiaRepository extends JpaRepository<PhieuGiamGia, Long> {

    // Tìm kiếm phiếu giảm giá sắp xếp theo id giảm dần (mới nhất lên đầu)
    List<PhieuGiamGia> findAllByOrderByIdDesc();

    @Query(value = "SELECT kh.ten_khach_hang FROM khach_hang_phieu_giam_gia kp " +
            "JOIN khach_hang kh ON kp.id_khach_hang = kh.id " +
            "WHERE kp.id_phieu_giam_gia = :idPhieu", nativeQuery = true)
    List<String> findTenKhachHangByIdPhieu(@Param("idPhieu") Long idPhieu);
}
