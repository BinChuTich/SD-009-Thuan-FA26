package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.chat.*;
import com.example.sd009thuan.entity.CuocHoiThoai;
import com.example.sd009thuan.entity.KhachHang;
import com.example.sd009thuan.entity.NhanVien;
import com.example.sd009thuan.entity.TinNhanChat;
import com.example.sd009thuan.repository.CuocHoiThoaiRepository;
import com.example.sd009thuan.repository.TinNhanChatRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.time.format.DateTimeFormatter;

@Service
public class ChatService {
    private final CuocHoiThoaiRepository conversationRepository;
    private final TinNhanChatRepository messageRepository;
    private final com.example.sd009thuan.repository.KhachHangRepository customerRepository;
    private final com.example.sd009thuan.repository.NhanVienRepository employeeRepository;
    private final ChatAiService chatAiService;
    private final ChatKnowledgeService chatKnowledgeService;

    public ChatService(CuocHoiThoaiRepository conversationRepository,
                       TinNhanChatRepository messageRepository,
                       com.example.sd009thuan.repository.KhachHangRepository customerRepository,
                       com.example.sd009thuan.repository.NhanVienRepository employeeRepository,
                       ChatAiService chatAiService, ChatKnowledgeService chatKnowledgeService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.chatAiService = chatAiService;
        this.chatKnowledgeService = chatKnowledgeService;
    }

    @Transactional
    public ConversationResponse sendKnowledgeMessage(Long id, String text) {
        CuocHoiThoai c = findConversation(id);

        if ("DA_DONG".equals(c.getTrangThai())) {
            throw new IllegalStateException("Phiên chat đã đóng.");
        }

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Nội dung tin nhắn không được để trống."
            );
        }
        saveMessage(c, "KHACH_HANG", text);
        String answer = chatKnowledgeService.answer(text.trim());
        saveMessage(c, "AI", answer);
        c.setCheDo("AI");
        c.setNgayCapNhat(LocalDateTime.now());
        return toResponse(conversationRepository.save(c));
    }


    @Transactional
    public ConversationResponse createConversation(CreateConversationRequest request) {
        KhachHang customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng id=" + request.customerId()));
        CuocHoiThoai c = new CuocHoiThoai();
        c.setIdKhachHang(customer);
        c.setCheDo(normalizeMode(request.mode()));
        c.setTrangThai("MOI");
        c.setNgayTao(LocalDateTime.now());
        c.setNgayCapNhat(LocalDateTime.now());
        return toResponse(conversationRepository.save(c));
    }

    @Transactional
    public List<ConversationResponse> getAdminConversations(String status) {
        List<CuocHoiThoai> rows;
        if (status == null || status.isBlank() || "all".equalsIgnoreCase(status)) {
            rows = conversationRepository.findAllByOrderByNgayCapNhatDescNgayTaoDesc();
        } else if ("active".equalsIgnoreCase(status)) {
            rows = conversationRepository.findByTrangThaiInOrderByNgayCapNhatDescNgayTaoDesc(List.of("MOI", "DANG_XU_LY", "CHO_NHAN_VIEN"));
        } else if ("history".equalsIgnoreCase(status)) {
            rows = conversationRepository.findByTrangThaiInOrderByNgayCapNhatDescNgayTaoDesc(List.of("DA_DONG"));
        } else {
            rows = conversationRepository.findAllByOrderByNgayCapNhatDescNgayTaoDesc();
        }
        return rows.stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<ConversationResponse> getCustomerConversations(Long customerId) {
        if (!customerRepository.existsById(customerId)) throw new IllegalArgumentException("Không tìm thấy khách hàng id=" + customerId);
        return conversationRepository.findByIdKhachHang_IdOrderByNgayTaoDesc(customerId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ConversationResponse getConversation(Long id) { return toResponse(findConversation(id)); }

    @Transactional
    public ConversationResponse sendEmployeeMessage(Long id, Long employeeId, String text) {
        CuocHoiThoai c = findConversation(id);
        if ("DA_DONG".equals(c.getTrangThai())) throw new IllegalStateException("Phiên chat đã đóng, không thể gửi tin nhắn.");
        if (employeeId != null) {
            NhanVien employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên id=" + employeeId));
            c.setIdNhanVien(employee);
        }
        c.setCheDo("NHAN_VIEN");
        c.setTrangThai("DANG_XU_LY");
        saveMessage(c, "NHAN_VIEN", text);
        c.setNgayCapNhat(LocalDateTime.now());
        conversationRepository.save(c);
        return toResponse(c);
    }

    @Transactional
    public ConversationResponse sendCustomerMessage(Long id, String text) {
        CuocHoiThoai c = findConversation(id);
        if ("DA_DONG".equals(c.getTrangThai())) throw new IllegalStateException("Phiên chat đã đóng.");
        saveMessage(c, "KHACH_HANG", text);
        c.setNgayCapNhat(LocalDateTime.now());
        return toResponse(conversationRepository.save(c));
    }

    @Transactional
    public ConversationResponse sendAiMessage(Long id, String text) {
        CuocHoiThoai c = findConversation(id);
        if ("DA_DONG".equals(c.getTrangThai())) throw new IllegalStateException("Phiên chat đã đóng.");
        saveMessage(c, "KHACH_HANG", text);
        String answer = chatAiService.reply(text);
        saveMessage(c, "AI", answer);
        c.setCheDo("AI");
        c.setNgayCapNhat(LocalDateTime.now());
        return toResponse(conversationRepository.save(c));
    }

    @Transactional
    public ConversationResponse requestHuman(Long id) {
        CuocHoiThoai c = findConversation(id);
        if ("DA_DONG".equals(c.getTrangThai())) throw new IllegalStateException("Phiên chat đã đóng.");
        c.setCheDo("NHAN_VIEN");
        c.setTrangThai("CHO_NHAN_VIEN");
        saveMessage(c, "AI", "Đã ghi nhận yêu cầu. Nhân viên tư vấn sẽ hỗ trợ bạn sớm nhất có thể.");
        c.setNgayCapNhat(LocalDateTime.now());
        return toResponse(conversationRepository.save(c));
    }

    @Transactional
    public ConversationResponse closeConversation(Long id) {
        CuocHoiThoai c = findConversation(id);
        c.setTrangThai("DA_DONG");
        c.setNgayCapNhat(LocalDateTime.now());
        return toResponse(conversationRepository.save(c));
    }

    @Transactional
    public ConversationResponse assignEmployee(Long id, Long employeeId) {
        CuocHoiThoai c = findConversation(id);
        NhanVien employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên id=" + employeeId));
        c.setIdNhanVien(employee);
        c.setCheDo("NHAN_VIEN");
        c.setTrangThai("DANG_XU_LY");
        c.setNgayCapNhat(LocalDateTime.now());
        return toResponse(conversationRepository.save(c));
    }

    private CuocHoiThoai findConversation(Long id) {
        return conversationRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc hội thoại id=" + id));
    }

    private void saveMessage(CuocHoiThoai c, String sender, String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Nội dung tin nhắn không được để trống.");
        TinNhanChat m = new TinNhanChat();
        m.setIdCuocHoiThoai(c);
        m.setNguoiGui(sender);
        m.setNoiDung(text.trim());
        m.setNgayGui(LocalDateTime.now());
        messageRepository.save(m);
        c.setNgayCapNhat(LocalDateTime.now());
    }

    private String normalizeMode(String mode) {
        return "NHAN_VIEN".equalsIgnoreCase(mode) ? "NHAN_VIEN" : "AI";
    }

    private ConversationResponse toResponse(CuocHoiThoai c) {
        List<ChatMessageResponse> messages = messageRepository
                .findByIdCuocHoiThoai_IdOrderByNgayGuiAscIdAsc(c.getId())
                .stream().map(m -> new ChatMessageResponse(m.getId(), role(m.getNguoiGui()), m.getNoiDung(), formatTime(m.getNgayGui()), m.getNgayGui())).toList();
        KhachHang customer = c.getIdKhachHang();
        NhanVien employee = c.getIdNhanVien();
        return new ConversationResponse(c.getId(), customer.getId(), customer.getTenKhachHang(), customer.getSoDienThoai(),
                employee == null ? null : employee.getId(), employee == null ? null : employee.getTenNhanVien(),
                c.getCheDo(), uiStatus(c.getTrangThai()), formatTime(c.getNgayCapNhat() == null ? c.getNgayTao() : c.getNgayCapNhat()),
                formatTime(c.getNgayTao()), c.getNgayCapNhat(), messages);
    }

    private String formatTime(LocalDateTime value) {
        if (value == null) return "";
        return value.format(DateTimeFormatter.ofPattern("HH:mm - d/M/yyyy"));
    }

    private String uiStatus(String status) {
        return switch (status == null ? "" : status) {
            case "CHO_NHAN_VIEN" -> "need";
            case "DA_DONG" -> "closed";
            default -> "active";
        };
    }

    private String role(String sender) {
        return switch (sender == null ? "" : sender.toUpperCase(Locale.ROOT)) {
            case "KHACH_HANG" -> "customer";
            case "AI" -> "ai";
            case "NHAN_VIEN" -> "agent";
            default -> "customer";
        };
    }
}
