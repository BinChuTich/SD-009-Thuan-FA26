import { createRouter, createWebHistory } from 'vue-router'

import TrangChu from '@/views/TrangChu.vue'
import CuaHang from '@/views/CuaHang.vue'
import ChiTietSanPham from '@/views/ChiTietSanPham.vue'
import GioHang from '@/views/GioHang.vue'
import ThanhToan from '@/views/ThanhToan.vue'
import DonHang from '@/views/DonHang.vue'
import TraCuuDonHang from '@/views/TraCuuDonHang.vue'
import GoiY from '@/views/GoiY.vue'
import LienHe from '@/views/LienHe.vue'
import VNPayReturn from '@/views/VNPayReturn.vue'

const routes = [
  {
    path: '/',
    name: 'TrangChu',
    component: TrangChu
  },
  {
    path: '/gioi-thieu',
    redirect: '/'
  },
  {
    path: '/uu-dai',
    redirect: { path: '/', hash: '#san-pham-uu-dai' }
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
  { path: '/thanh-toan', redirect: { path: '/gio-hang', query: { step: 'checkout' } } },
  {
    path: '/don-hang',
    name: 'DonHang',
    component: DonHang
  },
  {
    path: '/tra-cuu',
    name: 'TraCuu',
    component: TraCuuDonHang
  },
  {
    path: '/tra-cuu-don-hang',
    name: 'TraCuuDonHang',
    component: TraCuuDonHang
  },
  {
    path: '/goi-y',
    redirect: '/cua-hang'
  },
  {
    path: '/lien-he',
    name: 'LienHe',
    component: LienHe
  },
  {
    path: '/thanh-toan/vnpay-return',
    name: 'VNPayReturn',
    component: VNPayReturn
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (to.hash) {
      return new Promise((resolve) => {
        setTimeout(() => {
          resolve({
            el: to.hash,
            behavior: 'smooth',
            top: 80
          })
        }, 150)
      })
    }
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

export default router
