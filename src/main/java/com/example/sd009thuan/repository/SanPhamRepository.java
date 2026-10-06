package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.SanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SanPhamRepository extends JpaRepository<SanPham, Long>, JpaSpecificationExecutor<SanPham> {

    boolean existsByMaSanPham(String maSanPham);

    boolean existsByMaSanPhamAndIdNot(String maSanPham, Long id);

    Optional<SanPham> findByMaSanPham(String maSanPham);

    boolean existsByIdChatLieu_Id(Long idChatLieu);

    boolean existsByIdThuongHieu_Id(Long idThuongHieu);

    boolean existsByIdXuatXu_Id(Long idXuatXu);

    boolean existsByIdDanhMuc_Id(Long idDanhMuc);

    boolean existsByIdCoAo_Id(Long idCoAo);

    boolean existsByIdTayAo_Id(Long idTayAo);

    boolean existsByIdHoaTiet_Id(Long idHoaTiet);

    @Query("SELECT sp FROM SanPham sp WHERE " +
           "(:keyword IS NULL OR LOWER(sp.maSanPham) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           " OR LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:idThuongHieu IS NULL OR sp.idThuongHieu.id = :idThuongHieu) " +
           "AND (:idXuatXu IS NULL OR sp.idXuatXu.id = :idXuatXu) " +
           "AND (:idDanhMuc IS NULL OR sp.idDanhMuc.id = :idDanhMuc) " +
           "AND (:idChatLieu IS NULL OR sp.idChatLieu.id = :idChatLieu) " +
           "AND (:idCoAo IS NULL OR sp.idCoAo.id = :idCoAo) " +
           "AND (:idTayAo IS NULL OR sp.idTayAo.id = :idTayAo) " +
           "AND (:idHoaTiet IS NULL OR sp.idHoaTiet.id = :idHoaTiet) " +
           "AND (:trangThai IS NULL OR sp.trangThai = :trangThai)")
    Page<SanPham> searchFilter(
            @Param("keyword") String keyword,
            @Param("idThuongHieu") Long idThuongHieu,
            @Param("idXuatXu") Long idXuatXu,
            @Param("idDanhMuc") Long idDanhMuc,
            @Param("idChatLieu") Long idChatLieu,
            @Param("idCoAo") Long idCoAo,
            @Param("idTayAo") Long idTayAo,
            @Param("idHoaTiet") Long idHoaTiet,
            @Param("trangThai") Integer trangThai,
            Pageable pageable
    );
}
