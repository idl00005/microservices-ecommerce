<template>
  <section class="checkout-root" aria-labelledby="checkout-title">
    <h1 id="checkout-title" class="title">Dirección de envío</h1>

    <div v-if="error" class="estado error" role="alert">{{ error }}</div>
    <div v-if="success" class="estado success" role="status">{{ success }}</div>

    <div class="checkout-container">
      <form class="checkout-card" @submit.prevent="enviarOrden" novalidate>
        <div class="grid">

          <div class="form-row full">
            <label for="calle">Calle</label>
            <input id="calle" v-model.trim="calle" required type="text" placeholder="Ej. Gran Vía" />
          </div>

          <div class="form-row">
            <label for="numero">Número</label>
            <input id="numero" v-model.trim="numero" required type="text" placeholder="Ej. 12" />
          </div>

          <div class="form-row">
            <label for="piso">Piso <small class="opcional">(opcional)</small></label>
            <input id="piso" v-model.trim="piso" type="text" placeholder="Ej. 3B" />
          </div>

          <div class="form-row">
            <label for="portal">Portal <small class="opcional">(opcional)</small></label>
            <input id="portal" v-model.trim="portal" type="text" placeholder="Ej. A" />
          </div>

          <div class="form-row">
            <label for="cp">Código Postal</label>
            <input
                id="cp"
                v-model.trim="cp"
                required
                type="text"
                inputmode="numeric"
                maxlength="5"
                placeholder="28001"
            />
            <small class="help">5 dígitos (ej. 28001)</small>
          </div>

          <div class="form-row">
            <label for="ciudad">Ciudad</label>
            <input id="ciudad" v-model.trim="ciudad" required type="text" placeholder="Madrid" />
          </div>

          <div class="form-row full">
            <label for="provincia">Provincia</label>
            <input id="provincia" v-model.trim="provincia" required type="text" placeholder="Madrid" />
          </div>

          <div class="form-row full">
            <label for="telefono">Teléfono</label>
            <input
                id="telefono"
                v-model.trim="telefono"
                required
                type="tel"
                inputmode="tel"
                placeholder="+34 600 000 000"
            />
            <small class="help">Incluye prefijo si procede (ej. +34 600 000 000)</small>
          </div>

        </div>

        <div class="actions">
          <button type="button" class="btn-volver" @click="volver" >
            Volver al carrito
          </button>
          <button
              class="btn-enviar"
              type="submit">Confirmar y pagar</button>
        </div>
      </form>
    </div>
  </section>
</template>

<script>
const URL_CARRITO= window.APP_CONFIG.API_CARRITO;

export default {
  name: "CheckoutCompra",
  data() {
    return {
      calle: "",
      numero: "",
      piso: "",
      portal: "",
      cp: "",
      ciudad: "",
      provincia: "",
      telefono: "",
      error: null,
      success: null,
      loading: false
    };
  },
  computed: {
    // Validaciones mínimas del cliente
    cpValido() {
      return /^[0-9]{5}$/.test(this.cp);
    },
    telefonoValido() {
      // Acepta dígitos con espacios, opcional prefijo +, entre 6 y 15 dígitos totales
      const cleaned = this.telefono.replace(/\s+/g, "");
      return /^\+?[0-9]{6,15}$/.test(cleaned);
    },
    camposObligatoriosCompletos() {
      return this.calle && this.numero && this.cp && this.ciudad && this.provincia && this.telefono;
    },
    canSubmit() {
      return this.camposObligatoriosCompletos && this.cpValido && this.telefonoValido;
    }
  },
  methods: {
    async enviarOrden() {
      this.error = null;
      this.success = null;

      if (!this.canSubmit) {
        this.error = "Rellena todos los campos obligatorios y corrige los formatos.";
        return;
      }

      const token = localStorage.getItem("jwt");
      if (!token) {
        alert("Debes iniciar sesión para continuar.");
        this.$router.push("/login");
      }

      // Construir la dirección completa
      const partes = [
        `${this.calle}`,
        `Nº ${this.numero}`,
        this.piso ? `Piso ${this.piso}` : null,
        this.portal ? `Portal ${this.portal}` : null,
        `${this.cp} ${this.ciudad}`,
        this.provincia
      ].filter(Boolean);
      const direccionCompleta = partes.join(", ");

      const payload = {
        direccion: direccionCompleta,
        telefono: this.telefono
      };

      this.loading = true;
      try {
        const res = await fetch(URL_CARRITO+"/ordenes-pago", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Accept": "application/json",
            "Authorization": `Bearer ${token}`
          },
          body: JSON.stringify(payload)
        });

        if (!res.ok) {
          const text = await res.text().catch(() => null);
          throw new Error(text || `Error ${res.status} al enviar la orden`);
        }

        this.success = "Orden enviada correctamente.";

        // Redirigir a confirmación
        setTimeout(() => {
          this.$router.push("/");
        }, 600);
      } catch (err) {
        console.error("Error enviando orden:", err);
        this.error = "No se pudo enviar la orden. Revisa la consola.";
      } finally {
        this.loading = false;
      }
    },
    volver() {
      if (this.$router && typeof this.$router.push === "function") {
        this.$router.push("/carrito");
      } else {
        window.location.href = "/carrito";
      }
    }
  }
};
</script>

<style scoped>
:root {
  --bg: #f6f8fa;
  --card: #ffffff;
  --accent: #00b3b3;
  --accent-hover: #468e8e;
  --muted: #6b7280;
  --danger: #c0392b;
  --success: #0f9d58;
}

.checkout-root {
  padding: 32px 16px;
  background: #f6f8fa;
  min-height: calc(100vh - 80px);
  color: #000;
}

.title {
  max-width: 960px;
  margin: 0 auto 20px auto;
  font-size: 1.6rem;
  font-weight: 700;
  color: #0b1720;
  text-align: center;
}

.checkout-container {
  max-width: 720px;
  margin: 0 auto;
}

.checkout-card {
  background: var(--card);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.06);
  border: 1px solid rgba(15,23,42,0.04);
}

.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.grid .full {
  grid-column: 1 / -1;
}

.form-row {
  display: flex;
  flex-direction: column;
}

.form-row label {
  font-weight: 600;
  margin-bottom: 6px;
  color: #0b1720;
}

.form-row .opcional {
  font-weight: 400;
  color: var(--muted);
  font-size: 0.85rem;
  margin-left: 6px;
}

.form-row input {
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 0.95rem;
  outline: none;
  transition: border-color 0.12s ease, box-shadow 0.12s ease;
  background: #fff;
  color: #0b1720;
  -webkit-appearance: none;
}

.form-row input::placeholder {
  color: #94a3b8;
}

.form-row input:focus {
  border-color: var(--accent);
  box-shadow: 0 4px 12px rgba(0,179,179,0.08);
}

.help {
  margin-top: 6px;
  font-size: 0.85rem;
  color: #6b7280;
}

.estado {
  max-width: 720px;
  margin: 0 auto 16px auto;
  padding: 12px 14px;
  border-radius: 8px;
  font-weight: 600;
}
.estado.error {
  background: #fff5f5;
  color: var(--danger);
  border: 1px solid rgba(192,57,43,0.08);
}
.estado.success {
  background: #f3faf3;
  color: var(--success);
  border: 1px solid rgba(15,157,88,0.08);
}

.actions {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;;
  margin-top: 18px;
}

.btn-enviar {
  background: #00b3b3;
  color: white;
  border: none;
  padding: 4px 10px;
  font-weight: bold;
  border-radius: 5px;
  cursor: pointer;
  transition: background 0.12s ease, transform 0.06s ease;
}

.btn-enviar:hover {
  background: #468e8e;
}

.btn-enviar[disabled] {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-volver {
  background: #ffffff;
  color: #2b2b2b;
  border: none;
  padding: 4px 10px;
  font-weight: bold;
  border-radius: 5px;
  cursor: pointer;
  transition: background 0.12s ease, transform 0.06s ease;
}

.btn-volver:hover {
  background: #d5d5d5;
}

@media (max-width: 720px) {
  .grid {
    grid-template-columns: 1fr;
  }
  .actions {
    justify-content: stretch;
    flex-direction: column-reverse;
  }
  .btn-enviar, .btn-volver {
    width: 100%;
  }
}
</style>
