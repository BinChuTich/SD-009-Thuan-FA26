import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import './style.css'

// 1. Thư mục gốc views
import ThongKe from './views/ThongKe.vue'
import BanHang from './views/BanHang.vue'
import KhachHang from './views/KhachHang.vue'
import NhanVien from './views/NhanVien.vue'

// 2. Thư mục views/HoaDon
import DanhSachHoaDon from './views/HoaDon/DanhSachHoaDon.vue'
import ChiTietHoaDon from './views/HoaDon/ChiTietHoaDon.vue'

// 3. Thư mục views/SanPham
import SanPham from './views/SanPham/SanPham.vue'
import BienTheSanPham from './views/SanPham/BienTheSanPham.vue'

// 4. Thư mục views/ThuocTinh
import DanhMuc from './views/ThuocTinh/DanhMuc.vue'
import ThuongHieu from './views/ThuocTinh/ThuongHieu.vue'
import ChatLieu from './views/ThuocTinh/ChatLieu.vue'
import XuatXu from './views/ThuocTinh/XuatXu.vue'
import CoAo from './views/ThuocTinh/CoAo.vue'
import TayAo from './views/ThuocTinh/TayAo.vue'
import MauSac from './views/ThuocTinh/MauSac.vue'
import KichCo from './views/ThuocTinh/KichCo.vue'

// 5. Thư mục views/GiamGia
import DotGiamGia from './views/GiamGia/DotGiamGia.vue'
import PhieuGiamGia from './views/GiamGia/PhieuGiamGia.vue'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        // Trang chủ / Thống kê
        { path: '/', component: ThongKe },
        { path: '/thong-ke', component: ThongKe },

        // Bán hàng tại quầy
        { path: '/ban-hang', component: BanHang },

        // Quản lý hóa đơn
        { path: '/hoa-don', component: DanhSachHoaDon },
        { path: '/hoa-don/chi-tiet', component: ChiTietHoaDon },

        // Quản lý sản phẩm
        { path: '/san-pham', component: SanPham },
        { path: '/bien-the-san-pham', component: BienTheSanPham },

        // Thuộc tính sản phẩm
        { path: '/danh-muc', component: DanhMuc },
        { path: '/thuong-hieu', component: ThuongHieu },
        { path: '/chat-lieu', component: ChatLieu },
        { path: '/xuat-xu', component: XuatXu },
        { path: '/co-ao', component: CoAo },
        { path: '/tay-ao', component: TayAo },
        { path: '/mau-sac', component: MauSac },
        { path: '/kich-co', component: KichCo },

        // Quản lý giảm giá
        { path: '/dot-giam-gia', component: DotGiamGia },
        { path: '/phieu-giam-gia', component: PhieuGiamGia },

        // Quản lý khách hàng & Nhân viên
        { path: '/khach-hang', component: KhachHang },
        { path: '/nhan-vien', component: NhanVien }
    ]
})

createApp(App)
    .use(router)
    .mount('#app')