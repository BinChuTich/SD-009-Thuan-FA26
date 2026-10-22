package com.example.sd009thuan.service;

import com.example.sd009thuan.entity.ChatKienThuc;
import com.example.sd009thuan.repository.ChatKienThucRepository;
import org.springframework.stereotype.Service;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.Locale;

@Service
public class ChatKnowledgeService {
    private final ChatKienThucRepository repository;
    public ChatKnowledgeService(ChatKienThucRepository repository) { this.repository = repository; }

    public String answer(String question) {
        String q = normalize(question);
        return repository.findByKichHoatTrue().stream()
            .map(k -> new Match(k, score(q, normalize(k.getTuKhoa() + " " + k.getChuDe()))))
            .filter(m -> m.score > 0)
            .max(Comparator.comparingInt(Match::score))
            .map(m -> m.row.getCauTraLoi())
            .orElse("Mình chưa tìm thấy thông tin phù hợp trong cơ sở dữ liệu. Bạn có thể chọn “Gặp nhân viên” để được hỗ trợ trực tiếp nhé.");
    }
    private int score(String question, String keywords) {
        int score = 0;
        for (String token : keywords.split("[,;| ]+")) {
            if (token.length() > 2 && question.contains(token)) score += token.length() > 5 ? 3 : 1;
        }
        return score;
    }
    private String normalize(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
            .replace('đ','d').replace('Đ','D').toLowerCase(Locale.ROOT);
    }
    private record Match(ChatKienThuc row, int score) {}
}
