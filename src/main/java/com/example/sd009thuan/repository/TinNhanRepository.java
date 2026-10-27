package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.TinNhan;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TinNhanRepository extends JpaRepository<TinNhan, Long> {

    List<TinNhan> findByCuocHoiThoai_IdOrderByNgayTaoAsc(Long cuocHoiThoaiId);

    List<TinNhan> findByCuocHoiThoai_IdAndIdGreaterThanOrderByNgayTaoAsc(Long cuocHoiThoaiId, Long lastId);

    Optional<TinNhan> findTopByCuocHoiThoai_IdOrderByNgayTaoDesc(Long cuocHoiThoaiId);

    long countByCuocHoiThoai_IdAndDaDocFalseAndNguoiGui(Long cuocHoiThoaiId, String nguoiGui);

    long countByDaDocFalseAndNguoiGui(String nguoiGui);

    @Modifying
    @Query("UPDATE TinNhan t SET t.daDoc = true WHERE t.cuocHoiThoai.id = :cuocHoiThoaiId AND t.nguoiGui = 'KHACH_HANG' AND t.daDoc = false")
    void danhDauAdminDaDoc(@Param("cuocHoiThoaiId") Long cuocHoiThoaiId);

    @Modifying
    @Query("UPDATE TinNhan t SET t.daDoc = true WHERE t.cuocHoiThoai.id = :cuocHoiThoaiId AND t.nguoiGui = 'NHAN_VIEN' AND t.daDoc = false")
    void danhDauKhachHangDaDoc(@Param("cuocHoiThoaiId") Long cuocHoiThoaiId);

    @Query("SELECT t FROM TinNhan t WHERE t.nguoiGui = 'KHACH_HANG' AND t.daDoc = false ORDER BY t.ngayTao DESC")
    List<TinNhan> findUnreadCustomerMessages(Pageable pageable);

    @Query("SELECT t FROM TinNhan t ORDER BY t.ngayTao DESC")
    List<TinNhan> findLatestMessages(Pageable pageable);
}
