<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import DauTrang from '@/components/BocCuc/DauTrang.vue'
import ChanTrang from '@/components/BocCuc/ChanTrang.vue'
import HopChatTuVan from '@/components/BocCuc/HopChatTuVan.vue'
import ModalThongBaoXacNhan from '@/components/DungChung/ModalThongBaoXacNhan.vue'
import BannerTrang from '@/components/BocCuc/BannerTrang.vue'

const route = useRoute()
const pageBanners = {
  CuaHang: ['CỬA HÀNG FF T-SHIRT', 'Khám phá những mẫu áo mới nhất dành cho phong cách của bạn'],
  ChiTietSanPham: ['CHI TIẾT SẢN PHẨM', 'Thông tin sản phẩm, màu sắc, kích thước và ưu đãi hiện có'],
  GioHang: ['GIỎ HÀNG CỦA BẠN', 'Kiểm tra sản phẩm và hoàn tất thông tin mua hàng'],
  DonHang: ['ĐƠN HÀNG CỦA TÔI', 'Theo dõi trạng thái và thông tin các đơn hàng của bạn'],
  TraCuu: ['TRA CỨU ĐƠN HÀNG', 'Kiểm tra hành trình đơn hàng nhanh chóng và chính xác'],
  TraCuuDonHang: ['TRA CỨU ĐƠN HÀNG', 'Kiểm tra hành trình đơn hàng nhanh chóng và chính xác'],
  LienHe: ['LIÊN HỆ VỚI FF T-SHIRT', 'Chúng tôi luôn lắng nghe và sẵn sàng giải đáp mọi thắc mắc của quý khách hàng'],
  GoiY: ['GỢI Ý THÔNG MINH', 'Trả lời vài câu hỏi để tìm ra mẫu áo phù hợp với bạn']
}
const currentPageBanner = computed(() => pageBanners[route.name] || null)
</script>

<template>
  <div class="client-app-shell">
    <DauTrang />
    <main class="client-main-content">
      <BannerTrang
        v-if="currentPageBanner"
        :title="currentPageBanner[0]"
        :subtitle="currentPageBanner[1]"
      />
      <router-view />
    </main>
    <ChanTrang />
    <HopChatTuVan />
    <ModalThongBaoXacNhan />
  </div>
</template>

<style>
html {
  scroll-behavior: smooth;
  width: 100%;
  /* `hidden` tạo scroll container và làm header position: sticky mất tác dụng. */
  overflow-x: clip;
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
  padding: 0;
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: #1e293b;
  background-color: #f8fafc;
  width: 100%;
  min-width: 320px;
  overflow-x: clip;
}

img,
svg,
video,
canvas {
  max-width: 100%;
}

img,
video {
  height: auto;
}

input,
select,
textarea,
button {
  max-width: 100%;
}

textarea {
  resize: vertical;
}

p,
h1,
h2,
h3,
h4 {
  overflow-wrap: break-word;
}

button, input, select, textarea {
  font-family: inherit;
}

a {
  color: inherit;
}

.client-app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.client-main-content {
  flex: 1;
  width: 100%;
  min-width: 0;
  overflow-x: clip;
}

/* Container chung toàn hệ thống: MÀN HÌNH RỘNG RA & ĐÁP ỨNG MỌI THIẾT BỊ */
.container-client {
  width: 100%;
  max-width: 1440px;
  margin-left: auto;
  margin-right: auto;
  padding-left: 1.5rem;
  padding-right: 1.5rem;
}

@media (min-width: 1600px) {
  .container-client {
    max-width: 1540px;
    padding-left: 2rem;
    padding-right: 2rem;
  }
}

@media (max-width: 1024px) {
  .container-client {
    padding-left: 1.25rem;
    padding-right: 1.25rem;
  }
}

@media (max-width: 640px) {
  .container-client {
    padding-left: 1rem;
    padding-right: 1rem;
  }

  /* Tránh iOS tự phóng trang khi focus trường nhập liệu. */
  input,
  select,
  textarea {
    font-size: 16px;
  }

  [class*="modal-backdrop"],
  [class*="modal-overlay"] {
    padding: 10px !important;
  }

  [class*="modal-card"],
  [class*="modal-content"],
  [class*="modal-box"] {
    max-width: calc(100vw - 20px) !important;
    max-height: calc(100dvh - 20px) !important;
  }

  .detail-top-nav,
  .list-hero-header,
  .card-foot-row,
  .foot-actions-and-total,
  .catalog-header-bar,
  .voucher-warning-actions {
    flex-wrap: wrap;
    gap: 10px;
  }

  .detail-top-nav > *,
  .list-hero-header > *,
  .catalog-header-bar > * {
    min-width: 0;
  }

  .detail-top-nav {
    padding: 12px !important;
  }

  .order-id-status {
    width: 100%;
    flex-wrap: wrap;
  }

  .order-stepper-box {
    padding: 16px 10px !important;
  }

  .stepper-track {
    scroll-snap-type: x proximity;
    scrollbar-width: thin;
  }

  .step-point {
    scroll-snap-align: center;
  }

  .loading-state,
  .empty-state-box {
    padding: 36px 14px !important;
  }

  .pagination-wrapper {
    max-width: 100%;
    overflow-x: auto;
    justify-content: flex-start !important;
    padding-bottom: 4px;
  }

  .pagination-wrapper .page-btn {
    flex: 0 0 auto;
  }

  /* Chừa thanh hành động checkout cố định giống ứng dụng mua sắm. */
  body:has(.checkout-page-wrap) .chat-widget-root,
  body:has(.checkout-step-container) .chat-widget-root {
    bottom: 70px !important;
  }

  body:has(.checkout-page-wrap) .chat-floating-btn,
  body:has(.checkout-step-container) .chat-floating-btn {
    width: 44px;
    height: 44px;
    padding: 0;
    justify-content: center;
    border-radius: 50%;
  }

  body:has(.checkout-page-wrap) .chat-floating-btn > span:not(.client-unread-badge),
  body:has(.checkout-step-container) .chat-floating-btn > span:not(.client-unread-badge) {
    display: none;
  }
}

@media (max-width: 390px) {
  .container-client {
    padding-left: 0.7rem;
    padding-right: 0.7rem;
  }
}
</style>

