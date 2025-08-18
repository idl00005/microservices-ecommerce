<template>
  <div v-if="producto" class="detalle-container">
    <img :src="producto.imagenUrl" alt="Imagen del producto" class="detalle-imagen"/>

    <div class="detalle-info">
      <h2>{{ producto.nombre }}</h2>
      <p class="categoria">Categoría: {{ producto.categoria }}</p>
      <p class="descripcion">{{ producto.descripcion }}</p>
      <p class="precio">Precio: {{ producto.precio }} €</p>
      <p class="stock">Stock: {{ producto.stock }}</p>
      <p class="puntuacion">Puntuación: {{ producto.puntuacion }}</p>

      <div class="detalles-extra">
        <h3>Detalles</h3>
        <ul>
          <li v-for="(valor, clave) in producto.detalles" :key="clave">
            {{ clave }}: {{ valor }}
          </li>
        </ul>
      </div>
    </div>
  </div>

  <div v-else class="cargando">
    Cargando producto...
  </div>
</template>

<script>
export default {
  name: "ProductoDetalle",
  data() {
    return {
      producto: null
    }
  },
  mounted() {
    const id = this.$route.params.id;
    fetch(`http://microservicios.local/catalogo/${id}`)
        .then(res => {
          if (!res.ok) {
            throw new Error("Error al obtener el producto");
          }
          return res.json();
        })
        .then(data => {
          this.producto = data;
        })
        .catch(err => {
          console.error(err);
        });
  }
}
</script>

<style scoped>
.detalle-container {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin: 20px;
  justify-content: center;
}

.detalle-imagen {
  max-width: 400px;
  width: 100%;
  object-fit: contain;
  border: 1px solid #ccc;
  border-radius: 8px;
}

.detalle-info {
  max-width: 500px;
}

.categoria, .descripcion, .precio, .stock, .puntuacion {
  margin: 5px 0;
}

.detalles-extra {
  margin-top: 20px;
}

.detalles-extra ul {
  list-style: none;
  padding: 0;
}

.detalles-extra li {
  background: #f3f3f3;
  margin-bottom: 5px;
  padding: 8px;
  border-radius: 4px;
}

.cargando {
  text-align: center;
  font-size: 18px;
  padding: 20px;
}
</style>
