<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'

const CHANNEL_NAME = 'fft-pos-customer-display'
const STORAGE_KEY = 'fft-pos-customer-display-v1'
const snapshot = ref(null)
let channel = null
let thankYouTimer = null
let thankYouLockedUntil = 0

const applySnapshot = (payload) => {
  if (!payload) return
  if (payload.viewState === 'COMPLETED') {
    thankYouLockedUntil = Number(payload.expiresAt || (Date.now() + 5000))
    snapshot.value = payload
    if (thankYouTimer) clearTimeout(thankYouTimer)
    thankYouTimer = setTimeout(() => {
      thankYouLockedUntil = 0
      readStoredSnapshot()
    }, Math.max(0, thankYouLockedUntil - Date.now()))
    return
  }
  if (Date.now() < thankYouLockedUntil) return
  snapshot.value = payload
}

const readStoredSnapshot = () => {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored) applySnapshot(JSON.parse(stored))
  } catch (e) {
    console.warn('Không đọc được dữ liệu màn hình khách hàng:', e)
  }
}

const handleStorage = (event) => {
  if (event.key === STORAGE_KEY && event.newValue) {
    try { applySnapshot(JSON.parse(event.newValue)) } catch (e) {}
  }
}

onMounted(() => {
  readStoredSnapshot()
  if ('BroadcastChannel' in window) {
    channel = new BroadcastChannel(CHANNEL_NAME)
    channel.onmessage = (event) => {
      if (event.data?.type === 'POS_SNAPSHOT' || event.data?.type === 'ORDER_COMPLETED') {
        applySnapshot(event.data.payload)
      }
    }
    channel.postMessage({ type: 'REQUEST_SNAPSHOT' })
  }
  window.addEventListener('storage', handleStorage)
})

onUnmounted(() => {
  channel?.close()
  if (thankYouTimer) clearTimeout(thankYouTimer)
  window.removeEventListener('storage', handleStorage)
})

const items = computed(() => Array.isArray(snapshot.value?.items) ? snapshot.value.items : [])
const isThankYou = computed(() => snapshot.value?.viewState === 'COMPLETED')
const hasOrder = computed(() => items.value.length > 0)
const formatMoney = (value) => `${Number(value || 0).toLocaleString('vi-VN')} đ`

const paymentLabel = computed(() => {
  const method = snapshot.value?.paymentMethod
  if (method === 'CHUYEN_KHOAN') return 'Chuyển khoản'
  if (method === 'KET_HOP') return 'Tiền mặt + Chuyển khoản'
  return 'Tiền mặt'
})
</script>

<template>
  <main class="customer-display">
    <header class="display-header">
      <div class="brand-mark">FF</div>
      <div class="brand-copy">
        <h1>FF T-SHIRT</h1>
        <p>Thời trang áo phông hiện đại</p>
      </div>
      <div class="invoice-meta" v-if="hasOrder">
        <span>HÓA ĐƠN</span>
        <strong>{{ snapshot?.invoiceCode || 'Đang tạo' }}</strong>
      </div>
    </header>

    <section v-if="isThankYou" class="thank-you-state">
      <div class="thank-you-check">✓</div>
      <span>THANH TOÁN THÀNH CÔNG</span>
      <h2>Cảm ơn quý khách!</h2>
      <p v-if="snapshot?.customerName">Cảm ơn <strong>{{ snapshot.customerName }}</strong> đã mua sắm tại FF T-Shirt.</p>
      <p v-else>Cảm ơn quý khách đã mua sắm tại FF T-Shirt.</p>
      <div class="thank-you-receipt">
        <div><span>Mã hóa đơn</span><strong>{{ snapshot?.invoiceCode }}</strong></div>
        <div><span>Tổng thanh toán</span><strong>{{ formatMoney(snapshot?.finalTotal) }}</strong></div>
      </div>
      <small>{{ snapshot?.isDelivery ? 'Đơn hàng sẽ được chuẩn bị và giao đến quý khách trong thời gian sớm nhất.' : 'Chúc quý khách một ngày thật vui vẻ và hẹn gặp lại!' }}</small>
    </section>

    <section v-else-if="!hasOrder" class="welcome-state">
      <div class="welcome-icon">👕</div>
      <h2>Xin chào quý khách!</h2>
      <p>Sản phẩm và thông tin thanh toán sẽ được hiển thị tại đây.</p>
      <div class="waiting-pulse"><i></i><i></i><i></i></div>
    </section>

    <section v-else class="display-content">
      <div class="products-panel">
        <div class="panel-heading">
          <div>
            <span class="eyebrow">SẢN PHẨM ĐÃ CHỌN</span>
            <h2>{{ items.length }} sản phẩm · {{ snapshot?.totalQuantity || 0 }} món</h2>
          </div>
          <div v-if="snapshot?.customerName" class="customer-greeting">
            Xin chào, <strong>{{ snapshot.customerName }}</strong>
          </div>
        </div>

        <div class="product-list">
          <article v-for="(item, index) in items" :key="item.key || index" class="product-row">
            <div class="product-image-wrap">
              <img :src="item.image" :alt="item.name" class="product-image" />
            </div>
            <div class="product-main">
              <div class="product-topline">
                <h3>{{ item.name }}</h3>
                <div class="product-price-block">
                  <strong class="line-total">{{ formatMoney(item.lineTotal) }}</strong>
                  <del v-if="item.hasDiscount && item.originalPrice > item.price" class="original-line-total">
                    {{ formatMoney(item.originalPrice * item.quantity) }}
                  </del>
                </div>
              </div>
              <div class="product-details">
                <span>{{ item.color || 'Mặc định' }} / {{ item.size || 'Freesize' }}</span>
                <span>{{ item.quantity }} × {{ formatMoney(item.price) }}</span>
              </div>
              <div v-if="item.hasDiscount" class="discount-info">
                <span>{{ item.discountText || item.discountCampaignName || 'Đang giảm giá' }}</span>
              </div>
            </div>
          </article>
        </div>
      </div>

      <aside class="summary-panel">
        <div>
          <span class="eyebrow light">THÔNG TIN THANH TOÁN</span>
          <h2>Tổng kết hóa đơn</h2>
        </div>

        <div v-if="snapshot?.isDelivery" class="delivery-card">
          <div class="delivery-card-title">
            <span>🚚</span>
            <strong>Thông tin người nhận</strong>
          </div>
          <div class="delivery-row">
            <span>Người nhận</span>
            <strong>{{ snapshot?.recipientName || 'Chưa nhập' }}</strong>
          </div>
          <div class="delivery-row">
            <span>Số điện thoại</span>
            <strong>{{ snapshot?.recipientPhone || 'Chưa nhập' }}</strong>
          </div>
          <div class="delivery-address">
            <span>Địa chỉ giao hàng</span>
            <strong>{{ snapshot?.deliveryAddress || 'Chưa nhập địa chỉ' }}</strong>
          </div>
        </div>

        <div v-if="snapshot?.voucher" class="voucher-card">
          <div class="voucher-icon">%</div>
          <div class="voucher-main">
            <span>Phiếu giảm giá đang áp dụng</span>
            <strong>{{ snapshot.voucher.code }} <i v-if="snapshot.voucher.personal">Cá nhân</i></strong>
            <small>{{ snapshot.voucher.discountLabel }}<template v-if="snapshot.voucher.name"> · {{ snapshot.voucher.name }}</template></small>
            <small>{{ snapshot.voucher.condition }}</small>
          </div>
          <b>-{{ formatMoney(snapshot.voucher.discountAmount) }}</b>
        </div>

        <div v-if="snapshot?.voucherSuggestion" class="voucher-suggestion">
          <strong>💡 Gợi ý ưu đãi tốt hơn</strong>
          <span>
            Mua thêm <b>{{ formatMoney(snapshot.voucherSuggestion.neededAmount) }}</b>
            để được giảm <b>{{ formatMoney(snapshot.voucherSuggestion.potentialDiscount) }}</b>
            với mã <b>{{ snapshot.voucherSuggestion.code }}</b>.
          </span>
        </div>

        <div class="summary-lines">
          <div><span>Tổng tiền sản phẩm</span><strong>{{ formatMoney(snapshot?.subtotal) }}</strong></div>
          <div v-if="snapshot?.promotionDiscount > 0" class="discount-line"><span>Ưu đãi sản phẩm</span><strong>-{{ formatMoney(snapshot.promotionDiscount) }}</strong></div>
          <div v-if="snapshot?.voucherDiscount > 0" class="discount-line"><span>Giảm giá voucher</span><strong>-{{ formatMoney(snapshot.voucherDiscount) }}</strong></div>
          <div v-if="snapshot?.isDelivery"><span>Phí vận chuyển</span><strong :class="{ free: snapshot?.freeshipApplied }">{{ snapshot?.freeshipApplied ? 'MIỄN PHÍ' : formatMoney(snapshot?.shippingFee) }}</strong></div>
        </div>

        <div class="grand-total">
          <span>KHÁCH CẦN THANH TOÁN</span>
          <strong>{{ formatMoney(snapshot?.finalTotal) }}</strong>
        </div>

        <div v-if="snapshot?.paymentMethod === 'KET_HOP'" class="split-payment-card">
          <div>
            <span>Tiền mặt khách đưa</span>
            <strong>{{ formatMoney(snapshot?.cashAmount) }}</strong>
          </div>
          <div>
            <span>Tiền cần chuyển khoản</span>
            <strong>{{ formatMoney(snapshot?.transferAmount) }}</strong>
          </div>
        </div>

        <div class="payment-method">
          <span>Phương thức dự kiến</span>
          <strong>{{ paymentLabel }}</strong>
        </div>

        <footer>Cảm ơn quý khách đã mua sắm tại FF T-Shirt</footer>
      </aside>
    </section>

    <div
      v-if="hasOrder && ['CHUYEN_KHOAN', 'KET_HOP'].includes(snapshot?.paymentMethod) && snapshot?.transferAmount > 0"
      class="customer-payment-overlay"
    >
      <div class="customer-payment-modal">
        <div class="payment-modal-header">
          <div>
            <h2>Thanh toán Chuyển khoản VietQR</h2>
            <p>Quét mã bằng ứng dụng ngân hàng hoặc ví điện tử</p>
          </div>
          <span>Mã HĐ: {{ snapshot?.invoiceCode }}</span>
        </div>

        <div class="payment-modal-body">
          <div class="payment-qr-column">
            <div class="payment-qr-frame">
              <img :src="snapshot?.transferQrUrl" alt="Mã VietQR thanh toán" />
            </div>
            <strong>{{ snapshot?.bankAccountName }}</strong>
            <span>{{ snapshot?.bankAccount }}</span>
            <b>Số tiền: {{ formatMoney(snapshot?.transferAmount) }}</b>
          </div>

          <div class="payment-detail-column">
            <div class="bank-detail-card">
              <div><span>Ngân hàng</span><strong>{{ snapshot?.bankName }}</strong></div>
              <div><span>Số tài khoản</span><strong>{{ snapshot?.bankAccount }}</strong></div>
              <div><span>Chủ tài khoản</span><strong>{{ snapshot?.bankAccountName }}</strong></div>
              <div class="transfer-highlight"><span>Số tiền cần chuyển</span><strong>{{ formatMoney(snapshot?.transferAmount) }}</strong></div>
              <div><span>Nội dung chuyển khoản</span><strong>{{ snapshot?.transferContent }}</strong></div>
            </div>

            <div class="payment-invoice-card">
              <div><span>Tổng cần thanh toán</span><strong>{{ formatMoney(snapshot?.finalTotal) }}</strong></div>
              <div v-if="snapshot?.paymentMethod === 'KET_HOP'"><span>Tiền mặt khách đưa</span><strong>{{ formatMoney(snapshot?.cashAmount) }}</strong></div>
              <div><span>Tiền chuyển khoản</span><strong>{{ formatMoney(snapshot?.transferAmount) }}</strong></div>
            </div>

            <div class="payment-scan-note">
              Vui lòng kiểm tra đúng số tiền và nội dung chuyển khoản trước khi xác nhận thanh toán.
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="snapshot?.changeWarning" class="customer-change-overlay">
      <div class="customer-change-modal">
        <div class="change-modal-header">
          <div>
            <h2>⚠ Cảnh báo: {{ snapshot.changeWarning.title }}</h2>
            <p>{{ snapshot.changeWarning.reason }}</p>
          </div>
          <span>{{ snapshot?.invoiceCode }}</span>
        </div>

        <div v-if="snapshot.changeWarning.type === 'VOUCHER'" class="voucher-change-comparison">
          <div class="change-side old-side">
            <span>PHIẾU TRƯỚC ĐÓ</span>
            <strong>{{ snapshot.changeWarning.oldLabel }}</strong>
            <small>{{ snapshot.changeWarning.oldDetail }}</small>
            <b>-{{ formatMoney(snapshot.changeWarning.oldAmount) }}</b>
          </div>
          <div class="change-arrow">→</div>
          <div class="change-side new-side">
            <span>PHIẾU MỚI</span>
            <strong>{{ snapshot.changeWarning.newLabel }}</strong>
            <small>{{ snapshot.changeWarning.newDetail }}</small>
            <b>-{{ formatMoney(snapshot.changeWarning.newAmount) }}</b>
          </div>
        </div>

        <div v-else class="product-change-list">
          <div v-for="(item, index) in snapshot.changeWarning.items" :key="index" class="product-change-row">
            <div><strong>{{ item.name }}</strong><small>{{ item.variant }}</small></div>
            <div v-if="item.oldPromotion || item.newPromotion" class="promotion-change"><del>{{ item.oldPromotion }}</del><span>→</span><b>{{ item.newPromotion }}</b></div>
            <div class="price-change"><del>{{ formatMoney(item.oldPrice) }}</del><span>→</span><b>{{ formatMoney(item.newPrice) }}</b></div>
          </div>
        </div>

        <div class="change-modal-note">
          Nhân viên đang kiểm tra thay đổi. Hóa đơn sẽ cập nhật theo lựa chọn xác nhận tại quầy.
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
.thank-you-state{flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;padding:36px;background:radial-gradient(circle at center,#fff 0,#f1f7f4 68%)}.thank-you-check{width:104px;height:104px;border-radius:50%;display:flex;align-items:center;justify-content:center;background:linear-gradient(145deg,#21b66f,#0b8f52);color:#fff;font-size:58px;font-weight:800;box-shadow:0 18px 42px rgba(15,157,91,.25);margin-bottom:22px}.thank-you-state>span{color:#12945a;font-size:12px;font-weight:900;letter-spacing:1.5px}.thank-you-state h2{margin:8px 0 8px;color:#17324d;font-size:38px}.thank-you-state>p{margin:0;color:#66758a;font-size:17px}.thank-you-receipt{width:min(470px,92vw);display:grid;grid-template-columns:1fr 1fr;gap:12px;margin:25px 0 20px}.thank-you-receipt>div{background:#fff;border:1px solid #dce6e1;border-radius:13px;padding:15px;box-shadow:0 7px 20px rgba(31,65,49,.06)}.thank-you-receipt span{display:block;color:#839087;font-size:11px;margin-bottom:6px}.thank-you-receipt strong{color:#17324d;font-size:19px}.thank-you-receipt>div:last-child strong{color:#0b9355}.thank-you-state>small{color:#728177;font-size:13px}.voucher-main{flex:1;min-width:0}.voucher-card>.voucher-main strong{display:flex;align-items:center;gap:6px}.voucher-main i{font-size:9px;font-style:normal;color:#8ff0b5;background:rgba(50,200,110,.14);border:1px solid rgba(126,224,165,.35);padding:2px 5px;border-radius:5px}.voucher-card>b{color:#86e8aa;font-size:14px;white-space:nowrap}.voucher-suggestion{display:flex;flex-direction:column;gap:4px;margin-top:10px;padding:11px 13px;border-radius:11px;background:rgba(242,204,135,.12);border:1px solid rgba(242,204,135,.3);font-size:11px;line-height:1.45}.voucher-suggestion>strong{color:#f2cc87}.voucher-suggestion>span{color:#d8e0e8}.voucher-suggestion b{color:#fff}.customer-change-overlay{position:fixed;inset:0;z-index:1200;background:rgba(15,23,42,.72);display:flex;align-items:center;justify-content:center;padding:24px}.customer-change-modal{width:min(820px,96vw);max-height:92vh;overflow:auto;background:#fff;border-radius:18px;box-shadow:0 30px 85px rgba(0,0,0,.38);color:#24364b}.change-modal-header{display:flex;justify-content:space-between;align-items:flex-start;gap:20px;padding:22px 25px;border-bottom:1px solid #e1e7ee;background:#fffaf0}.change-modal-header h2{margin:0;color:#b45309;font-size:21px}.change-modal-header p{margin:7px 0 0;color:#6b7280;font-size:13px}.change-modal-header>span{background:#fff0d6;color:#9a5b08;border-radius:8px;padding:7px 11px;font-size:11px;font-weight:800}.voucher-change-comparison{display:grid;grid-template-columns:1fr 42px 1fr;align-items:stretch;gap:12px;padding:24px}.change-side{display:flex;flex-direction:column;gap:7px;border-radius:13px;padding:16px;border:1px solid #dce3eb}.change-side>span{font-size:10px;font-weight:800;color:#718096}.change-side>strong{font-size:17px}.change-side>small{color:#718096;line-height:1.45;min-height:34px}.change-side>b{font-size:22px}.old-side{background:#f6f8fa}.old-side>strong,.old-side>b{color:#64748b}.new-side{background:#effcf4;border-color:#86e8aa}.new-side>strong,.new-side>b{color:#17723b}.change-arrow{display:flex;align-items:center;justify-content:center;color:#d97706;font-size:24px}.product-change-list{padding:18px 24px}.product-change-row{display:grid;grid-template-columns:minmax(150px,1fr) minmax(150px,1fr) minmax(180px,auto);align-items:center;gap:18px;padding:14px 0;border-bottom:1px solid #e5e9ef}.product-change-row:last-child{border-bottom:0}.product-change-row>div:first-child{display:flex;flex-direction:column;gap:4px}.product-change-row small{color:#718096}.promotion-change,.price-change{display:flex;align-items:center;justify-content:flex-end;gap:8px;font-size:12px}.promotion-change del,.price-change del{color:#8b97a7}.promotion-change b,.price-change b{color:#b45309}.change-modal-note{margin:0 24px 24px;padding:13px;border-radius:10px;background:#fff6df;border-left:4px solid #f2a51a;color:#89580d;font-size:12px}@media(max-width:700px){.voucher-change-comparison{grid-template-columns:1fr}.change-arrow{transform:rotate(90deg)}.product-change-row{grid-template-columns:1fr}.promotion-change,.price-change{justify-content:flex-start}}
*{box-sizing:border-box}.customer-display{min-height:100vh;background:#f4f6f8;color:#142033;font-family:Inter,"Segoe UI",Arial,sans-serif;display:flex;flex-direction:column}.display-header{height:94px;background:#fff;border-bottom:1px solid #dde3ea;display:flex;align-items:center;padding:0 42px;gap:15px;box-shadow:0 3px 14px rgba(15,23,42,.05)}.brand-mark{width:54px;height:54px;border-radius:15px;background:linear-gradient(145deg,#1f3a56,#0f2237);color:#fff;display:flex;align-items:center;justify-content:center;font-size:22px;font-weight:900;border:2px solid #d5aa63}.brand-copy h1{margin:0;font-size:22px;letter-spacing:1px}.brand-copy p{margin:3px 0 0;color:#768397;font-size:12px}.invoice-meta{margin-left:auto;text-align:right}.invoice-meta span{display:block;color:#8090a3;font-size:11px;font-weight:700}.invoice-meta strong{font-size:18px;color:#1f3a56}.welcome-state{flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;background:radial-gradient(circle at center,#fff 0,#f4f6f8 67%)}.welcome-icon{width:108px;height:108px;border-radius:30px;background:#fff;display:flex;align-items:center;justify-content:center;font-size:55px;box-shadow:0 18px 45px rgba(31,58,86,.12);margin-bottom:23px}.welcome-state h2{font-size:34px;margin:0 0 10px}.welcome-state p{font-size:17px;color:#6b788b}.waiting-pulse{display:flex;gap:8px;margin-top:18px}.waiting-pulse i{width:9px;height:9px;border-radius:50%;background:#d5aa63;animation:pulse 1.2s infinite}.waiting-pulse i:nth-child(2){animation-delay:.2s}.waiting-pulse i:nth-child(3){animation-delay:.4s}@keyframes pulse{0%,100%{opacity:.25;transform:translateY(0)}50%{opacity:1;transform:translateY(-5px)}}.display-content{flex:1;display:grid;grid-template-columns:minmax(0,1.65fr) minmax(340px,.75fr);gap:22px;padding:24px;min-height:0}.products-panel,.summary-panel{border-radius:20px;overflow:hidden}.products-panel{background:#fff;border:1px solid #e0e5eb;display:flex;flex-direction:column}.panel-heading{padding:24px 27px 19px;border-bottom:1px solid #e8ecf1;display:flex;align-items:flex-end;justify-content:space-between;gap:20px}.eyebrow{color:#b38235;font-size:11px;font-weight:850;letter-spacing:1.1px}.eyebrow.light{color:#f2cf93}.panel-heading h2,.summary-panel h2{margin:5px 0 0;font-size:21px}.customer-greeting{background:#f5f7fa;border-radius:10px;padding:9px 13px;color:#59677a;font-size:13px}.product-list{padding:7px 25px 20px;overflow:auto}.product-row{display:flex;gap:17px;padding:12px 2px;border-bottom:1px solid #edf0f4}.product-row:last-child{border-bottom:0}.product-image-wrap{width:76px;height:76px;border-radius:12px;background:#f1f4f7;border:1px solid #e0e5ea;overflow:hidden;flex-shrink:0}.product-image{width:100%;height:100%;object-fit:cover}.product-main{position:relative;flex:1;min-width:0;padding-right:150px}.product-topline{display:block}.product-topline h3{font-size:16px;margin:2px 0;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.product-price-block{position:absolute;top:0;right:0;display:flex;flex-direction:column;align-items:flex-end;gap:3px}.line-total{font-size:17px;color:#162d44;white-space:nowrap}.original-line-total{color:#9aa4b1;font-size:12px;white-space:nowrap}.product-details{display:flex;gap:18px;color:#6d7a8c;font-size:13px;margin-top:5px}.discount-info{display:flex;gap:12px;align-items:center;margin-top:6px;font-size:11px}.discount-info span{background:#fff4dc;color:#9b6616;border-radius:6px;padding:3px 7px;font-weight:700}.summary-panel{background:linear-gradient(158deg,#223f5d,#10263c);color:#fff;padding:27px;display:flex;flex-direction:column;box-shadow:0 18px 40px rgba(18,42,66,.22);overflow-y:auto}.voucher-card{display:flex;gap:12px;align-items:center;background:rgba(255,255,255,.1);border:1px solid rgba(255,255,255,.14);padding:13px;border-radius:13px;margin-top:22px}.voucher-icon{width:40px;height:40px;border-radius:11px;background:#d5aa63;color:#1a324a;display:flex;align-items:center;justify-content:center;font-size:19px;font-weight:900}.voucher-card span,.voucher-card small{display:block;color:#cbd5df;font-size:11px}.voucher-card strong{display:block;margin:2px 0}.delivery-card{margin-top:14px;padding:14px;border-radius:13px;background:rgba(126,224,165,.09);border:1px solid rgba(126,224,165,.25)}.delivery-card-title{display:flex;align-items:center;gap:8px;margin-bottom:10px;color:#91e9b3;font-size:13px}.delivery-row{display:flex;justify-content:space-between;gap:12px;padding:4px 0;font-size:12px}.delivery-row span,.delivery-address span{color:#b8c6d2}.delivery-row strong{text-align:right}.delivery-address{padding-top:8px;margin-top:6px;border-top:1px solid rgba(255,255,255,.1);font-size:12px}.delivery-address span{display:block;margin-bottom:4px}.delivery-address strong{display:block;line-height:1.45}.summary-lines{border-top:1px solid rgba(255,255,255,.14);border-bottom:1px solid rgba(255,255,255,.14);margin-top:22px;padding:15px 0}.summary-lines>div{display:flex;justify-content:space-between;gap:15px;padding:8px 0;font-size:13px}.discount-line strong,.free{color:#7ee0a5}.grand-total{padding:22px 0}.grand-total span{font-size:12px;color:#c6d0da;font-weight:800}.grand-total strong{display:block;color:#f2cc87;font-size:34px;margin-top:6px;letter-spacing:-1px}.split-payment-card{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-bottom:14px}.split-payment-card>div{padding:11px;border-radius:10px;background:rgba(255,255,255,.08)}.split-payment-card span{display:block;color:#bdcad5;font-size:10px;margin-bottom:5px}.split-payment-card strong{font-size:14px;color:#fff}.customer-qr-card{background:#fff;color:#172c42;border-radius:14px;padding:13px;margin-bottom:14px;text-align:center}.qr-title{display:flex;align-items:center;justify-content:space-between;gap:8px;font-size:12px}.qr-title span{color:#d62828;font-weight:800}.customer-qr-image{display:block;width:min(190px,100%);aspect-ratio:1;object-fit:contain;margin:9px auto}.qr-bank-info{display:flex;flex-direction:column;gap:3px;font-size:10px;color:#607083}.qr-bank-info strong{font-size:11px;color:#203c57}.qr-bank-info b{color:#203c57}.payment-method{display:flex;justify-content:space-between;gap:15px;background:rgba(255,255,255,.08);padding:13px;border-radius:11px;font-size:12px}.summary-panel footer{margin-top:auto;text-align:center;color:#aebdca;font-size:11px;padding-top:23px}@media(max-width:900px){.display-content{grid-template-columns:1fr}.summary-panel{min-height:420px}.display-header{padding:0 20px}}
</style>

<style scoped>
.voucher-main{flex:1;min-width:0}.voucher-card>.voucher-main strong{display:flex;align-items:center;gap:6px}.voucher-main i{font-size:9px;font-style:normal;color:#8ff0b5;background:rgba(50,200,110,.14);border:1px solid rgba(126,224,165,.35);padding:2px 5px;border-radius:5px}.voucher-card>b{color:#86e8aa;font-size:14px;white-space:nowrap}.voucher-suggestion{display:flex;flex-direction:column;gap:4px;margin-top:10px;padding:11px 13px;border-radius:11px;background:rgba(242,204,135,.12);border:1px solid rgba(242,204,135,.3);font-size:11px;line-height:1.45}.voucher-suggestion>strong{color:#f2cc87}.voucher-suggestion>span{color:#d8e0e8}.voucher-suggestion b{color:#fff}.customer-change-overlay{position:fixed;inset:0;z-index:1200;background:rgba(15,23,42,.72);display:flex;align-items:center;justify-content:center;padding:24px}.customer-change-modal{width:min(820px,96vw);max-height:92vh;overflow:auto;background:#fff;border-radius:18px;box-shadow:0 30px 85px rgba(0,0,0,.38);color:#24364b}.change-modal-header{display:flex;justify-content:space-between;align-items:flex-start;gap:20px;padding:22px 25px;border-bottom:1px solid #e1e7ee;background:#fffaf0}.change-modal-header h2{margin:0;color:#b45309;font-size:21px}.change-modal-header p{margin:7px 0 0;color:#6b7280;font-size:13px}.change-modal-header>span{background:#fff0d6;color:#9a5b08;border-radius:8px;padding:7px 11px;font-size:11px;font-weight:800}.voucher-change-comparison{display:grid;grid-template-columns:1fr 42px 1fr;align-items:stretch;gap:12px;padding:24px}.change-side{display:flex;flex-direction:column;gap:7px;border-radius:13px;padding:16px;border:1px solid #dce3eb}.change-side>span{font-size:10px;font-weight:800;color:#718096}.change-side>strong{font-size:17px}.change-side>small{color:#718096;line-height:1.45;min-height:34px}.change-side>b{font-size:22px}.old-side{background:#f6f8fa}.old-side>strong,.old-side>b{color:#64748b}.new-side{background:#effcf4;border-color:#86e8aa}.new-side>strong,.new-side>b{color:#17723b}.change-arrow{display:flex;align-items:center;justify-content:center;color:#d97706;font-size:24px}.product-change-list{padding:18px 24px}.product-change-row{display:grid;grid-template-columns:minmax(150px,1fr) minmax(150px,1fr) minmax(180px,auto);align-items:center;gap:18px;padding:14px 0;border-bottom:1px solid #e5e9ef}.product-change-row:last-child{border-bottom:0}.product-change-row>div:first-child{display:flex;flex-direction:column;gap:4px}.product-change-row small{color:#718096}.promotion-change,.price-change{display:flex;align-items:center;justify-content:flex-end;gap:8px;font-size:12px}.promotion-change del,.price-change del{color:#8b97a7}.promotion-change b,.price-change b{color:#b45309}.change-modal-note{margin:0 24px 24px;padding:13px;border-radius:10px;background:#fff6df;border-left:4px solid #f2a51a;color:#89580d;font-size:12px}@media(max-width:700px){.voucher-change-comparison{grid-template-columns:1fr}.change-arrow{transform:rotate(90deg)}.product-change-row{grid-template-columns:1fr}.promotion-change,.price-change{justify-content:flex-start}}
</style>

<style scoped>
.customer-payment-overlay{position:fixed;inset:0;z-index:1000;background:rgba(10,24,38,.72);display:flex;align-items:center;justify-content:center;padding:22px}.customer-payment-modal{width:min(1040px,97vw);max-height:96vh;overflow:auto;background:#f8fafc;border-radius:18px;box-shadow:0 28px 80px rgba(0,0,0,.38);color:#17324d}.payment-modal-header{display:flex;align-items:center;justify-content:space-between;gap:20px;padding:20px 26px 16px;border-bottom:1px solid #dce4ec}.payment-modal-header h2{margin:0;color:#07549a;font-size:22px}.payment-modal-header p{margin:5px 0 0;color:#718198;font-size:13px}.payment-modal-header>span{background:#e7edf4;border-radius:9px;padding:8px 13px;font-size:12px;font-weight:800;white-space:nowrap}.payment-modal-body{display:grid;grid-template-columns:470px 1fr;gap:24px;padding:22px}.payment-qr-column{background:#fff;border:1px solid #ccd8e5;border-radius:15px;padding:14px;display:flex;flex-direction:column;align-items:center;text-align:center}.payment-qr-frame{position:relative;width:400px;max-width:100%;aspect-ratio:1;border-radius:10px;display:flex;align-items:center;justify-content:center;overflow:hidden}.payment-qr-frame img{width:100%;height:100%;object-fit:contain}.payment-qr-column>strong{font-size:14px;margin-top:7px}.payment-qr-column>span{font-size:12px;color:#52647a;margin-top:3px}.payment-qr-column>b{font-size:14px;margin-top:6px}.payment-detail-column{display:flex;flex-direction:column;gap:12px}.bank-detail-card,.payment-invoice-card{background:#fff;border:1px solid #d8e1ea;border-radius:13px;padding:6px 15px}.bank-detail-card>div,.payment-invoice-card>div{display:flex;align-items:center;justify-content:space-between;gap:18px;padding:10px 0;border-bottom:1px dashed #d6dee7;font-size:12px}.bank-detail-card>div:last-child,.payment-invoice-card>div:last-child{border-bottom:0}.bank-detail-card span,.payment-invoice-card span{color:#718096}.bank-detail-card strong,.payment-invoice-card strong{text-align:right}.bank-detail-card .transfer-highlight strong{color:#df1f2d;font-size:20px}.payment-scan-note{margin-top:auto;background:#fff5d9;border-left:4px solid #f5a623;border-radius:7px;padding:13px;color:#9a5b08;font-size:12px;line-height:1.5}@media(max-width:760px){.customer-payment-overlay{padding:12px}.payment-modal-body{grid-template-columns:1fr}.payment-modal-header{align-items:flex-start}.payment-qr-frame{width:320px}}
</style>
