# Dockerfile (en api-gateway/nginx/)
FROM nginx:1.21-alpine

# Copiar la configuración personalizada de NGINX
COPY nginx.conf /etc/nginx/nginx.conf

# (Opcional) Copiar clave pública o JWK para validar JWT
# COPY jwt_public.pem /etc/nginx/jwt_public.pem

# Exponer el puerto 80
EXPOSE 80

# ENTRYPOINT y CMD ya vienen definidos en la imagen base de nginx
