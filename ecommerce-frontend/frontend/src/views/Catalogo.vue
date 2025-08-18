<script setup>
import Producto from "@/components/Producto.vue";
</script>

<script>
export default {
  data() {
    return {
      listaproductos: [],
      page: 1,
    }
  },
  mounted() {
    fetch("http://microservicios.local/catalogo?size=16")
        .then(response => response.json())
        .then(data => {
          this.listaproductos = data;
        })
        .catch(error => console.error("Error al obtener los datos:", error));
  },
  methods: {
    agregarAlCarrito(idProducto) {
      console.log("Producto agregado al carrito:", idProducto);
    },
    cargarProductos() {
      this.page++;
      fetch(`http://microservicios.local/catalogo?page=${this.page}&size=16`)
          .then(response => response.json())
          .then(data => {
            this.listaproductos = [...this.listaproductos, ...data];
          })
          .catch(error => console.error("Error al obtener los datos:", error));
    }
  }
}
</script>

<template>
  <div class="cards-container">
    <Producto
        v-for="producto in listaproductos"
        :key="producto.id"
        @agregarAlCarrito="agregarAlCarrito($event)"
        :id="producto.id"
        :nombre="producto.nombre"
        :precio="producto.precio"
        :urlImg="producto.imagenUrl"
    />
  </div>

  <div class="load-more-container">
    <button @click="cargarProductos">Cargar más</button>
  </div>
</template>

<style scoped>
.cards-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(275px, 1fr));
  width: auto;
  justify-items: center;
}

.load-more-container {
  text-align: center;
  margin: 20px;
}

.load-more-container button {
  padding: 10px 20px;
  font-size: 16px;
  cursor: pointer;
}
</style>
