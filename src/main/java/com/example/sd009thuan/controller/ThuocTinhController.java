package com.example.sd009thuan.controller;

import com.example.sd009thuan.dto.ThuocTinhRequest;
import com.example.sd009thuan.entity.*;
import com.example.sd009thuan.service.ThuocTinhService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/thuoc-tinh")
@CrossOrigin("*")
public class ThuocTinhController {

    private final ThuocTinhService thuocTinhService;

    public ThuocTinhController(ThuocTinhService thuocTinhService) {
        this.thuocTinhService = thuocTinhService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllThuocTinh() {
        return ResponseEntity.ok(thuocTinhService.getAllThuocTinh());
    }

    // Các endpoint legacy GET danh sách
    @GetMapping("/chat-lieu")
    public ResponseEntity<List<ChatLieu>> getChatLieu() {
        return ResponseEntity.ok(thuocTinhService.getChatLieu());
    }

    @GetMapping("/thuong-hieu")
    public ResponseEntity<List<ThuongHieu>> getThuongHieu() {
        return ResponseEntity.ok(thuocTinhService.getThuongHieu());
    }

    @GetMapping("/xuat-xu")
    public ResponseEntity<List<XuatXu>> getXuatXu() {
        return ResponseEntity.ok(thuocTinhService.getXuatXu());
    }

    @GetMapping("/danh-muc")
    public ResponseEntity<List<DanhMuc>> getDanhMuc() {
        return ResponseEntity.ok(thuocTinhService.getDanhMuc());
    }

    @GetMapping("/co-ao")
    public ResponseEntity<List<CoAo>> getCoAo() {
        return ResponseEntity.ok(thuocTinhService.getCoAo());
    }

    @GetMapping("/tay-ao")
    public ResponseEntity<List<TayAo>> getTayAo() {
        return ResponseEntity.ok(thuocTinhService.getTayAo());
    }

    @GetMapping("/hoa-tiet")
    public ResponseEntity<List<HoaTiet>> getHoaTiet() {
        return ResponseEntity.ok(thuocTinhService.getHoaTiet());
    }

    @GetMapping("/mau-sac")
    public ResponseEntity<List<MauSac>> getMauSac() {
        return ResponseEntity.ok(thuocTinhService.getMauSac());
    }

    @GetMapping("/kich-co")
    public ResponseEntity<List<KichCo>> getKichCo() {
        return ResponseEntity.ok(thuocTinhService.getKichCo());
    }

    // ==========================================
    // CÁC ENDPOINT CRUD ĐỘNG CHO TỪNG THUỘC TÍNH
    // ==========================================

    @GetMapping("/type/{type}")
    public ResponseEntity<List<?>> getByType(@PathVariable String type) {
        return ResponseEntity.ok(thuocTinhService.getByType(type));
    }

    @GetMapping("/type/{type}/{id}")
    public ResponseEntity<?> getById(@PathVariable String type, @PathVariable Long id) {
        return ResponseEntity.ok(thuocTinhService.getById(type, id));
    }

    @PostMapping("/type/{type}")
    public ResponseEntity<?> create(@PathVariable String type, @RequestBody ThuocTinhRequest req) {
        return ResponseEntity.ok(thuocTinhService.create(type, req));
    }

    @PutMapping("/type/{type}/{id}")
    public ResponseEntity<?> update(@PathVariable String type, @PathVariable Long id, @RequestBody ThuocTinhRequest req) {
        return ResponseEntity.ok(thuocTinhService.update(type, id, req));
    }

    @PatchMapping("/type/{type}/{id}/status")
    public ResponseEntity<Void> changeStatus(
            @PathVariable String type,
            @PathVariable Long id,
            @RequestParam Integer trangThai
    ) {
        thuocTinhService.changeStatus(type, id, trangThai);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/type/{type}/{id}")
    public ResponseEntity<Void> delete(@PathVariable String type, @PathVariable Long id) {
        thuocTinhService.delete(type, id);
        return ResponseEntity.ok().build();
    }
}
