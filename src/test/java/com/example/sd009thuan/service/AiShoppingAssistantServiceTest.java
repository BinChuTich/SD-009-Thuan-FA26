package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.ChiTietSanPhamResponse;
import com.example.sd009thuan.dto.ai.AiChatResponse;
import com.example.sd009thuan.dto.ai.AiChatTurn;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    private ChiTietSanPhamResponse variant(String color, String size) {
        return ChiTietSanPhamResponse.builder()
                .id(11L).idSanPham(1L).idMauSac(2L).idKichCo(3L)
                .maSanPham("SP01").tenSanPham("Áo thun basic")
                .tenMauSac(color).tenKichCo(size).soLuong(10)
                .giaBan(BigDecimal.valueOf(250_000)).coGiamGia(false)
                .anhDaiDien("/image.jpg").build();
    }
}
