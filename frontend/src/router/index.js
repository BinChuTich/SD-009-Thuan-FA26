import { createRouter, createWebHistory } from 'vue-router'

// 1. Các trang chính nằm ở thư mục gốc /views
import ThongKe from '../views/ThongKe.vue'
import BanHang from '../views/BanHang.vue'
import KhachHang from '../views/KhachHang.vue'
import NhanVien from '../views/NhanVien.vue'

// 2. Thư mục Hóa đơn (views/HoaDon)
import DanhSachHoaDon from '../views/HoaDon/DanhSachHoaDon.vue'
import ChiTietHoaDon from '../views/HoaDon/ChiTietHoaDon.vue'

// 3. Thư mục Sản phẩm (views/SanPham)
import SanPham from '../views/SanPham/SanPham.vue'
import BienTheSanPham from '../views/SanPham/BienTheSanPham.vue'

// 4. Thư mục Giảm giá (views/GiamGia)
import DotGiamGia from '../views/GiamGia/DotGiamGia.vue'
import PhieuGiamGia from '../views/GiamGia/PhieuGiamGia.vue'

// 5. Thư mục Thuộc tính sản phẩm (views/ThuocTinh)
import DanhMuc from '../views/ThuocTinh/DanhMuc.vue'
import ThuongHieu from '../views/ThuocTinh/ThuongHieu.vue'
import ChatLieu from '../views/ThuocTinh/ChatLieu.vue'
import XuatXu from '../views/ThuocTinh/XuatXu.vue'
import CoAo from '../views/ThuocTinh/CoAo.vue'
import TayAo from '../views/ThuocTinh/TayAo.vue'
import MauSac from '../views/ThuocTinh/MauSac.vue'
import KichCo from '../views/ThuocTinh/KichCo.vue'

const routes = [
    // --- Thống kê (Mặc định trang chủ) ---
    { path: '/', name: 'ThongKeDefault', component: ThongKe },
    { path: '/thong-ke', name: 'ThongKe', component: ThongKe },

    // --- Bán hàng tại quầy ---
    { path: '/ban-hang', name: 'BanHang', component: BanHang },

    // --- Quản lý hóa đơn ---
    // Cập nhật route hóa đơn
    {
        path: '/hoa-don',
        component: DanhSachHoaDon
    },
    {
        path: '/hoa-don/:ma', // Dùng dynamic param :ma để chuyển trang kèm mã hóa đơn
        component: ChiTietHoaDon
    },

    // --- Quản lý sản phẩm ---
    { path: '/san-pham', name: 'SanPham', component: SanPham },
    { path: '/bien-the-san-pham', name: 'BienTheSanPham', component: BienTheSanPham },

    // --- Thuộc tính sản phẩm ---
    { path: '/danh-muc', name: 'DanhMuc', component: DanhMuc },
    { path: '/thuong-hieu', name: 'ThuongHieu', component: ThuongHieu },
    { path: '/chat-lieu', name: 'ChatLieu', component: ChatLieu },
    { path: '/xuat-xu', name: 'XuatXu', component: XuatXu },
    { path: '/co-ao', name: 'CoAo', component: CoAo },
    { path: '/tay-ao', name: 'TayAo', component: TayAo },
    { path: '/mau-sac', name: 'MauSac', component: MauSac },
    { path: '/kich-co', name: 'KichCo', component: KichCo },

    // --- Quản lý giảm giá ---
    { path: '/dot-giam-gia', name: 'DotGiamGia', component: DotGiamGia },
    { path: '/phieu-giam-gia', name: 'PhieuGiamGia', component: PhieuGiamGia },

    // --- Tài khoản & Nhân sự ---
    { path: '/khach-hang', name: 'KhachHang', component: KhachHang },
    { path: '/nhan-vien', name: 'NhanVien', component: NhanVien }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router