import { createRouter, createWebHistory } from 'vue-router'

// Thống kê
import ThongKe from '../views/ThongKe.vue'

// Bán hàng
import BanHang from '../views/BanHang.vue'

// Khách hàng
import KhachHang from '../views/KhachHang.vue'

// Nhân viên
import NhanVien from '../views/NhanVien.vue'

// Hóa đơn
import DanhSachHoaDon from '../views/HoaDon/DanhSachHoaDon.vue'
import ChiTietHoaDon from '../views/HoaDon/ChiTietHoaDon.vue'
import ThemHoaDon from "@/views/HoaDon/ThemHoaDon.vue";

// Sản phẩm
import SanPham from '../views/SanPham/SanPham.vue'
import BienTheSanPham from '../views/SanPham/BienTheSanPham.vue'

// Giảm giá
import DotGiamGia from '../views/GiamGia/DotGiamGia.vue'
import PhieuGiamGia from '../views/GiamGia/PhieuGiamGia.vue'

// Thuộc tính
import DanhMuc from '../views/ThuocTinh/DanhMuc.vue'
import ThuongHieu from '../views/ThuocTinh/ThuongHieu.vue'
import ChatLieu from '../views/ThuocTinh/ChatLieu.vue'
import XuatXu from '../views/ThuocTinh/XuatXu.vue'
import CoAo from '../views/ThuocTinh/CoAo.vue'
import TayAo from '../views/ThuocTinh/TayAo.vue'
import MauSac from '../views/ThuocTinh/MauSac.vue'
import KichCo from '../views/ThuocTinh/KichCo.vue'

const routes = [
    // Thống kê
    {
        path: '/',
        name: 'ThongKeDefault',
        component: ThongKe
    },
    {
        path: '/thong-ke',
        name: 'ThongKe',
        component: ThongKe
    },

    // Bán hàng
    {
        path: '/ban-hang',
        name: 'BanHang',
        component: BanHang
    },

    // =========================
    // HÓA ĐƠN
    // =========================
    {
        path: '/hoa-don',
        name: 'DanhSachHoaDon',
        component: DanhSachHoaDon
    },
    {
        path: '/hoa-don/them',
        name: 'ThemHoaDon',
        component: () => import('@/views/hoaDon/ThemHoaDon.vue')
    },
    {
        path: '/hoa-don/:maHoaDon/xac-nhan',
        name: 'XacNhanDonHang',
        component: () => import('@/views/hoaDon/XacNhanDonHang.vue')
    },
    {
        path: '/hoa-don/:maHoaDon',
        name: 'HoaDonChiTiet',
        component: ChiTietHoaDon
    },

    // Sản phẩm
    {
        path: '/san-pham',
        name: 'SanPham',
        component: SanPham
    },
    {
        path: '/bien-the-san-pham',
        name: 'BienTheSanPham',
        component: BienTheSanPham
    },

    // Thuộc tính
    {
        path: '/danh-muc',
        name: 'DanhMuc',
        component: DanhMuc
    },
    {
        path: '/thuong-hieu',
        name: 'ThuongHieu',
        component: ThuongHieu
    },
    {
        path: '/chat-lieu',
        name: 'ChatLieu',
        component: ChatLieu
    },
    {
        path: '/xuat-xu',
        name: 'XuatXu',
        component: XuatXu
    },
    {
        path: '/co-ao',
        name: 'CoAo',
        component: CoAo
    },
    {
        path: '/tay-ao',
        name: 'TayAo',
        component: TayAo
    },
    {
        path: '/mau-sac',
        name: 'MauSac',
        component: MauSac
    },
    {
        path: '/kich-co',
        name: 'KichCo',
        component: KichCo
    },

    // Giảm giá
    {
        path: '/dot-giam-gia',
        name: 'DotGiamGia',
        component: DotGiamGia
    },
    {
        path: '/phieu-giam-gia',
        name: 'PhieuGiamGia',
        component: PhieuGiamGia
    },

    // Khách hàng
    {
        path: '/khach-hang',
        name: 'KhachHang',
        component: KhachHang
    },

    // Nhân viên
    {
        path: '/nhan-vien',
        name: 'NhanVien',
        component: NhanVien
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router