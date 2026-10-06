package com.example.sd009thuan.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ThuocTinhRequest {
    private String ma;
    private String ten;
    private String maHex;
    private Integer trangThai;
}
