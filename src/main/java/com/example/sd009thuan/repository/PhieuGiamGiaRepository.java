package com.example.sd009thuan.repository;


import com.example.sd009thuan.entity.PhieuGiamGia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PhieuGiamGiaRepository extends JpaRepository<PhieuGiamGia, Long> {


    List<PhieuGiamGia> findAllByOrderByIdDesc();


    Page<PhieuGiamGia> findAllByOrderByIdDesc(Pageable pageable);

    @Query(value = "SELECT kh.ten_khach_hang FROM khach_hang_phieu_giam_gia kp " +
            "JOIN khach_hang kh ON kp.id_khach_hang = kh.id " +
            "WHERE kp.id_phieu_giam_gia = :idPhieu", nativeQuery = true)
    List<String> findTenKhachHangByIdPhieu(@Param("idPhieu") Long idPhieu);
}
