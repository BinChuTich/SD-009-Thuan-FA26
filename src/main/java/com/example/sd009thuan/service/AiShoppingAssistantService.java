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
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiShoppingAssistantService {
    private static final Logger log = LoggerFactory.getLogger(AiShoppingAssistantService.class);
    private static final Pattern SIZE_PATTERN = Pattern.compile("(?:size|cỡ|co)\\s*(xxl|xl|l|m|s)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(triệu|trieu|tr|k|nghìn|nghin|đ|d)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern WEIGHT_PATTERN = Pattern.compile("(?:(?:can\\s*nang|nang)\\s*)?(\\d{2,3})\\s*(?:kg|ki|ky|can)\\b|(?:can\\s*nang|nang)\\s*(\\d{2,3})\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEIGHT_METERS_PATTERN = Pattern.compile("\\b1\\s*[m,.]\\s*(\\d{1,2})\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEIGHT_CM_PATTERN = Pattern.compile("(?:(?:chieu\\s*cao|cao)\\s*)?(1\\d{2})\\s*(?:cm|phan)\\b|(?:chieu\\s*cao|cao)\\s*(1\\d{2})\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FIT_PREFERENCE_PATTERN = Pattern.compile(
            "\\b(vua van|vua nguoi|om gon|om|rong thoai mai|thoai mai|oversize|form rong|rong)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern RECOMMENDED_SIZE_PATTERN = Pattern.compile(
            "(?:chot|goi y|chon)\\s*size\\s*(xxl|xl|l|m|s)\\b", Pattern.CASE_INSENSITIVE);
    private static final List<String> COLORS = List.of("xanh navy", "xanh dương", "xanh lá", "xanh", "đen", "trắng", "đỏ", "vàng", "nâu", "be", "xám", "hồng", "tím", "cam");
    private static final List<String> GENERIC_KEYWORDS = List.of(
            "ao", "ao thun", "t shirt", "tshirt", "san pham", "hang", "mau", "size", "kich co");
    private static final List<String> CONVERSATION_FILLERS = List.of(
            "toi", "tao", "minh", "tui", "em", "anh", "chi", "ban", "shop",
            "muon", "can", "mua", "tim", "giup", "cho", "voi", "nhe", "a", "oi");

    private final ChiTietSanPhamService variantService;
    private final ObjectMapper objectMapper;
    private final ShippingConfigService shippingConfigService;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final Map<String, CachedReply> replyCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 10 * 60 * 1000L;
    private static final int MAX_CACHE_ENTRIES = 200;

    public AiShoppingAssistantService(
            ChiTietSanPhamService variantService,
            ShippingConfigService shippingConfigService,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder,
            @Value("${ai.gemini.api-key:}") String apiKey,
            @Value("${ai.gemini.model:gemini-3.5-flash-lite}") String model) {
        this.variantService = variantService;
        this.shippingConfigService = shippingConfigService;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.baseUrl("https://generativelanguage.googleapis.com/v1beta").build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
    }

    public AiChatResponse reply(String rawMessage) {
        return reply(rawMessage, List.of());
    }

    public AiChatResponse reply(String rawMessage, List<AiChatTurn> history) {
        String message = rawMessage == null ? "" : rawMessage.trim();
        SearchIntent intent;
        boolean aiPowered = false;
        List<ChiTietSanPhamResponse> variants = variantService.getAvailableVariantsForAssistant();
        SearchIntent localIntent = understandLocally(message);
        // Câu tìm sản phẩm đầy đủ không phụ thuộc lịch sử, nhờ vậy hỏi lại vẫn trúng cache
        // dù giao diện đã bổ sung lượt hỏi/đáp trước đó vào history.
        List<AiChatTurn> cacheHistory = "SEARCH_PRODUCT".equals(localIntent.intent()) ? List.of() : history;
        String cacheKey = buildCacheKey(message, cacheHistory, variants);
        CachedReply cached = replyCache.get(cacheKey);
        if (cached != null && System.currentTimeMillis() - cached.createdAt() <= CACHE_TTL_MS) {
            return cached.response();
        }
        if (cached != null) replyCache.remove(cacheKey);

        SizeAdvice sizeAdvice = detectSizeAdvice(message, history);
        if (sizeAdvice != null) {
            AiChatResponse localSizeResponse = buildSizeAdviceResponse(sizeAdvice, variants);
            String fitPreference = detectFitPreference(message);
            String baseSize = sizeAdvice.heightCm() != null && sizeAdvice.weightKg() != null
                    ? suggestSize(sizeAdvice.heightCm(), sizeAdvice.weightKg()) : null;
            String previousSize = extractLastRecommendedSize(history);
            String selectedSize = baseSize;
            if ("rộng thoải mái".equals(fitPreference) && baseSize != null) {
                selectedSize = nextSize(previousSize == null ? baseSize : previousSize);
            }
            List<AiProductCard> fitProducts = localSizeResponse.products();
            if (fitPreference != null && selectedSize != null) {
                fitProducts = searchProducts(
                        new SearchIntent("SEARCH_PRODUCT", null, null, selectedSize, null, null, null), variants);
            }
            String sizeReply = fitPreference == null
                    ? localSizeResponse.reply()
                    : buildFitFollowUpReply(baseSize, selectedSize, fitPreference, fitProducts);
            boolean sizeAiPowered = false;
            if (!apiKey.isBlank()
                    && sizeAdvice.heightCm() != null && sizeAdvice.weightKg() != null) {
                try {
                    sizeReply = draftVerifiedSizeAdviceWithGemini(
                            message, history, sizeAdvice, selectedSize, fitPreference, fitProducts, sizeReply);
                    sizeAiPowered = true;
                } catch (Exception ex) {
                    log.warn("Gemini could not personalize size advice, using safe fallback: {}", ex.getMessage());
                }
            }
            // Gắn lại đúng thẻ sản phẩm ngay dưới câu chốt form để khách không phải kéo lên.
            List<AiProductCard> displayedProducts = fitPreference == null
                    ? localSizeResponse.products()
                    : fitProducts;
            AiChatResponse sizeResult = new AiChatResponse(sizeReply, displayedProducts, sizeAiPowered);
            cacheReply(cacheKey, sizeResult);
            return sizeResult;
        }

        // Yêu cầu mua hàng có màu/size/giá được parser cục bộ hiểu ngay. Không gọi Gemini
        // thêm một lượt chỉ để trích xuất lại cùng dữ liệu, giúp giảm gần một nửa độ trễ.
        if ("SEARCH_PRODUCT".equals(localIntent.intent())) {
            intent = sanitizeIntent(localIntent);
        } else if (!apiKey.isBlank()) {
            try {
                intent = understandWithGemini(message, history, buildPublicDataContext(variants));
                aiPowered = true;
            } catch (Exception ex) {
                log.warn("Gemini unavailable, using local product intent parser: {}", ex.getMessage());
                intent = localIntent;
            }
        } else {
            intent = localIntent;
        }
        // Các thuộc tính khách gõ rõ ràng (màu/size/giá) đáng tin cậy hơn suy đoán của AI.
        // Đồng thời loại keyword chung như "sản phẩm" vì chúng không phải tên sản phẩm trong DB.
        intent = reconcileIntent(intent, localIntent);

        List<AiProductCard> products = searchProducts(intent, variants);
        String responseText = buildReply(intent, products);
        if (!apiKey.isBlank() && "SEARCH_PRODUCT".equals(intent.intent())) {
            try {
                responseText = draftVerifiedSearchReplyWithGemini(message, history, intent, products, responseText);
                aiPowered = true;
            } catch (Exception ex) {
                log.warn("Gemini could not draft the verified product reply, using safe fallback: {}", ex.getMessage());
            }
        }
        AiChatResponse result = new AiChatResponse(responseText, products, aiPowered);
        cacheReply(cacheKey, result);
        return result;
    }

    private String draftVerifiedSearchReplyWithGemini(
            String message, List<AiChatTurn> history, SearchIntent intent,
            List<AiProductCard> products, String fallback) throws Exception {
        Map<String, Object> schema = Map.of(
                "type", "object", "additionalProperties", false,
                "properties", Map.of("answer", Map.of("type", "string")),
                "required", List.of("answer"));
        StringBuilder verified = new StringBuilder(products.isEmpty()
                ? "Không có sản phẩm khớp đồng thời các điều kiện và còn hàng.\n"
                : "Chỉ được giới thiệu các sản phẩm đã kiểm tra sau:\n");
        products.forEach(product -> verified.append("- ").append(product.tenSanPham())
                .append(" | màu ").append(product.tenMauSac()).append(" | size ").append(product.tenKichCo())
                .append(" | còn ").append(product.soLuong()).append(" | giá ")
                .append(Boolean.TRUE.equals(product.coGiamGia()) ? product.minGiaSauGiam() : product.minGia())
                .append(" VND\n"));
        String prompt = "Bạn là nhân viên FF T-Shirt. Trả lời tự nhiên, thân thiện bằng tiếng Việt, tối đa 3 câu. " +
                "Chỉ dùng kết quả backend bên dưới, tuyệt đối không bịa sản phẩm, giá hay tồn kho. " +
                "Nếu không có kết quả, nói rõ màu/size khách tìm và gợi ý đổi một điều kiện.\n" +
                "Bộ lọc: màu=" + intent.color() + ", size=" + intent.size() +
                ", giá từ=" + intent.minPrice() + ", giá đến=" + intent.maxPrice() + "\n" + verified;
        JsonNode response = restClient.post()
                .uri("/models/{model}:generateContent", model)
                .header("x-goog-api-key", apiKey)
                .body(Map.of(
                        "systemInstruction", Map.of("parts", List.of(Map.of("text", prompt))),
                        "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", message)))),
                        "generationConfig", Map.of(
                                "temperature", 0.35,
                                "responseMimeType", "application/json",
                                "responseJsonSchema", schema)))
                .retrieve().body(JsonNode.class);
        if (response == null) return fallback;
        JsonNode textNode = response.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (!textNode.isTextual()) return fallback;
        String answer = nullableText(objectMapper.readTree(textNode.asText()), "answer");
        if (answer == null || answer.isBlank()) return fallback;
        answer = answer.trim();
        String normalizedAnswer = normalize(answer);
        boolean namedAnyProduct = products.stream().anyMatch(product ->
                normalizedAnswer.contains(normalize(product.tenSanPham())));
        if (!products.isEmpty() && !namedAnyProduct) {
            answer += buildProductReferenceSentence(products);
        }
        return answer;
    }

    private String draftVerifiedSizeAdviceWithGemini(
            String message, List<AiChatTurn> history, SizeAdvice advice, String selectedSize, String fitPreference,
            List<AiProductCard> products, String fallback) throws Exception {
        String bodyNote;
        if (advice.heightCm() >= 173 && advice.weightKg() < 60) {
            bodyNote = "dáng cao và mảnh, cần ưu tiên đủ chiều dài áo";
        } else if (advice.weightKg() >= 80) {
            bodyNote = "vóc dáng đầy đặn, cần ưu tiên độ thoải mái phần thân";
        } else if (advice.heightCm() < 160) {
            bodyNote = "chiều cao nhỏ, cần tránh áo quá dài";
        } else {
            bodyNote = "vóc dáng cân đối";
        }
        String availability = products.isEmpty()
                ? "Shop hiện không có sản phẩm size " + selectedSize + " còn hàng."
                : "Shop đang có " + products.size() + " sản phẩm size " + selectedSize + " còn hàng.";
        String productDetails = buildProductDetailsForPrompt(products);
        Map<String, Object> schema = Map.of(
                "type", "object", "additionalProperties", false,
                "properties", Map.of("answer", Map.of("type", "string")),
                "required", List.of("answer"));
        String continuityRule = fitPreference == null
                ? "Đây là lượt tư vấn đầu: giải thích ngắn gọn lý do chọn size. "
                : "Đây là câu nối tiếp; khách đã chọn kiểu mặc '" + fitPreference + "'. " +
                  "Hãy tiếp nối quyết định trước đó, không chào lại, không nhắc lại toàn bộ chiều cao/cân nặng. " +
                  "Nếu khách nói 'rộng hơn' thì size dưới đây đã được backend tăng tiếp từ size vừa chốt; hãy nói rõ sự thay đổi và dẫn sang chọn sản phẩm. ";
        String prompt = "Bạn là stylist của FF T-Shirt. Viết lời tư vấn size tự nhiên bằng tiếng Việt trong 2-3 câu. " +
                continuityRule +
                "Không dùng một công thức lặp lại; hãy thay đổi cách mở đầu và giải thích theo vóc dáng cụ thể. " +
                "Không được thay đổi size backend đã tính, không bịa tồn kho và không đưa số đo bảng size chưa được cung cấp. " +
                "Nếu nhắc sản phẩm thì phải gọi rõ tên từng mẫu, màu và giá từ danh sách; không được chỉ nói chung chung 'có N sản phẩm'. " +
                "Có thể hỏi khách thích mặc ôm, vừa hay oversize ở câu cuối.\n" +
                "Dữ liệu đã xác minh: cao " + advice.heightCm() + "cm, nặng " + advice.weightKg() +
                "kg, size cần chốt ở lượt này " + selectedSize + ", nhận xét " + bodyNote + ". " + availability + "\n" + productDetails;
        List<Map<String, Object>> contents = new ArrayList<>();
        if (history != null) {
            history.stream().skip(Math.max(0, history.size() - 4L)).forEach(turn -> {
                if (turn != null && turn.getRole() != null && turn.getContent() != null) {
                    contents.add(Map.of(
                            "role", "assistant".equals(turn.getRole()) ? "model" : "user",
                            "parts", List.of(Map.of("text", turn.getContent()))));
                }
            });
        }
        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", message))));
        JsonNode response = restClient.post()
                .uri("/models/{model}:generateContent", model)
                .header("x-goog-api-key", apiKey)
                .body(Map.of(
                        "systemInstruction", Map.of("parts", List.of(Map.of("text", prompt))),
                        "contents", contents,
                        "generationConfig", Map.of(
                                "temperature", 0.7,
                                "responseMimeType", "application/json",
                                "responseJsonSchema", schema)))
                .retrieve().body(JsonNode.class);
        if (response == null) return fallback;
        JsonNode textNode = response.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (!textNode.isTextual()) return fallback;
        String answer = nullableText(objectMapper.readTree(textNode.asText()), "answer");
        if (answer == null || answer.isBlank()) return fallback;
        answer = answer.trim();
        String normalizedAnswer = normalize(answer);
        boolean namedAnyProduct = products.stream().anyMatch(product ->
                normalizedAnswer.contains(normalize(product.tenSanPham())));
        if (fitPreference != null && !products.isEmpty() && !namedAnyProduct) {
            answer += buildProductReferenceSentence(products);
        }
        return answer;
    }

    private String detectFitPreference(String message) {
        Matcher matcher = FIT_PREFERENCE_PATTERN.matcher(normalize(message));
        if (!matcher.find()) return null;
        String value = matcher.group(1);
        if (value.contains("oversize") || value.contains("rong") || value.contains("thoai mai")) return "rộng thoải mái";
        if (value.contains("om")) return "ôm gọn";
        return "vừa vặn";
    }

    private String buildFitFollowUpReply(
            String baseSize, String selectedSize, String fitPreference, List<AiProductCard> products) {
        String productChoice = buildActionableProductChoice(products);
        if ("rộng thoải mái".equals(fitPreference)) {
            return "Nếu bạn muốn rộng hơn, mình chuyển sang size " + selectedSize
                    + (baseSize != null && !baseSize.equals(selectedSize) ? " thay cho size " + baseSize : "")
                    + ". " + productChoice;
        }
        if ("ôm gọn".equals(fitPreference)) {
            return "Vậy mình chốt size " + selectedSize + " theo kiểu ôm gọn cho bạn. " + productChoice;
        }
        return "Vậy mình chốt size " + selectedSize + " theo kiểu vừa vặn cho bạn. " + productChoice;
    }

    private String extractLastRecommendedSize(List<AiChatTurn> history) {
        if (history == null) return null;
        for (int i = history.size() - 1; i >= 0; i--) {
            AiChatTurn turn = history.get(i);
            if (turn == null || !"assistant".equals(turn.getRole()) || turn.getContent() == null) continue;
            Matcher matcher = RECOMMENDED_SIZE_PATTERN.matcher(normalize(turn.getContent()));
            if (matcher.find()) return matcher.group(1).toUpperCase(Locale.ROOT);
        }
        return null;
    }

    private String buildActionableProductChoice(List<AiProductCard> products) {
        if (products.isEmpty()) {
            return "Hiện size này chưa còn mẫu phù hợp; bạn muốn mình kiểm tra màu khác hay chuyển sang size gần nhất?";
        }
        String choices = products.stream().limit(3).map(product ->
                product.tenSanPham() + " màu " + product.tenMauSac() + " – "
                        + formatVnd(Boolean.TRUE.equals(product.coGiamGia()) ? product.minGiaSauGiam() : product.minGia()))
                .distinct().reduce((a, b) -> a + "; " + b).orElse("");
        return "Các lựa chọn đang còn là " + choices
                + ". Bạn chọn tên mẫu nào, hoặc bấm “Xem chi tiết sản phẩm” ở thẻ ngay bên dưới nhé.";
    }

    private String nextSize(String size) {
        return switch (size) {
            case "S" -> "M";
            case "M" -> "L";
            case "L" -> "XL";
            default -> "XXL";
        };
    }

    private String formatVnd(BigDecimal price) {
        if (price == null) return "chưa có giá";
        return NumberFormat.getIntegerInstance(new Locale("vi", "VN")).format(price) + "đ";
    }

    private String buildProductReferenceSentence(List<AiProductCard> products) {
        String references = products.stream().limit(3).map(product -> {
            BigDecimal price = Boolean.TRUE.equals(product.coGiamGia()) ? product.minGiaSauGiam() : product.minGia();
            return product.tenSanPham() + " màu " + product.tenMauSac() + " (" + formatVnd(price) + ")";
        }).distinct().reduce((a, b) -> a + "; " + b).orElse(null);
        return references == null ? "" : " Các mẫu phù hợp đang còn là: " + references + ".";
    }

    private String buildProductDetailsForPrompt(List<AiProductCard> products) {
        if (products.isEmpty()) return "Danh sách sản phẩm: không có mẫu phù hợp còn hàng.";
        StringBuilder details = new StringBuilder("Danh sách sản phẩm đã hiển thị ở lượt trước:\n");
        for (int i = 0; i < Math.min(products.size(), 4); i++) {
            AiProductCard product = products.get(i);
            BigDecimal price = Boolean.TRUE.equals(product.coGiamGia()) ? product.minGiaSauGiam() : product.minGia();
            details.append(i + 1).append(". ").append(product.tenSanPham())
                    .append(" | màu ").append(product.tenMauSac())
                    .append(" | size ").append(product.tenKichCo())
                    .append(" | giá ").append(formatVnd(price)).append('\n');
        }
        return details.toString();
    }

    private String buildCacheKey(String message, List<AiChatTurn> history, List<ChiTietSanPhamResponse> variants) {
        StringBuilder key = new StringBuilder(normalize(message)).append('|');
        if (history != null) history.stream().skip(Math.max(0, history.size() - 4L))
                .filter(turn -> turn != null && turn.getContent() != null)
                .forEach(turn -> key.append(turn.getRole()).append(':').append(normalize(turn.getContent())).append('|'));
        variants.stream().limit(120).forEach(v -> key.append(v.getId()).append(':')
                .append(v.getSoLuong()).append(':').append(v.getGiaSauGiam()).append(';'));
        return Integer.toHexString(key.toString().hashCode()) + ':' + normalize(message);
    }

    private void cacheReply(String key, AiChatResponse response) {
        if (replyCache.size() >= MAX_CACHE_ENTRIES) {
            long cutoff = System.currentTimeMillis() - CACHE_TTL_MS;
            replyCache.entrySet().removeIf(entry -> entry.getValue().createdAt() < cutoff);
            if (replyCache.size() >= MAX_CACHE_ENTRIES) replyCache.clear();
        }
        replyCache.put(key, new CachedReply(response, System.currentTimeMillis()));
    }

    private SearchIntent understandWithGemini(String message, List<AiChatTurn> history, String dataContext) throws Exception {
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

        List<Map<String, Object>> contents = new ArrayList<>();
        if (history != null) {
            history.stream().skip(Math.max(0, history.size() - 8L)).forEach(turn -> {
                if (turn != null && turn.getRole() != null && turn.getContent() != null) {
                    String role = "assistant".equals(turn.getRole()) ? "model" : "user";
                    contents.add(Map.of("role", role, "parts", List.of(Map.of("text", turn.getContent()))));
                }
            });
        }
        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", message))));

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", developerPrompt))),
                "contents", contents,
                "generationConfig", Map.of(
                        "temperature", 0.1,
                        "responseMimeType", "application/json",
                        "responseJsonSchema", schema));

        JsonNode response = restClient.post()
                .uri("/models/{model}:generateContent", model)
                .header("x-goog-api-key", apiKey)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) throw new IllegalStateException("Gemini returned an empty response");
        JsonNode textNode = response.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (!textNode.isTextual()) throw new IllegalStateException("Gemini response did not contain structured output");
        JsonNode parsed = objectMapper.readTree(textNode.asText());
        return new SearchIntent(
                parsed.path("intent").asText("OTHER"),
                nullableText(parsed, "keyword"), nullableText(parsed, "color"), nullableText(parsed, "size"),
                nullableDecimal(parsed, "minPrice"), nullableDecimal(parsed, "maxPrice"), nullableText(parsed, "answer"));
    }

    private SizeAdvice detectSizeAdvice(String message, List<AiChatTurn> history) {
        String normalizedMessage = normalize(message);
        StringBuilder conversationContext = new StringBuilder();
        if (history != null) {
            history.stream().skip(Math.max(0, history.size() - 6L))
                    .filter(turn -> turn != null && "user".equals(turn.getRole()) && turn.getContent() != null)
                    .forEach(turn -> conversationContext.append(turn.getContent()).append(' '));
        }
        String normalizedContext = normalize(conversationContext + " " + message);
        boolean explicitRequest = normalizedContext.matches(".*(goi y size|tu van(?:\\s+chon)? size|chon size|mac size gi|nen mac size|bang size|vua size nao).*" );

        // Tin mới nhất luôn có độ ưu tiên cao nhất. Lịch sử chỉ bổ sung trường còn thiếu.
        Integer weight = firstNumber(WEIGHT_PATTERN.matcher(normalizedMessage));
        Integer height = extractHeight(normalizedMessage);
        if (weight == null && (explicitRequest || height != null)) {
            weight = inferBareWeight(normalizedMessage);
        }
        if ((weight == null || height == null) && history != null) {
            for (int i = history.size() - 1; i >= 0 && (weight == null || height == null); i--) {
                AiChatTurn turn = history.get(i);
                if (turn == null || !"user".equals(turn.getRole()) || turn.getContent() == null) continue;
                String previous = normalize(turn.getContent());
                if (height == null) height = extractHeight(previous);
                if (weight == null) {
                    weight = firstNumber(WEIGHT_PATTERN.matcher(previous));
                    if (weight == null && (explicitRequest || extractHeight(previous) != null)) {
                        weight = inferBareWeight(previous);
                    }
                }
            }
        }
        if (!explicitRequest && !(weight != null && height != null)) return null;
        return new SizeAdvice(height, weight);
    }

    private Integer firstNumber(Matcher matcher) {
        if (!matcher.find()) return null;
        for (int i = 1; i <= matcher.groupCount(); i++) {
            if (matcher.group(i) != null) return Integer.parseInt(matcher.group(i));
        }
        return null;
    }

    private Integer extractHeight(String normalized) {
        Matcher meters = HEIGHT_METERS_PATTERN.matcher(normalized);
        if (meters.find()) {
            String fraction = meters.group(1);
            // Cách viết phổ biến ở Việt Nam: 1m5 = 1m50, 1m7 = 1m70.
            return 100 + (fraction.length() == 1 ? Integer.parseInt(fraction) * 10 : Integer.parseInt(fraction));
        }
        return firstNumber(HEIGHT_CM_PATTERN.matcher(normalized));
    }

    private Integer inferBareWeight(String normalized) {
        String withoutHeight = HEIGHT_METERS_PATTERN.matcher(normalized).replaceAll(" ");
        withoutHeight = HEIGHT_CM_PATTERN.matcher(withoutHeight).replaceAll(" ");
        Matcher number = Pattern.compile("\\b(\\d{2,3})\\b").matcher(withoutHeight);
        while (number.find()) {
            int value = Integer.parseInt(number.group(1));
            if (value >= 25 && value <= 200) return value;
        }
        return null;
    }

    private AiChatResponse buildSizeAdviceResponse(SizeAdvice advice, List<ChiTietSanPhamResponse> variants) {
        if (advice.heightCm() == null && advice.weightKg() == null) {
            return new AiChatResponse("Bạn gửi giúp mình chiều cao và cân nặng nhé, ví dụ: “1m70, 65kg”. Mình sẽ chọn size và kiểm tra mẫu đang còn hàng cho bạn.", List.of(), false);
        }
        if (advice.heightCm() == null) {
            return new AiChatResponse("Mình đã ghi nhận cân nặng " + advice.weightKg() + "kg. Bạn cho mình thêm chiều cao (ví dụ 1m70) để chọn size chính xác nhé.", List.of(), false);
        }
        if (advice.weightKg() == null) {
            return new AiChatResponse("Mình đã ghi nhận chiều cao " + advice.heightCm() + "cm. Bạn cho mình thêm cân nặng để chọn size chính xác nhé.", List.of(), false);
        }

        String suggestedSize = suggestSize(advice.heightCm(), advice.weightKg());
        SearchIntent productIntent = new SearchIntent("SEARCH_PRODUCT", null, null, suggestedSize, null, null, null);
        List<AiProductCard> products = searchProducts(productIntent, variants);
        String fitNote = advice.heightCm() >= 173 && advice.weightKg() < 60
                ? "Dáng của bạn khá cao và mảnh nên size này ưu tiên đủ chiều dài áo. "
                : "Size này cân đối giữa chiều dài áo và độ rộng thân. ";
        String availability = products.isEmpty()
                ? "Hiện shop chưa có sản phẩm size " + suggestedSize + " còn hàng; bạn có thể nhờ nhân viên kiểm tra thêm."
                : "Mình đã lọc các mẫu size " + suggestedSize + " đang còn hàng bên dưới.";
        String reply = "Với " + advice.heightCm() + "cm và " + advice.weightKg() + "kg, mình gợi ý size " + suggestedSize + ". "
                + fitNote + "Nếu thích mặc oversize, bạn có thể tăng thêm 1 size. " + availability;
        return new AiChatResponse(reply, products, false);
    }

    private String suggestSize(int heightCm, int weightKg) {
        if (weightKg < 54 && heightCm < 165) return "S";
        if (weightKg <= 64 && heightCm <= 172) return "M";
        if (weightKg <= 73 && heightCm <= 177) return "L";
        if (weightKg <= 83 && heightCm <= 183) return "XL";
        return "XXL";
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
        String keyword = cleanProductKeyword(intent.keyword(), intent.color());
        String size = intent.size() == null ? null : extractSize(intent.size());
        return new SearchIntent(intent.intent(), keyword, intent.color(), size, intent.minPrice(), intent.maxPrice(), intent.answer());
    }

    private String firstMeaningfulKeyword(String preferred, String fallback) {
        String preferredClean = cleanProductKeyword(preferred, null);
        if (preferredClean != null) return preferredClean;
        return cleanProductKeyword(fallback, null);
    }

    private boolean meaningfulKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return false;
        String value = normalize(keyword).replace('-', ' ').replaceAll("\\s+", " ").trim();
        return !GENERIC_KEYWORDS.contains(value);
    }

    private String cleanProductKeyword(String keyword, String color) {
        if (keyword == null || keyword.isBlank()) return null;
        String value = normalize(keyword).replace('-', ' ');
        if (color != null) value = value.replace(normalize(color), " ");
        for (String knownColor : COLORS) value = value.replace(normalize(knownColor), " ");
        value = value.replaceAll("\\b(?:size|co)\\s*(?:xxl|xl|l|m|s)\\b", " ");
        List<String> meaningful = Arrays.stream(value.replaceAll("[^a-z0-9 ]", " ").split("\\s+"))
                .filter(word -> !word.isBlank())
                .filter(word -> !CONVERSATION_FILLERS.contains(word))
                .filter(word -> !GENERIC_KEYWORDS.contains(word))
                .toList();
        if (meaningful.isEmpty()) return null;
        String cleaned = String.join(" ", meaningful);
        return meaningfulKeyword(cleaned) ? cleaned : null;
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
            String examples = products.stream().limit(2).map(AiProductCard::tenSanPham)
                    .distinct().reduce((a, b) -> a + " và " + b).orElse("mẫu phù hợp");
            return "Có nhé! Shop đang còn " + products.size() + " sản phẩm " + criteria
                    + ", gồm " + examples + ". Mình để đúng các mẫu đang còn hàng bên dưới để bạn xem nhanh nhé:";
        }
        if ("SEARCH_PRODUCT".equals(intent.intent())) {
            String criteria = Arrays.asList(intent.color(), intent.size() == null ? null : "size " + intent.size())
                    .stream().filter(s -> s != null && !s.isBlank()).reduce((a, b) -> a + ", " + b).orElse("bạn yêu cầu");
            return "Hiện shop chưa có sản phẩm " + criteria
                    + " còn hàng. Bạn muốn mình giữ nguyên " + (intent.color() != null ? "màu này và đổi size" : "size này và đổi màu")
                    + ", hay tìm mẫu có mức giá gần nhất?";
        }
        if (intent.answer() != null && !intent.answer().isBlank()) return intent.answer();
        return "Bạn cho mình biết mẫu áo, màu, size hoặc khoảng giá mong muốn nhé. Ví dụ: “Tìm áo màu xanh size L dưới 500k”.";
    }

    private String buildPublicDataContext(List<ChiTietSanPhamResponse> variants) {
        StringBuilder context = new StringBuilder("DATA CÔNG KHAI HIỆN TẠI:\nSẢN PHẨM/BIẾN THỂ CÒN HÀNG:\n");
        variants.stream().limit(60).forEach(v -> {
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
    private record SizeAdvice(Integer heightCm, Integer weightKg) {}
    private record CachedReply(AiChatResponse response, long createdAt) {}
}
