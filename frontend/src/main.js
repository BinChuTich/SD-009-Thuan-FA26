import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import TrangChu from './views/TrangChu.vue'
import HoaDon from './views/HoaDon.vue'
import './style.css'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            component: TrangChu
        },
        {
            path: '/hoa-don',
            component: HoaDon
        }
    ]
})

createApp(App)
    .use(router)
    .mount('#app')