## Ejecutar la aplicación empleando kubernetes
Para ejecutar la aplicación empleando kubernetes, sigue los siguientes pasos:
1. Instala kubectl:
```shell script
  curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
  sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
```
2. Instala minikube:
```shell script
  curl -LO https://github.com/kubernetes/minikube/releases/latest/download/minikube-linux-amd64
  sudo install minikube-linux-amd64 /usr/local/bin/minikube && rm minikube-linux-amd64
```
3. Inicia minikube:
```shell script
  minikube start
```
> **_NOTA:_**  Para esto será necesario docker, aunque hay otras formas de virtualizar minikube como VBox.
4. Creamos las imágenes de los distintos servicios de la aplicación:
```shell script
  eval $(minikube docker-env)

  # Creamos la imagen de docker para cada undo de los servicios y para el frontend (desde la raíz del proyecto):
  docker build -t autenticacion_servicio_quarkus:v1 -f ./autenticacion_servicio/src/main/docker/Dockerfile.native ./autenticacion_servicio
  docker build -t catalogo_servicio_quarkus:v1 -f ./catalogo_servicio/src/main/docker/Dockerfile.native ./catalogo_servicio
  docker build -t carrito_servicio_quarkus:v1 -f ./carrito_servicio/src/main/docker/Dockerfile.native ./carrito_servicio
  docker build -t pedido_servicio_quarkus:v1 -f ./pedido_servicio/src/main/docker/Dockerfile.native ./pedido_servicio
  docker build -t frontend_vue:v1 -f ./frontend/Dockerfile ./frontend
```
5. Activamos el plugin de ingress y metrics-server de minikube:
```shell script
  minikube addons enable ingress
  minikube addons enable metrics-server
```
6. Necesitaremos que resuelva el nombre de dominio `microservicios.local` a la IP de minikube. Para ello, añadimos la siguiente línea al archivo `/etc/hosts`:
```shell script
  echo "$(minikube ip) microservicios.local" | sudo tee -a /etc/hosts
```
7. Desplegamos los distintos servicios de la aplicación:
```shell script
    kubectl apply -f kubernetes/postgres
    kubectl apply -f kubernetes/kafka
    kubectl apply -f kubernetes/microservicios
    kubectl apply -f kubernetes/ingress
    kubectl apply -f kubernetes/redis
    kubectl apply -f kubernetes/metrics-server
    kubectl apply -f kubernetes/frontend
```
En este punto, la aplicación debería estar disponible en `http://microservicios.local`.
8. Para ejecutar el servicio de monitorización realizamos los siguientes pasos:
Clonamos el repositorio de kube-prometheus:
```shell script
  cd kubernetes/prometheus
  git clone https://github.com/prometheus-operator/kube-prometheus.git
```
Creamos el servicio de monitoreo empleando la configuración de los manifiestos:
```shell script
  kubectl apply --server-side -f kubernetes/prometheus/kube-prometheus/manifests/setup
  kubectl wait \
      --for condition=Established \
      --all CustomResourceDefinition \
      --namespace=monitoring
  kubectl apply -f kubernetes/prometheus/kube-prometheus/manifests/
```
Para poder obtener las métricas personalizadas, deberemos aplicar los siguientes manifiestos:
```shell script
  kubectl apply -f kubernetes/prometheus/monitores-prometheus
```
Si queremos acceder a grafana, para ello primero necesitamos saber cuál es 
el nombre del pod de grafana:
```shell script
  kubectl get pods -n monitoring
```
Una vez que tenemos el nombre del pod, podemos acceder a grafana mediante el siguiente comando:
```shell script
    kubectl port-forward -n monitoring <nombre_pod_grafana> 3000
```
Ahora grafana estará disponible en `http://localhost:3000`, y podremos acceder con las credenciales.
Finalmente, si queremos acceder a los dashboards personalizados, deberemos importarlos primero.
`admin:admin`.
## Ejecutar los test de integración mediante Postman
Éxisten dos métodos diferentes para ejecutar los test de integración mediante Postman,
se podrían importar las colecciones y los entornos de Postman a la aplicación y ejecutarlos
manualmente, o bien, se pueden ejecutar de forma automática mediante Newman. A continuación,
se va a explicar cómo ejecutar los test de integración empleando Newman:
1. Instala Newman:
```shell script
  sudo npm install -g newman
```
2. Ejecuta la colección deseada. De forma de que se ejecutan un conjunto de test que prueban una o varias funcionalidades
del sistema. En el caso de la colección `Carrito_Tests`, se recomienda configurarla con un delay alto
ya que el proceso de propagación de los eventos de Kafka puede tardar un tiempo en completarse.
```shell script
  newman run postman_integracion/Carrito_Tests.postman_collection.json -e postman_integracion/KUBERNETES_ENV.postman_environment.jso
```