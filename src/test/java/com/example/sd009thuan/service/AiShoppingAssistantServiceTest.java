package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.ChiTietSanPhamResponse;
import com.example.sd009thuan.dto.ai.AiChatResponse;
import com.example.sd009thuan.dto.ai.AiChatTurn;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiShoppingAssistantServiceTest {

    @Test
    void findsBlackSizeLProductFromNaturalVietnameseRequest() {
        ChiTietSanPhamService variantService = mock(ChiTietSanPhamService.class);
        when(variantService.getAvailableVariantsForAssistant()).thenReturn(List.of(variant("Đen", "L")));
        AiShoppingAssistantService service = new AiShoppingAssistantService(
                variantService, mock(ShippingConfigService.class), new ObjectMapper(), RestClient.builder(), "", "gpt-4o-mini");

        AiChatResponse response = service.reply("tôi muốn mua sản phẩm màu đen size L");

        assertEquals(1, response.products().size());
        assertTrue(response.reply().contains("đen, size L"));
    }

    @Test
    void acceptsSizeLabelStoredWithSizePrefix() {
        ChiTietSanPhamService variantService = mock(ChiTietSanPhamService.class);
        when(variantService.getAvailableVariantsForAssistant()).thenReturn(List.of(variant("Màu đen", "Size L")));
        AiShoppingAssistantService service = new AiShoppingAssistantService(
                variantService, mock(ShippingConfigService.class), new ObjectMapper(), RestClient.builder(), "", "gpt-4o-mini");

        assertEquals(1, service.reply("cần áo đen cỡ L").products().size());
    }

    @Test
    void ignoresConversationalPronounsWhenFilteringColorAndSize() {
        ChiTietSanPhamService variantService = mock(ChiTietSanPhamService.class);
        when(variantService.getAvailableVariantsForAssistant()).thenReturn(List.of(variant("Xanh", "S")));
        AiShoppingAssistantService service = new AiShoppingAssistantService(
                variantService, mock(ShippingConfigService.class), new ObjectMapper(), RestClient.builder(), "", "gemini-3.5-flash-lite");

        AiChatResponse response = service.reply("tao cần mua áo màu xanh size s");

        assertEquals(1, response.products().size());
        assertTrue(response.reply().contains("xanh, size S"));
    }

    @Test
    void repeatedProductSearchUsesCachedGeminiReply() {
        ChiTietSanPhamService variantService = mock(ChiTietSanPhamService.class);
        when(variantService.getAvailableVariantsForAssistant()).thenReturn(List.of(variant("Xanh", "S")));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent"))
                .andExpect(header("x-goog-api-key", "test-gemini-key"))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"{\\\"answer\\\":\\\"Shop has a matching blue size S shirt.\\\"}"}]}}]}
                        """, MediaType.APPLICATION_JSON));
        AiShoppingAssistantService service = new AiShoppingAssistantService(
                variantService, mock(ShippingConfigService.class), new ObjectMapper(), builder,
                "test-gemini-key", "gemini-3.5-flash-lite");

        AiChatResponse response = service.reply("tao cần mua áo màu xanh size s");
        AiChatResponse cachedResponse = service.reply("tao cần mua áo màu xanh size s");

        assertTrue(response.reply().startsWith("Shop has a matching blue size S shirt."));
        assertTrue(response.reply().contains("Áo thun basic"));
        assertEquals(response, cachedResponse);
        assertTrue(response.aiPowered());
        server.verify();
    }

    @Test
    void suggestsSizeFromVietnameseHeightAndWeightAndReturnsAvailableProducts() {
        ChiTietSanPhamService variantService = mock(ChiTietSanPhamService.class);
        when(variantService.getAvailableVariantsForAssistant()).thenReturn(List.of(variant("Đen", "L")));
        AiShoppingAssistantService service = new AiShoppingAssistantService(
                variantService, mock(ShippingConfigService.class), new ObjectMapper(), RestClient.builder(), "", "gpt-4o-mini");

        AiChatResponse response = service.reply("cân nặng 50, chiều cao 1m75");

        assertTrue(response.reply().contains("gợi ý size L"));
        assertEquals(1, response.products().size());
    }

    @Test
    void letsGeminiPersonalizeVerifiedSizeAdvice() {
        ChiTietSanPhamService variantService = mock(ChiTietSanPhamService.class);
        when(variantService.getAvailableVariantsForAssistant()).thenReturn(List.of(variant("Đen", "S")));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent"))
                .andExpect(header("x-goog-api-key", "test-gemini-key"))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"{\\\"answer\\\":\\\"Size S will suit this compact build; choose M for a roomier fit.\\\"}"}]}}]}
                        """, MediaType.APPLICATION_JSON));
        AiShoppingAssistantService service = new AiShoppingAssistantService(
                variantService, mock(ShippingConfigService.class), new ObjectMapper(), builder,
                "test-gemini-key", "gemini-3.5-flash-lite");

        AiChatResponse response = service.reply("Tư vấn size: 50kg, 1m4");

        assertEquals("Size S will suit this compact build; choose M for a roomier fit.", response.reply());
        assertEquals(1, response.products().size());
        assertTrue(response.aiPowered());
        server.verify();
    }

    @Test
    void asksForMissingMeasurementInsteadOfProductSearchFallback() {
        ChiTietSanPhamService variantService = mock(ChiTietSanPhamService.class);
        when(variantService.getAvailableVariantsForAssistant()).thenReturn(List.of());
        AiShoppingAssistantService service = new AiShoppingAssistantService(
                variantService, mock(ShippingConfigService.class), new ObjectMapper(), RestClient.builder(), "", "gpt-4o-mini");

        AiChatResponse response = service.reply("gợi ý size, mình nặng 60kg");

        assertTrue(response.reply().contains("thêm chiều cao"));
    }

    @Test
    void infersBareWeightAndShortMeterNotationInSizeContext() {
        ChiTietSanPhamService variantService = mock(ChiTietSanPhamService.class);
        when(variantService.getAvailableVariantsForAssistant()).thenReturn(List.of(variant("Đen", "S")));
        AiShoppingAssistantService service = new AiShoppingAssistantService(
                variantService, mock(ShippingConfigService.class), new ObjectMapper(), RestClient.builder(), "", "gpt-4o-mini");

        AiChatResponse response = service.reply("Tư vấn chọn size: 50, 1m5");

        assertTrue(response.reply().contains("150cm và 50kg"));
        assertTrue(response.reply().contains("gợi ý size S"));
    }

    @Test
    void understandsBareWeightInFollowUpMessage() {
        ChiTietSanPhamService variantService = mock(ChiTietSanPhamService.class);
        when(variantService.getAvailableVariantsForAssistant()).thenReturn(List.of(variant("Đen", "S")));
        AiShoppingAssistantService service = new AiShoppingAssistantService(
                variantService, mock(ShippingConfigService.class), new ObjectMapper(), RestClient.builder(), "", "gpt-4o-mini");
        AiChatTurn request = new AiChatTurn();
        request.setRole("user");
        request.setContent("Tư vấn chọn size, chiều cao 1m5");

        AiChatResponse response = service.reply("50", List.of(request));

        assertTrue(response.reply().contains("150cm và 50kg"));
    }

    @Test
    void fitPreferenceFollowUpContinuesConversationWithoutRepeatingProducts() {
        ChiTietSanPhamService variantService = mock(ChiTietSanPhamService.class);
        when(variantService.getAvailableVariantsForAssistant()).thenReturn(List.of(variant("Đen", "S")));
        AiShoppingAssistantService service = new AiShoppingAssistantService(
                variantService, mock(ShippingConfigService.class), new ObjectMapper(), RestClient.builder(), "", "gemini-3.5-flash-lite");
        AiChatTurn measurement = new AiChatTurn();
        measurement.setRole("user");
        measurement.setContent("50kg, 1m4");
        AiChatTurn previousAdvice = new AiChatTurn();
        previousAdvice.setRole("assistant");
        previousAdvice.setContent("Mình gợi ý size S. Bạn thích mặc ôm, vừa hay rộng?");

        AiChatResponse response = service.reply("vừa vặn đi", List.of(measurement, previousAdvice));

        assertTrue(response.reply().contains("chốt size S"));
        assertTrue(!response.reply().contains("140cm"));
        assertTrue(response.reply().contains("Áo thun basic"));
        assertTrue(response.reply().contains("250.000đ"));
        assertTrue(response.reply().contains("Xem chi tiết sản phẩm"));
        assertEquals(1, response.products().size());
    }

    @Test
    void widerFollowUpIncreasesFromLastConfirmedSizeInsteadOfStartingOver() {
        ChiTietSanPhamService variantService = mock(ChiTietSanPhamService.class);
        when(variantService.getAvailableVariantsForAssistant()).thenReturn(List.of(variant("Đen", "L")));
        AiShoppingAssistantService service = new AiShoppingAssistantService(
                variantService, mock(ShippingConfigService.class), new ObjectMapper(), RestClient.builder(), "", "gemini-3.5-flash-lite");
        AiChatTurn measurement = new AiChatTurn();
        measurement.setRole("user");
        measurement.setContent("40kg, 1m6");
        AiChatTurn previousChoice = new AiChatTurn();
        previousChoice.setRole("assistant");
        previousChoice.setContent("Mình chốt size M để mặc rộng hơn size S.");

        AiChatResponse response = service.reply("thế muốn rộng hơn thì sao", List.of(measurement, previousChoice));

        assertTrue(response.reply().contains("size L"));
        assertTrue(!response.reply().contains("chốt size M"));
        assertEquals("L", response.products().get(0).tenKichCo());
    }

    @Test
    void newestMeasurementsOverridePreviousSearchHistory() {
        ChiTietSanPhamService variantService = mock(ChiTietSanPhamService.class);
        when(variantService.getAvailableVariantsForAssistant()).thenReturn(List.of(variant("Đen", "XL")));
        AiShoppingAssistantService service = new AiShoppingAssistantService(
                variantService, mock(ShippingConfigService.class), new ObjectMapper(), RestClient.builder(), "", "gpt-4o-mini");
        AiChatTurn oldRequest = new AiChatTurn();
        oldRequest.setRole("user");
        oldRequest.setContent("Tư vấn chọn size: 50, 1m5");

        AiChatResponse response = service.reply("90, 1m7", List.of(oldRequest));

        assertTrue(response.reply().contains("170cm và 90kg"));
        assertTrue(response.reply().contains("gợi ý size XXL"));
    }

    @Test
    void callsGeminiStructuredOutputWithoutExposingKeyInUrl() {
        ChiTietSanPhamService variantService = mock(ChiTietSanPhamService.class);
        when(variantService.getAvailableVariantsForAssistant()).thenReturn(List.of());
        ShippingConfigService shipping = mock(ShippingConfigService.class);
        when(shipping.getConfig()).thenReturn(new com.example.sd009thuan.dto.ShippingConfigDTO());
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent"))
                .andExpect(header("x-goog-api-key", "test-gemini-key"))
                .andExpect(jsonPath("$.generationConfig.responseJsonSchema").exists())
                .andExpect(jsonPath("$.generationConfig.responseSchema").doesNotExist())
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"{\\"intent\\":\\"SHIPPING\\",\\"keyword\\":null,\\"color\\":null,\\"size\\":null,\\"minPrice\\":null,\\"maxPrice\\":null,\\"answer\\":\\"Shop có hỗ trợ giao hàng.\\"}"}]}}]}
                        """, MediaType.APPLICATION_JSON));
        AiShoppingAssistantService service = new AiShoppingAssistantService(
                variantService, shipping, new ObjectMapper(), builder, "test-gemini-key", "gemini-3.5-flash-lite");

        AiChatResponse response = service.reply("Shop có giao hàng không?");

        assertEquals("Shop có hỗ trợ giao hàng.", response.reply());
        assertTrue(response.aiPowered());
        server.verify();
    }

    private ChiTietSanPhamResponse variant(String color, String size) {
        return ChiTietSanPhamResponse.builder()
                .id(11L).idSanPham(1L).idMauSac(2L).idKichCo(3L)
                .maSanPham("SP01").tenSanPham("Áo thun basic")
                .tenMauSac(color).tenKichCo(size).soLuong(10)
                .giaBan(BigDecimal.valueOf(250_000)).coGiamGia(false)
                .anhDaiDien("/image.jpg").build();
    }
}
