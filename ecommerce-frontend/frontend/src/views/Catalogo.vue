<template>
  <div class="catalog-page">
    <section class="filters">
      <h2>Filtrar productos</h2>
      <div class="filter-row">
        <label>
          Nombre
          <input v-model="filters.nombre" placeholder="Buscar por nombre" @input="onNombreInput" />
        </label>

        <label>
          Categoría
          <input v-model="filters.categoria" placeholder="Categoría" />
        </label>

        <label>
          Precio min
          <input v-model="filters.precioMin" type="number" min="0" step="0.01" placeholder="0.00" />
        </label>

        <label>
          Precio max
          <input v-model="filters.precioMax" type="number" min="0" step="0.01" placeholder="0.00" />
        </label>
      </div>

      <div class="filter-actions">
        <button @click="applyFilters" :disabled="loading">Aplicar filtros</button>
        <button @click="resetFilters" :disabled="loading">Resetear</button>
      </div>

      <p v-if="validationError" class="validation-error">{{ validationError }}</p>
    </section>

    <transition name="fade-down">
      <div
          v-if="toast.visible"
          class="toast-success"
          role="status"
          aria-live="polite"
      >
        {{ toast.message }}
      </div>
    </transition>

    <section class="cards-container">
      <Producto
          v-for="producto in listaproductos"
          :key="producto.id"
          @agregarAlCarrito="agregarAlCarrito"
          :id="producto.id"
          :nombre="producto.nombre"
          :precio="producto.precio"
          :urlImg="producto.imagenUrl"
      />
    </section>

    <div class="load-more-container">
      <button @click="cargarProductos" :disabled="loading">
        <span v-if="loading">Cargando...</span>
        <span v-else>Cargar más</span>
      </button>
    </div>

    <p v-if="errorMsg" class="error-msg">Error: {{ errorMsg }}</p>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import Producto from "@/components/Producto.vue";

/**
 * Config
 */
const PAGE_SIZE = 16; // <= 100 (backend limita 100)
const BASE_URL = "http://microservicios.local/catalogo";

/**
 * Reactive state
 */
const listaproductos = ref([]);
const page = ref(1);
const loading = ref(false);
const errorMsg = ref("");
const validationError = ref("");

// Filtros
const filters = reactive({
  nombre: "",
  categoria: "",
  precioMin: "",
  precioMax: ""
});

let nombreDebounceTimer = null;
function onNombreInput() {
  debounceApplyFilters();
}

function debounceApplyFilters() {
  clearTimeout(nombreDebounceTimer);
  nombreDebounceTimer = setTimeout(() => {
    applyFilters();
  }, 500);
}

function buildQuery(pageNum = 1) {
  const params = new URLSearchParams();
  params.append("page", String(pageNum));
  params.append("size", String(Math.min(PAGE_SIZE, 100)));

  if (filters.nombre && filters.nombre.trim() !== "") {
    params.append("nombre", filters.nombre.trim());
  }
  if (filters.categoria && filters.categoria.trim() !== "") {
    params.append("categoria", filters.categoria.trim());
  }
  // Solo añadir precioMin/Max si son números válidos
  const min = filters.precioMin === "" ? null : Number(filters.precioMin);
  const max = filters.precioMax === "" ? null : Number(filters.precioMax);

  if (min !== null && !Number.isFinite(min)) {
    validationError.value = "Precio mínimo no es un número válido";
  }
  if (max !== null && !Number.isFinite(max)) {
    validationError.value = "Precio máximo no es un número válido";
  }
  validationError.value = "";

  if (min !== null) params.append("precioMin", String(min));
  if (max !== null) params.append("precioMax", String(max));

  // Validación adicional: precioMin <= precioMax
  if (min !== null && max !== null && min > max) {
    validationError.value = "El precio mínimo no puede ser mayor que el precio máximo.";
  }

  return `${BASE_URL}?${params.toString()}`;
}

/**
 * Fetch de productos
 */
async function fetchProducts(pageNum = 1, append = false) {
  if (validationError.value) return;

  loading.value = true;
  errorMsg.value = "";
  try {
    const url = buildQuery(pageNum);
    const resp = await fetch(url);
    if (!resp.ok) {
      // Mensaje amigable. Si el backend devuelve texto (p. ej. límite size) lo mostramos.
      const text = await resp.text().catch(() => null);
      throw new Error(text || `HTTP ${resp.status}`);
    }
    const data = await resp.json();
    if (append) {
      // en caso de paginación añadimos
      listaproductos.value = [...listaproductos.value, ...data];
    } else {
      // reemplazamos lista (aplicar filtros o carga inicial)
      listaproductos.value = data;
    }
    console.log("Productos obtenidos:", listaproductos.value);
  } catch (err) {
    console.error("Error al obtener los productos:", err);
    errorMsg.value = err.message || "Error al obtener los productos";
  } finally {
    loading.value = false;
  }
}

/**
 * Acciones públicas
 */
function applyFilters() {
  // validar rango de precios antes de enviar
  const min = filters.precioMin === "" ? null : Number(filters.precioMin);
  const max = filters.precioMax === "" ? null : Number(filters.precioMax);
  if (min !== null && max !== null && min > max) {
    validationError.value = "El precio mínimo no puede ser mayor que el precio máximo.";
    return;
  }
  validationError.value = "";
  page.value = 1;
  fetchProducts(page.value, false);
}

function resetFilters() {
  filters.nombre = "";
  filters.categoria = "";
  filters.precioMin = "";
  filters.precioMax = "";
  validationError.value = "";
  page.value = 1;
  fetchProducts(page.value, false);
}

function cargarProductos() {
  page.value++;
  fetchProducts(page.value, true);
}

function agregarAlCarrito(productId) {
  // Evento hacia componente padre o llamada a API de carrito
  console.log("Producto agregado al carrito:", productId);
  showSuccessToast("Producto agregado al carrito correctamente");
}

const toast = reactive({
  visible: false,
  message: "",
  timerId: null
});

function showSuccessToast(msg = "Producto agregado al carrito correctamente") {
  // limpia cualquier temporizador previo
  if (toast.timerId) {
    clearTimeout(toast.timerId);
    toast.timerId = null;
  }
  toast.message = msg;
  toast.visible = true;
  // Oculta automáticamente a los 2.5s
  toast.timerId = setTimeout(() => {
    toast.visible = false;
    toast.timerId = null;
  }, 2500);
}

/**
 * Carga inicial
 */
onMounted(() => {
  fetchProducts(page.value, false);
});
</script>

<style scoped>
.catalog-page {
  padding: 16px;
  align-items: center;
}

/* filtros */
.filters {
  border: 1px solid #e4e4e4;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 16px;
}
.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.filter-row label {
  display: flex;
  flex-direction: column;
  min-width: 160px;
}
.filter-actions {
  margin-top: 10px;
  display: flex;
  gap: 8px;
}
.validation-error {
  color: #b00020;
  margin-top: 8px;
}

.cards-container {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  justify-content: flex-start;
  width: 95%;
  margin: auto;
}


@media (max-width: 600px) {
  .cards-container {
    justify-items: center;
    justify-content: center;
  }
}

/* cargar más */
.load-more-container {
  text-align: center;
  margin: 20px;
}

.load-more-container button {
  padding: 3px 5px;
  font-size: 16px;
  cursor: pointer;
}

.error-msg {
  color: #b00020;
  text-align: center;
  margin-top: 8px;
}

.toast-success {
  position: fixed;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  background: rgba(111, 207, 107, 0.6);
  color: #275511;
  padding: 12px 18px;
  border-radius: 10px;
  box-shadow: 0 6px 20px rgba(0,0,0,0.2);
  font-weight: 600;
  backdrop-filter: blur(2px);
}

/* Transición de entrada/salida hacia abajo */
.fade-down-enter-active,
.fade-down-leave-active {
  transition: opacity 200ms ease, transform 200ms ease;
}
.fade-down-enter-from,
.fade-down-leave-to {
  opacity: 0;
  transform: translate(-50%, -8px);
}
</style>
