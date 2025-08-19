<template>
  <div v-if="producto" class="detalle-root">
    <div class="detalle-main">
      <!-- Imagen -->
      <div class="imagen-wrapper" role="img" :aria-label="`Imagen de ${producto.nombre}`">
        <img
            :src="producto.imagenUrl"
            :alt="producto.nombre"
            class="detalle-imagen"
        />
      </div>

      <!-- Información central -->
      <div class="detalle-info">
        <h1 class="nombre">{{ producto.nombre }}</h1>
        <p class="categoria">Categoría: {{ producto.categoria }}</p>
        <p class="descripcion">{{ producto.descripcion }}</p>

        <div
            class="detalles-extra"
            v-if="producto.detalles && Object.keys(producto.detalles).length"
        >
          <h3>Detalles</h3>
          <ul>
            <li v-for="(valor, clave) in producto.detalles" :key="clave">
              <strong>{{ clave }}:</strong> {{ valor }}
            </li>
          </ul>
        </div>

        <p class="stock" v-if="producto.stock !== undefined">Stock: {{ producto.stock }}</p>

        <!-- Puntuación con estrellas -->
        <div class="puntuacion" v-if="producto.puntuacion !== undefined">
          <span v-for="(star, index) in getEstrellas(producto.puntuacion)"
                :key="index"
                class="estrella"
                :class="star">
          </span>
        </div>
      </div>

      <!-- Panel de compra a la derecha -->
      <aside class="compra-panel" aria-labelledby="compra-title">
        <div id="compra-title" class="compra-header">
          <div class="precio">
            <span class="label">Precio</span>
            <span class="valor">{{ formatoPrecio(producto.precio) }}</span>
          </div>
        </div>

        <div class="compra-body">
          <label for="cantidad" class="label-cantidad">Cantidad</label>
          <input
              id="cantidad"
              type="number"
              min="1"
              :max="producto.stock ?? 9999"
              v-model.number="cantidad"
              class="input-cantidad"
              aria-live="polite"
          />

          <button
              class="btn-comprar"
              @click="agregarAlCarrito"
              :disabled="loading || (producto.stock !== undefined && cantidad > producto.stock) || cantidad < 1"
              aria-busy="loading ? 'true' : 'false'"
          >
            <span v-if="!loading">Añadir al carrito</span>
            <span v-else>Agregando...</span>
            <div class="btn-precio">{{ formatoPrecio(producto.precio * (cantidad || 1)) }}</div>
          </button>

          <!-- Clase condicional: agrega 'agotado' cuando stock <= 0 -->
          <p
              class="nota-stock"
              v-if="producto.stock !== undefined"
              :class="{ agotado: producto.stock <= 0 }"
              aria-live="polite"
          >
            {{ producto.stock > 0 ? 'En stock' : 'Agotado' }}
          </p>
        </div>
      </aside>
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
      producto: null,
      id: null,
      cantidad: 1,
      loading: false
    };
  },
  mounted() {
    const id = this.$route?.params?.id || null;
    this.id = id;
    if (!id) return;

    fetch(`http://microservicios.local/catalogo/${id}`)
        .then(res => {
          if (!res.ok) throw new Error("Error al obtener el producto");
          return res.json();
        })
        .then(data => {
          this.producto = data;
          this.cantidad = 1;
        })
        .catch(err => console.error(err));
  },
  methods: {
    formatoPrecio(valor) {
      if (typeof valor !== "number") return valor;
      return valor.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + " €";
    },
    getEstrellas(puntuacion) {
      const estrellas = [];
      const llenas = Math.floor(puntuacion);
      const hayMedia = puntuacion - llenas >= 0.5;
      const vacias = 5 - llenas - (hayMedia ? 1 : 0);

      for (let i = 0; i < llenas; i++) estrellas.push('llena');
      if (hayMedia) estrellas.push('media');
      for (let i = 0; i < vacias; i++) estrellas.push('vacia');
      return estrellas;
    },
    async agregarAlCarrito() {
      const token = localStorage.getItem("jwt");
      if (!token) {
        alert("Debes iniciar sesión para agregar productos al carrito.");
        if (this.$router && typeof this.$router.push === "function") {
          this.$router.push("/login");
        } else {
          window.location.href = "/login";
        }
        return;
      }

      const cantidadEnviar = Number(this.cantidad) || 1;
      if (cantidadEnviar < 1) {
        alert("La cantidad debe ser al menos 1.");
        return;
      }
      if (this.producto?.stock !== undefined && cantidadEnviar > this.producto.stock) {
        alert("La cantidad supera el stock disponible.");
        return;
      }

      const bodyPayload = {
        productoId: this.producto.id ?? this.id,
        cantidad: cantidadEnviar
      };

      this.loading = true;
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
          const text = await res.text().catch(() => null);
          alert("Error al agregar al carrito" + (text ? `: ${text}` : "."));
          this.loading = false;
          return;
        }

        const data = await res.json();
        console.log("Producto agregado al carrito:", data);
        alert("Producto agregado al carrito correctamente.");
      } catch (err) {
        console.error("Error al agregar al carrito:", err);
        alert("Error al agregar al carrito. Revisa la consola.");
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>

<style scoped>
/* Layout principal */
.detalle-main {
  display: flex;
  gap: 24px;
  align-items: flex-start;
  padding: 24px;
  box-sizing: border-box;
  justify-content: center;
}

.imagen-wrapper {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  min-width: 320px;
  max-width: 640px;
  width: 45%;
  box-shadow: 0 2px 6px rgba(0,0,0,0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
}

.detalle-imagen {
  max-width: 100%;
  max-height: 70vh;
  width: auto;
  height: auto;
  display: block;
  object-fit: contain;
  background: white;
  border-radius: 4px;
}

.detalle-info {
  width: 35%;
  max-width: 600px;
  min-width: 220px;
  box-sizing: border-box;
  color: #222222;
}

.nombre {
  margin: 0 0 8px 0;
  font-size: 1.6rem;
}

.categoria,
.descripcion,
.stock,
.puntuacion {
  margin: 8px 0;
  line-height: 1.4;
}

.detalles-extra {
  margin-top: 12px;
}

.detalles-extra ul {
  list-style: none;
  padding: 0;
  margin: 8px 0 0 0;
}

.detalles-extra li {
  background: #f3f3f3;
  margin-bottom: 6px;
  padding: 8px;
  border-radius: 4px;
}

.puntuacion {
  font-size: 1.2rem;
  margin: 8px 0;
}

.estrella {
  display: inline-block;
  font-size: 1.4rem;
  color: #ccc;
  margin-right: 2px;
}
.estrella.llena::before {
  content: "★";
  color: #FFD700;
}
.estrella.media::before {
  content: "★";
  background: linear-gradient(90deg, #FFD700 50%, #ccc 50%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.estrella.vacia::before {
  content: "★";
  color: #ccc;
}

.compra-panel {
  width: 20%;
  min-width: 200px;
  background: #fafafa;
  border: 1px solid #e6e6e6;
  border-radius: 8px;
  padding: 16px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: stretch;
  color: #222222;
}

.compra-header .precio {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 8px;
}
.precio .label {
  font-size: 0.9rem;
  color: #333;
}
.precio .valor {
  font-size: 1.25rem;
  font-weight: 700;
}

/* Cuerpo del panel */
.compra-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.label-cantidad {
  font-size: 0.9rem;
}

.input-cantidad {
  width: 100%;
  padding: 8px;
  border-radius: 6px;
  border: 1px solid #ccc;
  font-size: 1rem;
  box-sizing: border-box;
}

/* Botón principal */
.btn-comprar {
  margin-top: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  transition: background-color 0.2s;
  background: #00b3b3;
  color: #fff;
}

.btn-comprar:hover {
  background: #468e8e;
  color: #fff;
}

.btn-comprar[disabled] {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-precio {
  font-weight: 700;
  font-size: 0.95rem;
}


.nota-stock {
  font-size: 0.9rem;
  color: #5fca3e;
  font-weight: bold;
}


.nota-stock.agotado {
  color: #000;
  font-style: italic;
  font-weight: 400;
}

/* Cargando */
.cargando {
  text-align: center;
  font-size: 18px;
  padding: 20px;
  color: #000;
}

@media (max-width: 900px) {
  .detalle-main {
    flex-direction: column;
    align-items: stretch;
  }

  .imagen-wrapper,
  .detalle-info,
  .compra-panel {
    width: 100%;
  }

  .imagen-wrapper {
    max-width: none;
  }
}
</style>
