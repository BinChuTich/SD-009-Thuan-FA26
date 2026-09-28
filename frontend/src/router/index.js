import { createRouter, createWebHistory } from 'vue-router'

import TrangChu from '../views/TrangChu.vue'
import BanHang from '../views/BanHang.vue'
import HoaDon from '../views/HoaDon.vue'
import SanPham from '../views/SanPham.vue'
import KhachHang from '../views/KhachHang.vue'
import NhanVien from '../views/NhanVien.vue'
import KhuyenMai from '../views/KhuyenMai.vue'
import ThongKe from '../views/ThongKe.vue'

const routes = [
    { path: '/', component: TrangChu },
    { path: '/ban-hang', component: BanHang },
    { path: '/hoa-don', component: HoaDon },
    { path: '/san-pham', component: SanPham },
    { path: '/khach-hang', component: KhachHang },
    { path: '/nhan-vien', component: NhanVien },
    { path: '/khuyen-mai', component: KhuyenMai },
    { path: '/thong-ke', component: ThongKe }
]

export default createRouter({
    history: createWebHistory(),
    routes
})
