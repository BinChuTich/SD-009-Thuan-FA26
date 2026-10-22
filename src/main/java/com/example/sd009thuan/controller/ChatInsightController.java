package com.example.sd009thuan.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/chat/insights")
public class ChatInsightController {

    private final JdbcTemplate jdbc;

    public ChatInsightController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Đợt giảm giá đang hiệu lực */
    @GetMapping("/promotions")
    public List<Map<String, Object>> promotions() {
        try {
            return jdbc.queryForList("""
                SELECT id,
                       ten_dot_giam_gia AS name,
                       phan_tram_giam AS value,
                       ngay_bat_dau AS startDate,
                       ngay_ket_thuc AS endDate
                FROM dot_giam_gia
                WHERE trang_thai = 1
                  AND (ngay_bat_dau IS NULL OR ngay_bat_dau <= GETDATE())
                  AND (ngay_ket_thuc IS NULL OR ngay_ket_thuc >= GETDATE())
                ORDER BY ngay_bat_dau DESC
                """);
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /** 5 sản phẩm mới nhất */
    @GetMapping("/newest-products")
    public List<Map<String, Object>> newestProducts() {
        try {
            return jdbc.queryForList("""
                SELECT TOP 5
                       sp.id,
                       sp.ten_san_pham AS name,
                       sp.ngay_tao AS createdAt,
                       MIN(ct.gia_ban) AS price
                FROM san_pham sp
                LEFT JOIN chi_tiet_san_pham ct ON ct.id_san_pham = sp.id AND ct.trang_thai = 1
                WHERE sp.trang_thai = 1
                GROUP BY sp.id, sp.ten_san_pham, sp.ngay_tao
                ORDER BY sp.ngay_tao DESC
                """);
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /** 5 sản phẩm bán chạy nhất */
    @GetMapping("/best-sellers")
    public List<Map<String, Object>> bestSellers() {
        try {
            return jdbc.queryForList("""
                SELECT TOP 5
                       sp.id,
                       sp.ten_san_pham AS name,
                       COALESCE(SUM(cthd.so_luong), 0) AS sold,
                       MIN(ct.gia_ban) AS price
                FROM san_pham sp
                LEFT JOIN chi_tiet_san_pham ct ON ct.id_san_pham = sp.id
                LEFT JOIN chi_tiet_hoa_don cthd ON cthd.id_chi_tiet_san_pham = ct.id
                WHERE sp.trang_thai = 1
                GROUP BY sp.id, sp.ten_san_pham
                ORDER BY sold DESC
                """);
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /** Sản phẩm tồn kho < 10 */
    @GetMapping("/low-stock")
    public List<Map<String, Object>> lowStock() {
        try {
            return jdbc.queryForList("""
                SELECT TOP 20
                       sp.id,
                       sp.ten_san_pham AS name,
                       SUM(ct.so_luong) AS stock,
                       MIN(ct.gia_ban) AS price
                FROM san_pham sp
                INNER JOIN chi_tiet_san_pham ct ON ct.id_san_pham = sp.id AND ct.trang_thai = 1
                WHERE sp.trang_thai = 1
                GROUP BY sp.id, sp.ten_san_pham
                HAVING SUM(ct.so_luong) < 10
                ORDER BY stock ASC
                """);
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /** Doanh thu hôm nay */
    @GetMapping("/revenue-today")
    public Map<String, Object> revenueToday() {
        try {
            Double total = jdbc.queryForObject("""
                SELECT COALESCE(SUM(tong_tien), 0)
                FROM hoa_don
                WHERE CAST(ngay_tao AS DATE) = CAST(GETDATE() AS DATE)
                  AND trang_thai BETWEEN 1 AND 6
                """, Double.class);
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("total", total == null ? 0 : total);
            res.put("label", "Doanh thu hôm nay");
            res.put("statsUrl", "/thong-ke");
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("total", 0);
            res.put("label", "Doanh thu hôm nay");
            res.put("statsUrl", "/thong-ke");
            return res;
        }
    }
}