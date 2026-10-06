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

// Sản phẩm
import SanPham from '../views/SanPham/SanPham.vue'
import BienTheSanPham from '../views/SanPham/BienTheSanPham.vue'
import ThemSuaSanPham from '../views/SanPham/ThemSuaSanPham.vue'

// Giảm giá
import DotGiamGia from '../views/GiamGia/DotGiamGia.vue'
import PhieuGiamGia from '../views/GiamGia/PhieuGiamGia.vue'

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
        path: '/san-pham/them',
        name: 'ThemSanPham',
        component: ThemSuaSanPham
    },
    {
        path: '/san-pham/chinh-sua/:id',
        name: 'SuaSanPham',
        component: ThemSuaSanPham
    },
    {
        path: '/bien-the-san-pham/:id?',
        name: 'BienTheSanPham',
        component: BienTheSanPham
    },

    // =========================
    // THUỘC TÍNH SẢN PHẨM
    // =========================
    {
        path: '/thuoc-tinh',
        name: 'ThuocTinh',
        component: ThuocTinh
    },
    {
        path: '/danh-sach-thuoc-tinh',
        redirect: '/thuoc-tinh'
    },
    {
        path: '/chat-lieu',
        redirect: '/thuoc-tinh?tab=chat-lieu'
    },
    {
        path: '/mau-sac',
        redirect: '/thuoc-tinh?tab=mau-sac'
    },
    {
        path: '/kich-co',
        redirect: '/thuoc-tinh?tab=kich-co'
    },
    {
        path: '/danh-muc',
        redirect: '/thuoc-tinh?tab=danh-muc'
    },
    {
        path: '/thuong-hieu',
        redirect: '/thuoc-tinh?tab=thuong-hieu'
    },
    {
        path: '/xuat-xu',
        redirect: '/thuoc-tinh?tab=xuat-xu'
    },
    {
        path: '/co-ao',
        redirect: '/thuoc-tinh?tab=co-ao'
    },
    {
        path: '/tay-ao',
        redirect: '/thuoc-tinh?tab=tay-ao'
    },
    {
        path: '/hoa-tiet',
        redirect: '/thuoc-tinh?tab=hoa-tiet'
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