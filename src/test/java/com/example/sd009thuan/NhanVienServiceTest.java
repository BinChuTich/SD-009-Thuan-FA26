package com.example.sd009thuan;

import com.example.sd009thuan.entity.NhanVien;
import com.example.sd009thuan.repository.NhanVienRepository;
import com.example.sd009thuan.service.NhanVienService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class NhanVienServiceTest {

    @Test
    void testPrefixNguyenThiVanAnh() {
        String prefix = NhanVienService.generateEmployeePrefix("Nguyễn Thị Vân Anh");
        assertEquals("anhntv", prefix);
    }

    @Test
    void testPrefixLowerCase() {
        String prefix = NhanVienService.generateEmployeePrefix("nguyễn thị vân anh");
        assertEquals("anhntv", prefix);
    }

    @Test
    void testPrefixD() {
        String prefix = NhanVienService.generateEmployeePrefix("Đặng Văn Đức");
        assertEquals("ducdv", prefix);
    }

    @Test
    void testPrefixTranVanB() {
        String prefix = NhanVienService.generateEmployeePrefix("Trần Văn B");
        assertEquals("btv", prefix);
    }

    @Test
    void testGenerateEmployeeCodeIncrement() {
        NhanVienRepository repo = Mockito.mock(NhanVienRepository.class);
        List<NhanVien> list = new ArrayList<>();
        
        NhanVien nv1 = new NhanVien();
        nv1.setMaNhanVien("anhntv1");
        list.add(nv1);

        NhanVien nv2 = new NhanVien();
        nv2.setMaNhanVien("anhntv2");
        list.add(nv2);

        when(repo.findAll()).thenReturn(list);

        NhanVienService service = new NhanVienService(repo, null, null, null);
        String code = service.generateEmployeeCode("Nguyễn Thị Vân Anh");

        assertEquals("anhntv3", code);
    }

    @Test
    void testGenerateEmployeeCodeFirstTime() {
        NhanVienRepository repo = Mockito.mock(NhanVienRepository.class);
        when(repo.findAll()).thenReturn(List.of());

        NhanVienService service = new NhanVienService(repo, null, null, null);
        String code = service.generateEmployeeCode("Nguyễn Thị Vân Anh");

        assertEquals("anhntv1", code);
    }
}
