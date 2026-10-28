package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.chat.*;

import java.util.List;

public interface ChatService {
    CuocHoiThoaiDTO khoiTaoHoacLayCuocHoiThoai(KhoiTaoChatRequest req);

    List<CuocHoiThoaiDTO> layDanhSachCuocHoiThoai(String keyword, Integer trangThai);

    CuocHoiThoaiDTO layChiTietCuocHoiThoai(Long id);

    List<TinNhanDTO> layDanhSachTinNhan(Long cuocHoiThoaiId, Long lastId);

    TinNhanDTO guiTinNhan(GuiTinNhanRequest req);

    void danhDauDaDoc(Long cuocHoiThoaiId, String role);

    ThongBaoAdminDTO layThongBaoAdmin();

    void dongCuocHoiThoai(Long id);

    CuocHoiThoaiDTO tiepNhanCuocHoiThoai(Long id, Long nhanVienId);

    CuocHoiThoaiDTO tiepNhanCuocHoiThoai(Long id, Long nhanVienId, String tenNhanVien);

    CuocHoiThoaiDTO yeuCauHoTro(Long id);

    void xoaCuocHoiThoai(Long id);

    void xoaTatCaCuocHoiThoai();
}

