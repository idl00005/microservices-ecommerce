<template>
  <section class="carrito-root">
    <h1 class="title">Tu carrito</h1>

    <div v-if="loading" class="estado">Cargando carrito...</div>
    <div v-else-if="error" class="estado error">{{ error }}</div>
    <div v-else>
      <div v-if="items.length === 0" class="vacío">
        Tu carrito está vacío.
      </div>

      <div v-else class="contenido">
        <div class="lista">
          <CarritoItem
              v-for="item in items"
              :key="item.productoId"
              :item="item"
              @eliminar="eliminarItem"
              @actualizar-cantidad="actualizarCantidad"
          />
        </div>

        <aside class="resumen">
          <div class="resumen-card">
            <h2>Resumen del pedido</h2>

            <div class="line">
              <span>Subtotal</span>
              <span>{{ formatoPrecio(subtotal) }}</span>
            </div>

            <div class="line total">
              <span>Total</span>
              <span>{{ formatoPrecio(total) }}</span>
            </div>

            <button class="btn-pagar" @click="continuarPago" :disabled="items.length === 0">
              Continuar al pago
            </button>
          </div>
        </aside>
      </div>
    </div>
  </section>
</template>

<script>
import CarritoItem from "@/components/CarritoItem.vue";

const URL_CARRITO= window.APP_CONFIG.API_CARRITO;

export default {
  name: "CarritoVista",
  components: { CarritoItem },
  data() {
    return {
      items: [],
      loading: true,
      error: null
    };
  },
  computed: {
    subtotal() {
      return this.items.reduce((acc, it) => acc + (Number(it.precio || 0) * Number(it.cantidad || 0)), 0);
    },
    total() {
      return this.subtotal;
    }
  },
  mounted() {
    this.cargarCarrito();
  },
  methods: {
    formatoPrecio(valor) {
      if (typeof valor !== "number") valor = Number(valor) || 0;
      return valor.toLocaleString("es-ES", { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + " €";
    },

    async cargarCarrito() {
      this.loading = true;
      this.error = null;

      const token = localStorage.getItem("jwt");
      if (!token) {
        alert("Debes iniciar sesión para ver el carrito.");
        if (this.$router && typeof this.$router.push === "function") {
          this.$router.push("/login");
        } else {
          window.location.href = "/login";
        }
        return;
      }

      try {
        const res = await fetch(URL_CARRITO, {
          method: "GET",
          headers: {
            "Accept": "application/json",
            "Authorization": `Bearer ${token}`
          }
        });
        if (!res.ok) {
          const text = await res.text().catch(() => null);
          throw new Error(text || `Error ${res.status} al obtener el carrito`);
        }
        const data = await res.json();

        this.items = Array.isArray(data) ? data.map(i => ({
          productoId: i.productoId,
          nombre: i.nombre,
          imagenUrl: i.imagenUrl,
          cantidad: Number(i.cantidad) || 1,
          precio: Number(i.precio) || 0
        })) : [];
      } catch (err) {
        console.error("Error cargando carrito:", err);
        this.error = "No se pudo cargar el carrito.";
      } finally {
        this.loading = false;
      }
    },

    async eliminarItem(productoId) {
      const token = localStorage.getItem("jwt");
      if (!token) {
        alert("Debes iniciar sesión.");
        return;
      }

      try {
        const res = await fetch(URL_CARRITO+`/${productoId}`, {
          method: "DELETE",
          headers: {
            "Accept": "application/json",
            "Authorization": `Bearer ${token}`
          }
        });
        if (!res.ok) {
          console.warn("DELETE carrito devolvió", res.status);
        }
        window.dispatchEvent(new Event("carrito-actualizado"));
      } catch (e) {
        console.warn("No se pudo llamar DELETE carrito:", e);
      } finally {
        this.items = this.items.filter(i => i.productoId !== productoId);
      }
    },

    // Actualiza cantidad local y, opcionalmente, llama al backend
    async actualizarCantidad({ productoId, cantidad }) {
      cantidad = Number(cantidad) || 1;
      if (cantidad < 1) cantidad = 1;

      const token = localStorage.getItem("jwt");
      if (!token) {
        alert("Debes iniciar sesión.");
        return;
      }

      try {
        const res = await fetch(URL_CARRITO+`/${productoId}`, {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
            "Accept": "application/json",
            "Authorization": `Bearer ${token}`
          },
          body: JSON.stringify({ cantidad })
        });
        if (!res.ok) {
          console.warn("PUT carrito devolvió", res.status);
        }
        window.dispatchEvent(new Event("carrito-actualizado"));
      } catch (e) {
        console.warn("Error actualizando cantidad en backend:", e);
      } finally {
        this.items = this.items.map(it => it.productoId === productoId ? { ...it, cantidad } : it);
      }
    },

    continuarPago() {
      if (this.$router && typeof this.$router.push === "function") {
        this.$router.push("/checkout");
      } else {
        window.location.href = "/checkout";
      }
    }
  }
};
</script>

<style scoped>
.carrito-root {
  padding: 24px;
  color: #000;
}

.title {
  margin: 0 0 16px 0;
  font-size: 1.6rem;
}

.estado {
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  color: #000;
}

.estado.error {
  color: #c0392b;
}

.contenido {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

/* Lista de items */
.lista {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* Resumen lateral */
.resumen {
  width: 320px;
  min-width: 240px;
}

.resumen-card {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
}

.resumen-card h2 {
  margin-top: 0;
  margin-bottom: 12px;
  font-size: 1.1rem;
}

.line {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  color: #222;
}

.line.total {
  font-weight: 700;
  margin-top: 8px;
  font-size: 1.05rem;
}

.btn-pagar {
  margin-top: 14px;
  width: 100%;
  padding: 10px 12px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  background: #00b3b3;
  color: #fff;
  font-weight: 700;
}

.btn-pagar:hover {
  background: #468e8e;
  color: #fff;
}

.btn-pagar[disabled] {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Mensaje vacío */
.vacío {
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  color: #000;
}

/* Responsive */
@media (max-width: 900px) {
  .contenido {
    flex-direction: column;
  }
  .resumen {
    width: 100%;
  }
}
</style>
