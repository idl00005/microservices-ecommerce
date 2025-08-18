import { createRouter, createWebHistory } from 'vue-router'
import Catalogo from '@/views/Catalogo.vue'
import Login from '@/views/Login.vue'

const routes = [
    { path: '/', name: 'catalogo', component: Catalogo },
    { path: '/login', name: 'login', component: Login }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
