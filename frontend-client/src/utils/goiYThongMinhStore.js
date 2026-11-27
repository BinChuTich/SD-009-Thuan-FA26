import { ref } from 'vue'

const createEmptyAnswers = () => ({
  category: '',
  style: '',
  material: '',
  color: '',
  size: '',
  budget: ''
})

// Store nằm trong bộ nhớ của ứng dụng: giữ dữ liệu khi chuyển route nhưng tự xóa khi reload trang.
export const goiYCurrentStep = ref(0)
export const goiYCompleted = ref(false)
export const goiYAnswers = ref(createEmptyAnswers())
export const goiYCustomBudgetMin = ref(null)
export const goiYCustomBudgetMax = ref(null)

export const resetGoiYThongMinh = () => {
  goiYAnswers.value = createEmptyAnswers()
  goiYCustomBudgetMin.value = null
  goiYCustomBudgetMax.value = null
  goiYCurrentStep.value = 0
  goiYCompleted.value = false
}
