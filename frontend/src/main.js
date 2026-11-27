import { createApp } from 'vue'
import App from './App.vue'
import './style.css'
import router from './router'
import AdminPageNumbers from './components/common/AdminPageNumbers.vue'

createApp(App)
    .use(router)
    .component('AdminPageNumbers', AdminPageNumbers)
    .mount('#app')
