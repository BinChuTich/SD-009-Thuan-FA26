<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import api from '@/api'
import TheSanPham from '@/components/DungChung/TheSanPham.vue'
import { getMergedProducts, attachSalesCountToProducts } from '@/utils/danhMucSanPham.js'
import {
  goiYAnswers as answers,
  goiYCompleted as completed,
  goiYCurrentStep as currentStep,
  goiYCustomBudgetMax as customBudgetMax,
  goiYCustomBudgetMin as customBudgetMin,
  resetGoiYThongMinh
} from '@/utils/goiYThongMinhStore.js'

const products = ref([])
const loading = ref(true)

const normalize = (value) => String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()
const categoryName = p => String(p.tenDanhMuc || p.danhMuc?.tenDanhMuc || p.idDanhMuc?.tenDanhMuc || p.loaiSanPham || '').trim()
const uniqueOptions = values => [...new Set(values.map(v => String(v || '').trim()).filter(Boolean))]
  .sort((a, b) => a.localeCompare(b, 'vi')).map(value => [value, value])

const categoryOptions = computed(() => {
  const options = uniqueOptions(products.value.map(categoryName))
  return [...options, ['any', 'Tất cả dòng áo']]
})

const styleOptions = computed(() => {
  const values = []
  products.value.forEach(p => {
    if (p.phongCach) values.push(p.phongCach)
    if (p.kieuDang) values.push(p.kieuDang)
    if (p.tenHoaTiet) values.push(p.tenHoaTiet)
    if (p.tenCoAo) values.push(p.tenCoAo)
    const text = normalize(`${p.tenSanPham || ''} ${categoryName(p)}`)
    if (text.includes('polo') || text.includes('co be')) values.push('Polo / Cổ bẻ')
    if (text.includes('oversize') || text.includes('form rong')) values.push('Oversize')
    if (text.includes('basic') || text.includes('ao tron') || text.includes('co tron')) values.push('Basic / Cổ tròn')
    if (text.includes('in hinh') || text.includes('hoa tiet') || text.includes('graphic')) values.push('In hình / Họa tiết')
  })
  return [...uniqueOptions(values), ['any', 'Chưa xác định phong cách']]
})

const materialOptions = computed(() => [
  ...uniqueOptions(products.value.map(p => p.tenChatLieu || p.chatLieu)),
  ['any', 'Không quan trọng chất liệu']
])

const colorOptions = computed(() => [
  ...uniqueOptions(products.value.flatMap(p => p.colors || [])),
  ['any', 'Không quan trọng màu']
])

const sizeOptions = computed(() => [
  ['S', 'Size S'], ['M', 'Size M'], ['L', 'Size L'],
  ['XL', 'Size XL'], ['XXL', 'Size XXL'], ['any', 'Chưa biết size']
])

const questions = computed(() => [
  {
    key: 'category', title: 'Bạn đang muốn tìm dòng áo nào?', hint: 'Các lựa chọn được lấy trực tiếp từ danh mục sản phẩm đang bán.',
    options: categoryOptions.value
  },
  {
    key: 'style', title: 'Phong cách áo bạn đang tìm?', hint: 'Chọn kiểu gần nhất với sản phẩm bạn muốn mua.',
    options: styleOptions.value
  },
  {
    key: 'material', title: 'Bạn ưu tiên chất liệu nào?', hint: 'Các chất liệu được lấy từ sản phẩm hiện có của cửa hàng.',
    options: materialOptions.value
  },
  {
    key: 'color', title: 'Bạn ưu tiên màu sắc nào?', hint: 'Hệ thống chỉ đề xuất màu đang có biến thể còn hàng.',
    options: colorOptions.value
  },
  {
    key: 'size', title: 'Bạn thường mặc size áo nào?', hint: 'Kết quả sẽ ưu tiên sản phẩm có đúng size đang còn hàng.',
    options: sizeOptions.value
  },
  {
    key: 'budget', title: 'Ngân sách của bạn khoảng bao nhiêu?', hint: 'Giá được xét sau đợt giảm giá đang áp dụng.',
    options: [
      ['under200', 'Dưới 200.000 đ'], ['200to300', '200.000 – 300.000 đ'],
      ['300to500', '300.000 – 500.000 đ'], ['over500', 'Trên 500.000 đ'],
      ['custom', 'Tự nhập khoảng giá'], ['any', 'Chưa có ngân sách cụ thể']
    ]
  }
])

const question = computed(() => questions.value[currentStep.value])
const progress = computed(() => Math.round(((currentStep.value + (completed.value ? 1 : 0)) / questions.value.length) * 100))
const selectedAnswer = computed(() => answers.value[question.value?.key] || '')
const canContinue = computed(() => {
  if (!selectedAnswer.value) return false
  if (question.value?.key !== 'budget' || selectedAnswer.value !== 'custom') return true
  const min = Number(customBudgetMin.value || 0)
  const max = Number(customBudgetMax.value || 0)
  return (min > 0 || max > 0) && (!min || !max || max >= min)
})

const productPrice = p => Number((p.minGiaSauGiam > 0 ? p.minGiaSauGiam : null) || p.minGia || p.giaBan || p.price || 0)
const productText = p => normalize([
  p.tenSanPham, p.tenDanhMuc, p.tenThuongHieu, p.moTa, p.chatLieu,
  p.kieuDang, p.phongCach, p.tenCoAo, p.tenTayAo, p.tenChatLieu, p.tenHoaTiet
].filter(Boolean).join(' '))

const containsAny = (text, words) => words.some(word => text.includes(normalize(word)))
const hasColor = (p, color) => (p.colors || []).some(c => normalize(c).includes(normalize(color)) || normalize(color).includes(normalize(c)))

const matchesStyle = (p, style) => {
  if (!style || style === 'any') return true
  const words = normalize(style).split(/\s*[/·-]\s*|\s+/).filter(word => word.length > 2)
  return containsAny(productText(p), words)
}

const matchesBudget = (price, budget) => budget === 'any'
  || (budget === 'under200' && price < 200000)
  || (budget === '200to300' && price >= 200000 && price <= 300000)
  || (budget === '300to500' && price > 300000 && price <= 500000)
  || (budget === 'over500' && price > 500000)
  || (budget === 'custom'
    && (!Number(customBudgetMin.value || 0) || price >= Number(customBudgetMin.value))
    && (!Number(customBudgetMax.value || 0) || price <= Number(customBudgetMax.value)))

const matchesRequiredAnswers = p => {
  const a = answers.value
  if (a.category !== 'any' && normalize(categoryName(p)) !== normalize(a.category)) return false
  if (!matchesStyle(p, a.style)) return false
  if (a.material !== 'any' && normalize(p.tenChatLieu || p.chatLieu) !== normalize(a.material)) return false
  if (a.color !== 'any' && !hasColor(p, a.color)) return false
  if (a.size !== 'any' && !(p.sizes || []).some(size => normalize(size) === normalize(a.size))) return false
  if (!matchesBudget(productPrice(p), a.budget)) return false
  return true
}

const scoreProduct = p => {
  const text = productText(p)
  const a = answers.value
  let score = 10
  const reasons = []
  let matchedCriteria = 0
  let consideredCriteria = 0

  if (a.category && a.category !== 'any') {
    consideredCriteria++
    if (normalize(categoryName(p)) === normalize(a.category) || text.includes(normalize(a.category))) {
      score += 35; matchedCriteria++; reasons.push(`Đúng dòng ${a.category}`)
    } else score -= 14
  }

  if (a.style && a.style !== 'any') {
    consideredCriteria++
    const styleWords = normalize(a.style).split(/\s*[/·-]\s*|\s+/).filter(word => word.length > 2)
    if (containsAny(text, styleWords)) { score += 30; matchedCriteria++; reasons.push('Đúng phong cách bạn chọn') }
    else score -= 8
  }

  if (a.material && a.material !== 'any') {
    consideredCriteria++
    if (normalize(p.tenChatLieu || p.chatLieu).includes(normalize(a.material))) {
      score += 18; matchedCriteria++; reasons.push(`Đúng chất liệu ${a.material}`)
    }
  }

  if (a.color && a.color !== 'any') {
    consideredCriteria++
    const matched = hasColor(p, a.color)
    if (matched) { score += 22; matchedCriteria++; reasons.push(`Có màu ${a.color} bạn ưu tiên`) }
    else score -= 8
  }

  if (a.size && a.size !== 'any') {
    consideredCriteria++
    if ((p.sizes || []).some(s => normalize(s) === normalize(a.size))) {
      score += 25; matchedCriteria++; reasons.push(`Còn size ${a.size}`)
    } else score -= 20
  }

  const price = productPrice(p)
  const budgetMatch = matchesBudget(price, a.budget)
  if (a.budget && a.budget !== 'any') {
    consideredCriteria++
    if (budgetMatch) { score += 28; matchedCriteria++; reasons.push('Nằm trong ngân sách đã chọn') }
    else score -= 16
  }

  if (p.coGiamGia && Number(p.phanTramGiam || 0) > 0) {
    score += 5
  }
  const matchPercent = consideredCriteria > 0
    ? Math.round((matchedCriteria / consideredCriteria) * 100)
    : 100
  return { product: p, score, reasons: reasons.slice(0, 4), matchedCriteria, consideredCriteria, matchPercent }
}

const exactRecommendations = computed(() => {
  if (!completed.value) return []
  return products.value
    .filter(matchesRequiredAnswers)
    .map(scoreProduct)
    .sort((a, b) => b.score - a.score || productPrice(a.product) - productPrice(b.product))
    .slice(0, 12)
})

const nearRecommendations = computed(() => {
  if (!completed.value || exactRecommendations.value.length) return []
  return products.value
    .map(scoreProduct)
    .filter(item => item.matchedCriteria > 0)
    .sort((a, b) =>
      b.matchedCriteria - a.matchedCriteria
      || b.score - a.score
      || productPrice(a.product) - productPrice(b.product))
    .slice(0, 12)
})

const isNearMatchResult = computed(() => completed.value && !exactRecommendations.value.length && nearRecommendations.value.length > 0)
const recommendations = computed(() => exactRecommendations.value.length ? exactRecommendations.value : nearRecommendations.value)
const resultTitle = computed(() => isNearMatchResult.value
  ? 'Sản phẩm gần phù hợp'
  : 'Sản phẩm dành cho bạn')
const resultDescription = computed(() => isNearMatchResult.value
  ? 'Chưa có mẫu khớp hoàn toàn — đây là những lựa chọn gần nhất.'
  : 'Các mẫu dưới đây phù hợp với những lựa chọn của bạn.')

const choose = value => { answers.value[question.value.key] = value }
const next = async () => {
  if (!canContinue.value) return
  if (currentStep.value < questions.value.length - 1) currentStep.value++
  else {
    completed.value = true
    await nextTick()
    document.querySelector('.results-shell')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}
const back = () => {
  if (completed.value) completed.value = false
  else if (currentStep.value > 0) currentStep.value--
}
const reset = () => {
  resetGoiYThongMinh()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const loadProducts = async () => {
  loading.value = true
  try {
    const res = await api.get('/api/san-pham', { params: { page: 0, size: 100, trangThai: 1 } })
    const list = getMergedProducts(res.data?.content || (Array.isArray(res.data) ? res.data : []))
      .filter(p => Number(p.trangThai ?? 1) === 1)
    await attachSalesCountToProducts(list)
    await Promise.allSettled(list.map(async p => {
      const vr = await api.get(`/api/chi-tiet-san-pham/by-san-pham/${p.id}`)
      const variants = Array.isArray(vr.data) ? vr.data : []
      const colors = new Set(), sizes = new Set(), colorImageMap = {}
      variants.forEach(v => {
        const active = Number(v.trangThai ?? 1) === 1 && Number(v.soLuongTon ?? v.soLuong ?? 0) > 0
        if (!active) return
        const color = v.tenMauSac || v.mauSac
        const size = v.tenKichCo || v.tenKichThuoc || v.kichCo || v.kichThuoc
        if (color) colors.add(String(color).trim())
        if (size) sizes.add(String(size).trim())
        if (color && v.anhDaiDien && !colorImageMap[color]) colorImageMap[color] = v.anhDaiDien
      })
      p.colors = [...colors]; p.sizes = [...sizes]; p.colorImageMap = colorImageMap
    }))
    try {
      const saleRes = await api.get('/api/dot-giam-gia/active-products-map')
      list.forEach(p => {
        const sale = saleRes.data?.[p.id]
        if (!sale) return
        p.coGiamGia = true; p.phanTramGiam = Number(sale.phanTramGiam || 0); p.tenDotGiamGia = sale.tenDotGiamGia
        if (p.minGia && p.phanTramGiam) p.minGiaSauGiam = Math.round(Number(p.minGia) * (100 - p.phanTramGiam) / 100)
      })
    } catch (e) { console.warn('Không tải được đợt giảm giá:', e) }
    products.value = list.filter(p => (p.colors?.length || p.sizes?.length))
  } catch (e) {
    console.error('Không tải được sản phẩm gợi ý:', e)
  } finally { loading.value = false }
}

onMounted(loadProducts)
</script>

<template>
  <main class="ai-finder-page">
    <section v-if="!completed" class="quiz-shell">
      <div class="progress-meta"><span>Câu hỏi {{ currentStep + 1 }} / {{ questions.length }}</span><b>{{ progress }}%</b></div>
      <div class="progress-track"><i :style="{ width: `${progress}%` }"></i></div>
      <article class="question-card">
        <span class="question-number">{{ String(currentStep + 1).padStart(2, '0') }}</span>
        <h2>{{ question.title }}</h2>
        <p>{{ question.hint }}</p>
        <div class="answer-grid">
          <button v-for="option in question.options" :key="option[0]" type="button"
                  :class="['answer-option', { selected: selectedAnswer === option[0] }]" @click="choose(option[0])">
            <i></i><span>{{ option[1] }}</span><b v-if="selectedAnswer === option[0]">✓</b>
          </button>
        </div>
        <div v-if="question.key === 'budget' && selectedAnswer === 'custom'" class="custom-budget-box">
          <label>
            <span>Từ giá</span>
            <div><input v-model.number="customBudgetMin" type="number" min="0" step="10000" placeholder="Ví dụ: 150000"><b>đ</b></div>
          </label>
          <label>
            <span>Đến giá</span>
            <div><input v-model.number="customBudgetMax" type="number" min="0" step="10000" placeholder="Ví dụ: 350000"><b>đ</b></div>
          </label>
          <small v-if="customBudgetMin && customBudgetMax && customBudgetMax < customBudgetMin">Giá tối đa phải lớn hơn hoặc bằng giá tối thiểu.</small>
        </div>
      </article>
      <div class="quiz-actions">
        <button type="button" class="back-btn" :disabled="currentStep === 0" @click="back">← Quay lại</button>
        <button type="button" class="next-btn" :disabled="!canContinue" @click="next">
          {{ currentStep === questions.length - 1 ? 'Xem sản phẩm phù hợp' : 'Tiếp tục' }} →
        </button>
      </div>
    </section>

    <section v-else class="results-shell">
      <div class="results-heading">
        <div class="results-heading-copy">
          <span>{{ isNearMatchResult ? 'GỢI Ý THAY THẾ' : 'KẾT QUẢ GỢI Ý' }}</span>
          <h2>{{ resultTitle }}</h2>
          <p>{{ resultDescription }}</p>
        </div>
        <button type="button" @click="reset"><b aria-hidden="true">↻</b> Chọn lại</button>
      </div>
      <div v-if="loading" class="state-box">Đang phân tích sản phẩm đang bán...</div>
      <div v-else-if="recommendations.length" class="recommend-grid">
        <article v-for="item in recommendations" :key="item.product.id" class="recommend-card">
          <div class="match-row">
            <strong>{{ isNearMatchResult ? 'Gần phù hợp' : 'Phù hợp' }} {{ item.matchPercent }}%</strong>
            <span>{{ item.consideredCriteria ? `${item.matchedCriteria}/${item.consideredCriteria} tiêu chí` : 'Đang còn hàng' }}</span>
          </div>
          <TheSanPham :product="item.product" />
          <div class="reason-list">
            <span v-for="reason in item.reasons" :key="reason">✓ {{ reason }}</span>
            <span v-if="!item.reasons.length">Sản phẩm đang còn biến thể để bạn tham khảo</span>
          </div>
        </article>
      </div>
      <div v-else class="state-box">Chưa có sản phẩm còn hàng phù hợp. Hãy thử thay đổi câu trả lời.</div>
    </section>
  </main>
</template>

<style scoped>
.ai-finder-page{min-height:100vh;background:#f7f9fc;padding-bottom:70px;color:#142033;font-family:'Be Vietnam Pro',sans-serif}.finder-hero{text-align:center;padding:48px 20px 36px;background:linear-gradient(180deg,#eef5fb,#f7f9fc)}.finder-hero>span,.results-heading span{font-size:12px;font-weight:800;letter-spacing:2px;color:#4d83b4}.finder-hero h1{margin:10px 0 8px;font-size:36px}.finder-hero p,.question-card>p,.results-heading p{color:#718096}.quiz-shell,.results-shell{width:min(1120px,calc(100% - 32px));margin:auto}.progress-meta{display:flex;justify-content:space-between;font-size:13px;color:#64748b;margin:18px 0 9px}.progress-meta b{color:#3b82c4}.progress-track{height:6px;background:#e2e8f0;border-radius:8px;overflow:hidden}.progress-track i{display:block;height:100%;background:linear-gradient(90deg,#2993dc,#496883);transition:width .25s}.question-card{margin-top:28px;background:#fff;border:1px solid #dde6ef;border-radius:16px;padding:38px 42px;box-shadow:0 12px 35px rgba(39,72,105,.06)}.question-number{font-size:13px;color:#8cb6d8;font-weight:800}.question-card h2{font-size:27px;margin:10px 0 6px}.answer-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px;margin-top:30px}.answer-option{min-height:68px;border:1px solid #dce5ee;border-radius:10px;background:white;padding:14px 18px;display:flex;align-items:center;gap:13px;text-align:left;color:#435269;font-size:14px;cursor:pointer}.answer-option i{width:18px;height:18px;border:2px solid #9badbf;border-radius:50%}.answer-option b{margin-left:auto}.answer-option:hover{border-color:#6da5d2}.answer-option.selected{border:2px solid #3c8dcb;background:#edf6ff;color:#245f91}.answer-option.selected i{border:5px solid #3c8dcb}.quiz-actions{display:grid;grid-template-columns:170px 1fr;gap:16px;margin-top:25px}.quiz-actions button,.results-heading button{height:54px;border-radius:9px;font-weight:750;cursor:pointer}.back-btn{background:#fff;border:1px solid #d9e2eb;color:#526174}.next-btn{border:0;background:#347fbd;color:white}.quiz-actions button:disabled{opacity:.45;cursor:not-allowed}.results-shell{padding-top:32px}.results-heading{display:flex;justify-content:space-between;align-items:flex-end;gap:20px;margin-bottom:25px}.results-heading h2{font-size:28px;margin:7px 0}.results-heading button{height:42px;padding:0 18px;background:#fff;border:1px solid #b9c9d8;color:#496883}.recommend-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:18px}.recommend-card{background:#fff;border:1px solid #dce5ee;border-radius:13px;padding:10px;box-shadow:0 8px 24px rgba(39,72,105,.06)}.match-row{display:flex;justify-content:space-between;gap:8px;padding:6px 4px 10px;font-size:11px}.match-row strong{color:#b17b2d}.match-row span{color:#328056;font-weight:700}.reason-list{border-top:1px dashed #dce5ee;padding:10px 5px 4px;display:flex;flex-direction:column;gap:4px;font-size:11px;color:#64748b}.state-box{padding:60px;text-align:center;background:white;border-radius:14px;color:#64748b}@media(max-width:950px){.recommend-grid{grid-template-columns:repeat(2,1fr)}}@media(max-width:640px){.finder-hero h1{font-size:27px}.question-card{padding:26px 20px}.answer-grid{grid-template-columns:1fr}.quiz-actions{grid-template-columns:110px 1fr}.recommend-grid{grid-template-columns:1fr}.results-heading{align-items:flex-start;flex-direction:column}}
.custom-budget-box{margin-top:18px;padding:18px;border:1px dashed #90b8d8;border-radius:10px;background:#f7fbff;display:grid;grid-template-columns:1fr 1fr;gap:14px}.custom-budget-box label>span{display:block;margin-bottom:7px;color:#526174;font-size:12px;font-weight:700}.custom-budget-box label>div{display:flex;align-items:center;border:1px solid #cedbea;border-radius:8px;background:#fff;overflow:hidden}.custom-budget-box input{width:100%;border:0;outline:0;padding:12px;font:inherit}.custom-budget-box label b{padding-right:12px;color:#64748b}.custom-budget-box small{grid-column:1/-1;color:#dc2626}.recommend-card{background:#fff;border:1px solid #dce5ee;border-radius:13px;padding:10px;box-shadow:0 8px 24px rgba(39,72,105,.06);display:flex;flex-direction:column;gap:0;overflow:hidden}.recommend-card :deep(.the-san-pham-card){height:auto;border:0;border-radius:0;box-shadow:none}.match-row{background:#fff;border:0;border-bottom:1px solid #e5ebf1;border-radius:0;padding:5px 4px 10px;margin:0}.reason-list{background:#fff;border:0;border-top:1px dashed #dce5ee;border-radius:0;padding:10px 4px 2px;margin:0}@media(max-width:640px){.custom-budget-box{grid-template-columns:1fr}}
.finder-hero--results{padding:32px 20px 28px;border-bottom:1px solid #dce8f3}.finder-hero--results h1{font-size:31px;margin:7px 0 5px}.finder-hero--results p{margin:0;font-size:14px}.results-shell{padding-top:30px}.results-heading{position:relative;align-items:center;margin-bottom:22px;padding:23px 24px 23px 28px;background:linear-gradient(135deg,#fff 0%,#f8fbff 100%);border:1px solid #dbe7f1;border-radius:15px;box-shadow:0 10px 28px rgba(39,72,105,.07);overflow:hidden}.results-heading::before{content:'';position:absolute;inset:0 auto 0 0;width:5px;background:linear-gradient(180deg,#3494d4,#496883)}.results-heading-copy{display:grid;gap:3px}.results-heading h2{margin:2px 0 0;font-size:25px}.results-heading p{margin:2px 0 0;font-size:13px}.results-heading button{display:inline-flex;align-items:center;gap:7px;height:40px;padding:0 16px;white-space:nowrap;color:#315f86;background:#fff;border:1px solid #aac3d8;border-radius:10px;transition:all .18s ease}.results-heading button:hover{color:#fff;background:#496883;border-color:#496883;box-shadow:0 6px 14px rgba(73,104,131,.18);transform:translateY(-1px)}.results-heading button b{font-size:16px}@media(max-width:640px){.finder-hero--results{padding:26px 18px 22px}.finder-hero--results h1{font-size:26px}.results-shell{padding-top:22px}.results-heading{padding:19px 18px 19px 22px;align-items:flex-start}.results-heading button{height:38px}.results-heading h2{font-size:22px}}
</style>
