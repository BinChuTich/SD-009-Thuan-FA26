package com.example.sd009thuan.controller;

import com.example.sd009thuan.entity.ChiTietSanPham;
import com.example.sd009thuan.repository.ChiTietSanPhamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/pos-sync")
@CrossOrigin(origins = "*")
public class PosSyncController {

    @Autowired(required = false)
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    // Lưu danh sách SseEmitter đang lắng nghe theo sessionId (hoặc invoiceId/code)
    private static final Map<String, List<SseEmitter>> emittersMap = new ConcurrentHashMap<>();

    // Lưu lịch sử các lần quét gần nhất để hỗ trợ fallback polling
    private static final Map<String, List<Map<String, Object>>> scanHistoryMap = new ConcurrentHashMap<>();

    /**
     * 1. Máy tính POS đăng ký SSE để lắng nghe sự kiện quét từ điện thoại
     */
    @GetMapping(value = "/stream/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable String sessionId) {
        // Timeout 30 phút cho mỗi phiên làm việc
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        emittersMap.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(sessionId, emitter));
        emitter.onTimeout(() -> removeEmitter(sessionId, emitter));
        emitter.onError((e) -> removeEmitter(sessionId, emitter));

        // Gửi ngay 1 event ping chào mừng
        try {
            Map<String, Object> welcome = new HashMap<>();
            welcome.put("type", "CONNECTED");
            welcome.put("sessionId", sessionId);
            welcome.put("message", "Kết nối đồng bộ POS thành công");
            welcome.put("timestamp", Instant.now().toString());
            emitter.send(SseEmitter.event().name("ping").data(welcome));
        } catch (IOException ignored) {}

        return emitter;
    }

    private void removeEmitter(String sessionId, SseEmitter emitter) {
        List<SseEmitter> list = emittersMap.get(sessionId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emittersMap.remove(sessionId);
            }
        }
    }

    /**
     * 2. Điện thoại gửi mã Barcode / QR quét được lên server để đồng bộ sang máy tính
     */
    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> scanBarcode(@RequestBody Map<String, Object> payload) {
        String sessionId = payload.get("sessionId") != null ? String.valueOf(payload.get("sessionId")).trim() : "";
        String barcode = payload.get("barcode") != null ? String.valueOf(payload.get("barcode")).trim() : "";
        String deviceName = payload.get("deviceName") != null ? String.valueOf(payload.get("deviceName")).trim() : "Điện thoại";

        Map<String, Object> res = new HashMap<>();
        if (sessionId.isEmpty() || barcode.isEmpty()) {
            res.put("success", false);
            res.put("message", "Thiếu sessionId hoặc mã barcode");
            return ResponseEntity.badRequest().body(res);
        }

        // Tìm thông tin biến thể sản phẩm nếu có
        Map<String, Object> productInfo = new HashMap<>();
        productInfo.put("barcode", barcode);
        boolean foundProduct = false;

        if (chiTietSanPhamRepository != null) {
            Optional<ChiTietSanPham> foundOpt = Optional.empty();

            // Thử tìm theo mã CTSP
            Optional<ChiTietSanPham> byCode = chiTietSanPhamRepository.findAll().stream()
                    .filter(c -> c.getMaChiTietSanPham() != null && c.getMaChiTietSanPham().equalsIgnoreCase(barcode))
                    .findFirst();

            if (byCode.isPresent()) {
                foundOpt = byCode;
            } else {
                // Thử tìm theo ID số
                try {
                    Long idNum = Long.parseLong(barcode);
                    foundOpt = chiTietSanPhamRepository.findById(idNum);
                } catch (Exception ignored) {}
            }

            if (foundOpt.isPresent()) {
                ChiTietSanPham ct = foundOpt.get();
                foundProduct = true;
                productInfo.put("id", ct.getId());
                productInfo.put("variantCode", ct.getMaChiTietSanPham());
                productInfo.put("name", ct.getIdSanPham() != null ? ct.getIdSanPham().getTenSanPham() : "Sản phẩm");
                productInfo.put("color", ct.getIdMauSac() != null ? ct.getIdMauSac().getTenMauSac() : "");
                productInfo.put("size", ct.getIdKichCo() != null ? ct.getIdKichCo().getTenKichCo() : "");
                productInfo.put("price", ct.getGiaBan());
                productInfo.put("stock", ct.getSoLuong());
            }
        }

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("id", UUID.randomUUID().toString());
        eventData.put("type", "BARCODE_SCANNED");
        eventData.put("sessionId", sessionId);
        eventData.put("barcode", barcode);
        eventData.put("deviceName", deviceName);
        eventData.put("timestamp", Instant.now().toString());
        eventData.put("product", productInfo);
        eventData.put("found", foundProduct);

        // Lưu vào scan history (tối đa 30 sự kiện gần nhất)
        List<Map<String, Object>> history = scanHistoryMap.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>());
        history.add(eventData);
        if (history.size() > 30) {
            history.remove(0);
        }

        // Phát sự kiện realtime qua SSE tới máy tính POS
        List<SseEmitter> emitters = emittersMap.get(sessionId);
        int receiverCount = 0;
        if (emitters != null) {
            List<SseEmitter> deadEmitters = new ArrayList<>();
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().name("scan").data(eventData));
                    receiverCount++;
                } catch (Exception e) {
                    deadEmitters.add(emitter);
                }
            }
            emitters.removeAll(deadEmitters);
        }

        res.put("success", true);
        res.put("message", foundProduct ? "Quét thành công: " + productInfo.get("name") : "Đã gửi mã " + barcode + " tới máy POS");
        res.put("receiverCount", receiverCount);
        res.put("product", productInfo);
        return ResponseEntity.ok(res);
    }

    /**
     * 3. Fallback polling dành cho máy tính nếu kết nối SSE bị ngắt hoặc firewall chặn
     */
    @GetMapping("/poll/{sessionId}")
    public ResponseEntity<List<Map<String, Object>>> pollScans(
            @PathVariable String sessionId,
            @RequestParam(required = false, defaultValue = "0") long sinceTimestamp
    ) {
        List<Map<String, Object>> history = scanHistoryMap.getOrDefault(sessionId, Collections.emptyList());
        return ResponseEntity.ok(history);
    }

    /**
     * 4. Lấy danh sách IP LAN cục bộ của máy tính chủ để tạo mã QR mở app trên điện thoại
     */
    @GetMapping("/lan-ip")
    public ResponseEntity<Map<String, Object>> getLanIp() {
        Map<String, Object> res = new HashMap<>();
        List<String> ipList = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) continue;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    String ip = addr.getHostAddress();
                    // Lọc IPv4 thông thường (192.168.x.x, 10.x.x.x, 172.16-31.x.x)
                    if (ip.contains(".") && !ip.startsWith("127.")) {
                        ipList.add(ip);
                    }
                }
            }
        } catch (Exception ignored) {}

        String primaryIp = ipList.stream()
                .filter(ip -> ip.startsWith("192.168."))
                .findFirst()
                .orElse(ipList.isEmpty() ? "localhost" : ipList.get(0));

        res.put("primaryIp", primaryIp);
        res.put("allIps", ipList);
        res.put("frontendPort", 5173);
        res.put("clientPort", 5174);
        return ResponseEntity.ok(res);
    }
}
