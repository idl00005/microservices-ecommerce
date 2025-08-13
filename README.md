# Microservicios con Quarkus
## Ejecutar la aplicación en local

Para ejecutar la aplicación en local, sigue los siguientes pasos:
1. Actualiza la lista de paquetes:
```shell script
  sudo apt update
```
2. Instala PostgreSQL y activa el servicio:
```shell script
  apt-get install -y postgresql postgresql-contrib
  systemctl enable postgresql
  systemctl start postgresql
```
3. Crea el usuario y las bases de datos necesarias:
```shell script
  sudo -u postgres psql -c "CREATE USER ignacio_ad WITH PASSWORD '1234';"
  sudo -u postgres psql -c "CREATE DATABASE "autenticacionBD" OWNER ignacio_ad;"
  sudo -u postgres psql -c "CREATE DATABASE "catalogoBD" OWNER ignacio_ad;"
  sudo -u postgres psql -c "CREATE DATABASE "carritoBD" OWNER ignacio_ad;"
  sudo -u postgres psql -c "CREATE DATABASE "catalogoBD" OWNER ignacio_ad;"
```
4. Instala java 17:
```shell script
  sudo apt install openjdk-17-jdk
```
5. Instala Maven:
```shell script
  sudo apt install maven
```
6. Iniciamos el servicio de kafka. Para este paso suponemos que docker y docker-compose ya están instalados,
en caso contrario será necesaria su instalación.
```shell script
  cd docker-compose_solo_kafka
  docker-compose up -d
```
> **_NOTA:_**  En ocasiones el contenedor de kafka se cierra al arrancar por primera vez debido a
que zookeeper no ha completado su arranque. En ese caso, volvemos a ejecutar "docker-compose up -d".
6. Arrancamos los distintos servicios de la aplicación:
```shell script
  # Para cada servicio:
  cd ejemplo_servicio
  ./mvnw quarkus:dev
```
## Ejecutar la aplicación empleando Docker-Compose
En este apartado se da por hecho que ya se dispone de Docker y Docker-Compose instalados en el sistema. 
Para ejecutar la aplicación empleando Docker-Compose, sigue los siguientes pasos:
1. Empaquetamos los distintos servicios:
```shell script
  # Para cada servicio:
  cd ejemplo_servicio
  ./mvnw clean package -DskipTests
```
2. En caso de que se disponga de postgresql instalado en el sistema, es necesario detener el servicio de postgresql:
```shell script
  sudo systemctl stop postgresql
```
3. Ejecutamos el comando de docker-compose para levantar los distintos servicios:
```shell script
  cd docker-compose
  docker-compose up -d
```
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
  # Para cada servicio:
  cd ejemplo_servicio
  ./mvnw clean package -DskipTests
  cd ..
  # Creamos la imagen de docker para cada undo de los servicios (desde la raíz del proyecto):
  docker build -t autenticacion_servicio_quarkus:v1 -f ./autenticacion_servicio/src/main/docker/Dockerfile.jvm ./autenticacion_servicio
  docker build -t catalogo_servicio_quarkus:v1 -f ./catalogo_servicio/src/main/docker/Dockerfile.jvm ./catalogo_servicio
  docker build -t carrito_servicio_quarkus:v1 -f ./carrito_servicio/src/main/docker/Dockerfile.jvm ./carrito_servicio
  docker build -t pedido_servicio_quarkus:v1 -f ./pedido_servicio/src/main/docker/Dockerfile.jvm ./pedido_servicio
```
5. Activamos el plugin de ingress de minikube:
```shell script
  minikube addons enable ingress
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
```
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
A continuación querremos acceder a grafana, para ello primero necesitamos saber cuál es 
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