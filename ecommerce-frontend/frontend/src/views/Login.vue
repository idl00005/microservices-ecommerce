<template>
  <div class="login-container">
    <div class="login-card">
      <h2>Iniciar Sesión</h2>

      <form @submit.prevent="iniciarSesion">
        <div class="form-group">
          <label for="email">Correo electrónico</label>
          <input
              type="email"
              id="email"
              v-model="email"
              required
              placeholder="tuemail@ejemplo.com"
          />
        </div>

        <div class="form-group">
          <label for="password">Contraseña</label>
          <input
              type="password"
              id="password"
              v-model="password"
              required
              placeholder="********"
          />
        </div>

        <button type="submit">Acceder</button>
        <p class="error-message" :class="{ visible: error }">
          {{ error || " " }}
        </p>
      </form>

      <p class="register-text">
        ¿No tienes cuenta?
        <a href="#">Regístrate</a>
      </p>
    </div>
  </div>
</template>

<script>
export default {
  name: "Login",
  data() {
    return {
      email: "",
      password: "",
      loading: false,
      error: null
    };
  },
  methods: {
    // heurística simple para detectar JWT (tres partes separadas por '.')
    isJwt(value) {
      return typeof value === 'string' && value.split('.').length === 3;
    },

    // decodifica el payload (base64url) -> objeto JS, o null si falla
    decodeJwtPayload(token) {
      try {
        const payload = token.split('.')[1];
        const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
        const padded = base64 + '='.repeat((4 - base64.length % 4) % 4);
        const json = atob(padded);
        return JSON.parse(json);
      } catch (e) {
        return null;
      }
    },

    async iniciarSesion() {
      this.error = null;
      this.loading = true;

      try {
        const bodyPayload = {
          username: this.email,
          password: this.password
        };

        const res = await fetch("http://microservicios.local/autenticacion/login", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Accept": "application/json"
          },
          body: JSON.stringify(bodyPayload)
        });

        // Si el código de estado no es 200-299, mostrar mensaje de error
        if (!res.ok) {
          this.error = "No ha sido posible iniciar sesión, inténtelo de nuevo.";
          console.error("Error iniciando sesion");
          return;
        }

        // Leemos la respuesta como texto
        const raw = await res.text();
        let token = null;

        if (this.isJwt(raw)) {
          token = raw.trim();
        } else {
          this.error = "Respuesta inesperada del servidor. No se recibió un token válido.";
          return;
        }

        localStorage.setItem("jwt", token);
        window.dispatchEvent(new Event("login"));

        // decodificar payload
        const payloadDecoded = this.decodeJwtPayload(token);
        if (payloadDecoded) {
          const userInfo = {
            sub: payloadDecoded.sub || null,
            roles: payloadDecoded.roles || payloadDecoded.role || null,
            exp: payloadDecoded.exp || null
          };
          localStorage.setItem("user_info", JSON.stringify(userInfo));
        }

        console.log("JWT obtenido:", token);
        console.log("Payload decodificado:", payloadDecoded);

        // redirigir a home (si usas router)
        if (this.$router && typeof this.$router.push === 'function') {
          this.$router.push('/');
        } else {
          window.location.href = '/';
        }

      } catch (err) {
        this.error = "No se pudo contactar con el servidor de autenticación.";
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>



<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh; /* ocupa toda la ventana */
  background-color: #f5f5f5;
}

.login-card {
  background: white;
  padding: 2rem;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  width: 100%;
  max-width: 400px;
  color: #282828;
}

h2 {
  margin-bottom: 1.5rem;
  text-align: center;
}

.form-group {
  margin-bottom: 1rem;
  display: flex;
  flex-direction: column;
}

label {
  font-size: 14px;
  margin-bottom: 5px;
}

input {
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 5px;
}

button {
  width: 100%;
  padding: 10px;
  background-color: #00d7c5;
  border: none;
  color: #373737;
  font-size: 16px;
  border-radius: 5px;
  cursor: pointer;
}

button:hover {
  background-color: #00b3b3;
}

.register-text {
  text-align: center;
  font-size: 14px;
  margin: 0;
}

.register-text a {
  color: #0078d7;
  text-decoration: none;
}

.register-text a:hover {
  text-decoration: underline;
}

.error-message {
  color: red;
  margin-top: 5px;
  font-size: 14px;
  text-align: center;
  min-height: 18px;
  visibility: hidden;
  margin-bottom: 0;
}

.error-message.visible {
  visibility: visible;
}


</style>
