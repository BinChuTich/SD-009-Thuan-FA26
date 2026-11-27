<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' }
})
</script>

<template>
  <section class="client-page-banner" aria-labelledby="client-page-banner-title">
    <div class="page-banner-content">
      <div class="banner-logo-wrapper" aria-hidden="true">
        <div class="banner-sparkles">
          <span class="sparkle-aura"></span>
          <span class="ray ray-1"></span>
          <span class="ray ray-2"></span>
          <span class="ray ray-3"></span>
        </div>
        <div class="banner-logo-content">
          <div class="banner-logo-name">
            <span class="f-blue">F</span>
            <span class="f-gold">F</span>
          </div>
          <div class="banner-logo-sub">T-shirt</div>
        </div>
      </div>
      <h1 id="client-page-banner-title">{{ title }}</h1>
      <p v-if="subtitle">{{ subtitle }}</p>
    </div>
  </section>
</template>

<style scoped>
.banner-logo-wrapper{position:relative;display:inline-flex;align-items:flex-end;font-family:'Fredoka',-apple-system,sans-serif;user-select:none}.banner-sparkles{position:absolute;top:-14px;left:-8px;width:36px;height:36px;pointer-events:none}.banner-sparkles .sparkle-aura{position:absolute;bottom:2px;right:6px;width:18px;height:18px;border-radius:50%;background:radial-gradient(circle,rgba(255,252,235,.95) 0%,rgba(251,191,36,.8) 45%,rgba(217,119,6,0) 75%);animation:fireworkAuraBanner 2.4s ease-in-out infinite}.banner-sparkles .ray{position:absolute;background-color:#d2a764;border-radius:99px;transform-origin:center center;animation:fireworkSparkleBanner 2.4s ease-in-out infinite}.banner-sparkles .ray-1{--rot:12deg;width:4.8px;height:14px;top:1px;right:8px;transform:rotate(12deg);animation-delay:0s}.banner-sparkles .ray-2{--rot:-42deg;width:4.8px;height:14px;top:6px;left:6px;transform:rotate(-42deg);animation-delay:.35s}.banner-sparkles .ray-3{--rot:-78deg;width:4.8px;height:14px;bottom:1px;left:0;transform:rotate(-78deg);animation-delay:.7s}.banner-logo-wrapper:hover .banner-sparkles .ray{animation-duration:1.1s}.banner-logo-content{display:flex;align-items:flex-end;filter:drop-shadow(0 6px 18px rgba(0,0,0,.75)) drop-shadow(0 2px 4px rgba(0,0,0,.6))}.banner-logo-name{display:flex;font-family:'Fredoka',-apple-system,sans-serif;font-size:64px;font-weight:800;line-height:.82;letter-spacing:-2px}.f-blue{color:#496883;font-weight:800;text-shadow:0 1px 2px rgba(0,0,0,.9),0 3px 6px rgba(0,0,0,.75),0 6px 16px rgba(0,0,0,.55),0 0 2px rgba(255,255,255,.45);filter:drop-shadow(0 2px 6px rgba(0,0,0,.7)) drop-shadow(0 0 1px rgba(255,255,255,.35))}.f-gold{color:#d2a764;margin-left:2px;font-weight:800;text-shadow:0 1px 2px rgba(0,0,0,.85),0 3px 6px rgba(0,0,0,.7),0 6px 14px rgba(0,0,0,.5),0 0 2px rgba(210,167,100,.8);filter:drop-shadow(0 2px 6px rgba(0,0,0,.65))}.banner-logo-sub{font-family:'Fredoka',-apple-system,sans-serif;color:#496883;font-size:22px;font-weight:800;line-height:1;margin-left:10px;margin-bottom:4px;letter-spacing:-.3px;text-shadow:0 1px 2px rgba(0,0,0,.9),0 2px 5px rgba(0,0,0,.7),0 0 2px rgba(255,255,255,.35);filter:drop-shadow(0 2px 5px rgba(0,0,0,.65))}@keyframes fireworkAuraBanner{0%,100%{opacity:.15;transform:scale(.75)}35%{opacity:.95;transform:scale(1.35);filter:drop-shadow(0 0 6px #f59e0b) drop-shadow(0 0 14px #fbbf24)}65%{opacity:.35;transform:scale(.95)}}@keyframes fireworkSparkleBanner{0%,100%{opacity:.35;transform:rotate(var(--rot,0deg)) scale(.88);background-color:#d1a868;box-shadow:0 0 3px rgba(209,168,104,.4);filter:drop-shadow(0 0 2px rgba(209,168,104,.3))}25%{opacity:.8;transform:rotate(var(--rot,0deg)) scale(1.15);background-color:#fde68a;box-shadow:0 0 8px #f59e0b,0 0 16px rgba(245,158,11,.75);filter:drop-shadow(0 0 4px #fbbf24)}45%{opacity:1;transform:rotate(var(--rot,0deg)) scale(1.4);background-color:#fff;box-shadow:0 0 8px #fff,0 0 18px #fbbf24,0 0 32px #f59e0b,0 0 44px rgba(245,158,11,.95);filter:drop-shadow(0 0 6px #fff) drop-shadow(0 0 12px #fbbf24)}65%{opacity:.85;transform:rotate(var(--rot,0deg)) scale(1.18);background-color:#fcd34d;box-shadow:0 0 8px #f59e0b,0 0 16px rgba(210,167,100,.6);filter:drop-shadow(0 0 4px #f59e0b)}85%{opacity:.45;transform:rotate(var(--rot,0deg)) scale(.95);background-color:#d1a868;box-shadow:0 0 3px rgba(209,168,104,.3)}}
.client-page-banner{position:relative;min-height:188px;display:grid;place-items:center;overflow:hidden;padding:30px 20px 32px;color:#fff;text-align:center;background:linear-gradient(112deg,#111b2d 0%,#19263a 48%,#26384b 100%);border-top:1px solid #d4ad69}.client-page-banner::before,.client-page-banner::after{content:'';position:absolute;border-radius:50%;pointer-events:none}.client-page-banner::before{width:360px;height:360px;right:-150px;top:-245px;background:rgba(255,255,255,.035)}.client-page-banner::after{width:280px;height:280px;left:-150px;bottom:-230px;background:rgba(210,167,100,.055)}.page-banner-content{position:relative;z-index:2;display:grid;justify-items:center}.page-banner-content h1{margin:15px 0 6px;font-size:24px;line-height:1.25;font-weight:850;letter-spacing:-.25px}.page-banner-content p{max-width:760px;margin:0;color:#e0e8ef;font-size:14px;line-height:1.55}@media(max-width:640px){.client-page-banner{min-height:166px;padding:24px 16px 26px}.banner-logo-name{font-size:52px}.banner-logo-sub{font-size:18px;margin-left:8px}.page-banner-content h1{margin-top:13px;font-size:20px}.page-banner-content p{font-size:12.5px}}@media(prefers-reduced-motion:reduce){.banner-sparkles .sparkle-aura,.banner-sparkles .ray{animation:none}}
</style>
