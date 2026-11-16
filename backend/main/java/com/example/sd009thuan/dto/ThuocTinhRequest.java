package com.example.sd009thuan.dto;

import jakarta.validation.constraints.*;

public class ThuocTinhRequest {

    @Size(max = 50, message = "Mã thuộc tính không được vượt quá 50 ký tự!")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]*$", message = "Mã thuộc tính chỉ được chứa chữ cái, chữ số và các ký tự _ - .")
    private String ma;

    @NotBlank(message = "Tên thuộc tính không được để trống!")
    @Size(min = 1, max = 255, message = "Tên thuộc tính phải từ 1 đến 255 ký tự!")
    private String ten;

    @Pattern(regexp = "^(#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3}))?$", message = "Mã màu HEX không hợp lệ (Ví dụ: #FF0000 hoặc #FFF)!")
    private String maHex;

    @Min(value = 0, message = "Trạng thái chỉ có thể là 0 (Ngừng dùng) hoặc 1 (Đang dùng)!")
    @Max(value = 1, message = "Trạng thái chỉ có thể là 0 (Ngừng dùng) hoặc 1 (Đang dùng)!")
    private Integer trangThai;

    public ThuocTinhRequest() {
    }

    public ThuocTinhRequest(String ma, String ten, String maHex, Integer trangThai) {
        this.ma = ma;
        this.ten = ten;
        this.maHex = maHex;
        this.trangThai = trangThai;
    }

    public String getMa() {
        return ma;
    }

    public void setMa(String ma) {
        this.ma = ma;
    }

    public String getTen() {
        return ten;
    }

    public void setTen(String ten) {
        this.ten = ten;
    }

    public String getMaHex() {
        return maHex;
    }

    public void setMaHex(String maHex) {
        this.maHex = maHex;
    }

    public Integer getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(Integer trangThai) {
        this.trangThai = trangThai;
    }
}
