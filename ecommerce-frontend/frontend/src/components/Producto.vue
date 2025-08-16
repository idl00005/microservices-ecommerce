<template>
  <div class="card">
    <img :src="urlImg" alt="" width="250">
    <h3>
      {{nombre}}
    </h3>
    <p class="price">
      {{precio}}€
    </p>
    <button>
      <i class="bi bi-cart-plus"></i>
    </button>
  </div>
</template>

<script>
export default {
  name: 'Producto',
  methods: {},
  props: {
    nombre: String,
    precio: Number,
    urlImg: String
  }
}
import 'bootstrap-icons/font/bootstrap-icons.css'
</script>

<style scoped>
  .card{
    border: 1px solid rgba(184, 184, 184, 0.5);
    border-radius: 5px;
    padding: 5px;
    width: auto;
    max-width: 275px;
    box-shadow: 2px 2px 8px rgba(248, 248, 248, 0.75);
    margin: 20px;
  }
  .price{
    font-size: 25px;
  }
</style>