package com.example.sd009thuan;

import com.example.sd009thuan.dto.NhanVienRequest;
import com.example.sd009thuan.entity.NhanVien;
import com.example.sd009thuan.repository.NhanVienRepository;
import com.example.sd009thuan.service.NhanVienService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

        NhanVienService service = new NhanVienService(repo, null, null, null, null, null);
        String code = service.generateEmployeeCode("Nguyễn Thị Vân Anh");

        assertEquals("anhntv3", code);
    }

    @Test
    void testGenerateEmployeeCodeFirstTime() {
        NhanVienRepository repo = Mockito.mock(NhanVienRepository.class);
        when(repo.findAll()).thenReturn(List.of());

        NhanVienService service = new NhanVienService(repo, null, null, null, null, null);
        String code = service.generateEmployeeCode("Nguyễn Thị Vân Anh");

        assertEquals("anhntv1", code);
    }

    @Test
    void testActiveUserGuardCannotToggleSelf() {
        NhanVienRepository repo = Mockito.mock(NhanVienRepository.class);
        NhanVien nv = new NhanVien();
        nv.setId(10L);
        nv.setTrangThai(1); // Đang hoạt động
        when(repo.findById(10L)).thenReturn(Optional.of(nv));

        NhanVienService service = new NhanVienService(repo, null, null, null, null, null);

        // Đang là user id=10 mà cố gắng toggle chính mình sang ngừng hoạt động -> Bị chặn
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            service.toggleStatus(10L, 10L);
        });

        assertEquals("Bạn không thể chuyển trạng thái ngừng hoạt động đối với tài khoản đang đăng nhập hệ thống", ex.getMessage());
    }

    @Test
    void testActiveUserGuardCannotDeactivateSelf() {
        NhanVienRepository repo = Mockito.mock(NhanVienRepository.class);
        NhanVienService service = new NhanVienService(repo, null, null, null, null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            service.deactivate(10L, 10L);
        });

        assertEquals("Bạn không thể chuyển trạng thái ngừng hoạt động đối với tài khoản đang đăng nhập hệ thống", ex.getMessage());
    }

    @Test
    void testActiveUserGuardCannotUpdateSelfToInactive() {
        NhanVienRepository repo = Mockito.mock(NhanVienRepository.class);
        NhanVienService service = new NhanVienService(repo, null, null, null, null, null);

        NhanVienRequest req = new NhanVienRequest(
                "NV001",
                "admin",
                "Nguyễn Văn Admin",
                null,
                "admin@ffshirt.com",
                "0901234567",
                null,
                true,
                LocalDate.of(1990, 1, 1),
                "Thành phố Hà Nội",
                "Phường Ba Đình",
                "Số 1",
                1L,
                0 // Cố gắng chuyển sang ngừng hoạt động
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            service.update(10L, req, null, 10L);
        });

        assertEquals("Bạn không thể chuyển trạng thái ngừng hoạt động đối với tài khoản đang đăng nhập hệ thống", ex.getMessage());
    }

    @Test
    void testAdminCannotToggleStatus() {
        NhanVienRepository repo = Mockito.mock(NhanVienRepository.class);
        NhanVien nv = new NhanVien();
        nv.setId(5L);
        nv.setTrangThai(1);
        com.example.sd009thuan.entity.VaiTro role = new com.example.sd009thuan.entity.VaiTro();
        role.setMaVaiTro("ADMIN");
        role.setTenVaiTro("Quản trị viên");
        nv.setIdVaiTro(role);

        when(repo.findById(5L)).thenReturn(Optional.of(nv));

        NhanVienService service = new NhanVienService(repo, null, null, null, null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            service.toggleStatus(5L);
        });

        assertEquals("Không thể thay đổi trạng thái đối với tài khoản Quản trị viên", ex.getMessage());
    }

    @Test
    void testAdminCannotDeactivate() {
        NhanVienRepository repo = Mockito.mock(NhanVienRepository.class);
        NhanVien nv = new NhanVien();
        nv.setId(5L);
        nv.setTrangThai(1);
        com.example.sd009thuan.entity.VaiTro role = new com.example.sd009thuan.entity.VaiTro();
        role.setMaVaiTro("ADMIN");
        role.setTenVaiTro("Quản trị viên");
        nv.setIdVaiTro(role);

        when(repo.findById(5L)).thenReturn(Optional.of(nv));

        NhanVienService service = new NhanVienService(repo, null, null, null, null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            service.deactivate(5L);
        });

        assertEquals("Không thể chuyển trạng thái ngừng hoạt động đối với tài khoản Quản trị viên", ex.getMessage());
    }

    @Test
    void testAdminCannotUpdateToInactive() {
        NhanVienRepository repo = Mockito.mock(NhanVienRepository.class);
        NhanVien nv = new NhanVien();
        nv.setId(5L);
        nv.setTrangThai(1);
        com.example.sd009thuan.entity.VaiTro role = new com.example.sd009thuan.entity.VaiTro();
        role.setMaVaiTro("ADMIN");
        role.setTenVaiTro("Quản trị viên");
        nv.setIdVaiTro(role);

        when(repo.findById(5L)).thenReturn(Optional.of(nv));

        NhanVienService service = new NhanVienService(repo, null, null, null, null, null);

        NhanVienRequest req = new NhanVienRequest(
                "NV001", "admin", "Nguyễn Văn Admin", null,
                "admin@ffshirt.com", "0901234567", null, true,
                LocalDate.of(1990, 1, 1), "Thành phố Hà Nội",
                "Phường Ba Đình", "Số 1", null, 0
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            service.update(5L, req, null);
        });

        assertEquals("Không thể chuyển trạng thái ngừng hoạt động đối với tài khoản Quản trị viên", ex.getMessage());
    }
}
