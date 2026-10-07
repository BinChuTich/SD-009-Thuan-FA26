import { createRouter, createWebHistory } from 'vue-router'

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
import ThemHoaDon from "@/views/HoaDon/ThemHoaDon.vue";

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
    {
        path: '/phieu-giam-gia/tao-moi',
        name: 'TaoPhieuGiamGia',
        component: TaoPhieuGiamGia
    },
    {
        path: '/phieu-giam-gia/chi-tiet/:id',
        name: 'ChiTietPhieuGiamGia',
        component: ChiTietPhieuGiamGia
    },

    // Khách hàng
    {
        path: '/khach-hang',
        name: 'KhachHang',
        component: KhachHang
    },
    {
        path: '/khach-hang/them',
        name: 'ThemKhachHang',
        component: ThemKhachHang
    },
    {
        path: '/khach-hang/:id',
        name: 'ChiTietKhachHang',
        component: ChiTietKhachHang
    },

    // Nhân viên
    {
        path: '/nhan-vien',
        name: 'NhanVien',
        component: NhanVien
    },
    {
        path: '/nhan-vien/them',
        name: 'ThemNhanVien',
        component: ThemNhanVien
    },
    {
        path: '/nhan-vien/:id',
        name: 'ChiTietNhanVien',
        component: ChiTietNhanVien
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
