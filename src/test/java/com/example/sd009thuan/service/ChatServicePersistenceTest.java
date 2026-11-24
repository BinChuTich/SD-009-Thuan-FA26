package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.chat.GuiTinNhanRequest;
import com.example.sd009thuan.dto.chat.DanhGiaCuocHoiThoaiDTO;
import com.example.sd009thuan.dto.chat.KhoiTaoChatRequest;
import com.example.sd009thuan.repository.HoaDonRepository;
import com.example.sd009thuan.repository.LichSuHoaDonRepository;
import com.example.sd009thuan.repository.NhanVienRepository;
import com.example.sd009thuan.service.impl.ChatServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ChatServicePersistenceTest {

    @TempDir
    Path tempDir;

    @Test
    void restoresConversationsAndMessagesAfterServiceRestart() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        ChatServiceImpl first = createService(mapper);

        KhoiTaoChatRequest init = new KhoiTaoChatRequest();
        init.setKhachHangId(9L);
        init.setTenKhachHang("Khách kiểm thử");
        Long conversationId = first.khoiTaoHoacLayCuocHoiThoai(init).getId();

        GuiTinNhanRequest message = new GuiTinNhanRequest();
        message.setCuocHoiThoaiId(conversationId);
        message.setKhachHangId(9L);
        message.setNguoiGui("KHACH_HANG");
        message.setNoiDung("Tin nhắn phải còn sau khi restart");
        first.guiTinNhan(message);
        first.dongCuocHoiThoai(conversationId);
        first.persistChatHistoryIfChanged();

        ChatServiceImpl restarted = createService(mapper);
        restarted.restoreChatHistory();

        assertEquals(1, restarted.layDanhSachCuocHoiThoai(null, 0).size());
        assertEquals("Tin nhắn phải còn sau khi restart",
                restarted.layDanhSachTinNhan(conversationId, null).get(0).getNoiDung());
    }

    @Test
    void classifiesNegativeRatingAndPersistsHandledState() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        ChatServiceImpl first = createService(mapper);
        KhoiTaoChatRequest init = new KhoiTaoChatRequest();
        init.setKhachHangId(10L);
        init.setTenKhachHang("Khách đánh giá");
        Long conversationId = first.khoiTaoHoacLayCuocHoiThoai(init).getId();

        DanhGiaCuocHoiThoaiDTO input = new DanhGiaCuocHoiThoaiDTO();
        input.setDiemDanhGia(1);
        input.setNoiDungDanhGia("Phản hồi chậm và tư vấn size sai");
        DanhGiaCuocHoiThoaiDTO classified = first.guiDanhGia(conversationId, input);

        assertEquals("TIEU_CUC", classified.getPhanLoai());
        assertTrue(classified.getCanXuLy());
        assertTrue(classified.getChuDe().contains("TOC_DO_PHAN_HOI"));
        assertTrue(classified.getChuDe().contains("TU_VAN_SIZE"));

        first.danhDauDanhGiaDaXuLy(conversationId, "Admin kiểm thử");
        first.persistChatHistoryIfChanged();
        ChatServiceImpl restarted = createService(mapper);
        restarted.restoreChatHistory();

        assertTrue(restarted.layDanhGia(conversationId).getDaXuLy());
        assertEquals("Admin kiểm thử", restarted.layDanhGia(conversationId).getNguoiXuLy());
    }

    @Test
    void sameCustomerCanRateEveryNewConversationOnce() {
        ChatServiceImpl service = createService(new ObjectMapper().findAndRegisterModules());
        KhoiTaoChatRequest init = new KhoiTaoChatRequest();
        init.setKhachHangId(20L);
        init.setTenKhachHang("Khách quay lại");

        Long firstId = service.khoiTaoHoacLayCuocHoiThoai(init).getId();
        service.dongCuocHoiThoai(firstId);
        DanhGiaCuocHoiThoaiDTO firstRating = new DanhGiaCuocHoiThoaiDTO();
        firstRating.setDiemDanhGia(4);
        service.guiDanhGia(firstId, firstRating);

        Long secondId = service.khoiTaoHoacLayCuocHoiThoai(init).getId();
        assertTrue(!firstId.equals(secondId));
        assertEquals(null, service.layDanhGia(secondId));
        DanhGiaCuocHoiThoaiDTO secondRating = new DanhGiaCuocHoiThoaiDTO();
        secondRating.setDiemDanhGia(5);
        service.guiDanhGia(secondId, secondRating);

        assertEquals(4, service.layDanhGia(firstId).getDiemDanhGia());
        assertEquals(5, service.layDanhGia(secondId).getDiemDanhGia());
    }

    @Test
    void automaticallyClosesAfterOneMinuteWithoutCustomerResponse() {
        ChatServiceImpl service = createService(new ObjectMapper().findAndRegisterModules());
        KhoiTaoChatRequest init = new KhoiTaoChatRequest();
        init.setKhachHangId(30L);
        init.setTenKhachHang("Khách không phản hồi");
        var conversation = service.khoiTaoHoacLayCuocHoiThoai(init);
        service.tiepNhanCuocHoiThoai(conversation.getId(), null, "Nhân viên test");
        conversation.setThoiGianKhachPhanHoiCuoi(LocalDateTime.now().minusSeconds(61));

        service.kiemTraTuDongDongPhienKhongPhanHoi();

        assertEquals(0, service.layChiTietCuocHoiThoai(conversation.getId()).getTrangThai());
        assertTrue(service.layDanhSachTinNhan(conversation.getId(), null).stream()
                .anyMatch(m -> m.getNoiDung().contains("không nhận được phản hồi từ bạn trong 1 phút")));
    }

    private ChatServiceImpl createService(ObjectMapper mapper) {
        ChatServiceImpl service = new ChatServiceImpl(
                mock(HoaDonRepository.class), mock(LichSuHoaDonRepository.class),
                mock(NhanVienRepository.class), mapper);
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
        return service;
    }
}
