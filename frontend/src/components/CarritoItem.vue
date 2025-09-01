<template>
  <article class="cart-item">
    <div class="img-wrap" role="img" :aria-label="item.nombre">
      <img :src="item.imagenUrl" :alt="item.nombre" class="img" />
    </div>

    <div class="meta">
      <h3 class="nombre">{{ item.nombre }}</h3>
      <p class="precio-unit">Precio: {{ formatoPrecio(item.precio) }}</p>
      <div class="cantidad-row">
        <label class="label-cantidad">Cantidad</label>
        <input
            type="number"
            min="1"
            :max="9999"
            v-model.number="localCantidad"
            @change="emitActualizar"
            class="input-cantidad"
        />
      </div>
    </div>

    <div class="acciones">
      <p class="subtotal">{{ formatoPrecio(subtotal) }}</p>
      <button class="btn-eliminar" @click="$emit('eliminar', item.productoId)">
        Eliminar
      </button>
    </div>
  </article>
</template>

<script>
export default {
  name: "CarritoItem",
  props: {
    item: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      localCantidad: this.item.cantidad ?? 1
    };
  },
  computed: {
    subtotal() {
      // Aseguramos cálculo en coma flotante
      const precio = Number(this.item.precio) || 0;
      const cant = Number(this.localCantidad) || 0;
      return precio * cant;
    }
  },
  methods: {
    formatoPrecio(valor) {
      if (typeof valor !== "number") valor = Number(valor) || 0;
      return valor.toLocaleString("es-ES", { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + " €";
    },
    emitActualizar() {
      // Emitimos evento para que el padre actualice el carrito (opcional)
      this.$emit("actualizar-cantidad", {
        productoId: this.item.productoId,
        cantidad: Number(this.localCantidad)
      });
    }
  },
  watch: {
    // Si el padre cambia la cantidad, actualizamos localmente
    "item.cantidad"(val) {
      this.localCantidad = val ?? 1;
    }
  }
};
</script>

<style scoped>
.cart-item {
  display: flex;
  gap: 16px;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #e8e8e8;
  color: #000;
}

.img-wrap {
  background: #fff;
  padding: 8px;
  border-radius: 6px;
  min-width: 96px;
  max-width: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.img {
  max-width: 100%;
  max-height: 80px;
  object-fit: contain;
  display: block;
  border-radius: 4px;
  background: #fff;
}

.meta {
  flex: 1;
  min-width: 0;
}

.nombre {
  margin: 0 0 6px 0;
  font-size: 1rem;
  line-height: 1.2;
}

.precio-unit {
  margin: 0 0 8px 0;
  font-size: 0.95rem;
  color: #222;
}

.cantidad-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.label-cantidad {
  font-size: 0.9rem;
}

.input-cantidad {
  width: 70px;
  padding: 6px 8px;
  border-radius: 6px;
  border: 1px solid #ccc;
  font-size: 0.95rem;
  box-sizing: border-box;
}

.acciones {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  min-width: 120px;
}

.subtotal {
  font-weight: 700;
  font-size: 1rem;
}

.btn-eliminar {
  background: transparent;
  border: none;
  color: #c0392b;
  cursor: pointer;
  padding: 6px 8px;
  border-radius: 6px;
}

.btn-eliminar:hover {
  background: rgba(192,57,43,0.06);
}
</style>