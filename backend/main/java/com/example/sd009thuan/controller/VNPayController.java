package com.example.sd009thuan.controller;

import com.example.sd009thuan.dto.VNPayDTO;
import com.example.sd009thuan.entity.HoaDon;
import com.example.sd009thuan.repository.HoaDonRepository;
import com.example.sd009thuan.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/vnpay")
@CrossOrigin("*")
public class VNPayController {

    @Autowired
    private VNPayService vnPayService;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @PostMapping("/create-payment")
    public ResponseEntity<?> createPayment(HttpServletRequest request, @RequestBody VNPayDTO.PaymentRequest req) {
        if (req.getOrderCode() == null || req.getOrderCode().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mã đơn hàng không được để trống!"));
        }

        // Tự động kiểm tra và phục hồi số tiền nếu bị thiếu hoặc <= 0 từ cơ sở dữ liệu
        if (req.getAmount() == null || req.getAmount() <= 0) {
            Optional<HoaDon> hdOpt = hoaDonRepository.findByMaHoaDon(req.getOrderCode().trim());
            if (hdOpt.isPresent()) {
                HoaDon hd = hdOpt.get();
                if (hd.getTienSauGiamGia() != null && hd.getTienSauGiamGia().longValue() > 0) {
                    req.setAmount(hd.getTienSauGiamGia().longValue());
                } else if (hd.getTongTien() != null && hd.getTongTien().longValue() > 0) {
                    req.setAmount(hd.getTongTien().longValue());
                }
            }
        }

        // Trường hợp khẩn cấp nếu số tiền vẫn <= 0, lấy hạn mức thanh toán tối thiểu của VNPAY (10.000đ)
        if (req.getAmount() == null || req.getAmount() <= 0) {
            req.setAmount(10000L);
        }

        String paymentUrl = vnPayService.createPaymentUrl(
                request,
                req.getOrderCode().trim(),
                req.getAmount(),
                req.getOrderInfo()
        );

        return ResponseEntity.ok(new VNPayDTO.PaymentResponse("00", "Tạo liên kết thanh toán VNPAY thành công", paymentUrl));
    }

    @GetMapping("/payment-return")
    public ResponseEntity<?> paymentReturn(@RequestParam Map<String, String> queryParams) {
        Map<String, Object> result = vnPayService.verifyPayment(queryParams);
        return ResponseEntity.ok(result);
    }
}
