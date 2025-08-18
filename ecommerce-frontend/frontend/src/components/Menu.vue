<template>
  <nav class="menu">
    <div class="logo">Mi Tienda</div>
    <ul class="menu-links">
      <li><router-link to="/">Productos</router-link></li>
      <li><router-link to="/carrito">Carrito</router-link></li>
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
export default {
  name: "Menu",
  data() {
    return {
      estaLogueado: false
    };
  },
  mounted() {
    this.checkLogin();
    window.addEventListener("login", this.checkLogin);
  },
  methods: {
    checkLogin() {
      const token = localStorage.getItem("jwt");
      this.estaLogueado = !!token;
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
</style>
