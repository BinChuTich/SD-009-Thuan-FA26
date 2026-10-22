package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.CuocHoiThoai;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CuocHoiThoaiRepository extends JpaRepository<CuocHoiThoai, Long> {
    List<CuocHoiThoai> findAllByOrderByNgayCapNhatDescNgayTaoDesc();
    List<CuocHoiThoai> findByIdKhachHang_IdOrderByNgayTaoDesc(Long customerId);
    List<CuocHoiThoai> findByTrangThaiInOrderByNgayCapNhatDescNgayTaoDesc(List<String> statuses);
}
