import { createRouter, createWebHistory } from 'vue-router'
import { getCurrentUser, getRoleCode } from '@/utils/auth.js'

// Trang đăng nhập
import Login from '../views/Login.vue'

// Thống kê
import ThongKe from '../views/ThongKe.vue'

// Bán hàng
import BanHang from '../views/BanHang.vue'

// Khách hàng
import KhachHang from '../views/KhachHang/KhachHang.vue'
import ThemKhachHang from '../views/KhachHang/ThemKhachHang.vue'
import ChiTietKhachHang from '../views/KhachHang/ChiTietKhachHang.vue'

// Nhân viên
import NhanVien from '../views/NhanVien/NhanVien.vue'
import ThemNhanVien from '../views/NhanVien/ThemNhanVien.vue'
import ChiTietNhanVien from '../views/NhanVien/ChiTietNhanVien.vue'

// Hóa đơn
import DanhSachHoaDon from '../views/HoaDon/DanhSachHoaDon.vue'
import ChiTietHoaDon from '../views/HoaDon/ChiTietHoaDon.vue'
import ThemHoaDon from "@/views/HoaDon/ThemHoaDon.vue"

// Sản phẩm
import SanPham from '../views/SanPham/SanPham.vue'
import BienTheSanPham from '../views/SanPham/BienTheSanPham.vue'
import ThemSuaSanPham from '../views/SanPham/ThemSuaSanPham.vue'

// Giảm giá
import DotGiamGia from '../views/GiamGia/DotGiamGia.vue'
import PhieuGiamGia from '../views/GiamGia/PhieuGiamGia.vue'
import ChiTietPhieuGiamGia from '../views/GiamGia/ChiTietPhieuGiamGia.vue'
import TaoPhieuGiamGia from '../views/GiamGia/TaoPhieuGiamGia.vue'

// Thuộc tính
import ThuocTinh from '../views/ThuocTinh/ThuocTinh.vue'
import DanhMuc from '../views/ThuocTinh/DanhMuc.vue'
import ThuongHieu from '../views/ThuocTinh/ThuongHieu.vue'
import ChatLieu from '../views/ThuocTinh/ChatLieu.vue'
import XuatXu from '../views/ThuocTinh/XuatXu.vue'
import CoAo from '../views/ThuocTinh/CoAo.vue'
import TayAo from '../views/ThuocTinh/TayAo.vue'
import MauSac from '../views/ThuocTinh/MauSac.vue'
import KichCo from '../views/ThuocTinh/KichCo.vue'
import HoaTiet from '../views/ThuocTinh/HoaTiet.vue'

// Lịch làm việc
import QuanLyCa from '../views/LichLamViec/QuanLyCa.vue'
import XepLich from '../views/LichLamViec/XepLich.vue'
import GiaoCa from '../views/LichLamViec/GiaoCa.vue'
import LichSuHoatDong from '../views/LichLamViec/LichSuHoatDong.vue'

const routes = [
    // Trang đăng nhập công khai
    {
        path: '/login',
        name: 'Login',
        component: Login,
        meta: { public: true }
    },

    // Trang chủ điều hướng theo vai trò
    {
        path: '/',
        name: 'Root',
        redirect: to => {
            const role = getRoleCode()
            return role === 'ADMIN' ? '/thong-ke' : '/ban-hang'
        }
    },

    // 1. Thống kê (Chỉ Admin)
    {
        path: '/thong-ke',
        name: 'ThongKe',
        component: ThongKe,
        meta: { roles: ['ADMIN'] }
    },

    // 2. Bán hàng tại quầy (Cả Admin và Nhân viên)
    {
        path: '/ban-hang',
        name: 'BanHang',
        component: BanHang,
        meta: { roles: ['ADMIN', 'NV'] }
    },

    // 3. Quản lý hóa đơn (Cả Admin và Nhân viên)
    {
        path: '/hoa-don',
        name: 'DanhSachHoaDon',
        component: DanhSachHoaDon,
        meta: { roles: ['ADMIN', 'NV'] }
    },
    {
        path: '/hoa-don/them',
        name: 'ThemHoaDon',
        component: ThemHoaDon,
        meta: { roles: ['ADMIN', 'NV'] }
    },
    {
        path: '/hoa-don/:maHoaDon/xac-nhan',
        name: 'XacNhanDonHang',
        component: () => import('@/views/HoaDon/XacNhanDonHang.vue'),
        meta: { roles: ['ADMIN', 'NV'] }
    },
    {
        path: '/hoa-don/:maHoaDon',
        name: 'HoaDonChiTiet',
        component: ChiTietHoaDon,
        meta: { roles: ['ADMIN', 'NV'] }
    },

    // 4. Quản lý sản phẩm (Xem: cả 2; Thêm/Sửa: Chỉ Admin)
    {
        path: '/san-pham',
        name: 'SanPham',
        component: SanPham,
        meta: { roles: ['ADMIN', 'NV'] }
    },
    {
        path: '/bien-the-san-pham/:id?',
        name: 'BienTheSanPham',
        component: BienTheSanPham,
        meta: { roles: ['ADMIN', 'NV'] }
    },
    {
        path: '/san-pham/them',
        name: 'ThemSanPham',
        component: ThemSuaSanPham,
        meta: { roles: ['ADMIN'] }
    },
    {
        path: '/san-pham/chinh-sua/:id',
        name: 'SuaSanPham',
        component: ThemSuaSanPham,
        meta: { roles: ['ADMIN'] }
    },

    // 5. Quản lý thuộc tính (Chỉ Admin)
    {
        path: '/thuoc-tinh',
        redirect: '/thuong-hieu',
        meta: { roles: ['ADMIN'] }
    },
    {
        path: '/danh-sach-thuoc-tinh',
        redirect: '/thuong-hieu',
        meta: { roles: ['ADMIN'] }
    },
    {
        path: '/thuong-hieu',
        name: 'ThuongHieu',
        component: ThuongHieu,
        meta: { roles: ['ADMIN'] }
    },
    {
        path: '/danh-muc',
        name: 'DanhMuc',
        component: DanhMuc,
        meta: { roles: ['ADMIN'] }
    },
    {
        path: '/chat-lieu',
        name: 'ChatLieu',
        component: ChatLieu,
        meta: { roles: ['ADMIN'] }
    },
    {
        path: '/xuat-xu',
        name: 'XuatXu',
        component: XuatXu,
        meta: { roles: ['ADMIN'] }
    },
    {
        path: '/kieu-dang',
        redirect: '/xuat-xu',
        meta: { roles: ['ADMIN'] }
    },
    {
        path: '/co-ao',
        name: 'CoAo',
        component: CoAo,
        meta: { roles: ['ADMIN'] }
    },
    {
        path: '/tay-ao',
        name: 'TayAo',
        component: TayAo,
        meta: { roles: ['ADMIN'] }
    },
    {
        path: '/hoa-tiet',
        name: 'HoaTiet',
        component: HoaTiet,
        meta: { roles: ['ADMIN'] }
    },
    {
        path: '/mau-sac',
        name: 'MauSac',
        component: MauSac,
        meta: { roles: ['ADMIN'] }
    },
    {
        path: '/kich-co',
        name: 'KichCo',
        component: KichCo,
        meta: { roles: ['ADMIN'] }
    },

    // 6. Quản lý giảm giá (Chỉ Admin)
    {
        path: '/dot-giam-gia',
        name: 'DotGiamGia',
        component: DotGiamGia,
        meta: { roles: ['ADMIN'] }
    },
    {
        path: '/phieu-giam-gia',
        name: 'PhieuGiamGia',
        component: PhieuGiamGia,
        meta: { roles: ['ADMIN'] }
    },
    {
        path: '/phieu-giam-gia/tao-moi',
        name: 'TaoPhieuGiamGia',
        component: TaoPhieuGiamGia,
        meta: { roles: ['ADMIN'] }
    },
    {
        path: '/phieu-giam-gia/chi-tiet/:id',
        name: 'ChiTietPhieuGiamGia',
        component: ChiTietPhieuGiamGia,
        meta: { roles: ['ADMIN'] }
    },

    // 7. Quản lý khách hàng (Cả Admin và Nhân viên)
    {
        path: '/khach-hang',
        name: 'KhachHang',
        component: KhachHang,
        meta: { roles: ['ADMIN', 'NV'] }
    },
    {
        path: '/khach-hang/them',
        name: 'ThemKhachHang',
        component: ThemKhachHang,
        meta: { roles: ['ADMIN', 'NV'] }
    },
    {
        path: '/khach-hang/:id',
        name: 'ChiTietKhachHang',
        component: ChiTietKhachHang,
        meta: { roles: ['ADMIN', 'NV'] }
    },

    // 8. Quản lý nhân viên (Chỉ Admin)
    {
        path: '/nhan-vien',
        name: 'NhanVien',
        component: NhanVien,
        meta: { roles: ['ADMIN'] }
    },
    {
        path: '/nhan-vien/them',
        name: 'ThemNhanVien',
        component: ThemNhanVien,
        meta: { roles: ['ADMIN'] }
    },
    {
        path: '/nhan-vien/:id',
        name: 'ChiTietNhanVien',
        component: ChiTietNhanVien,
        meta: { roles: ['ADMIN'] }
    },

    // 9. Lịch làm việc (Cả Admin và Nhân viên)
    {
        path: '/lich-lam-viec',
        redirect: () => {
            return getRoleCode() === 'ADMIN' ? '/ca-lam-viec' : '/xep-lich'
        },
        meta: { roles: ['ADMIN', 'NV'] }
    },
    {
        path: '/ca-lam-viec',
        name: 'QuanLyCa',
        component: QuanLyCa,
        meta: { roles: ['ADMIN'] }
    },
    {
        path: '/xep-lich',
        name: 'XepLich',
        component: XepLich,
        meta: { roles: ['ADMIN', 'NV'] }
    },
    {
        path: '/giao-ca',
        redirect: '/lich-su-ca',
        meta: { roles: ['ADMIN', 'NV'] }
    },
    {
        path: '/lich-su-ca',
        name: 'LichSuHoatDong',
        component: LichSuHoatDong,
        meta: { roles: ['ADMIN', 'NV'] }
    },
    {
        path: '/tin-nhan',
        name: 'QuanLyChat',
        component: () => import('@/views/Chat/QuanLyChat.vue'),
        meta: { roles: ['ADMIN', 'NV'] }
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// Navigation Guard kiểm tra quyền đăng nhập và phân quyền route
router.beforeEach((to, from, next) => {
    const user = getCurrentUser()
    const isPublic = to.meta && to.meta.public === true

    // 1. Chưa đăng nhập mà truy cập trang yêu cầu đăng nhập
    if (!user && !isPublic) {
        return next({ path: '/login', query: { redirect: to.fullPath } })
    }

    // 2. Đã đăng nhập mà cố vào trang /login
    if (user && to.path === '/login') {
        const role = getRoleCode()
        if (role === 'ADMIN') return next('/thong-ke')
        return next('/ban-hang')
    }

    // 3. Kiểm tra vai trò được phép truy cập
    if (user && to.meta && to.meta.roles && to.meta.roles.length > 0) {
        const role = getRoleCode()
        if (!to.meta.roles.includes(role)) {
            // Không có quyền: đưa về trang Bán hàng cho Nhân viên
            return next('/ban-hang')
        }
    }

    next()
})

export default router
