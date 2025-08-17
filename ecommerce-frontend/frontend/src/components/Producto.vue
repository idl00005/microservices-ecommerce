<template>
  <div class="card">
    <img :src="urlImg" alt="producto" class="card-img">
    <h4>
      {{nombre}}
    </h4>
    <p class="price">
      {{precio}}€
    </p>
    <button @click="agregarAlCarrito">
      <i class="bi bi-cart-plus"></i>
    </button>
  </div>
</template>

<script>
export default {
  name: 'Producto',
  methods: {
    agregarAlCarrito() {
      this.$emit('agregarAlCarrito',this.id);
    }
  },
  props: {
    id: Number,
    nombre: String,
    precio: Number,
    urlImg: String
  }
}
import 'bootstrap-icons/font/bootstrap-icons.css'
</script>


<style scoped>
.card {
  border: 1px solid rgba(184, 184, 184, 0.5);
  border-radius: 5px;
  padding: 5px;
  width: 275px;
  box-shadow: 2px 2px 8px rgba(248, 248, 248, 0.75);
  margin: 20px;
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

.price {
  font-size: 25px;
}
</style>