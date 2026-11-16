package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.ChiTietSanPhamResponse;
import com.example.sd009thuan.dto.ai.AiChatResponse;
import com.example.sd009thuan.dto.ai.AiChatTurn;
import com.example.sd009thuan.dto.ai.AiProductCard;
import com.example.sd009thuan.dto.ShippingConfigDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiShoppingAssistantService {
    private static final Logger log = LoggerFactory.getLogger(AiShoppingAssistantService.class);
    private static final Pattern SIZE_PATTERN = Pattern.compile("(?:size|cỡ|co)\\s*(xxl|xl|l|m|s)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(triệu|trieu|tr|k|nghìn|nghin|đ|d)?", Pattern.CASE_INSENSITIVE);
    private static final List<String> COLORS = List.of("xanh navy", "xanh dương", "xanh lá", "xanh", "đen", "trắng", "đỏ", "vàng", "nâu", "be", "xám", "hồng", "tím", "cam");
    private static final List<String> GENERIC_KEYWORDS = List.of(
            "ao", "ao thun", "t shirt", "tshirt", "san pham", "hang", "mau", "size", "kich co");

    private final ChiTietSanPhamService variantService;
    private final ObjectMapper objectMapper;
    private final ShippingConfigService shippingConfigService;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public AiShoppingAssistantService(
            ChiTietSanPhamService variantService,
            ShippingConfigService shippingConfigService,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder,
            @Value("${ai.openai.api-key:}") String apiKey,
            @Value("${ai.openai.model:gpt-6.1-sol}") String model) {
        this.variantService = variantService;
        this.shippingConfigService = shippingConfigService;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.baseUrl("https://api.openai.com/v1").build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
    }

    public AiChatResponse reply(String rawMessage) {
        return reply(rawMessage, List.of());
    }

    public AiChatResponse reply(String rawMessage, List<AiChatTurn> history) {
        String message = rawMessage == null ? "" : rawMessage.trim();
        SearchIntent intent = null;
        boolean aiPowered = false;
        List<ChiTietSanPhamResponse> variants = variantService.getAvailableVariantsForAssistant();

        if (!apiKey.isBlank()) {
            try {
                intent = understandWithOpenAi(message, history, buildPublicDataContext(variants));
                aiPowered = true;
            } catch (Exception ex) {
                log.warn("OpenAI unavailable, using local product intent parser: {}", ex.getMessage());
            }
        }
        SearchIntent localIntent = understandLocally(message);
        // Các thuộc tính khách gõ rõ ràng (màu/size/giá) đáng tin cậy hơn suy đoán của AI.
        // Đồng thời loại keyword chung như "sản phẩm" vì chúng không phải tên sản phẩm trong DB.
        intent = reconcileIntent(intent, localIntent);

        List<AiProductCard> products = searchProducts(intent, variants);
        String responseText = buildReply(intent, products);
        return new AiChatResponse(responseText, products, aiPowered);
    }

    private SearchIntent understandWithOpenAi(String message, List<AiChatTurn> history, String dataContext) throws Exception {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "intent", Map.of("type", "string", "enum", List.of("SEARCH_PRODUCT", "GREETING", "SIZE_ADVICE", "SHIPPING", "STORE_INFO", "OTHER")),
                        "keyword", nullableString(),
                        "color", nullableString(),
                        "size", nullableString(),
                        "minPrice", nullableNumber(),
                        "maxPrice", nullableNumber(),
                        "answer", nullableString()),
                "required", List.of("intent", "keyword", "color", "size", "minPrice", "maxPrice", "answer"));

        String developerPrompt = "Bạn là bộ phân tích nhu cầu mua áo của FF T-Shirt. " +
                "Chỉ tách ý định và bộ lọc từ câu khách hàng, không tự tạo sản phẩm. " +
                "Chuẩn hóa size thành S/M/L/XL/XXL. Giá dùng VND. " +
                "Ví dụ 'dưới 500k' => maxPrice 500000; 'từ 300k' => minPrice 300000. " +
                "Bạn được cung cấp DATA CÔNG KHAI lấy trực tiếp từ hệ thống. Chỉ trả lời từ DATA, không đoán hay bịa. " +
                "Nếu là tìm/mua/so sánh sản phẩm, điền bộ lọc và đặt answer=null để backend kiểm tra tồn kho. " +
                "Với câu hỏi khác có thể trả lời chắc chắn từ DATA, viết answer ngắn gọn bằng tiếng Việt. " +
                "Nếu DATA không đủ, nói rõ chưa có thông tin và gợi ý gặp nhân viên.\n\n" + dataContext;

        List<Map<String, Object>> input = new ArrayList<>();
        input.add(Map.of("role", "developer", "content", List.of(Map.of("type", "input_text", "text", developerPrompt))));
        if (history != null) {
            history.stream().skip(Math.max(0, history.size() - 8L)).forEach(turn -> {
                if (turn != null && turn.getRole() != null && turn.getContent() != null) {
                    String role = "assistant".equals(turn.getRole()) ? "assistant" : "user";
                    input.add(Map.of("role", role, "content", List.of(Map.of("type", "input_text", "text", turn.getContent()))));
                }
            });
        }
        input.add(Map.of("role", "user", "content", List.of(Map.of("type", "input_text", "text", message))));

        Map<String, Object> body = Map.of(
                "model", model,
                "store", false,
                "input", input,
                "text", Map.of("format", Map.of(
                        "type", "json_schema", "name", "shopping_intent", "strict", true, "schema", schema)));

        JsonNode response = restClient.post()
                .uri("/responses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) throw new IllegalStateException("OpenAI returned an empty response");
        for (JsonNode output : response.path("output")) {
            for (JsonNode content : output.path("content")) {
                if ("output_text".equals(content.path("type").asText())) {
                    JsonNode parsed = objectMapper.readTree(content.path("text").asText());
                    return new SearchIntent(
                            parsed.path("intent").asText("OTHER"),
                            nullableText(parsed, "keyword"), nullableText(parsed, "color"), nullableText(parsed, "size"),
                            nullableDecimal(parsed, "minPrice"), nullableDecimal(parsed, "maxPrice"), nullableText(parsed, "answer"));
                }
            }
        }
        throw new IllegalStateException("OpenAI response did not contain structured output");
    }

    private SearchIntent understandLocally(String message) {
        String normalized = normalize(message);
        if (normalized.matches("^(xin chao|chao|hi|hello|alo).*")) {
            return new SearchIntent("GREETING", null, null, null, null, null, null);
        }
        if (normalized.matches(".*(phi ship|giao hang|van chuyen|freeship|mien phi ship|bao lau nhan).*")) {
            return new SearchIntent("SHIPPING", null, null, null, null, null, null);
        }
        String color = COLORS.stream().filter(c -> normalized.contains(normalize(c))).findFirst().orElse(null);
        Matcher sizeMatcher = SIZE_PATTERN.matcher(message);
        String size = sizeMatcher.find() ? sizeMatcher.group(1).toUpperCase(Locale.ROOT) : null;
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;
        Matcher priceMatcher = PRICE_PATTERN.matcher(normalized);
        while (priceMatcher.find()) {
            String unit = priceMatcher.group(2);
            if (unit == null) continue;
            BigDecimal price = parsePrice(priceMatcher.group(1), unit);
            String prefix = normalized.substring(Math.max(0, priceMatcher.start() - 12), priceMatcher.start());
            if (prefix.matches(".*(duoi|toi da|khong qua|nho hon).*")) maxPrice = price;
            else if (prefix.matches(".*(tu|tren|toi thieu|lon hon).*")) minPrice = price;
            else maxPrice = price;
        }
        String keyword = normalized
                .replaceAll("\\b(?:toi muon|can mua|tim giup|cho toi|cho minh|san pham|kich co|tim|mua|can|ao|size|co|mau)\\b", " ")
                .replaceAll("\\b(?:xxl|xl|l|m|s)\\b", " ")
                .replaceAll("\\d+(?:[.,]\\d+)?\\s*(?:trieu|tr|k|nghin|d)?", " ")
                .replaceAll("\\s+", " ").trim();
        if (color != null) keyword = keyword.replace(normalize(color), "").trim();
        if (keyword.isBlank()) keyword = null;
        boolean productRequest = color != null || size != null || minPrice != null || maxPrice != null
                || normalized.matches(".*\\b(tim|mua|ao|san pham|hang|gia|con hang)\\b.*");
        if (!productRequest) return new SearchIntent("OTHER", null, null, null, null, null, null);
        return new SearchIntent("SEARCH_PRODUCT", keyword, color, size, minPrice, maxPrice, null);
    }

    private SearchIntent reconcileIntent(SearchIntent ai, SearchIntent local) {
        if (ai == null) return sanitizeIntent(local);
        if ("GREETING".equals(local.intent())) return local;

        String intentType = "SEARCH_PRODUCT".equals(local.intent()) ? "SEARCH_PRODUCT" : ai.intent();
        return sanitizeIntent(new SearchIntent(
                intentType,
                firstMeaningfulKeyword(ai.keyword(), local.keyword()),
                local.color() != null ? local.color() : ai.color(),
                local.size() != null ? local.size() : ai.size(),
                local.minPrice() != null ? local.minPrice() : ai.minPrice(),
                local.maxPrice() != null ? local.maxPrice() : ai.maxPrice(),
                ai.answer()));
    }

    private SearchIntent sanitizeIntent(SearchIntent intent) {
        String keyword = meaningfulKeyword(intent.keyword()) ? intent.keyword().trim() : null;
        String size = intent.size() == null ? null : extractSize(intent.size());
        return new SearchIntent(intent.intent(), keyword, intent.color(), size, intent.minPrice(), intent.maxPrice(), intent.answer());
    }

    private String firstMeaningfulKeyword(String preferred, String fallback) {
        if (meaningfulKeyword(preferred)) return preferred;
        return meaningfulKeyword(fallback) ? fallback : null;
    }

    private boolean meaningfulKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return false;
        String value = normalize(keyword).replace('-', ' ').replaceAll("\\s+", " ").trim();
        return !GENERIC_KEYWORDS.contains(value);
    }

    private String extractSize(String value) {
        Matcher matcher = Pattern.compile("(?:size|co)?\\s*(xxl|xl|l|m|s)\\b", Pattern.CASE_INSENSITIVE)
                .matcher(normalize(value));
        return matcher.find() ? matcher.group(1).toUpperCase(Locale.ROOT) : value.trim().toUpperCase(Locale.ROOT);
    }

    private List<AiProductCard> searchProducts(SearchIntent intent, List<ChiTietSanPhamResponse> variants) {
        if (!"SEARCH_PRODUCT".equals(intent.intent())) return List.of();
        Map<Long, AiProductCard> uniqueProducts = new LinkedHashMap<>();
        for (ChiTietSanPhamResponse variant : variants) {
            if (!matches(variant, intent)) continue;
            BigDecimal original = variant.getGiaBan();
            BigDecimal current = Boolean.TRUE.equals(variant.getCoGiamGia()) ? variant.getGiaSauGiam() : original;
            uniqueProducts.putIfAbsent(variant.getIdSanPham(), new AiProductCard(
                    variant.getIdSanPham(), variant.getId(), variant.getIdMauSac(), variant.getIdKichCo(),
                    variant.getTenSanPham(), variant.getTenMauSac(), variant.getTenKichCo(), variant.getSoLuong(),
                    variant.getAnhDaiDien(), original, current, variant.getCoGiamGia(),
                    variant.getPhanTramGiam(), variant.getTenDotGiamGia()));
            if (uniqueProducts.size() >= 6) break;
        }
        return new ArrayList<>(uniqueProducts.values());
    }

    private boolean matches(ChiTietSanPhamResponse v, SearchIntent intent) {
        if (intent.color() != null && !normalize(v.getTenMauSac()).contains(normalize(intent.color()))) return false;
        if (intent.size() != null && !extractSize(v.getTenKichCo()).equals(extractSize(intent.size()))) return false;
        String searchable = normalize(v.getTenSanPham() + " " + v.getMaSanPham() + " " + v.getTenMauSac() + " " + v.getTenKichCo());
        if (intent.keyword() != null) {
            for (String word : normalize(intent.keyword()).split(" ")) {
                if (word.length() > 1 && !searchable.contains(word)) return false;
            }
        }
        BigDecimal price = Boolean.TRUE.equals(v.getCoGiamGia()) ? v.getGiaSauGiam() : v.getGiaBan();
        if (price == null) return false;
        return (intent.minPrice() == null || price.compareTo(intent.minPrice()) >= 0)
                && (intent.maxPrice() == null || price.compareTo(intent.maxPrice()) <= 0);
    }

    private String buildReply(SearchIntent intent, List<AiProductCard> products) {
        if ("GREETING".equals(intent.intent())) {
            return "Chào bạn! Bạn muốn tìm áo màu gì, size nào hoặc trong khoảng giá bao nhiêu? Mình sẽ kiểm tra đúng mẫu còn hàng cho bạn nhé 😊";
        }
        if (!products.isEmpty()) {
            String criteria = Arrays.asList(intent.color(), intent.size() == null ? null : "size " + intent.size())
                    .stream().filter(s -> s != null && !s.isBlank()).reduce((a, b) -> a + ", " + b).orElse("phù hợp");
            return "Mình tìm thấy " + products.size() + " sản phẩm " + criteria + " đang còn hàng. Bạn bấm “Xem chi tiết” để xem đầy đủ thông tin sản phẩm nhé:";
        }
        if ("SEARCH_PRODUCT".equals(intent.intent())) {
            return "Hiện shop chưa tìm thấy sản phẩm khớp đủ yêu cầu và còn hàng. Bạn thử đổi màu, size hoặc khoảng giá khác giúp mình nhé.";
        }
        if (intent.answer() != null && !intent.answer().isBlank()) return intent.answer();
        return "Bạn cho mình biết mẫu áo, màu, size hoặc khoảng giá mong muốn nhé. Ví dụ: “Tìm áo màu xanh size L dưới 500k”.";
    }

    private String buildPublicDataContext(List<ChiTietSanPhamResponse> variants) {
        StringBuilder context = new StringBuilder("DATA CÔNG KHAI HIỆN TẠI:\nSẢN PHẨM/BIẾN THỂ CÒN HÀNG:\n");
        variants.stream().limit(120).forEach(v -> {
            BigDecimal price = Boolean.TRUE.equals(v.getCoGiamGia()) ? v.getGiaSauGiam() : v.getGiaBan();
            context.append("- ").append(v.getTenSanPham()).append(" | mã ").append(v.getMaSanPham())
                    .append(" | màu ").append(v.getTenMauSac()).append(" | size ").append(v.getTenKichCo())
                    .append(" | tồn ").append(v.getSoLuong()).append(" | giá ").append(price).append(" VND");
            if (Boolean.TRUE.equals(v.getCoGiamGia())) context.append(" | giá gốc ").append(v.getGiaBan()).append(" VND | ưu đãi ").append(v.getTenDotGiamGia());
            context.append('\n');
        });
        ShippingConfigDTO shipping = shippingConfigService.getConfig();
        context.append("VẬN CHUYỂN:\n")
                .append("- Nội thành Hà Nội: ").append(shipping.getPhiNoiThanhHaNoi()).append(" VND\n")
                .append("- Ngoại thành Hà Nội: ").append(shipping.getPhiNgoaiThanhHaNoi()).append(" VND\n")
                .append("- Miền Bắc: ").append(shipping.getPhiMienBac()).append(" VND\n")
                .append("- Miền Trung: ").append(shipping.getPhiMienTrung()).append(" VND\n")
                .append("- Miền Nam: ").append(shipping.getPhiMienNam()).append(" VND\n");
        if (Boolean.TRUE.equals(shipping.getApDungMienPhiShip())) {
            context.append("- Miễn phí vận chuyển từ: ").append(shipping.getNguongMienPhiShip()).append(" VND\n");
        }
        return context.toString();
    }

    private Map<String, Object> nullableString() { return Map.of("type", List.of("string", "null")); }
    private Map<String, Object> nullableNumber() { return Map.of("type", List.of("number", "null")); }
    private String nullableText(JsonNode node, String field) { return node.path(field).isNull() || node.path(field).isMissingNode() ? null : node.path(field).asText(); }
    private BigDecimal nullableDecimal(JsonNode node, String field) { return node.path(field).isNumber() ? node.path(field).decimalValue() : null; }

    private BigDecimal parsePrice(String raw, String unit) {
        BigDecimal value = new BigDecimal(raw.replace(',', '.'));
        String u = normalize(unit);
        if (u.equals("trieu") || u.equals("tr")) return value.multiply(BigDecimal.valueOf(1_000_000));
        if (u.equals("k") || u.equals("nghin")) return value.multiply(BigDecimal.valueOf(1_000));
        return value;
    }

    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT).trim();
    }

    private record SearchIntent(String intent, String keyword, String color, String size, BigDecimal minPrice, BigDecimal maxPrice, String answer) {}
}
