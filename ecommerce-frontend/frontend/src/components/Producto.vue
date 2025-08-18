<template>
  <div class="card">
    <img :src="urlImg" alt="producto" class="card-img clickable" @click="irADetalle">
    <h6 @click="irADetalle" class="clickable">
      {{nombre}}
    </h6>
    <p class="price clickable" @click="irADetalle">
      {{precio}}€
    </p>
    <button @click="agregarAlCarrito" class="buy-btn">Comprar
      <i class="bi bi-cart-plus"></i>
    </button>
  </div>
</template>

<script>
export default {
  name: 'Producto',
  props: {
    id: Number,
    nombre: String,
    precio: Number,
    urlImg: String
  },
  methods: {
    async agregarAlCarrito() {
      const token = localStorage.getItem("jwt");

      if (!token) {
        // No hay sesión, redirigir al login
        alert("Debes iniciar sesión para agregar productos al carrito.");
        if (this.$router && typeof this.$router.push === 'function') {
          this.$router.push('/login');
        } else {
          window.location.href = '/login';
        }
        return;
      }

      const bodyPayload = {
        productoId: this.id,
        cantidad: 1
      };

      try {
        const res = await fetch("http://microservicios.local/carrito", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Accept": "application/json",
            "Authorization": `Bearer ${token}`
          },
          body: JSON.stringify(bodyPayload)
        });

        if (!res.ok) {
          console.error("No se pudo agregar el producto al carrito.");
          return;
        }

        const data = await res.json();
        console.log("Producto agregado al carrito:", data);

      } catch (err) {
        console.error("Error al agregar al carrito:", err);
      }
    },
    irADetalle() {
      this.$router.push({
        name: "detalle",
        params: { id: this.id }
      });
    }
  }
}
import 'bootstrap-icons/font/bootstrap-icons.css'
</script>


<style scoped>
.card {
  border: 1px solid rgba(184, 184, 184, 0.5);
  border-radius: 5px;
  padding: 5px;
  width: 225px;
  box-shadow: 2px 2px 8px rgba(248, 248, 248, 0.75);
  margin-bottom: 10px;
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}


.card-img {
  width: 100%;
  height: 200px;
  object-fit: contain;
  border-radius: 5px;
}

.clickable:hover {
  cursor: pointer;
}

.price {
  font-size: 17px;
}

.buy-btn {
  margin-top: auto;
  align-self: flex-start;
}
</style>