package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.ChiTietSanPhamResponse;
import com.example.sd009thuan.dto.ai.AiChatResponse;
import com.example.sd009thuan.dto.ai.AiProductCard;
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

    private final ChiTietSanPhamService variantService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public AiShoppingAssistantService(
            ChiTietSanPhamService variantService,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder,
            @Value("${ai.openai.api-key:}") String apiKey,
            @Value("${ai.openai.model:gpt-4o-mini}") String model) {
        this.variantService = variantService;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.baseUrl("https://api.openai.com/v1").build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
    }

    public AiChatResponse reply(String rawMessage) {
        String message = rawMessage == null ? "" : rawMessage.trim();
        SearchIntent intent = null;
        boolean aiPowered = false;

        if (!apiKey.isBlank()) {
            try {
                intent = understandWithOpenAi(message);
                aiPowered = true;
            } catch (Exception ex) {
                log.warn("OpenAI unavailable, using local product intent parser: {}", ex.getMessage());
            }
        }
        if (intent == null) intent = understandLocally(message);

        List<AiProductCard> products = searchProducts(intent);
        String responseText = buildReply(intent, products);
        return new AiChatResponse(responseText, products, aiPowered);
    }

    private SearchIntent understandWithOpenAi(String message) throws Exception {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "intent", Map.of("type", "string", "enum", List.of("SEARCH_PRODUCT", "GREETING", "OTHER")),
                        "keyword", nullableString(),
                        "color", nullableString(),
                        "size", nullableString(),
                        "minPrice", nullableNumber(),
                        "maxPrice", nullableNumber()),
                "required", List.of("intent", "keyword", "color", "size", "minPrice", "maxPrice"));

        String developerPrompt = "Bạn là bộ phân tích nhu cầu mua áo của FF T-Shirt. " +
                "Chỉ tách ý định và bộ lọc từ câu khách hàng, không tự tạo sản phẩm. " +
                "Chuẩn hóa size thành S/M/L/XL/XXL. Giá dùng VND. " +
                "Ví dụ 'dưới 500k' => maxPrice 500000; 'từ 300k' => minPrice 300000.";

        Map<String, Object> body = Map.of(
                "model", model,
                "store", false,
                "input", List.of(
                        Map.of("role", "developer", "content", List.of(Map.of("type", "input_text", "text", developerPrompt))),
                        Map.of("role", "user", "content", List.of(Map.of("type", "input_text", "text", message)))),
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
                            nullableDecimal(parsed, "minPrice"), nullableDecimal(parsed, "maxPrice"));
                }
            }
        }
        throw new IllegalStateException("OpenAI response did not contain structured output");
    }

    private SearchIntent understandLocally(String message) {
        String normalized = normalize(message);
        if (normalized.matches("^(xin chao|chao|hi|hello|alo).*")) {
            return new SearchIntent("GREETING", null, null, null, null, null);
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
                .replaceAll("(?:toi muon|can mua|tim giup|tim|mua|cho toi|cho minh|san pham|ao|size|mau)", " ")
                .replaceAll("\\b(?:xxl|xl|l|m|s)\\b", " ")
                .replaceAll("\\d+(?:[.,]\\d+)?\\s*(?:trieu|tr|k|nghin|d)?", " ")
                .replaceAll("\\s+", " ").trim();
        if (color != null) keyword = keyword.replace(normalize(color), "").trim();
        if (keyword.isBlank()) keyword = null;
        return new SearchIntent("SEARCH_PRODUCT", keyword, color, size, minPrice, maxPrice);
    }

    private List<AiProductCard> searchProducts(SearchIntent intent) {
        if (!"SEARCH_PRODUCT".equals(intent.intent())) return List.of();
        Map<Long, AiProductCard> uniqueProducts = new LinkedHashMap<>();
        for (ChiTietSanPhamResponse variant : variantService.getAvailableVariantsForAssistant()) {
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
        if (intent.size() != null && !normalize(v.getTenKichCo()).equals(normalize(intent.size()))) return false;
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
        return "Bạn cho mình biết mẫu áo, màu, size hoặc khoảng giá mong muốn nhé. Ví dụ: “Tìm áo màu xanh size L dưới 500k”.";
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

    private record SearchIntent(String intent, String keyword, String color, String size, BigDecimal minPrice, BigDecimal maxPrice) {}
}
