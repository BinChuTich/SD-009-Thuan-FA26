import { createRouter, createWebHistory } from 'vue-router'

import TrangChu from '@/views/TrangChu.vue'
import GioiThieu from '@/views/GioiThieu.vue'
import CuaHang from '@/views/CuaHang.vue'
import ChiTietSanPham from '@/views/ChiTietSanPham.vue'
import GioHang from '@/views/GioHang.vue'
import ThanhToan from '@/views/ThanhToan.vue'
import DonHang from '@/views/DonHang.vue'
import TraCuuDonHang from '@/views/TraCuuDonHang.vue'
import GoiY from '@/views/GoiY.vue'
import LienHe from '@/views/LienHe.vue'

const routes = [
  {
    path: '/',
    name: 'TrangChu',
    component: TrangChu
  },
  {
    path: '/gioi-thieu',
    name: 'GioiThieu',
    component: GioiThieu
  },
  {
    path: '/cua-hang',
    name: 'CuaHang',
    component: CuaHang
  },
  {
    path: '/san-pham/:id',
    name: 'ChiTietSanPham',
    component: ChiTietSanPham
  },
  {
    path: '/gio-hang',
    name: 'GioHang',
    component: GioHang
  },
  {
    path: '/thanh-toan',
    name: 'ThanhToan',
    component: ThanhToan
  },
  {
    path: '/don-hang',
    name: 'DonHang',
    component: DonHang
  },
  {
    path: '/tra-cuu-don-hang',
    name: 'TraCuuDonHang',
    component: TraCuuDonHang
  },
  {
    path: '/goi-y',
    name: 'GoiY',
    component: GoiY
  },
  {
    path: '/lien-he',
    name: 'LienHe',
    component: LienHe
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

export default router
