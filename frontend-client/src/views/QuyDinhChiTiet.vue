<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { findStoreRule, storeRules } from '@/data/quyDinhCuaHang.js'

const route = useRoute()
const rule = computed(() => findStoreRule(route.params.slug))
const relatedRules = computed(() => storeRules.filter(item => item.slug !== route.params.slug).slice(0, 3))
</script>

<template>
  <div class="policy-detail-page">
    <section v-if="rule" class="policy-hero">
      <div class="container-client policy-hero-inner">
        <router-link to="/lien-he" class="back-link">← Quay lại trang liên hệ</router-link>
        <div class="policy-title-row">
          <span class="policy-main-icon">{{ rule.icon }}</span>
          <div>
            <span class="policy-eyebrow">QUY ĐỊNH CỬA HÀNG FF T-SHIRT</span>
            <h1>{{ rule.title }}</h1>
            <p>{{ rule.summary }}</p>
          </div>
        </div>
      </div>
    </section>

    <main v-if="rule" class="container-client policy-layout">
      <article class="policy-content-card">
        <div class="policy-intro">
          <strong>Thông tin quan trọng</strong>
          <p>Các nội dung dưới đây được áp dụng theo luồng xử lý hiện tại của hệ thống FF T-Shirt. Khách hàng nên kiểm tra kỹ trước khi xác nhận đơn.</p>
        </div>

        <section v-for="(section, index) in rule.sections" :key="section.title" class="policy-section">
          <span class="section-number">{{ String(index + 1).padStart(2, '0') }}</span>
          <div><h2>{{ section.title }}</h2><p>{{ section.content }}</p></div>
        </section>

        <div class="policy-contact-box">
          <div><span>✉️</span><div><strong>Bạn vẫn còn thắc mắc?</strong><p>Gửi yêu cầu để FF T-Shirt kiểm tra theo đơn hàng thực tế của bạn.</p></div></div>
          <router-link to="/lien-he">Liên hệ hỗ trợ →</router-link>
        </div>
      </article>

      <aside class="policy-sidebar">
        <div class="sidebar-card">
          <span class="sidebar-kicker">NỘI DUNG LIÊN QUAN</span>
          <h3>Xem thêm quy định</h3>
          <router-link v-for="item in relatedRules" :key="item.slug" :to="`/lien-he/quy-dinh/${item.slug}`" class="related-rule">
            <span>{{ item.icon }}</span><div><strong>{{ item.title }}</strong><small>{{ item.summary }}</small></div><b>›</b>
          </router-link>
        </div>
        <div class="sidebar-note"><span>💡</span><p>Giá, phí vận chuyển và ưu đãi cụ thể luôn được hiển thị lại tại bước xác nhận đơn hàng.</p></div>
      </aside>
    </main>

    <main v-else class="container-client policy-not-found">
      <span>📄</span><h1>Không tìm thấy quy định</h1><p>Nội dung bạn đang tìm không tồn tại hoặc đã được chuyển.</p><router-link to="/lien-he">Quay lại trang liên hệ</router-link>
    </main>
  </div>
</template>

<style scoped>
.policy-detail-page{min-height:75vh;background:#f5f7fa;padding-bottom:70px}.policy-hero{position:relative;overflow:hidden;background:linear-gradient(135deg,#172235,#263d52 60%,#496883);color:#fff;border-bottom:3px solid #d2a764}.policy-hero::after{content:'';position:absolute;width:360px;height:360px;right:-90px;top:-190px;border-radius:50%;background:rgba(255,255,255,.055)}.policy-hero-inner{position:relative;z-index:1;padding-top:30px;padding-bottom:42px}.back-link{display:inline-flex;margin-bottom:26px;color:#dbe6ee;text-decoration:none;font-size:13px;font-weight:700}.back-link:hover{color:#f1cd8a}.policy-title-row{display:flex;align-items:center;gap:22px}.policy-main-icon{width:82px;height:82px;display:grid;place-items:center;flex:0 0 82px;border:1px solid rgba(255,255,255,.18);border-radius:22px;background:rgba(255,255,255,.1);font-size:38px;box-shadow:inset 0 1px rgba(255,255,255,.12)}.policy-eyebrow,.sidebar-kicker{color:#d2a764;font-size:10px;font-weight:900;letter-spacing:1.2px}.policy-title-row h1{margin:5px 0 7px;font-size:32px}.policy-title-row p{margin:0;max-width:720px;color:#dbe4ec;font-size:14px;line-height:1.6}.policy-layout{display:grid;grid-template-columns:minmax(0,1fr) 330px;gap:22px;margin-top:24px;align-items:start}.policy-content-card,.sidebar-card{background:#fff;border:1px solid #e2e8f0;border-radius:16px;box-shadow:0 5px 20px rgba(15,23,42,.05)}.policy-content-card{padding:26px}.policy-intro{padding:17px 18px;border:1px solid #ead5ad;border-radius:12px;background:#fffbf3}.policy-intro strong{color:#8a632e;font-size:14px}.policy-intro p{margin:5px 0 0;color:#6b7280;font-size:12.5px;line-height:1.6}.policy-section{display:grid;grid-template-columns:48px 1fr;gap:15px;padding:24px 0;border-bottom:1px solid #edf2f7}.section-number{width:42px;height:42px;display:grid;place-items:center;border-radius:12px;background:#edf3f7;color:#496883;font-size:12px;font-weight:900}.policy-section h2{margin:1px 0 7px;color:#263d52;font-size:17px}.policy-section p{margin:0;color:#64748b;font-size:13.5px;line-height:1.75}.policy-contact-box{display:flex;align-items:center;justify-content:space-between;gap:18px;margin-top:24px;padding:17px 18px;border-radius:13px;background:#263d52;color:#fff}.policy-contact-box>div{display:flex;align-items:center;gap:12px}.policy-contact-box>div>span{font-size:24px}.policy-contact-box strong{font-size:13px}.policy-contact-box p{margin:3px 0 0;color:#cbd5e1;font-size:11.5px}.policy-contact-box>a{padding:10px 14px;border-radius:9px;background:#d2a764;color:#172235;text-decoration:none;font-size:12px;font-weight:900;white-space:nowrap}.policy-sidebar{position:sticky;top:90px}.sidebar-card{padding:20px}.sidebar-card h3{margin:5px 0 16px;color:#263d52;font-size:18px}.related-rule{display:grid;grid-template-columns:36px 1fr 12px;align-items:center;gap:10px;padding:12px 0;border-top:1px solid #edf2f7;text-decoration:none}.related-rule>span{width:34px;height:34px;display:grid;place-items:center;border-radius:9px;background:#f1f5f9}.related-rule strong{display:block;color:#334155;font-size:12px}.related-rule small{display:-webkit-box;margin-top:3px;overflow:hidden;color:#94a3b8;font-size:10px;-webkit-line-clamp:1;-webkit-box-orient:vertical}.related-rule>b{color:#94a3b8;font-size:20px}.sidebar-note{display:flex;gap:9px;margin-top:14px;padding:14px;border:1px solid #dbe4ec;border-radius:12px;background:#eef4f7}.sidebar-note p{margin:0;color:#526273;font-size:11.5px;line-height:1.55}.policy-not-found{text-align:center;padding-top:100px}.policy-not-found>span{font-size:55px}.policy-not-found h1{color:#263d52}.policy-not-found p{color:#64748b}.policy-not-found a{display:inline-block;margin-top:12px;padding:11px 18px;border-radius:9px;background:#496883;color:#fff;text-decoration:none;font-weight:800}
@media(max-width:900px){.policy-layout{grid-template-columns:1fr}.policy-sidebar{position:static}.policy-title-row h1{font-size:26px}}
@media(max-width:600px){.policy-hero-inner{padding-top:22px;padding-bottom:30px}.policy-title-row{align-items:flex-start;gap:13px}.policy-main-icon{width:58px;height:58px;flex-basis:58px;border-radius:15px;font-size:27px}.policy-title-row h1{font-size:21px}.policy-layout{margin-top:12px;padding-inline:10px}.policy-content-card{padding:16px 14px}.policy-section{grid-template-columns:38px 1fr;gap:10px;padding:19px 0}.section-number{width:34px;height:34px}.policy-contact-box{align-items:flex-start;flex-direction:column}.policy-contact-box>a{width:100%;text-align:center}}
</style>
