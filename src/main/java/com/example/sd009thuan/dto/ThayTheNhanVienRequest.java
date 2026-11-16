package com.example.sd009thuan.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ThayTheNhanVienRequest {
    @NotNull(message = "Thiếu ID nhân viên cần thay thế")
    private Long nhanVienCuId;

    @NotNull(message = "Vui lòng chọn nhân viên mới thay thế")
    private Long nhanVienMoiId;

    private List<Long> lichIds; // Nếu null hoặc rỗng thì thay thế toàn bộ ca tương lai

    public ThayTheNhanVienRequest() {
        super();
    }
}

