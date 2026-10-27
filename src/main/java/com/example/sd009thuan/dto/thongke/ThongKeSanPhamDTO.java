package com.example.sd009thuan.dto.thongke;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThongKeSanPhamDTO {
    private Long id;
    private String code;
    private String name;
    private String brand;
    private String sizes;
    private Long soldCount;
    private Long returnedCount;
    private BigDecimal revenue;
    private Long stock;
    private String image;
    private Integer invoiceCount;
    @Builder.Default
    private List<ThongKeHoaDonItemDTO> invoices = new ArrayList<>();
}

