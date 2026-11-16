package com.example.sd009thuan.dto.thongke;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThongKeFilterRequest {
    private String fromDate;
    private String toDate;
    private String statType; // day, week, month, year
    private Long idThuongHieu;
    private String keyword;
    private Integer trangThaiHoaDon;
}

