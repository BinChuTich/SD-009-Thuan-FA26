package com.example.sd009thuan;

import com.example.sd009thuan.service.AdministrativeRegionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AdministrativeRegionServiceTest {

    private AdministrativeRegionService service;

    @BeforeEach
    void setUp() {
        service = new AdministrativeRegionService();
        service.init();
    }

    @Test
    void testValidProvinceAndWardExact() {
        AdministrativeRegionService.AddressValidationResult res = service.validateAddress("Thành phố Hà Nội", "Phường Ba Đình");
        assertTrue(res.valid(), "Phải hợp lệ với Thành phố Hà Nội và Phường Ba Đình");
        assertNotNull(res.matchedProvince());
        assertNotNull(res.matchedWard());
        assertEquals("Thành phố Hà Nội", res.matchedProvince().name());
        assertEquals("Phường Ba Đình", res.matchedWard().name());
    }

    @Test
    void testValidProvinceAndWardNormalized() {
        AdministrativeRegionService.AddressValidationResult res = service.validateAddress("Hà Nội", "Ba Đình");
        assertTrue(res.valid(), "Phải hợp lệ với tên không có tiền tố Thành phố / Phường");
        assertEquals("Thành phố Hà Nội", res.matchedProvince().name());
        assertEquals("Phường Ba Đình", res.matchedWard().name());
    }

    @Test
    void testBacNinhProvinceAndWard() {
        AdministrativeRegionService.AddressValidationResult res = service.validateAddress("Thành phố Bắc Ninh", "Phường Bắc Giang");
        assertTrue(res.valid(), "Phường Bắc Giang phải thuộc Thành phố Bắc Ninh");
    }

    @Test
    void testWardNotBelongToProvince() {
        // Phường Bến Nghé thuộc TP. Hồ Chí Minh, không thuộc TP. Hà Nội
        AdministrativeRegionService.AddressValidationResult res = service.validateAddress("Thành phố Hà Nội", "Phường Bến Nghé");
        assertFalse(res.valid(), "Phường Bến Nghé không được phép thuộc Hà Nội");
        assertFalse(res.errors().isEmpty());
        assertTrue(res.errors().get(0).contains("không thuộc Tỉnh / Thành phố"), "Thông báo lỗi phải chỉ rõ không thuộc tỉnh thành: " + res.errors().get(0));
    }

    @Test
    void testNonExistingProvince() {
        AdministrativeRegionService.AddressValidationResult res = service.validateAddress("Tỉnh Atlantis Không Tồn Tại", "Phường 1");
        assertFalse(res.valid());
        assertFalse(res.errors().isEmpty());
        assertTrue(res.errors().get(0).contains("không tồn tại"), "Thông báo lỗi phải chỉ rõ tỉnh không tồn tại: " + res.errors().get(0));
    }

    @Test
    void testEmptyFields() {
        AdministrativeRegionService.AddressValidationResult res = service.validateAddress("", "");
        assertFalse(res.valid());
        assertEquals(2, res.errors().size());
    }
}

