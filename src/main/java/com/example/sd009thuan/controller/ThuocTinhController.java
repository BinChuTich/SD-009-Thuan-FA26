package com.example.sd009thuan.controller;

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
}
