import { createRouter, createWebHistory } from 'vue-router'
import Catalogo from '@/views/Catalogo.vue'
import Login from '@/views/Login.vue'
import ProductoDetalle from "@/views/ProductoDetalle.vue";
import CarritoVista from "@/views/CarritoVista.vue";
import CheckoutCompra from "@/views/CheckoutCompra.vue";

const routes = [
    { path: '/', name: 'catalogo', component: Catalogo },
    { path: '/login', name: 'login', component: Login },
    { path: '/producto/:id', name: 'detalle', component: ProductoDetalle, props: true },
    { path: '/carrito', name: 'carrito', component: CarritoVista},
    { path: '/checkout', name: 'checkout', component: CheckoutCompra}
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
