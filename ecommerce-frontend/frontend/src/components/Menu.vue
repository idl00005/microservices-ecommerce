<template>
  <nav class="menu">
    <div class="logo">Mi Tienda</div>
    <ul class="menu-links">
      <li><router-link to="/">Productos</router-link></li>
      <li class="carrito-item">
        <router-link to="/carrito">
          Carrito
          <span v-if="tieneProductos" class="cart-indicator"></span>
        </router-link>
      </li>
      <li v-if="!estaLogueado">
        <router-link to="/login" custom v-slot="{ navigate }">
          <button @click="navigate" class="btn-login">Iniciar Sesión</button>
        </router-link>
      </li>
      <li v-else>
        <button @click="cerrarSesion" class="btn-logout">Cerrar Sesión</button>
      </li>
    </ul>
  </nav>
</template>

<script>
const URL_CARRITO = window.APP_CONFIG.API_CARRITO;
export default {
  name: "Menu",
  data() {
    return {
      estaLogueado: false,
      tieneProductos: false
    };
  },
  mounted() {
    this.checkLogin();
    window.addEventListener("login", this.checkLogin);
    window.addEventListener("carrito-actualizado", this.checkCart);
  },
  methods: {
    checkLogin() {
      const token = localStorage.getItem("jwt");
      this.estaLogueado = !!token;
    },
    async checkCart() {
      try {
        const token = localStorage.getItem("jwt");
        if (!token) {
          this.tieneProductos = false;
          return;
        }
        const resp = await fetch(URL_CARRITO, {
          headers: { Authorization: `Bearer ${token}` }
        });
        if (!resp.ok) {
          this.tieneProductos = false;
          return;
        }
        const data = await resp.json();
        this.tieneProductos = Array.isArray(data) && data.length > 0;

      } catch (e) {
        console.error("Error consultando el carrito:", e);
        this.tieneProductos = false;
      }
    },
    cerrarSesion() {
      localStorage.removeItem("jwt");
      localStorage.removeItem("user_info");
      this.estaLogueado = false;
      this.$router.push('/');
    }
  }
};
</script>

<style scoped>
.menu {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 34px;
  background-color: rgba(99, 99, 99, 0.5);
  color: white;
}

.logo {
  font-size: 30px;
  font-weight: bold;
}

.menu-links {
  list-style: none;
  display: flex;
  gap: 20px;
  margin: 0;
  padding: 0;
}

.menu-links a,
.menu-links button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin: 8px 8px;
  font-size: 14px;
  border-radius: 5px;
  text-decoration: none;
  cursor: pointer;
  border: none;
  color: white;
  transition: background-color 0.2s;
}

.menu-links a:hover{
  background-color: #84acac;
}

.btn-login {
  background-color: #00b3b3;
  color: white;
  padding: 4px 10px;
  border-radius: 5px;
  font-weight: bold;
  transition: background-color 0.2s;
}

.btn-login:hover {
  background-color: #468e8e;
  text-decoration: none;
}

.btn-logout {
  background-color: #dc3545;
  color: white;
  padding: 4px 10px;
  border-radius: 5px;
  font-weight: bold;
  border: none;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn-logout:hover {
  background-color: #a71d2a;
}

.carrito-item {
  position: relative;
}

.cart-indicator {
  position: absolute;
  top: 5px;
  right: 0px;
  width: 10px;
  height: 10px;
  background-color: red;
  border-radius: 50%;
  z-index: 1;
}

/* ondas */
.cart-indicator::before,
.cart-indicator::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 11px;
  height: 11px;
  background-color: rgba(255, 0, 0, 0.4);
  border-radius: 50%;
  transform: translate(-50%, -50%);
  z-index: -1;
  animation: wave 1.6s infinite;
}

.cart-indicator::after {
  animation-delay: 1s;
}

@keyframes wave {
  0% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 0.7;
  }
  70% {
    transform: translate(-50%, -50%) scale(2.5);
    opacity: 0;
  }
  100% {
    opacity: 0;
  }
}
</style>
