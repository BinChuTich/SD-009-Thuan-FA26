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
    void testRejectMissingPrefixes() {
        // Chuỗi địa chính thiếu tiền tố chuẩn (Ví dụ chỉ điền "Hà Nội" hoặc "Ba Đình") bắt buộc phải tính là lỗi
        AdministrativeRegionService.AddressValidationResult res = service.validateAddress("Hà Nội", "Ba Đình");
        assertFalse(res.valid(), "Bắt buộc phải tính là lỗi khi thiếu tiền tố chuẩn Thành phố/Tỉnh và Phường/Xã");
        assertEquals(2, res.errors().size());
        assertTrue(res.errors().get(0).contains("thiếu tiền tố chuẩn"), "Lỗi tỉnh: " + res.errors().get(0));
        assertTrue(res.errors().get(1).contains("thiếu tiền tố chuẩn"), "Lỗi phường: " + res.errors().get(1));
    }

    @Test
    void testRejectAbbreviatedPrefixes() {
        // Chuỗi địa chính viết tắt tiền tố ("TP.", "P.") bắt buộc phải tính là lỗi
        AdministrativeRegionService.AddressValidationResult res = service.validateAddress("TP. Hà Nội", "P. Ba Đình");
        assertFalse(res.valid(), "Bắt buộc phải tính là lỗi khi viết tắt tiền tố");
        assertEquals(2, res.errors().size());
        assertTrue(res.errors().get(0).contains("viết tắt tiền tố"), "Lỗi viết tắt tỉnh: " + res.errors().get(0));
        assertTrue(res.errors().get(1).contains("viết tắt tiền tố"), "Lỗi viết tắt phường: " + res.errors().get(1));
    }

    @Test
    void testBacNinhProvinceAndWardExact() {
        AdministrativeRegionService.AddressValidationResult res = service.validateAddress("Thành phố Bắc Ninh", "Phường Bắc Giang");
        assertTrue(res.valid(), "Phường Bắc Giang phải thuộc Thành phố Bắc Ninh");
        assertEquals("Thành phố Bắc Ninh", res.matchedProvince().name());
        assertEquals("Phường Bắc Giang", res.matchedWard().name());
    }

    @Test
    void testBacNinhWithAlternativeStandardPrefix() {
        // "Tỉnh Bắc Ninh" hoặc "Thành phố Bắc Ninh" đều có tiền tố chuẩn hợp lệ
        AdministrativeRegionService.AddressValidationResult res = service.validateAddress("Tỉnh Bắc Ninh", "Phường Bắc Giang");
        assertTrue(res.valid(), "Phải hợp lệ với Tỉnh Bắc Ninh và Phường Bắc Giang");
        assertEquals("Thành phố Bắc Ninh", res.matchedProvince().name());
        assertEquals("Phường Bắc Giang", res.matchedWard().name());
    }

    @Test
    void testRejectProvinceMissingPrefixEvenIfWardValid() {
        AdministrativeRegionService.AddressValidationResult res = service.validateAddress("Bắc Ninh", "Phường Bắc Giang");
        assertFalse(res.valid(), "Bắc Ninh thiếu tiền tố chuẩn phải bị tính là lỗi");
        assertEquals(1, res.errors().size());
        assertTrue(res.errors().get(0).contains("thiếu tiền tố chuẩn"));
    }

    @Test
    void testRejectWardMissingPrefixEvenIfProvinceValid() {
        AdministrativeRegionService.AddressValidationResult res = service.validateAddress("Thành phố Bắc Ninh", "Bắc Giang");
        assertFalse(res.valid(), "Bắc Giang thiếu tiền tố Phường phải bị tính là lỗi");
        assertEquals(1, res.errors().size());
        assertTrue(res.errors().get(0).contains("thiếu tiền tố chuẩn"));
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
