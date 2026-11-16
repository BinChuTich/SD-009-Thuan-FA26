package com.example.sd009thuan.dto.ai;

import java.math.BigDecimal;

public record AiProductCard(
        Long id,
        Long variantId,
        Long idMauSac,
        Long idKichCo,
        String tenSanPham,
        String tenMauSac,
        String tenKichCo,
        Integer soLuong,
        String anhDaiDien,
        BigDecimal minGia,
        BigDecimal minGiaSauGiam,
        Boolean coGiamGia,
        BigDecimal phanTramGiam,
        String tenDotGiamGia
) {}
