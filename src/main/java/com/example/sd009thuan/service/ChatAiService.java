package com.example.sd009thuan.service;

import org.springframework.stereotype.Service;

@Service
public class ChatAiService {
    /** Phản hồi mẫu để hoàn thiện luồng lưu tin nhắn. Thay bằng API AI thật khi đã cấu hình nhà cung cấp. */
    public String reply(String question) {
        String q = question == null ? "" : question.toLowerCase();
        if (q.contains("đơn hàng") || q.contains("giao hàng"))
            return "Mình có thể hỗ trợ kiểm tra đơn hàng. Bạn vui lòng cung cấp mã đơn hàng để nhân viên kiểm tra chi tiết nhé.";
        if (q.contains("đổi") || q.contains("trả") || q.contains("size"))
            return "Về đổi trả hoặc đổi size, bạn cho mình biết mã đơn hàng và sản phẩm cần hỗ trợ nhé.";
        return "Cảm ơn bạn đã liên hệ FF T-Shirt Store. Mình đã ghi nhận câu hỏi; bạn có thể cung cấp thêm thông tin để được hỗ trợ chính xác hơn.";
    }
}
