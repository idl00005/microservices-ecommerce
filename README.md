# 🏗️ Microservices-based Application

This project is built with a **modern microservices architecture** designed for scalability, resilience, and high performance. It consists of **four independent microservices**, each responsible for a core business capability:

- **📦 Orders Service** – Manages customer orders and workflows.  
- **🛒 Cart Service** – Handles shopping cart operations.  
- **🔐 Authentication Service** – Provides secure user login and identity management.  
- **📚 Catalog Service** – Manages products and catalog information.  

All services are developed with **[Quarkus](https://quarkus.io/)**, leveraging the best practices of cloud-native development.

---

## ✨ Key Features

Each microservice integrates a set of powerful solutions:

- **🔑 JWT Authentication** – Ensures secure and stateless authentication across services.  
- **⚡ Redis Caching** – Improves performance by caching frequently accessed data.  
- **🛡️ Fault Tolerance (MicroProfile)** – Implements retries, circuit breakers, and fallback mechanisms for resilience.  
- **❤️ Health Checks** – Provides readiness and liveness probes to monitor system health.  
- **📊 Custom Metrics** – Exposes fine-grained metrics for monitoring and observability.  
- **📨 Asynchronous Communication with Kafka** – Enables decoupled, event-driven interactions between services.  

---

## 📂 Project Structure

The repository is organized into well-defined modules:

- **`frontend/`** – A modern **Vue.js** frontend application that interacts with the backend services.  
- **`kubernetes/`** – Kubernetes manifests and configuration files for seamless deployment in containerized environments.  
- **`test_escalabilidad_jmeter/`** – Contains **JMeter** test plans for scalability and performance testing.  
- **`test_integracion_postman/`** – Includes **Postman** collections for automated integration testing.  

---

## 🚀 Deployment & Scalability

- Designed to run on **Kubernetes**, enabling horizontal scalability and automated orchestration.  
- Health checks and metrics allow smooth integration with observability stacks like **Prometheus + Grafana**.  
- Kafka-based communication ensures event-driven scalability without tight coupling between services.  
