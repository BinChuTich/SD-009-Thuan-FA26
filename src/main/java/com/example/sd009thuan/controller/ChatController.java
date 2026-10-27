package com.example.sd009thuan.controller;

import com.example.sd009thuan.dto.chat.*;
import com.example.sd009thuan.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    // Khởi tạo hoặc lấy lại cuộc trò chuyện (dùng cho Khách online hoặc Admin)
    @PostMapping("/khoi-tao")
    public ResponseEntity<?> khoiTaoCuocHoiThoai(@RequestBody(required = false) KhoiTaoChatRequest req) {
        try {
            CuocHoiThoaiDTO dto = chatService.khoiTaoHoacLayCuocHoiThoai(req);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Danh sách các cuộc hội thoại cho Admin
    @GetMapping("/cuoc-hoi-thoai")
    public ResponseEntity<?> layDanhSachCuocHoiThoai(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer trangThai
    ) {
        try {
            List<CuocHoiThoaiDTO> list = chatService.layDanhSachCuocHoiThoai(keyword, trangThai);
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Chi tiết một cuộc hội thoại
    @GetMapping("/cuoc-hoi-thoai/{id}")
    public ResponseEntity<?> layChiTietCuocHoiThoai(@PathVariable Long id) {
        try {
            CuocHoiThoaiDTO dto = chatService.layChiTietCuocHoiThoai(id);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Lấy tin nhắn của cuộc trò chuyện (hỗ trợ polling bằng lastId)
    @GetMapping("/cuoc-hoi-thoai/{id}/tin-nhan")
    public ResponseEntity<?> layDanhSachTinNhan(
            @PathVariable Long id,
            @RequestParam(required = false) Long lastId
    ) {
        try {
            List<TinNhanDTO> list = chatService.layDanhSachTinNhan(id, lastId);
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Gửi tin nhắn mới (Khách hoặc Admin)
    @PostMapping("/gui-tin-nhan")
    public ResponseEntity<?> guiTinNhan(@Valid @RequestBody GuiTinNhanRequest req) {
        try {
            TinNhanDTO dto = chatService.guiTinNhan(req);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Đánh dấu đã đọc
    @PutMapping("/cuoc-hoi-thoai/{id}/da-doc")
    public ResponseEntity<?> danhDauDaDoc(
            @PathVariable Long id,
            @RequestParam(defaultValue = "ADMIN") String role
    ) {
        try {
            chatService.danhDauDaDoc(id, role);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Chuông thông báo cho Admin (số lượng tin nhắn chưa đọc & tin nhắn mới nhất)
    @GetMapping("/thong-bao-admin")
    public ResponseEntity<?> layThongBaoAdmin() {
        try {
            ThongBaoAdminDTO dto = chatService.layThongBaoAdmin();
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Đóng cuộc trò chuyện
    @PutMapping("/cuoc-hoi-thoai/{id}/dong")
    public ResponseEntity<?> dongCuocHoiThoai(@PathVariable Long id) {
        try {
            chatService.dongCuocHoiThoai(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Tiếp nhận cuộc trò chuyện (Admin chuyển từ Chờ nhận sang Đang hoạt động)
    @PutMapping("/cuoc-hoi-thoai/{id}/tiep-nhan")
    public ResponseEntity<?> tiepNhanCuocHoiThoai(
            @PathVariable Long id,
            @RequestParam(required = false) Long nhanVienId
    ) {
        try {
            CuocHoiThoaiDTO dto = chatService.tiepNhanCuocHoiThoai(id, nhanVienId);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Khách hàng bấm gặp nhân viên / Chat với người bán (chuyển sang Chờ nhận)
    @PutMapping({"/cuoc-hoi-thoai/{id}/yeu-cau-ho-tro", "/cuoc-hoi-thoai/{id}/yeu-cau-nhan-vien"})
    public ResponseEntity<?> yeuCauHoTro(@PathVariable Long id) {
        try {
            CuocHoiThoaiDTO dto = chatService.yeuCauHoTro(id);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Xóa tất cả các cuộc trò chuyện
    @DeleteMapping({"/cuoc-hoi-thoai/tat-ca", "/xoa-tat-ca"})
    public ResponseEntity<?> xoaTatCaCuocHoiThoai() {
        try {
            chatService.xoaTatCaCuocHoiThoai();
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã dọn sạch toàn bộ cuộc trò chuyện"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Xóa một cuộc trò chuyện
    @DeleteMapping("/cuoc-hoi-thoai/{id}")
    public ResponseEntity<?> xoaCuocHoiThoai(@PathVariable Long id) {
        try {
            chatService.xoaCuocHoiThoai(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã xóa cuộc trò chuyện"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}

